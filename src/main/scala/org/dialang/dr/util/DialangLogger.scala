package org.dialang.dr.util

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

trait DialangLogger {

  private val logger = LoggerFactory.getLogger(getClass)

  def debug(message: String): Unit = logger.debug(message)

  def error(message:String): Unit = logger.error(message)

  def error(message: String, t: Throwable): Unit = logger.error(message,t)
}
