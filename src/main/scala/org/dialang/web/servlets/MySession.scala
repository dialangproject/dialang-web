package org.dialang.web.servlets

import org.dialang.web.model.DialangSession
import org.dialang.common.model.Item

import scala.collection.JavaConversions._

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.scalatra.ScalatraServlet

import org.dialang.web.db.DB

import org.json4s.{DefaultFormats, Formats}

// JSON handling support from Scalatra
import org.scalatra.json._

class MySession extends ScalatraServlet with JacksonJsonSupport {

  protected implicit val jsonFormats: Formats = DefaultFormats

  private val logger = LoggerFactory.getLogger(classOf[MySession])

  private val db = DB

  // Before every action runs, set the content type to be in JSON format.
  before() {
    contentType = formats("json")
  }

  get("/items.json") {

    /*
    List(
        JSONItem(1,23,45,"mcq","reading","mi",3,3,true),
        JSONItem(4,24,56,"mcq","reading","mi",1,1,false),
        JSONItem(9,35,72,"shortanswer","reading","ov",2,2,true))
    */

    session.get("dialangSession") match {
      case Some(s:DialangSession) => {
        s.scoredItemList.map(i => {
          val answers = i.answers.map(a => JSONAnswer(a.id,a.itemId,a.text,a.correct))
          JSONItem(i.id,i.basketId,i.responseId,i.responseText,i.itemType,i.skill,i.subskill.toLowerCase,i.weight,i.score,i.correct,answers)
        })
      }
      case Some(a:Any) => {
        logger.error("dialangSession should be a DialangSession!")
        List()
      }
      case None => {
        logger.debug("Session does not exist")
        List()
      }
    }
  }
}
