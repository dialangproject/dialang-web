package org.dialang.web.datacapture

import java.sql.{Connection, Statement, PreparedStatement, ResultSet, SQLException}
import javax.naming.InitialContext
import javax.sql.DataSource

import org.dialang.db.DB
import org.dialang.common.model.{Answer, Basket, DialangSession, ImmutableAnswer, ImmutableItem, ScoredItem}

import scala.collection.mutable.{HashMap, ListBuffer}

import grizzled.slf4j.Logging

class Loader(dUrl: String, dcUrl: String) extends Logging {

  val ctx = new InitialContext
  val dDS = ctx.lookup(dUrl).asInstanceOf[DataSource]
  val dcDS = ctx.lookup(dcUrl).asInstanceOf[DataSource]

  def loadDialangSession(token: String, db: DB): Option[DialangSession] = {

    lazy val dConn = dDS.getConnection
    lazy val dcConn = dcDS.getConnection
    lazy val tokenST = dcConn.prepareStatement("SELECT * FROM tokens WHERE token = ?")
    lazy val passST = dcConn.prepareStatement("SELECT * FROM passes WHERE id = (SELECT pass_id FROM tokens WHERE token = ?)")
    lazy val vsptST = dcConn.prepareStatement("SELECT * FROM vsp_test_scores WHERE pass_id = ?")
    lazy val saST = dcConn.prepareStatement("SELECT * FROM sa_scores WHERE pass_id = ?")
    lazy val bookletST = dcConn.prepareStatement("SELECT * FROM pass_booklet WHERE pass_id = ?")
    lazy val dcItemST = dcConn.prepareStatement("SELECT * FROM item_responses WHERE item_id = ?")
    lazy val dBasketItemST = dConn.prepareStatement("SELECT items.*,basket_item.position FROM items, basket_item WHERE id IN (SELECT item_id FROM basket_item WHERE basket_id = ?) AND id = item_id")
    lazy val dcBasketST = dcConn.prepareStatement("SELECT * FROM baskets WHERE pass_id = ? ORDER BY basket_id")
    lazy val dBasketST = dConn.prepareStatement("SELECT * FROM baskets WHERE id = ?")
    lazy val dcItemsST = dcConn.prepareStatement("SELECT * FROM item_responses WHERE pass_id = ? ORDER BY pass_order")

    tokenST.setString(1, token)
    lazy val tokenRS = tokenST.executeQuery

    passST.setString(1, token)
    lazy val passRS = passST.executeQuery

    try {
      if (passRS.next && tokenRS.next) {
        val dialangSession = new DialangSession

        dialangSession.sessionId = passRS.getString("session_id")
        dialangSession.passId = passRS.getString("id")
        dialangSession.tes.al = passRS.getString("al")
        dialangSession.tes.tl = passRS.getString("tl")
        dialangSession.tes.skill = passRS.getString("skill")
        dialangSession.vsptSkipped = tokenRS.getBoolean("vspt_skipped")
        dialangSession.saSkipped = tokenRS.getBoolean("sa_skipped")

        if (!dialangSession.vsptSkipped) {
          // Load up the vspt results
          vsptST.setString(1, dialangSession.passId)
          val vsptRS = vsptST.executeQuery
          if (vsptRS.next) {
            dialangSession.vsptSubmitted = true
            dialangSession.vsptZScore = vsptRS.getInt("z_score")
            dialangSession.vsptMearaScore = vsptRS.getInt("meara_score")
            dialangSession.vsptLevel = vsptRS.getString("level")
          }
          vsptRS.close()
        }

        if (!dialangSession.saSkipped) {
          // Load up the sa results
          saST.setString(1, dialangSession.passId)
          val saRS = saST.executeQuery
          if (saRS.next) {
            dialangSession.saSubmitted = true
            dialangSession.saPPE = saRS.getInt("ppe")
            dialangSession.saLevel = saRS.getString("level")
          }
          saRS.close()
        }

        bookletST.setString(1, dialangSession.passId)
        val bookletRS = bookletST.executeQuery
        if (bookletRS.next) {
          // This pass was mid-booklet, load up the booklet id and length.
          dialangSession.bookletId = bookletRS.getInt("booklet_id")
          dialangSession.bookletLength = bookletRS.getInt("length")

          val scoredBaskets = new ListBuffer[Basket]()
          val allItemsList = new ListBuffer[ImmutableItem]()

          // Get the list of basket ids for this pass
          val basketIds: List[Int] = db.getBasketIdsForBooklet(dialangSession.bookletId)

          var itemCounter = 0

          dcBasketST.setString(1, dialangSession.passId)
          val dcBasketRS = dcBasketST.executeQuery

          //for (basketId <- basketIds) {
          while (dcBasketRS.next) {
            val basketId = dcBasketRS.getInt("basket_id")

            // Grab the basket from DIALANG
            dBasketST.setInt(1, basketId)
            val dBasketRS = dBasketST.executeQuery

            if (dBasketRS.next) {
              val basketItemList = new ListBuffer[ImmutableItem]();
              val basketType = dBasketRS.getString("type")
              val basketSkill = dBasketRS.getString("skill")

              // Grab the items for this basket
              dBasketItemST.setInt(1, basketId)
              val dItemsRS = dBasketItemST.executeQuery

              while (dItemsRS.next) {
                val itemId = dItemsRS.getInt("id")
                val itemOption = db.getItem(itemId)
                if (itemOption.isDefined) {
                  val item = itemOption.get
                  val scoredItem = new ScoredItem(item.id, item.itemType, item.skill, item.subskill, item.text, item.weight)

                  scoredItem.basketId = basketId
                  scoredItem.positionInBasket = dItemsRS.getInt("position")

                  itemCounter += 1
                  scoredItem.positionInTest = itemCounter

                  // Now get the scored response data for this item
                  dcItemST.setInt(1, itemId)
                  val dcItemRS = dcItemST.executeQuery
                  if (dcItemRS.next) {
                    scoredItem.responseId = dcItemRS.getInt("answer_id")
                    scoredItem.responseText = dcItemRS.getString("answer_text")
                    scoredItem.score = dcItemRS.getInt("score")
                    scoredItem.correct = dcItemRS.getBoolean("correct")

                    // Load up the answers
                    val answersOption = db.getAnswers(itemId)

                    if (answersOption.isDefined) {
                      scoredItem.answers = answersOption.get
                      basketItemList += scoredItem.toCase
                    } else {
                      logger.error("No answers for item '" + itemId + "'.")
                    }
                  } else {
                      logger.error("No item '" + itemId + "' in DIALANGDATACAPTURE.item_responses.")
                  }
                }
              } // items

              // Create the basket and add it to the scored basket list
              scoredBaskets += Basket(basketId, basketType, basketSkill, basketItemList.toList)
              allItemsList ++= basketItemList
            } else {
              logger.error("No basket '" + basketId + "' in DIALANG.baskets.")
            }
          }

          dcBasketRS.close()

          if (allItemsList.length > 0) {
            dialangSession.scoredItemList = allItemsList.toList
            dialangSession.currentBasketNumber = tokenRS.getInt("current_basket")
            dialangSession.scoredBasketList = scoredBaskets.toList
            dialangSession.nextBasketId = basketIds(dialangSession.currentBasketNumber)
          }
        }
        bookletRS.close()

        Some(dialangSession)
      } else {
        None
      }
    } catch {
      case e: SQLException => {
        logger.error("Caught exception whilst loading session.", e)
        None
      }
    } finally {
      if (passRS != null) {
        try {
          passRS.close()
        } catch { case _ : SQLException => }
      }
      if (tokenRS != null) {
        try {
          tokenRS.close()
        } catch { case _ : SQLException => }
      }
      if (passST != null) {
        try {
          passST.close()
        } catch { case _ : SQLException => }
      }
      if (vsptST != null) {
        try {
          vsptST.close()
        } catch { case _ : SQLException => }
      }
      if (saST != null) {
        try {
          saST.close()
        } catch { case _ : SQLException => }
      }
      if (dBasketST != null) {
        try {
          dBasketST.close()
        } catch { case _ : SQLException => }
      }
      if (dcItemST != null) {
        try {
          dcItemST.close()
        } catch { case _ : SQLException => }
      }

      if (dcConn != null) {
        try {
          dcConn.close()
        } catch { case _ : SQLException => }
      }

      if (dConn != null) {
        try {
          dConn.close()
        } catch { case _ : SQLException => }
      }
    }
  }
}
