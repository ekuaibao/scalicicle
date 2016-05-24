package com.ekuaibao.scalicicle

import java.io.{ByteArrayOutputStream, IOException}
import java.security.MessageDigest

import scala.collection.immutable.NumericRange
import scala.concurrent._
import scala.util.control.NoStackTrace

/**
  * Generates IDs using Redis that have strong guarantees of k-ordering, and include a timestamp that can be considered
  * issued by a time oracle so long as time is kept in check on the Redis instances used.
  *
  * This allows events to be generated in a distributed fashion, stored in a immutable data-store, and a fetch to
  * reconstruct time ordering at any point in the future with strong guarantees that the order is the intended one.
  *
  * We are generating an ID that will be comprised of the following:
  *
  * > 41 bit time + 10 bit logical shard id + 12 bit sequence id
  *
  * Note this adds to 63 bit, because the MSB is reserved in some languages and we value interoperability.
  */
class IdGenerator(redis: Redis, luaScript: String, luaScriptSha: String) {

  import IdGenerator._

  /**
    * Generate an ID.
    *
    * @return A ID.
    */
  def generateId()(implicit executor: ExecutionContext): Future[Long] = {
    executeOrLoadLuaScript(1)
  }

  /**
    * Generate a batch of IDs.
    *
    * @param batchSize The number IDs to return.
    * @return A list of IDs. The number of IDs may be less than or equal to the batch size depending on if the sequence needs to roll in Redis.
    */
  def generateIdBatch(batchSize: Int)(implicit executor: ExecutionContext): Future[NumericRange[Long]] = {
    validateBatchSize(batchSize)
    executeOrLoadLuaScript(batchSize) map { id =>
      NumericRange(id, id + batchSize, 1L)
    }
  }

  /**
    * Try executing the Lua script using the SHA of its contents.
    *
    * If the Lua script hasn't been loaded before, we'll load it first and then try executing it again. This should
    * only need to be done once per version of the given Lua script. This guards against a Redis server being added
    * into the pool to help increase capacity, as the script will just be loaded again if missing.
    *
    * This also gives a performance gain:
    *
    * * If the Lua script is already loaded, it's already parsed, tokenised and in memory. This is MUCH faster
    * than loading it again every time using eval instead of evalsha.
    * * If the script with this SHA was already loaded by another process, we can use it instead of loading it
    * again, giving us a small performance gain.
    *
    * @param batchSize The number to increment the sequence by in Redis.
    * @return The result of executing the Lua script.
    */
  private def executeOrLoadLuaScript(batchSize: Int)(implicit executor: ExecutionContext): Future[Long] = {
    val size = batchSize.toString
    // Great! The script was already loaded and ran, so we saved a call.
    redis.evalSha(luaScriptSha, size).recoverWith {
      case RedisScriptNotFoundException =>
        // Otherwise we need to load and try again, failing if it doesn't work the second time.
        redis.scriptLoad(luaScript).flatMap { sha =>
          redis.evalSha(luaScriptSha, size)
        }
    }
  }

  /**
    * Check that the given batch size is within the bounds that we allow. This is important to
    * check, as otherwise someone may set the batch size to a negative causing the sequencing
    * in Redis to fail.
    *
    * @param batchSize The batch size as specified by the user.
    */
  private def validateBatchSize(batchSize: Int) = {
    if (batchSize <= 0 || batchSize > MAX_BATCH_SIZE) {
      throw new InvalidBatchSizeException(s"The batch size is less than 1 or is greater than the supported maximum of $MAX_BATCH_SIZE")
    }
  }
}

object IdGenerator {
  val LUA_SCRIPT_RESOURCE_PATH = "/id-generation.lua"

  val LOGICAL_SHARD_ID_BITS = 10
  val SEQUENCE_BITS = 12

  val MAX_LOGICAL_SHARD_ID = (1 << LOGICAL_SHARD_ID_BITS) - 1
  val MIN_LOGICAL_SHARD_ID = 0L

  val MAX_BATCH_SIZE = (1 << SEQUENCE_BITS) - 1

  /**
    * Create an ID generator that will operate using the given Redis client.
    *
    * Note that this constructor means that if a failure occurs, we will attempt to retry generating the ID up to the
    * number of `maximumAttempts` specified. Specify 1 to try only once.
    *
    * @param redis The abstract RedisClient interface to use for ID generation.
    */
  def apply(redis: Redis): IdGenerator = {
    val script = try {
      val out = new ByteArrayOutputStream(1024)
      val is = this.getClass.getResourceAsStream(LUA_SCRIPT_RESOURCE_PATH)
      try {
        val buf = Array.ofDim[Byte](512)
        var len = is.read(buf)
        while (len != -1) {
          out.write(buf, 0, len)
          len = is.read(buf)
        }
        out.toByteArray
      } finally {
        out.close()
        is.close()
      }
    } catch {
      case ex: IOException =>
        throw new LuaScriptFailedToLoadException("Could not load Icicle Lua script from the resources in the JAR.", ex);
    }

    val digest = MessageDigest.getInstance("SHA1")
    val sha = HexUtil.bytes2hex(digest.digest(script))
    new IdGenerator(redis, new String(script, "UTF-8").intern(), sha)
  }
}

trait Redis {
  def scriptLoad(lua: String): Future[String]

  def evalSha(sha: String, args: String*): Future[Long]
}

object RedisScriptNotFoundException extends RuntimeException with NoStackTrace