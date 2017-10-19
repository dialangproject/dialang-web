package org.dialang.dr.util

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

trait DialangLogger {

 private val logger = LoggerFactory.getLogger(getClass)

 def debug(message:String) {
  logger.debug(message)
 }

 def error(message:String) {
  logger.error(message)
 }

 def error(message:String,t:Throwable) {
  logger.error(message,t)
 }
}
