package org.dialang.web.servlets

import org.dialang.web.db.DB
import org.dialang.web.model.{DialangSession,Basket}
import org.dialang.web.scoring.ScoringMethods
import org.dialang.common.model._

import scala.collection.JavaConversions._
import scala.collection.mutable.HashMap
import scala.collection.mutable.ListBuffer

import org.slf4j.LoggerFactory;

class SubmitBasket extends DialangServlet {

  private val logger = LoggerFactory.getLogger(classOf[SubmitBasket])
  
  private val db = DB
  private val scoringMethods = new ScoringMethods

  post("/") {

    val dialangSession = getDialangSession

    if(dialangSession.testLanguage == "" || dialangSession.skill == "") {
      logger.error("Neither the test language or skill were set in the session. Returning 500 ...")
      halt(500)
    }

    val currentBasketId:Int = params.get("basketId") match {
        case Some(s:String) => s.toInt
        case None => {
          logger.error("No basket id supplied. Returning 400 (Bad Request) ...")
          halt(400)
        }
      } 

    if(logger.isDebugEnabled) {
      logger.debug("currentBasketId = " + currentBasketId)
      logger.debug("Current item list length: " + dialangSession.scoredItemList.length)
    }

    val basketList:ListBuffer[Basket] = (new ListBuffer[Basket]) ++ dialangSession.scoredBasketList
    val itemList:ListBuffer[Item] = (new ListBuffer[Item]) ++ dialangSession.scoredItemList

    params.get("basketType") match {

      case Some("mcq") => {
        val itemId = params.get("itemId") match {
            case Some(s:String) => s.toInt
            case None => {
              logger.error("No item id supplied. Returning 400 (Bad Request) ...")
              halt(400)
            }
          }
        val answerId = params.get("response") match {
            case Some(s:String) => s.toInt
            case None => {
              logger.error("No response supplied. Returning 400 (Bad Request) ...")
              halt(400)
            }
          }
        val itemOption = scoringMethods.getScoredIdResponseItem(itemId,answerId)
        if(itemOption.isDefined) {
          val item:Item = itemOption.get
          item.basketId = currentBasketId
          item.positionInBasket = 1
          item.positionInTest = itemList.length + 1
          if(logger.isDebugEnabled) logger.debug("Item position in test: " + item.positionInTest)
          item.responseId = answerId
          item.answers = db.getAnswers(itemId) match {
              case Some(l:List[Answer]) => l
              case None => {
                logger.error("No answers returned from db for item " + itemId)
                List[Answer]()
              }
            }
          itemList += item
          basketList += new Basket(currentBasketId,"mcq",List(item))
        } else {
          logger.error("No item returned from scoring")
        }
        dataCapture.logSingleIdResponse(dialangSession.passId,currentBasketId,itemId,answerId)
      }

      case Some("tabbedpane") => {
        val responses = getMultipleIdResponses
        val basketItems = new ListBuffer[Item]()
        responses.foreach(t => {
          val itemOption = scoringMethods.getScoredIdResponseItem(t._1,t._2)
          if(itemOption.isDefined) {
            val item:Item = itemOption.get
            item.positionInBasket = params.get(item.id + "-position") match {
                case Some(s:String) => s.toInt
                case None => {
                  logger.error("No position supplied for item '" + item.id + "'. Returning 400 (Bad Request) ...")
                  halt(400)
                }
              }
            item.basketId = currentBasketId
            item.positionInTest = itemList.length + 1
            if(logger.isDebugEnabled) logger.debug("Item position in test: " + item.positionInTest)
            logger.debug(item.basketId.toString)
            item.responseId = t._2
            item.answers = db.getAnswers(item.id) match {
                case Some(l:List[Answer]) => l
                case None => {
                  logger.error("No answers returned from db for item " + item.id)
                  List[Answer]()
                }
              }
            itemList += item
            basketItems += item
          } else {
            logger.error("No item returned from scoring")
          }
        })
        basketList += new Basket(currentBasketId,"tabbedpane",basketItems.toList)
        dataCapture.logMultipleIdResponses(dialangSession.passId,currentBasketId,responses.toMap)
      }

      case Some("shortanswer") => {
        val responses = getMultipleTextualResponses
        val basketItems = new ListBuffer[Item]()
        responses.foreach(t => {
          logger.error(t._1 + " -> " + t._2)
          val itemOption = scoringMethods.getScoredTextResponseItem(t._1,t._2)
          if(itemOption.isDefined) {
            val item = itemOption.get
            item.basketId = currentBasketId
            item.positionInBasket = params.get(item.id + "-position") match {
                case Some(s:String) => s.toInt
                case None => {
                  logger.error("No position supplied for item '" + item.id + "'. Returning 400 (Bad Request) ...")
                  halt(400)
                }
              }
            item.positionInTest = itemList.length + 1
            if(logger.isDebugEnabled) logger.debug("Item position in test: " + item.positionInTest)
            item.responseText = t._2
            item.answers = db.getAnswers(item.id) match {
                case Some(l:List[Answer]) => l
                case None => {
                  logger.error("No answers returned from db for item " + item.id)
                  List[Answer]()
                }
              }
            itemList += item
            basketItems += item
          } else {
            logger.error("No item returned from scoring")
          }
        })
        basketList += new Basket(currentBasketId,"shortanswer",basketItems.toList)
        dataCapture.logMultipleTextualResponses(dialangSession.passId,currentBasketId,responses.toMap)
      }

      case Some("gaptext") => {
        val responses = getMultipleTextualResponses
        val basketItems = new ListBuffer[Item]()
        responses.foreach(t => {
          val itemOption = scoringMethods.getScoredTextResponseItem(t._1,t._2)
          if(itemOption.isDefined) {
            val item:Item = itemOption.get
            item.basketId = currentBasketId
            item.positionInBasket = params.get(item.id + "-position") match {
                case Some(s:String) => s.toInt
                case None => {
                  logger.error("No position supplied for item '" + item.id + "'. Returning 400 (Bad Request) ...")
                  halt(400)
                }
              }
            item.responseText = t._2
            item.positionInTest = itemList.length + 1
            if(logger.isDebugEnabled) logger.debug("Item position in test: " + item.positionInTest)
            item.answers = db.getAnswers(item.id) match {
                case Some(l:List[Answer]) => l
                case None => {
                  logger.error("No answers returned from db for item " + item.id)
                  List[Answer]()
                }
              }
            itemList += item
            basketItems += item
          } else {
            logger.error("No item returned from scoring")
          }
        })
        basketList += new Basket(currentBasketId,"gaptext",basketItems.toList)
        dataCapture.logMultipleTextualResponses(dialangSession.passId,currentBasketId,responses.toMap)
      }

      case Some("gapdrop") => {
        val responses = getMultipleIdResponses
        val basketItems = new ListBuffer[Item]()
        responses.foreach(t => {
          val itemOption = scoringMethods.getScoredIdResponseItem(t._1,t._2)
          if(itemOption.isDefined) {
            val item:Item = itemOption.get
            item.basketId = currentBasketId
            item.positionInBasket = params.get(item.id + "-position") match {
                case Some(s:String) => s.toInt
                case None => {
                  logger.error("No position supplied for item '" + item.id + "'. Returning 400 (Bad Request) ...")
                  halt(400)
                }
              }
            item.positionInTest = itemList.length + 1
            if(logger.isDebugEnabled) logger.debug("Item position in test: " + item.positionInTest)
            item.responseId = t._2
            item.answers = db.getAnswers(item.id) match {
                case Some(l:List[Answer]) => l
                case None => {
                  logger.error("No answers returned from db for item " + item.id)
                  List[Answer]()
                }
              }
            itemList += item
            basketItems += item
          } else {
            logger.error("No item returned from scoring")
          }
        })
        basketList += new Basket(currentBasketId,"gapdrop",basketItems.toList)
        dataCapture.logMultipleIdResponses(dialangSession.passId,currentBasketId,responses.toMap)
      }
      case Some(s:String) => {
        logger.error("Unrecognised basketType '" + s + "'. Returning 400 (Bad Request) ...")
        halt(400)
      }
      case None => {
        logger.error("No basketType supplied. Returning 400 (Bad Request) ...")
        halt(400)
      }
    }

    dialangSession.scoredItemList = itemList.toList

    dialangSession.scoredBasketList = basketList.toList

    val nextBasketNumber = dialangSession.currentBasketNumber + 1
    if(logger.isDebugEnabled) logger.debug("nextBasketNumber:" + nextBasketNumber)

    val basketIds:List[Int] = db.getBasketIdsForBooklet(dialangSession.bookletId)

    if(nextBasketNumber >= basketIds.length) {

      val (itemGrade:Int,itemLevel:String) = scoringMethods.getItemGrade(dialangSession,itemList.toList)

      if(logger.isDebugEnabled) logger.debug("ITEM GRADE:" + itemGrade)

      dialangSession.itemGrade = itemGrade
      dialangSession.itemLevel = itemLevel

      saveDialangSession(dialangSession)

      dataCapture.logTestResult(dialangSession)

      contentType = "application/json"

      // We set testDone to true so the client js knows to enable the sa feedback and advice buttons
      "{\"itemLevel\":\"" + itemLevel + "\",\"testDone\":\"true\"}"

    } else {

      val nextBasketId = basketIds(nextBasketNumber)

      dialangSession.currentBasketNumber = nextBasketNumber
      saveDialangSession(dialangSession)
     
      contentType = "application/json"
      "{\"nextBasketId\":\"" + nextBasketId.toString + "\",\"itemsCompleted\":\"" + itemList.length.toString + "\"}"
    }
  }

  private def getMultipleIdResponses: Map[Int,Int] = {

    val responses = new HashMap[Int,Int]

    params.foreach(t => {
      val name = t._1.asInstanceOf[String]
      if(name.endsWith("-response")) {
        val itemId = name.split("-")(0).toInt
        val answerId = t._2.toInt
        responses += ((itemId,answerId))
      }
    })
    responses.toMap
  }

  private def getMultipleTextualResponses: Map[Int,String] = {

    val responses = new HashMap[Int,String]

    params.foreach(t => {
      val name = t._1.asInstanceOf[String]
      if(name.endsWith("-response")) {
        responses += ((name.split("-")(0).toInt,t._2))
      }
    })
    responses.toMap
  }
}
