package org.dialang.web.datacapture

import java.sql.{Connection,Statement,PreparedStatement,ResultSet,SQLException}
import java.util.Date
import javax.naming.InitialContext
import javax.sql.DataSource

import net.oauth.OAuth

import org.dialang.common.model.{DialangSession, ImmutableItem}

import scala.collection.mutable.ListBuffer

import org.apache.commons.validator.routines.EmailValidator

import grizzled.slf4j.Logging

class DataCaptureImpl(dsUrl: String) extends Logging {

  private val itemResponseSql =
    """INSERT INTO item_responses (pass_id,basket_id,item_id,answer_id,score,correct,pass_order)
          VALUES(?,?,?,?,?,?,?)"""

  private val basketSql = "INSERT INTO baskets (pass_id,basket_id,basket_number) VALUES(?,?,?)"

  private val emailValidator = EmailValidator.getInstance

  val ctx = new InitialContext
  val ds = ctx.lookup(dsUrl).asInstanceOf[DataSource]

  def createSessionAndPass(dialangSession: DialangSession) {

    if (dialangSession.browserReferrer == null) dialangSession.browserReferrer = ""

    lazy val conn = ds.getConnection
    lazy val sessionST
      = conn.prepareStatement("INSERT INTO sessions (id,user_id, consumer_key,resource_link_id,resource_link_title,ip_address,browser_locale,referrer,started) VALUES(?,?,?,?,?,?,?,?,?)")
    lazy val updateResourceLinkTitleST
      = conn.prepareStatement("UPDATE sessions SET resource_link_title = ? WHERE resource_link_id = ?")

    try {

      conn.setAutoCommit(false)

      sessionST.setString(1, dialangSession.sessionId)
      sessionST.setString(2, dialangSession.userId)
      sessionST.setString(3, dialangSession.consumerKey)
      sessionST.setString(4, dialangSession.resourceLinkId)
      sessionST.setString(5, dialangSession.resourceLinkTitle)
      sessionST.setString(6, dialangSession.ipAddress)
      sessionST.setString(7, dialangSession.browserLocale)
      sessionST.setString(8, dialangSession.browserReferrer)
      sessionST.setLong(9, dialangSession.started)
      if (sessionST.executeUpdate != 1) {
        logger.error("Failed to insert session.")
      }

      /*
      updateResourceLinkTitleST.setString(1, dialangSession.resourceLinkTitle)
      updateResourceLinkTitleST.setString(2, dialangSession.resourceLinkId)
      if (updateResourceLinkTitleST.executeUpdate != 1) {
        logger.error("Failed to update resource link title.")
      }
      */

      createPass(dialangSession, conn)

      conn.commit()
    } catch {
      case e: Exception => {
        logger.error("Caught exception whilst creating session and pass.", e)
        conn.rollback()
      }
    } finally {

      if (sessionST != null) {
        try {
          sessionST.close()
        } catch { case e: SQLException => { logger.error("Failed to close statement.", e) } }
      }

      if (conn != null) {
        try {
          conn.setAutoCommit(true)
          conn.close()
        } catch { case e: SQLException => { logger.error("Failed to close connection.", e) } }
      }
    }
  }


  @throws(classOf[SQLException])
  def getSession(sessionId: String): Option[DialangSession] = {

    lazy val conn = ds.getConnection
    lazy val st = conn.prepareStatement("SELECT * FROM sessions WHERE id = ?")

    try {

      st.setString(1,sessionId)
      val rs = st.executeQuery
      if (rs.next) {
        val dialangSession = new DialangSession
        dialangSession.sessionId = sessionId
        dialangSession.userId = rs.getString("user_id")
        dialangSession.consumerKey = rs.getString("consumer_key")
        dialangSession.ipAddress = rs.getString("ip_address")
        dialangSession.started = rs.getLong("started")
        Some(dialangSession)
      } else {
        None
      }
    } finally {

      if (st != null) {
        try {
          st.close()
        } catch { case e: SQLException => { logger.error("Failed to close statement.", e) } }
      }

      if (conn != null) {
        try {
          conn.close()
        } catch { case e: SQLException => { logger.error("Failed to close connection.", e) } }
      }
    }
  }

  @throws(classOf[SQLException])
  def getPass(passId: String): Option[DialangSession] = {

    lazy val conn = ds.getConnection
    lazy val st = conn.prepareStatement("SELECT * FROM passes WHERE id = ?")

    try {

      st.setString(1,passId)
      val rs = st.executeQuery
      if (rs.next) {
        val dialangSession = new DialangSession
        dialangSession.passId = passId
        dialangSession.tes.al = rs.getString("al")
        dialangSession.tes.tl = rs.getString("tl")
        dialangSession.tes.skill = rs.getString("skill")
        Some(dialangSession)
      } else {
        None
      }
    } finally {

      if (st != null) {
        try {
          st.close()
        } catch { case e: SQLException => { logger.error("Failed to close statement.", e) } }
      }

      if (conn != null) {
        try {
          conn.close()
        } catch { case e: SQLException => { logger.error("Failed to close connection.", e) } }
      }
    }
  }

  def createPass(dialangSession: DialangSession, suppliedConn: Connection = null) {

    lazy val st = conn.prepareStatement("INSERT INTO passes (id,session_id,al,tl,skill,started) VALUES(?,?,?,?,?,?)")

    lazy val conn = { if (suppliedConn != null) suppliedConn else ds.getConnection }

    try {
      st.setString(1,dialangSession.passId)
      st.setString(2,dialangSession.sessionId)
      st.setString(3,dialangSession.tes.al)
      st.setString(4,dialangSession.tes.tl)
      st.setString(5,dialangSession.tes.skill)
      st.setLong(6,dialangSession.started)
      if (st.executeUpdate != 1) {
        logger.error("Failed to log pass creation.")
      }
    } catch {
      case e:Exception => {
        logger.error("Caught exception whilst creating pass", e)
      }
    } finally {

      if (st != null) {
        try {
          st.close()
        } catch { case e: SQLException => { logger.error("Failed to close statement.", e) } }
      }

      // If we created a connection locally, close it.
      if (suppliedConn == null && conn != null) {
        try {
          conn.close()
        } catch { case e: SQLException => { logger.error("Failed to close connection.", e) } }
      }
    }
  }

  def logVSPTResponses(dialangSession: DialangSession, responses: Map[String,Boolean]) {

    lazy val conn = ds.getConnection
    lazy val st = conn.prepareStatement("INSERT INTO vsp_test_responses (pass_id,word_id,response) VALUES(?,?,?)")

    try {

      // Insert this session's vspt responses in a transaction
      conn.setAutoCommit(false)

      st.setString(1, dialangSession.passId)
      responses.foreach(t => {
        st.setString(2,t._1)
        st.setBoolean(3,t._2)
        if (st.executeUpdate != 1) {
          logger.error("Failed to log vspt word response.")
        }
      })

      conn.commit
    } catch {
      case e:Exception => {
        logger.error("Caught exception whilst logging VSPT responses", e)
      }
    } finally {
      if (st != null) {
        try {
          st.close()
        } catch { case e: SQLException => { logger.error("Failed to close statement.", e) } }
      }

      if (conn != null) {
        try {
          conn.setAutoCommit(true)
          conn.close()
        } catch { case e: SQLException => { logger.error("Failed to close connection.", e) } }
      }
    }
  }

  def logVSPTScores(dialangSession: DialangSession) {

    lazy val conn = ds.getConnection
    lazy val st = conn.prepareStatement("INSERT INTO vsp_test_scores (pass_id,z_score,meara_score,level) VALUES(?,?,?,?)")

    try {

      // Now insert the scores
      st.setString(1,dialangSession.passId)
      st.setDouble(2,dialangSession.vsptZScore)
      st.setInt(3,dialangSession.vsptMearaScore)
      st.setString(4,dialangSession.vsptLevel)
      if (st.executeUpdate != 1) {
        logger.error("Failed to log vspt scores.")
      }
    } catch {
      case e:Exception => {
        logger.error("Caught exception whilst logging VSPT scores", e)
      }
    } finally {

      if (st != null) {
        try {
          st.close()
        } catch { case e: SQLException => { logger.error("Failed to close statement.", e) } }
      }

      if (conn != null) {
        try {
          conn.close()
        } catch { case e: SQLException => { logger.error("Failed to close connection.", e) } }
      }

    }
  }

  def logSAResponses(dialangSession: DialangSession, responses: Map[String, Boolean]) {

    lazy val conn = ds.getConnection
    lazy val st = conn.prepareStatement("INSERT INTO sa_responses (pass_id,statement_id,response) VALUES(?,?,?)")

    try {

      // Insert this pass's sa responses in a transaction
      conn.setAutoCommit(false)

      st.setString(1,dialangSession.passId)
      responses.foreach(t => {
        st.setString(2,t._1)
        st.setBoolean(3,t._2)
        if (st.executeUpdate != 1) {
          logger.error("Failed to log sa response.")
        }
      })

      conn.commit
    } catch {
      case e:Exception => {
        logger.error("Caught exception whilst logging SA responses", e)
      }
    } finally {

      if (st != null) {
        try {
          st.close()
        } catch { case e: SQLException => { logger.error("Failed to close statement.", e) } }
      }

      if (conn != null) {
        try {
          conn.setAutoCommit(true)
          conn.close()
        } catch { case e: SQLException => { logger.error("Failed to close connection.", e) } }
      }
    }
  }

  def logSAScores(dialangSession: DialangSession) {

    lazy val conn = ds.getConnection
    lazy val st = conn.prepareStatement("INSERT INTO sa_scores (pass_id,ppe,level) VALUES(?,?,?)")

    try {
      // Now insert the scores
      st.setString(1, dialangSession.passId)
      st.setDouble(2, dialangSession.saPPE)
      st.setString(3, dialangSession.saLevel)
      if (st.executeUpdate != 1) {
        logger.error("Failed to log SA scores.")
      }
    } catch {
      case e:Exception => {
        logger.error("Caught exception whilst logging SA scores", e)
      }
    } finally {
      if (st != null) {
        try {
          st.close()
        } catch { case e: SQLException => { logger.error("Failed to close statement.", e) } }
      }

      if (conn != null) {
        try {
          conn.close()
        } catch { case e: SQLException => { logger.error("Failed to close connection.", e) } }
      }
    }
  }

  def logTestStart(passId: String, bookletId: Int, bookletLength: Int) {

    lazy val conn = ds.getConnection
    lazy val st = conn.prepareStatement("INSERT INTO test_durations (pass_id,start) VALUES(?,?)")
    lazy val bookletST = conn.prepareStatement("INSERT INTO pass_booklet (pass_id,booklet_id, length) VALUES(?,?,?)")

    try {
      st.setString(1, passId)
      st.setLong(2, ((new Date()).getTime()) / 1000L)
      if (st.executeUpdate != 1) {
        logger.error("Failed to log test start time.")
      }
      bookletST.setString(1, passId)
      bookletST.setInt(2, bookletId)
      bookletST.setInt(3, bookletLength)
      if (bookletST.executeUpdate != 1) {
        logger.error("Failed to log booklet.")
      }
    } catch {
      case e: Exception => {
        logger.error("Caught exception whilst logging test start time", e)
      }
    } finally {

      if (st != null) {
        try {
          st.close()
        } catch { case e: SQLException => { logger.error("Failed to close statement.", e) } }
      }

      if (conn != null) {
        try {
          conn.close()
        } catch { case e: SQLException => { logger.error("Failed to close connection.", e) } }
      }
    }
  }

  def logBasket(passId: String, basketId: Int, basketNumber: Int) {

    logger.debug("logBasket(" + passId + "," + basketId + "," + basketNumber+ ")")

    lazy val conn = ds.getConnection
    lazy val st = conn.prepareStatement(basketSql)

    try {
      st.setString(1, passId)
      st.setInt(2, basketId)
      st.setInt(3,basketNumber)
      if (st.executeUpdate != 1) {
        logger.error("Failed to log basket.")
      }
    } catch {
      case e: Exception => {
        logger.error("Caught exception whilst logging basket.", e)
      }
    } finally {
      if (st != null) {
        try {
          st.close()
        } catch { case e: SQLException => { logger.error("Failed to close statement.", e) } }
      }

      if (conn != null) {
        try {
          conn.close()
        } catch { case e: SQLException => { logger.error("Failed to close connection.", e) } }
      }
    }
  }

  def logSingleIdResponse(passId: String, item: ImmutableItem) {

    logger.debug("PASS ID: " + passId + ". BASKET ID: " + item.basketId + ". ITEM ID: " + item.id + ". ANSWER ID: " + item.responseId)

    lazy val conn = ds.getConnection
    lazy val st = conn.prepareStatement(itemResponseSql)

    try {
      st.setString(1, passId)
      st.setInt(2, item.basketId)
      st.setInt(3,item.id)
      st.setInt(4, item.responseId)
      st.setInt(5, item.score)
      st.setBoolean(6, item.correct)
      st.setInt(7, item.positionInTest)
      if (st.executeUpdate != 1) {
        logger.error("Failed to log single id response.")
      }
    } catch {
      case e: Exception => {
        logger.error("Caught exception whilst logging single id response.", e)
      }
    } finally {

      if (st != null) {
        try {
          st.close()
        } catch { case e: SQLException => { logger.error("Failed to close statement.", e) } }
      }

      if (conn != null) {
        try {
          conn.close()
        } catch { case e: SQLException => { logger.error("Failed to close connection.", e) } }
      }
    }
  }

  def logMultipleTextualResponses(passId: String, items: List[ImmutableItem]) {

    lazy val conn = ds.getConnection
    lazy val st:PreparedStatement = conn.prepareStatement("INSERT INTO item_responses (pass_id,basket_id,item_id,answer_text,score,correct,pass_order) VALUES(?,?,?,?,?,?,?)")

    try {

      st.setString(1, passId)
      st.setInt(2, items.last.basketId)

      items.foreach(item => {
        st.setInt(3, item.id)
        st.setString(4, item.responseText)
        st.setInt(5, item.score)
        st.setBoolean(6, item.correct)
        st.setInt(7, item.positionInTest)
        if (st.executeUpdate != 1) {
          logger.error("Failed to log textual response.")
        }
      })
    } catch {
      case e:Exception => {
        logger.error("Caught exception whilst logging multiple textual response.", e)
      }
    } finally {

      if (st != null) {
        try {
          st.close()
        } catch { case e: SQLException => { logger.error("Failed to close statement.", e) } }
      }

      if (conn != null) {
        try {
          conn.close()
        } catch { case e: SQLException => { logger.error("Failed to close connection.", e) } }
      }
    }
  }

  def logMultipleIdResponses(passId: String, items: List[ImmutableItem]) {

    logger.debug("PASS ID: " + passId + ". BASKET ID: " + items.last.basketId)

    lazy val conn = ds.getConnection
    lazy val st = conn.prepareStatement(itemResponseSql)

    try {

      st.setString(1, passId)
      st.setInt(2, items.last.basketId)

      items.foreach(item => {
        st.setInt(3, item.id)
        st.setInt(4, item.responseId)
        st.setInt(5, item.score)
        st.setBoolean(6, item.correct)
        st.setInt(7, item.positionInTest)
        if (st.executeUpdate != 1) {
          logger.error("Failed to log id response.")
        }
      })
    } catch {
      case e:Exception => {
        logger.error("Caught exception whilst logging multiple id response.", e)
      }
    } finally {

      if (st != null) {
        try {
          st.close()
        } catch { case e: SQLException => { logger.error("Failed to close statement.", e) } }
      }

      if (conn != null) {
        try {
          conn.close()
        } catch { case e: SQLException => { logger.error("Failed to close connection.", e) } }
      }
    }
  }

  def logTestResult(dialangSession: DialangSession) {

    lazy val conn = ds.getConnection
    lazy val st = conn.prepareStatement("INSERT INTO test_results (pass_id,raw_score,grade,level) VALUES(?,?,?,?)")

    try {

      // Now insert the scores
      st.setString(1,dialangSession.passId)
      st.setInt(2,dialangSession.itemRawScore)
      st.setInt(3,dialangSession.itemGrade)
      st.setString(4,dialangSession.itemLevel)
      if (st.executeUpdate != 1) {
        logger.error("Failed to log test result.")
      }
    } catch {
      case e:Exception => {
        logger.error("Caught exception whilst logging test result.", e)
      }
    } finally {

      if (st != null) {
        try {
          st.close()
        } catch { case e: SQLException => { logger.error("Failed to close statement.", e) } }
      }

      if (conn != null) {
        try {
          conn.close()
        } catch { case e: SQLException => { logger.error("Failed to close connection.", e) } }
      }
    }
  }

  def logTestFinish(passId:String) {

    lazy val conn = ds.getConnection
    lazy val st = conn.prepareStatement("UPDATE test_durations SET finish = ? WHERE pass_id = ?")

    try {

      st.setLong(1,((new Date()).getTime()) / 1000L)
      st.setString(2,passId)
      if (st.executeUpdate != 1) {
        logger.error("Failed to log test finish time.")
      }
    } catch {
      case e:Exception => {
        logger.error("Caught exception whilst logging test finish time.", e)
      }
    } finally {

      if (st != null) {
        try {
          st.close()
        } catch { case e: SQLException => { logger.error("Failed to close statement.", e) } }
      }

      if (conn != null) {
        try {
          conn.close()
        } catch { case e: SQLException => { logger.error("Failed to close connection.", e) } }
      }
    }
  }

  def deleteToken(token: String): Boolean = {

    lazy val conn = ds.getConnection
    lazy val st = conn.prepareStatement("DELETE FROM tokens WHERE token = ?")

    try {
      st.setString(1, token)
      if (st.executeUpdate != 1) {
        logger.error("Failed to delete token.")
        false
      } else {
        true
      }
    } catch {
      case e:Exception => {
        logger.error("Caught exception whilst deleting token.", e)
        false
      }
    } finally {

      if (st != null) {
        try {
          st.close()
        } catch { case e: SQLException => { logger.error("Failed to close statement.", e) } }
      }

      if (conn != null) {
        try {
          conn.close()
        } catch { case e: SQLException => { logger.error("Failed to close connection.", e) } }
      }
    }
  }

  def getScores(consumerKey: String, fromDate: String = "", toDate: String = "", userId: String = "", resourceLinkId: String = "") = {

    logger.debug("consumerKey:" + consumerKey + ". fromDate: " + fromDate
                    + ". toDate: " + toDate + ". userId: " + userId + ". resourceLinkId: " + resourceLinkId)

    lazy val conn = ds.getConnection

    val vsptQuery = "SELECT meara_score,level FROM vsp_test_scores WHERE pass_id = ?"

    lazy val vsptST = conn.prepareStatement(vsptQuery)

    val saQuery = "SELECT ppe,level FROM sa_scores WHERE pass_id = ?"

    lazy val saST = conn.prepareStatement(saQuery)

    val testQuery = "SELECT raw_score,level FROM test_results WHERE pass_id = ?"

    lazy val testST = conn.prepareStatement(testQuery)

    var passesQuery = 
      """SELECT s.user_id,p.*,s.ip_address FROM sessions as s, passes as p
            WHERE s.id = p.session_id
              AND s.consumer_key = ?
              AND s.resource_link_id = ?"""

    var flag = 0
    if (fromDate != "") {
      flag = flag + 4
      passesQuery += " AND p.started > ?"
    }

    if (toDate != "") {
      flag = flag + 2
      passesQuery += " AND p.started < ?"
    }

    if (userId != "") {
      flag = flag + 1
      passesQuery += " AND s.user_id = ?"
    }

    logger.debug("passesQuery: " + passesQuery)

    lazy val passesST = conn.prepareStatement(passesQuery)

    val list = new ListBuffer[Tuple10[String, String, String, Integer, String, Double, String, Integer, String, Double]]

    try {
      passesST.setString(1, consumerKey)
      passesST.setString(2, resourceLinkId)
      if (fromDate != "") {
        passesST.setDouble(3, fromDate.toDouble/1000L)
      }
      if (toDate != "") {
        val pos = if (flag == 2) 3 else 4
        passesST.setDouble(pos, toDate.toDouble/1000L)
      }
      if (userId != "") {
        val pos = if (flag == 1) 3 else if (flag == 3) 4 else 5
        passesST.setString(pos, userId)
      }

      val passesRS = passesST.executeQuery

      while (passesRS.next) {
        val userId = passesRS.getString("user_id")
        val passId = passesRS.getString("id")
        val al = passesRS.getString("al")
        val tl = passesRS.getString("tl")
        val started = passesRS.getDouble("started")

        logger.debug(userId + "," + passId + "," + al + "," + tl)

        val (vsptScore, vsptLevel) = {
            vsptST.setString(1, passId)
            val vsptRS = vsptST.executeQuery
            if (vsptRS.next) {
              (vsptRS.getInt("meara_score"), vsptRS.getString("level"))
            } else {
              (-1, "-1")
            }
          }

        val (saScore, saLevel) = {
            saST.setString(1, passId)
            val saRS = saST.executeQuery
            if (saRS.next) {
              (saRS.getDouble("ppe"), saRS.getString("level"))
            } else {
              (-1.0, "-1")
            }
          }

        val (rawScore, testLevel) = {
            testST.setString(1, passId)
            val testRS = testST.executeQuery
            if (testRS.next) {
              (testRS.getInt("raw_score"), testRS.getString("level"))
            } else {
              (-1, "-1")
            }
          }

        logger.debug("userId: " + userId)
        logger.debug("passId: " + passId)
        logger.debug("al: " + al)
        logger.debug("tl: " + tl)
        logger.debug("started: " + started)
        logger.debug("vsptLevel: " + vsptLevel)
        logger.debug("saScore: " + saScore)
        logger.debug("saLevel: " + saLevel)
        logger.debug("rawScore: " + rawScore)
        logger.debug("testLevel: " + testLevel)

        list += ((userId, al, tl, vsptScore, vsptLevel, saScore, saLevel, rawScore, testLevel, started))
      }
      passesRS.close()
    } catch {
      case t: Throwable => {
        logger.error("Caught exception whilst getting scores.", t)
      }
    } finally {
      if (passesST != null) {
        try {
          passesST.close()
        } catch { case _ : SQLException => }
      }

      if (conn != null) {
        try {
          conn.close()
        } catch { case e: SQLException => { logger.error("Failed to close connection.", e) } }
      }
    }
    list.toList
  }

  def storeQuestionnaire(sessionId: String, data: Map[String, String]) = {

    val ageGroup = data.getOrElse("agegroup", "")
    val gender = data.getOrElse("gender", "")
    val otherGender = data.getOrElse("othergender", "")
    val firstLanguage = data.getOrElse("firstlanguage", "")
    val nationality = data.getOrElse("nationality", "")
    val institution = data.getOrElse("institution", "")
    val reason = data.getOrElse("reason", "")
    val accuracy = data.getOrElse("accuracy", "")
    val comments = data.getOrElse("comments", "")
    val email = data.get("email") match {
      case Some(s) => {
        if (emailValidator.isValid(s)) {
          s
        } else {
          logger.info(s + " is not a valid email. Setting to \"\"")
          ""
        }
      }
      case None => ""
    }

    logger.debug("ageGroup: " + ageGroup)
    logger.debug("gender: " + gender)
    logger.debug("otherGender: " + otherGender)
    logger.debug("firstLanguage: " + firstLanguage)
    logger.debug("nationality: " + nationality)
    logger.debug("institution: " + institution)
    logger.debug("reason: " + reason)
    logger.debug("accuracy: " + accuracy)
    logger.debug("comments: " + comments)
    logger.debug("email: " + email)

    lazy val conn = ds.getConnection
    lazy val st
      = conn.prepareStatement("INSERT INTO questionnaire VALUES(?,?,?,?,?,?,?,?,?,?,?)")

    try {
      st.setString(1, sessionId)
      st.setInt(2, ageGroup.toInt)
      st.setString(3, gender)
      st.setString(4, otherGender)
      st.setString(5, firstLanguage)
      st.setString(6, nationality)
      st.setString(7, institution)
      st.setInt(8, reason.toInt)
      st.setInt(9, accuracy.toInt)
      st.setString(10, comments)
      st.setString(11, email)
      st.executeUpdate
    } catch {
      case e: Exception => {
        logger.error("Caught exception whilst storing questionnaire.", e)
      }
    } finally {
      if (st != null) {
        try {
          st .close()
        } catch { case e: SQLException => { logger.error("Failed to close statement.", e) } }
      }

      if (conn != null) {
        try {
          conn.close()
        } catch { case e: SQLException => { logger.error("Failed to close connection.", e) } }
      }
    }
  }
}
