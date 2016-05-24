package com.ekuaibao.scalicicle

object HexUtil {
  def hex2bytes(s: String): Array[Byte] = {
    s.sliding(2, 2).toArray.map(Integer.parseInt(_, 16).toByte)
  }

  def bytes2hex(buf: Array[Byte], sep: Option[String] = None): String = {
    sep match {
      case None => buf.map("%02x".format(_)).mkString
      case _ => buf.map("%02x".format(_)).mkString(sep.get)
    }
  }

  def bytes2readable(buf: Array[Byte]): String = {
    val sb = StringBuilder.newBuilder
    val len = buf.length
    var i = 0
    var bits = 0
    var n = 0
    while (i < len) {
      val b = buf(i)
      n = (n << 8) | (b & 0xff)
      bits += 8
      do {
        sb.append(charTable((n >>> (bits - 6)) & 63))
        bits -= 6
      } while (bits >= 6)
      n = n & (1 << bits)
      i += 1
    }
    sb.toString()
  }

  val charTable = {
    (('0' to '9') ++ ('a' to 'z') ++ ('A' to 'Z') :+ '=' :+ '+').toArray
  }
}
