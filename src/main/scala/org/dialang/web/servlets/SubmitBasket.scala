package org.dialang.web.servlets

import java.io.IOException
import javax.servlet.ServletException
import javax.servlet.http.{HttpServletRequest,HttpServletResponse}

import org.dialang.web.db.DB
import org.dialang.web.model.{DialangSession,Basket}
import org.dialang.web.scoring.ScoringMethods
import org.dialang.common.model._

import scala.collection.JavaConversions._
import scala.collection.mutable.HashMap
import scala.collection.mutable.ListBuffer

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SubmitBasket extends DialangServlet {

  private val logger = LoggerFactory.getLogger(classOf[SubmitBasket])
  
  private val db = DB
  private val scoringMethods = new ScoringMethods

  @throws[ServletException]
  @throws[IOException]
  override def doPost(req: HttpServletRequest, resp: HttpServletResponse) {

    val dialangSession = getDialangSession(req)

    if(dialangSession.testLanguage == "" || dialangSession.skill == "") {
      logger.error("Neither the test language or skill were set in the cookie.")
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST,"tl and skill must be set.")
      return
    }

    val currentBasketId:Int = req.getParameter("basketId") match {
        case s:String => s.toInt
        case _ => {
          logger.error("The basket id isn't set in the cookie.")
          resp.sendError(HttpServletResponse.SC_BAD_REQUEST,"basketId must be set.")
          return
        }
      }

    if(logger.isDebugEnabled) logger.debug("currentBasketId = " + currentBasketId)

    val basketList:ListBuffer[Basket] = (new ListBuffer[Basket]) ++ dialangSession.scoredBasketList
    val itemList:ListBuffer[Item] = (new ListBuffer[Item]) ++ dialangSession.scoredItemList

    req.getParameter("type") match {

      case "mcq" => {
        val itemId = req.getParameter("itemId").toInt
        val positionInBasket = req.getParameter(itemId + "-position").toInt
        val answerId = req.getParameter("response").toInt
        val itemOption = scoringMethods.getScoredIdResponseItem(itemId,answerId)
        if(itemOption.isDefined) {
          val item:Item = itemOption.get
          item.basketId = currentBasketId
          item.positionInBasket = req.getParameter(itemId + "-position").toInt
          item.positionInTest = itemList.length + 1
          item.responseId = answerId
          item.answers = db.getAnswers(itemId) match {
              case Some(l:List[Answer]) => l
              case None => {
                logger.error("No answers returned from db for item " + itemId)
                List[Answer]()
              }
            }
          if(logger.isDebugEnabled) logger.debug(item.toString)
          itemList += item
          basketList += new Basket(currentBasketId,"mcq",List(item))
        } else {
          logger.error("No item returned from scoring")
        }
        dataCapture.logSingleIdResponse(dialangSession.sessionId,currentBasketId,itemId,answerId)
      }

      case "tabbedpane" => {
        val responses = getMultipleIdResponses(req)
        val basketItems = new ListBuffer[Item]()
        responses.foreach(t => {
          val itemOption = scoringMethods.getScoredIdResponseItem(t._1,t._2)
          if(itemOption.isDefined) {
            val item:Item = itemOption.get
            item.positionInBasket = req.getParameter(item.id + "-position").toInt
            item.basketId = currentBasketId
            item.positionInTest = itemList.length + 1
            logger.debug(item.basketId.toString)
            item.responseId = t._2
            item.answers = db.getAnswers(item.id) match {
                case Some(l:List[Answer]) => l
                case None => {
                  logger.error("No answers returned from db for item " + item.id)
                  List[Answer]()
                }
              }
            if(logger.isDebugEnabled) logger.debug(item.toString)
            itemList += item
            basketItems += item
          } else {
            logger.error("No item returned from scoring")
          }
        })
        basketList += new Basket(currentBasketId,"tabbedpane",basketItems.toList)
        dataCapture.logMultipleIdResponses(dialangSession.sessionId,currentBasketId,responses.toMap)
      }

      case "shortanswer" => {
        val responses = getMultipleTextualResponses(req)
        val basketItems = new ListBuffer[Item]()
        responses.foreach(t => {
          val itemOption = scoringMethods.getScoredTextResponseItem(t._1,t._2)
          if(itemOption.isDefined) {
            val item = itemOption.get
            item.basketId = currentBasketId
            item.positionInBasket = req.getParameter(item.id + "-position").toInt
            item.positionInTest = itemList.length + 1
            item.responseText = t._2
            if(logger.isDebugEnabled) logger.debug(item.toString)
            itemList += item
            basketItems += item
          } else {
            logger.error("No item returned from scoring")
          }
        })
        basketList += new Basket(currentBasketId,"shortanswer",basketItems.toList)
        dataCapture.logMultipleTextualResponses(dialangSession.sessionId,currentBasketId,responses.toMap)
      }

      case "gaptext" => {
        val responses = getMultipleTextualResponses(req)
        val basketItems = new ListBuffer[Item]()
        responses.foreach(t => {
          val itemOption = scoringMethods.getScoredTextResponseItem(t._1,t._2)
          if(itemOption.isDefined) {
            val item:Item = itemOption.get
            item.basketId = currentBasketId
            item.positionInBasket = req.getParameter(item.id + "-position").toInt
            item.responseText = t._2
            item.positionInTest = itemList.length + 1
            itemList += item
            basketItems += item
          } else {
            logger.error("No item returned from scoring")
          }
        })
        basketList += new Basket(currentBasketId,"gaptext",basketItems.toList)
        dataCapture.logMultipleTextualResponses(dialangSession.sessionId,currentBasketId,responses.toMap)
      }

      case "gapdrop" => {
        val responses = getMultipleIdResponses(req)
        val basketItems = new ListBuffer[Item]()
        responses.foreach(t => {
          val itemOption = scoringMethods.getScoredIdResponseItem(t._1,t._2)
          if(itemOption.isDefined) {
            val item:Item = itemOption.get
            item.basketId = currentBasketId
            item.positionInBasket = req.getParameter(item.id + "-position").toInt
            item.positionInTest = itemList.length + 1
            item.responseId = t._2
            itemList += item
            basketItems += item
          } else {
            logger.error("No item returned from scoring")
          }
        })
        basketList += new Basket(currentBasketId,"gapdrop",basketItems.toList)
        dataCapture.logMultipleIdResponses(dialangSession.sessionId,currentBasketId,responses.toMap)
      }
      case _ =>
    }

    dialangSession.scoredItemList = itemList.toList

    dialangSession.scoredBasketList = basketList.toList

    val nextBasketNumber = dialangSession.currentBasketNumber + 1

    val basketIds = db.getBasketIdsForBooklet(dialangSession.bookletId)

    if(nextBasketNumber >= basketIds.length) {

      val (itemGrade:Int,itemLevel:String) = scoringMethods.getItemGrade(dialangSession,itemList.toList)

      if(logger.isDebugEnabled) logger.debug("ITEM GRADE:" + itemGrade)

      dialangSession.itemGrade = itemGrade
      dialangSession.itemLevel = itemLevel

      saveDialangSession(dialangSession,req)

      dataCapture.logTestResult(dialangSession)

      // We set itemsDone to true so the client js knows to enable the item review button
      // We set testDone to true so the client js knows to enable the sa feedback and advice buttons
      val cookie = getUpdatedCookie(req,Map("itemLevel" -> itemLevel,"testDone" -> "true","itemsDone" -> "true"))

      resp.setStatus(HttpServletResponse.SC_OK)
      resp.addCookie(cookie)
      resp.setContentType("text/html")
      resp.sendRedirect("/endoftest/" + dialangSession.adminLanguage + ".html")
      return
    }

    val nextBasketId = basketIds(nextBasketNumber)

    dialangSession.currentBasketNumber = nextBasketNumber
    saveDialangSession(dialangSession,req)
     
    // We set itemsDone to true so the client js knows to enable the item review button
    val map = Map("currentBasketNumber" -> nextBasketNumber.toString,"itemsDone" -> "true")
    val cookie = getUpdatedCookie(req,map)

    resp.setStatus(HttpServletResponse.SC_OK)
    resp.addCookie(cookie)
    resp.setContentType("text/html")
    resp.sendRedirect("/baskets/" + dialangSession.adminLanguage + "/" + nextBasketId + ".html")
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
