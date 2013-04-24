package org.dialang.servlets

import java.io.IOException
import javax.servlet.ServletException
import javax.servlet.http.{HttpServletRequest,HttpServletResponse}
import java.util.concurrent.ConcurrentHashMap

import org.dialang.db.DB
import org.dialang.model.DialangSession
import org.dialang.scoring.ScoringMethods
import org.dialang.common.model.Item

import scala.collection.JavaConversions._
import scala.collection.mutable.ConcurrentMap
import scala.collection.mutable.HashMap
import scala.collection.mutable.ListBuffer

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SubmitBasket extends DialangServlet {

  private val logger = LoggerFactory.getLogger(classOf[SubmitBasket])
  
  private val db = DB
  private val scoringMethods = new ScoringMethods
  private val scoredItemMap = new ConcurrentHashMap[String,ListBuffer[Item]]

  @throws[ServletException]
  @throws[IOException]
  override def doPost(req: HttpServletRequest, resp: HttpServletResponse) {

    logger.debug("doPost")

    val dialangSession = getDialangSession(req)

    if(dialangSession.tl == "" || dialangSession.skill == "") {
      logger.error("Neither the test language or skill were set in the cookie.")
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST,"tl and skill must be set.")
      return
    }

    val currentBasketId = req.getParameter("basketId")

    if(currentBasketId == null || currentBasketId.length < 1) {
      logger.error("The basket id isn't set in the cookie.")
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST,"basketId must be set.")
      return
    }

    if(logger.isDebugEnabled) logger.debug("currentBasketId = " + currentBasketId)

    val itemList:ListBuffer[Item] = scoredItemMap.getOrElse(dialangSession.sessionId,new ListBuffer[Item])

    req.getParameter("type") match {
      case "mcq" => {
        val itemId = req.getParameter("itemId").toInt
        val answerId = req.getParameter("response").toInt
        val itemOption = scoringMethods.getScoredIdResponseItem(itemId,answerId)
        if(itemOption.isDefined) {
          val item = itemOption.get
          if(logger.isDebugEnabled) logger.debug(item.toString)
          itemList += item
        } else {
        }
        dataCapture.logSingleIdResponse(dialangSession.sessionId,currentBasketId.toInt,itemId,answerId)
      }
      case "tabbedpane" => {
        val responses = getMultipleIdResponses(req)
        responses.foreach(t => {
          val itemOption = scoringMethods.getScoredIdResponseItem(t._1,t._2)
          if(itemOption.isDefined) {
            val item = itemOption.get
            if(logger.isDebugEnabled) logger.debug(item.toString)
            itemList += item
          } else {
          }
        })
        dataCapture.logMultipleIdResponses(dialangSession.sessionId,currentBasketId.toInt,responses.toMap)
      }
      case "shortanswer" => {
        val responses = getMultipleTextualResponses(req)
        responses.foreach(t => {
          val itemOption = scoringMethods.getScoredTextResponseItem(t._1,t._2)
          if(itemOption.isDefined) {
            val item = itemOption.get
            if(logger.isDebugEnabled) logger.debug(item.toString)
            itemList += item
          } else {
          }
        })
        dataCapture.logMultipleTextualResponses(dialangSession.sessionId,currentBasketId.toInt,responses.toMap)
      }
      case "gaptext" => {
        val responses = getMultipleTextualResponses(req)
        responses.foreach(t => {
          val itemOption = scoringMethods.getScoredTextResponseItem(t._1,t._2)
          if(itemOption.isDefined) {
            val item = itemOption.get
            if(logger.isDebugEnabled) logger.debug(item.toString)
            itemList += item
          } else {
          }
        })
        dataCapture.logMultipleTextualResponses(dialangSession.sessionId,currentBasketId.toInt,responses.toMap)
      }
      case "gapdrop" => {
        val responses = getMultipleIdResponses(req)
        responses.foreach(t => {
          val itemOption = scoringMethods.getScoredIdResponseItem(t._1,t._2)
          if(itemOption.isDefined) {
            val item = itemOption.get
            if(logger.isDebugEnabled) logger.debug(item.toString)
            itemList += item
          } else {
          }
        })
        dataCapture.logMultipleIdResponses(dialangSession.sessionId,currentBasketId.toInt,responses.toMap)
      }
      case _ =>
    }

    scoredItemMap += ((dialangSession.sessionId,itemList))

    val nextBasketNumber = dialangSession.currentBasketNumber + 1

    val basketIds = db.getBasketIdsForBooklet(dialangSession.bookletId)

    if(nextBasketNumber >= basketIds.length) {
      println("ITEMS:")
      scoredItemMap(dialangSession.sessionId).foreach(item => {
        println(item)
      })

      val (itemGrade,itemLevel) = scoringMethods.getItemGrade(dialangSession,scoredItemMap(dialangSession.sessionId).toList)

      println("ITEM GRADE:" + itemGrade)

      val cookie = getUpdatedCookie(req,Map("itemLevel" -> itemLevel))

      resp.setStatus(HttpServletResponse.SC_OK)
      resp.addCookie(cookie)
      resp.setContentType("text/html")
      resp.sendRedirect(staticContentRoot + "endoftest/" + dialangSession.al + ".html")
      return
    }

    val nextBasketId = basketIds(nextBasketNumber)

    val cookie = getUpdatedCookie(req,Map("currentBasketNumber" -> nextBasketNumber.toString))

    resp.setStatus(HttpServletResponse.SC_OK)
    resp.addCookie(cookie)
    resp.setContentType("text/html")
    resp.sendRedirect(staticContentRoot + "baskets/" + dialangSession.al + "/" + nextBasketId + ".html")
  }

  private def getMultipleIdResponses(req: HttpServletRequest): Map[Int,Int] = {

    val responses = new HashMap[Int,Int]
    val params = req.getParameterMap

    req.getParameterNames.foreach(n => {
      val name = n.asInstanceOf[String]
      if(name.endsWith("-response")) {
        val itemId = name.split("-")(0).toInt
        val answerId = params.get(name).asInstanceOf[Array[String]](0).toInt
        responses += ((itemId,answerId))
      }
    })
    responses.toMap
  }

  private def getMultipleTextualResponses(req: HttpServletRequest): Map[Int,String] = {

    val responses = new HashMap[Int,String]
    val params = req.getParameterMap

    req.getParameterNames.foreach(n => {
      val name = n.asInstanceOf[String]
      if(name.endsWith("-response")) {
        responses += ((name.split("-")(0).toInt,params.get(name).asInstanceOf[Array[String]](0)))
      }
    })
    responses.toMap
  }
}
