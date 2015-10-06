package org.dialang.web.model

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * We're representing records (mappings of booket id onto personal estimation pe), by Tuple2 instances.
 */
class PreestAssign(assign: Map[String, Vector[(Float, Int)]]) {

  private val logger = LoggerFactory.getLogger(getClass)

  def getEasyBookletId(tl: String, skill: String): Int = {

    // Get the first element. They've been sorted already.
    val bookletId = assign.get(tl + "#" + skill).get.head._2

    if(logger.isDebugEnabled) logger.debug("Booklet ID: " + bookletId)

    bookletId
  }

  def getMediumBookletId(tl: String, skill: String): Int = {

    val list = assign.get(tl + "#" + skill).get

    // Get the middle element.
    val bookletId = list( ((list.length + 1) / 2) - 1 )._2
    if(logger.isDebugEnabled) logger.debug("Booklet ID: " + bookletId)
    bookletId
  }

  def getHardBookletId(tl: String, skill: String): Int = {

    // Get the last element. They've been sorted already.
    val bookletId = assign.get(tl + "#" + skill).get.last._2

    if(logger.isDebugEnabled) logger.debug("Booklet ID: " + bookletId)

    bookletId
  }

  def getBookletId(tl: String, skill: String, pe: Float): Int = {

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
