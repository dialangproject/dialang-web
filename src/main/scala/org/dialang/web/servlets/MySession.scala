package org.dialang.web.servlets

import org.dialang.web.model.DialangSession
import org.dialang.common.model.Item

import org.slf4j.LoggerFactory;

import org.json4s.{DefaultFormats, Formats}

import org.scalatra.json._

class MySession extends DialangServlet with JacksonJsonSupport {

  protected implicit val jsonFormats: Formats = DefaultFormats

  private val logger = LoggerFactory.getLogger(classOf[MySession])

  // Before every action runs, set the content type to be in JSON format.
  before() {
    contentType = formats("json")
  }

  get("/items.json") {

    val s = getDialangSession
    s.scoredItemList.sortWith((a,b) => {a.positionInBasket > b.positionInBasket})
      .map(i => {
          val answers = i.answers.map(a => JSONAnswer(a.id,a.itemId,a.text,a.correct))
          JSONItem(i.id,i.basketId,i.responseId,i.responseText,i.itemType,i.skill,i.subskill.toLowerCase,i.positionInTest,i.positionInBasket,i.weight,i.score,i.correct,answers)
        })
  }
}
