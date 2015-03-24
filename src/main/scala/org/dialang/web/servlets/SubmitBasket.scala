package org.dialang.web.servlets

import org.dialang.web.db.DBFactory
import org.dialang.web.model.{DialangSession,Basket}
import org.dialang.web.scoring.ScoringMethods
import org.dialang.common.model.{Answer,ImmutableItem,ScoredItem}

import scala.collection.JavaConversions._
import scala.collection.mutable.HashMap
import scala.collection.mutable.ListBuffer

import org.slf4j.LoggerFactory

import org.json4s.{DefaultFormats, Formats}

import org.scalatra.json._

class SubmitBasket extends DialangServlet with JacksonJsonSupport {

  private val logger = LoggerFactory.getLogger(classOf[SubmitBasket])

  protected implicit val jsonFormats: Formats = DefaultFormats
 
  private val scoringMethods = new ScoringMethods

  post("/") {

    val dialangSession = getDialangSession

    if (dialangSession.tes.tl == "" || dialangSession.tes.skill == "") {
      logger.error("Neither the test language or skill were set in the session. Returning 500 ...")
      halt(500)
    }

    val currentBasketId = params.get("basketId") match {
        case Some(s: String) => s.toInt
        case None => {
          logger.error("No basket id supplied. Returning 400 (Bad Request) ...")
          halt(400)
        }
      } 

    if (logger.isDebugEnabled) {
      logger.debug("currentBasketId = " + currentBasketId)
      logger.debug("currentBasketNumber = " + dialangSession.currentBasketNumber)
      logger.debug("Current item list length: " + dialangSession.scoredItemList.length)
    }

    val returnMap = new HashMap[String, Any]()

    def positionInBasketSorter = (a: ImmutableItem, b: ImmutableItem) => {a.positionInBasket < b.positionInBasket}

    val basketList = (new ListBuffer[Basket]) ++ dialangSession.scoredBasketList
    val itemList = (new ListBuffer[ImmutableItem]) ++ dialangSession.scoredItemList

    // We need this for calculating the position in the test of an item. itemList gets
    // appended as we go so we need to grab the length now.
    val numScoredItems = itemList.length

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

        scoringMethods.getScoredIdResponseItem(itemId, answerId) match {

            case Some(item: ScoredItem) => {
              item.basketId = currentBasketId
              item.positionInBasket = 1
              item.responseId = answerId
              item.positionInTest = numScoredItems + 1
              if (logger.isDebugEnabled) logger.debug("Item position in test: " + item.positionInTest)
              item.answers = db.getAnswers(itemId) match {
                  case Some(l:List[Answer]) => l
                  case None => {
                    logger.error("No answers returned from db for item " + itemId)
                    List[Answer]()
                  }
                }
              val immutableItem:ImmutableItem = item.toCase
              itemList += immutableItem
              val scoredBasket = new Basket(currentBasketId, "mcq", item.skill, List(immutableItem))
              basketList += scoredBasket
              returnMap += ("scoredBasket" -> scoredBasket)
              dataCapture.logSingleIdResponse(dialangSession.passId, immutableItem)
            }
            case None => {
              logger.error("No item returned from scoring")
            }
        }
      }

      case Some("tabbedpane") => {
        val itemsToLog = new ListBuffer[ImmutableItem]()
        val responses = getMultipleIdResponses
        val basketItems = new ListBuffer[ImmutableItem]()
        responses.foreach(t => {
          val itemOption = scoringMethods.getScoredIdResponseItem(t._1, t._2)
          if(itemOption.isDefined) {
            val item: ScoredItem = itemOption.get
            item.responseId = t._2
            item.positionInBasket = params.get(item.id + "-position") match {
                case Some(s: String) => s.toInt
                case None => {
                  logger.error("No position supplied for item '" + item.id + "'. Returning 400 (Bad Request) ...")
                  halt(400)
                }
              }

            item.basketId = currentBasketId
            item.positionInTest = numScoredItems + item.positionInBasket
            if(logger.isDebugEnabled) logger.debug("Item position in basket: " + item.positionInBasket)
            if(logger.isDebugEnabled) logger.debug("Item position in test: " + item.positionInTest)
            logger.debug(item.basketId.toString)
            item.answers = db.getAnswers(item.id) match {
                case Some(l:List[Answer]) => l
                case None => {
                  logger.error("No answers returned from db for item " + item.id)
                  List[Answer]()
                }
              }
            val immutableItem:ImmutableItem = item.toCase
            itemsToLog += immutableItem
            itemList += immutableItem
            basketItems += immutableItem
          } else {
            logger.error("No item returned from scoring")
          }
        })
        val scoredBasket = new Basket(currentBasketId,"tabbedpane",basketItems.head.skill,basketItems.toList.sortWith(positionInBasketSorter))
        basketList += scoredBasket
        returnMap += ("scoredBasket" -> scoredBasket)
        dataCapture.logMultipleIdResponses(dialangSession.passId, itemsToLog.toList)
      }

      case Some("shortanswer") => {
        val responses = getMultipleTextualResponses
        val basketItems = new ListBuffer[ImmutableItem]()
        val itemsToLog = new ListBuffer[ImmutableItem]()
        responses.foreach(t => {
          logger.error(t._1 + " -> " + t._2)
          val itemOption = scoringMethods.getScoredTextResponseItem(t._1,t._2)
          if(itemOption.isDefined) {
            val item = itemOption.get
            item.basketId = currentBasketId
            item.responseText = t._2
            item.positionInBasket = params.get(item.id + "-position") match {
                case Some(s:String) => s.toInt
                case None => {
                  logger.error("No position supplied for item '" + item.id + "'. Returning 400 (Bad Request) ...")
                  halt(400)
                }
              }
            item.positionInTest = numScoredItems + item.positionInBasket
            if(logger.isDebugEnabled) logger.debug("Item position in test: " + item.positionInTest)
            item.answers = db.getAnswers(item.id) match {
                case Some(l:List[Answer]) => l
                case None => {
                  logger.error("No answers returned from db for item " + item.id)
                  List[Answer]()
                }
              }
            val immutableItem:ImmutableItem = item.toCase
            itemList += immutableItem
            itemsToLog += immutableItem
            basketItems += immutableItem
          } else {
            logger.error("No item returned from scoring")
          }
        })
        val scoredBasket = new Basket(currentBasketId,"shortanswer",basketItems.head.skill,basketItems.toList.sortWith(positionInBasketSorter))
        basketList += scoredBasket
        returnMap += ("scoredBasket" -> scoredBasket)
        dataCapture.logMultipleTextualResponses(dialangSession.passId, itemsToLog.toList)
      }

      case Some("gaptext") => {
        val responses = getMultipleTextualResponses
        val basketItems = new ListBuffer[ImmutableItem]()
        val itemsToLog = new ListBuffer[ImmutableItem]()

        responses.foreach(t => {

          val itemOption = scoringMethods.getScoredTextResponseItem(t._1,t._2)
          if(itemOption.isDefined) {
            val item: ScoredItem = itemOption.get
            item.basketId = currentBasketId
            item.responseText = t._2
            item.positionInBasket = params.get(item.id + "-position") match {
                case Some(s:String) => s.toInt
                case None => {
                  logger.error("No position supplied for item '" + item.id + "'. Returning 400 (Bad Request) ...")
                  halt(400)
                }
              }
            item.positionInTest = numScoredItems + item.positionInBasket
            if(logger.isDebugEnabled) logger.debug("Item position in test: " + item.positionInTest)
            item.answers = db.getAnswers(item.id) match {
                case Some(l:List[Answer]) => l
                case None => {
                  logger.error("No answers returned from db for item " + item.id)
                  List[Answer]()
                }
              }
            val immutableItem:ImmutableItem = item.toCase
            itemList += immutableItem
            itemsToLog += immutableItem
            basketItems += immutableItem
          } else {
            logger.error("No item returned from scoring")
          }
        })

        val scoredBasket = new Basket(currentBasketId,"gaptext",basketItems.head.skill,basketItems.toList.sortWith(positionInBasketSorter))
        basketList += scoredBasket
        returnMap += ("scoredBasket" -> scoredBasket)
        dataCapture.logMultipleTextualResponses(dialangSession.passId,itemsToLog.toList)
      }

      case Some("gapdrop") => {
        val responses = getMultipleIdResponses
        val basketItems = new ListBuffer[ImmutableItem]()
        val itemsToLog = new ListBuffer[ImmutableItem]()
        responses.foreach(t => {
          val itemOption = scoringMethods.getScoredIdResponseItem(t._1,t._2)
          if (itemOption.isDefined) {
            val item: ScoredItem = itemOption.get
            item.basketId = currentBasketId
            item.responseId = t._2

            item.positionInBasket = params.get(item.id + "-position") match {
                case Some(s: String) => s.toInt
                case None => {
                  logger.error("No position supplied for item '" + item.id + "'. Returning 400 (Bad Request) ...")
                  halt(400)
                }
              }

            item.positionInTest = numScoredItems + item.positionInBasket
            if(logger.isDebugEnabled) logger.debug("Item position in test: " + item.positionInTest)
            item.answers = db.getAnswers(item.id) match {
                case Some(l: List[Answer]) => l
                case None => {
                  logger.error("No answers returned from db for item " + item.id)
                  List[Answer]()
                }
              }
            val immutableItem: ImmutableItem = item.toCase
            itemList += immutableItem
            itemsToLog += immutableItem
            basketItems += immutableItem
          } else {
            logger.error("No item returned from scoring")
          }
        })

        val scoredBasket = new Basket(currentBasketId, "gapdrop", basketItems.head.skill, basketItems.toList.sortWith(positionInBasketSorter))
        basketList += scoredBasket
        returnMap += ("scoredBasket" -> scoredBasket)
        dataCapture.logMultipleIdResponses(dialangSession.passId, itemsToLog.toList)
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
    if (logger.isDebugEnabled) logger.debug("nextBasketNumber:" + nextBasketNumber)

    val basketIds:List[Int] = db.getBasketIdsForBooklet(dialangSession.bookletId)

    if (nextBasketNumber >= basketIds.length) {
      // The test has finished. Grade it.
      val (itemGrade:Int,itemLevel:String) = scoringMethods.getItemGrade(dialangSession,itemList.toList)

      if (logger.isDebugEnabled) logger.debug("ITEM GRADE:" + itemGrade)

      dialangSession.itemGrade = itemGrade
      dialangSession.itemLevel = itemLevel

      saveDialangSession(dialangSession)

      dataCapture.logTestResult(dialangSession)
      dataCapture.logTestFinish(dialangSession.passId)

      notifyTestCompletion(dialangSession)

      // We set testDone to true so the client js knows to enable the sa feedback and advice buttons
      returnMap += (("itemLevel" -> itemLevel),("testDone" -> "true"))

      contentType = formats("json")
      returnMap.toMap

    } else {
      dataCapture.logBasket(dialangSession.passId, currentBasketId, dialangSession.currentBasketNumber)

      val nextBasketId = basketIds(nextBasketNumber)

      dialangSession.currentBasketNumber = nextBasketNumber
      saveDialangSession(dialangSession)

      returnMap += (("nextBasketId" -> nextBasketId.toString),("itemsCompleted" -> itemList.length.toString))

      contentType = formats("json")
      returnMap.toMap
    }
  }

  /**
   * Returns a map of responseId onto itemId
   */
  private def getMultipleIdResponses: Map[Int,Int] = {

    val responses = new HashMap[Int,Int]

    params.foreach(t => {
      val name = t._1.asInstanceOf[String]
      if(name.endsWith("-response")) {
        val itemId = name.split("-")(0).toInt
        val answerId = t._2.toInt
        responses += ((itemId, answerId))
      }
    })
    responses.toMap
  }

  /**
   * Returns a map of response text on to itemId
   */
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
