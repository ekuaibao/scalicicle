package com.ekuaibao.scalicicle

/**
  * Exception thrown if the requested ID batch size is not within the minimum and maximum bounds.
  */
class InvalidBatchSizeException(message: String) extends RuntimeException(message)


/**
  * Exception thrown if the logical shard ID assigned to a Redis node is not within the minimum and maximum bounds, or is
  * not set.
  */
class InvalidLogicalShardIdException(message: String) extends RuntimeException(message)


/**
  * Exception thrown if the Lua script embedded in the JAR resources fails to load. This is an unrecoverable exception
  * as the ID generation cannot function without the Lua script.
  */
class LuaScriptFailedToLoadException(message: String, cause: Throwable) extends RuntimeException(message, cause)