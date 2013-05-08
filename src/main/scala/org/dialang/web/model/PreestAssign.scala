package org.dialang.web.model

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * We're representing records (mappings of booket id onto personal estimation pe), by Tuple2 instances.
 */
class PreestAssign(assign: Map[String,Vector[(Float,Int)]]) {

  private val logger = LoggerFactory.getLogger(getClass)

  def getBookletId(tl: String,skill: String) = {
    val list = assign.get(tl + "#" + skill).get

    // obtain the  element at (set.size () +1) / 2.
    list( (list.length + 1) / 2 )._2
  }

  def getBookletId(tl: String,skill: String,pe: Float) = {

    if(logger.isDebugEnabled) logger.debug("PE: " + pe)

    // Make our compound key and get our list of records
    val list = assign.get(tl + "#" + skill).get

    try {
      list.filter(t => pe <= t._1)(0)._2
    } catch {
      case e:NoSuchElementException => {
        if(logger.isInfoEnabled) logger.info("PreestAssign/get: max return forced for " + pe);
        list.last._2
      }
    }
  }
}
