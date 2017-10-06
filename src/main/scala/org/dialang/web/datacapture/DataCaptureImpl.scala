package org.dialang.web.datacapture

import java.sql.{Connection,Statement,PreparedStatement,ResultSet,SQLException}
import java.util.Date
import javax.naming.InitialContext
import javax.sql.DataSource

import net.oauth.OAuth

import org.dialang.common.model.{DialangSession, ImmutableItem}

import scala.collection.mutable.ListBuffer

import org.slf4j.LoggerFactory

class DataCaptureImpl(dsUrl: String) {

  private val log = LoggerFactory.getLogger(classOf[DataCaptureImpl])

  private val itemResponseSql =
    """INSERT INTO item_responses (pass_id,basket_id,item_id,answer_id,score,correct,pass_order)
          VALUES(?,?,?,?,?,?,?)"""

  private val basketSql = "INSERT INTO baskets (pass_id,basket_id,basket_number) VALUES(?,?,?)"

  val ctx = new InitialContext
  val ds = ctx.lookup(dsUrl).asInstanceOf[DataSource]

  def createSessionAndPass(dialangSession: DialangSession) {

    lazy val conn = ds.getConnection
    lazy val sessionST
      = conn.prepareStatement("INSERT INTO sessions (id,user_id,first_name, last_name, consumer_key,resource_link_id,resource_link_title,ip_address,browser_locale,started) VALUES(?,?,?,?,?,?,?,?,?,?)")
    lazy val updateResourceLinkTitleST
      = conn.prepareStatement("UPDATE sessions SET resource_link_title = ? WHERE resource_link_id = ?")

    try {

      conn.setAutoCommit(false)

      sessionST.setString(1, dialangSession.sessionId)
      sessionST.setString(2, dialangSession.userId)
      sessionST.setString(3, dialangSession.firstName)
      sessionST.setString(4, dialangSession.lastName)
      sessionST.setString(5, dialangSession.consumerKey)
      sessionST.setString(6, dialangSession.resourceLinkId)
      sessionST.setString(7, dialangSession.resourceLinkTitle)
      sessionST.setString(8, dialangSession.ipAddress)
      sessionST.setString(9, dialangSession.browserLocale)
      sessionST.setLong(10, dialangSession.started)
      if (sessionST.executeUpdate != 1) {
        log.error("Failed to insert session.")
      }

      /*
      updateResourceLinkTitleST.setString(1, dialangSession.resourceLinkTitle)
      updateResourceLinkTitleST.setString(2, dialangSession.resourceLinkId)
      if (updateResourceLinkTitleST.executeUpdate != 1) {
        log.error("Failed to update resource link title.")
      }
      */

      createPass(dialangSession, conn)

      conn.commit()
    } catch {
      case e: Exception => {
        log.error("Caught exception whilst creating session and pass.", e)
        conn.rollback()
      }
    } finally {

      if (sessionST != null) {
        try {
          sessionST.close()
        } catch { case _ : SQLException => }
      }

      if (conn != null) {
        try {
          conn.setAutoCommit(true)
          conn.close()
        } catch { case _ : SQLException => }
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
        } catch { case _ : SQLException => }
      }

      if (conn != null) {
        try {
          conn.close()
        } catch { case _ : SQLException => }
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
        } catch { case _ : SQLException => }
      }

      if (conn != null) {
        try {
          conn.close()
        } catch { case _ : SQLException => }
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
        log.error("Failed to log pass creation.")
      }
    } catch {
      case e:Exception => {
        log.error("Caught exception whilst creating pass", e)
      }
    } finally {

      if (st != null) {
        try {
          st.close()
        } catch { case _ : SQLException => }
      }

      // If we created a connection locally, close it.
      if (suppliedConn == null && conn != null) {
        try {
          conn.close()
        } catch { case _ : SQLException => }
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
          log.error("Failed to log vspt word response.")
        }
      })

      conn.commit
    } catch {
      case e:Exception => {
        log.error("Caught exception whilst logging VSPT responses", e)
      }
    } finally {
      if (st != null) {
        try {
          st.close()
        } catch { case _ : SQLException => }
      }

      if (conn != null) {
        try {
          conn.setAutoCommit(true)
          conn.close()
        } catch { case _ : SQLException => }
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
        log.error("Failed to log vspt scores.")
      }
    } catch {
      case e:Exception => {
        log.error("Caught exception whilst logging VSPT scores", e)
      }
    } finally {

      if (st != null) {
        try {
          st.close()
        } catch { case _ : SQLException => }
      }

      if (conn != null) {
        try {
          conn.close()
        } catch { case _ : SQLException => }
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
          log.error("Failed to log sa response.")
        }
      })

      conn.commit
    } catch {
      case e:Exception => {
        log.error("Caught exception whilst logging SA responses", e)
      }
    } finally {

      if (st != null) {
        try {
          st.close()
        } catch { case _ : SQLException => }
      }

      if (conn != null) {
        try {
          conn.setAutoCommit(true)
          conn.close()
        } catch { case _ : SQLException => }
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
        log.error("Failed to log SA scores.")
      }
    } catch {
      case e:Exception => {
        log.error("Caught exception whilst logging SA scores", e)
      }
    } finally {
      if (st != null) {
        try {
          st.close()
        } catch { case _ : SQLException => }
      }

      if (conn != null) {
        try {
          conn.close()
        } catch { case _ : SQLException => }
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
        log.error("Failed to log test start time.")
      }
      bookletST.setString(1, passId)
      bookletST.setInt(2, bookletId)
      bookletST.setInt(3, bookletLength)
      if (bookletST.executeUpdate != 1) {
        log.error("Failed to log booklet.")
      }
    } catch {
      case e: Exception => {
        log.error("Caught exception whilst logging test start time", e)
      }
    } finally {

      if (st != null) {
        try {
          st.close()
        } catch { case _ : SQLException => }
      }

      if (conn != null) {
        try {
          conn.close()
        } catch { case _ : SQLException => }
      }
    }
  }

  def logBasket(passId: String, basketId: Int, basketNumber: Int) {

    if (log.isDebugEnabled()) {
      log.debug("logBasket(" + passId + "," + basketId + "," + basketNumber + ")")
    }

    lazy val conn = ds.getConnection
    lazy val st = conn.prepareStatement(basketSql)

    try {
      st.setString(1, passId)
      st.setInt(2, basketId)
      st.setInt(3,basketNumber)
      if (st.executeUpdate != 1) {
        log.error("Failed to log basket.")
      }
    } catch {
      case e: Exception => {
        log.error("Caught exception whilst logging basket.", e)
      }
    } finally {
      if (st != null) {
        try {
          st.close()
        } catch { case _ : SQLException => }
      }

      if (conn != null) {
        try {
          conn.close()
        } catch { case _ : SQLException => }
      }
    }
  }

  def logSingleIdResponse(passId: String, item: ImmutableItem) {

    if (log.isDebugEnabled()) {
      log.debug("PASS ID: " + passId + ". BASKET ID: " + item.basketId
                    + ". ITEM ID: " + item.id + ". ANSWER ID: " + item.responseId)
    }

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
        log.error("Failed to log single id response.")
      }
    } catch {
      case e: Exception => {
        log.error("Caught exception whilst logging single id response.", e)
      }
    } finally {

      if (st != null) {
        try {
          st.close()
        } catch { case _ : SQLException => }
      }

      if (conn != null) {
        try {
          conn.close()
        } catch { case _ : SQLException => }
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
          log.error("Failed to log textual response.")
        }
      })
    } catch {
      case e:Exception => {
        log.error("Caught exception whilst logging multiple textual response.", e)
      }
    } finally {

      if (st != null) {
        try {
          st.close()
        } catch { case _ : SQLException => }
      }

      if (conn != null) {
        try {
          conn.close()
        } catch { case _ : SQLException => }
      }
    }
  }

  def logMultipleIdResponses(passId: String, items: List[ImmutableItem]) {

    if (log.isDebugEnabled()) {
      log.debug("PASS ID: " + passId + ". BASKET ID: " + items.last.basketId)
    }

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
          log.error("Failed to log id response.")
        }
      })
    } catch {
      case e:Exception => {
        log.error("Caught exception whilst logging multiple id response.", e)
      }
    } finally {

      if (st != null) {
        try {
          st.close()
        } catch { case _ : SQLException => }
      }

      if (conn != null) {
        try {
          conn.close()
        } catch { case _ : SQLException => }
      }
    }
  }

  def logTestResult(dialangSession:DialangSession) {

    lazy val conn = ds.getConnection
    lazy val st = conn.prepareStatement("INSERT INTO test_results (pass_id,grade,level) VALUES(?,?,?)")

    try {

      // Now insert the scores
      st.setString(1,dialangSession.passId)
      st.setInt(2,dialangSession.itemGrade)
      st.setString(3,dialangSession.itemLevel)
      if (st.executeUpdate != 1) {
        log.error("Failed to log test result.")
      }
    } catch {
      case e:Exception => {
        log.error("Caught exception whilst logging test result.", e)
      }
    } finally {

      if (st != null) {
        try {
          st.close()
        } catch { case _ : SQLException => }
      }

      if (conn != null) {
        try {
          conn.close()
        } catch { case _ : SQLException => }
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
        log.error("Failed to log test finish time.")
      }
    } catch {
      case e:Exception => {
        log.error("Caught exception whilst logging test finish time.", e)
      }
    } finally {

      if (st != null) {
        try {
          st.close()
        } catch { case _ : SQLException => }
      }

      if (conn != null) {
        try {
          conn.close()
        } catch { case _ : SQLException => }
      }
    }
  }

  def deleteToken(token: String): Boolean = {

    lazy val conn = ds.getConnection
    lazy val st = conn.prepareStatement("DELETE FROM tokens WHERE token = ?")

    try {
      st.setString(1, token)
      if (st.executeUpdate != 1) {
        log.error("Failed to delete token.")
        false
      } else {
        true
      }
    } catch {
      case e:Exception => {
        log.error("Caught exception whilst deleting token.", e)
        false
      }
    } finally {

      if (st != null) {
        try {
          st.close()
        } catch { case _ : SQLException => }
      }

      if (conn != null) {
        try {
          conn.close()
        } catch { case _ : SQLException => }
      }
    }
  }

  def getScores(consumerKey: String, fromDate: String, toDate: String, userId: String, resourceLinkId: String) = {

    lazy val conn = ds.getConnection

    val vsptQuery = "SELECT level FROM vsp_test_scores WHERE pass_id = ?"

    lazy val vsptST = conn.prepareStatement(vsptQuery)

    val saQuery = "SELECT level FROM sa_scores WHERE pass_id = ?"

    lazy val saST = conn.prepareStatement(saQuery)

    val testQuery = "SELECT level FROM test_results WHERE pass_id = ?"

    lazy val testST = conn.prepareStatement(testQuery)

    var passesQuery = 
      """SELECT s.user_id,s.first_name,s.last_name,p.*,s.ip_address FROM sessions as s, passes as p
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

    if (log.isDebugEnabled) {
      log.debug("passesQuery: " + passesQuery)
    }

    lazy val passesST = conn.prepareStatement(passesQuery)

    val list = new ListBuffer[Tuple9[String, String, String, String, String, String, String, String, Double]]

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
        val firstName = passesRS.getString("first_name")
        val lastName = passesRS.getString("last_name")
        val passId = passesRS.getString("id")
        val al = passesRS.getString("al")
        val tl = passesRS.getString("tl")
        val started = passesRS.getDouble("started")

        val vsptLevel = {
            vsptST.setString(1, passId)
            val vsptRS = vsptST.executeQuery
            if (vsptRS.next) {
              vsptRS.getString("level")
            } else {
              ""
            }
          }

        val saLevel = {
            saST.setString(1, passId)
            val saRS = saST.executeQuery
            if (saRS.next) {
              saRS.getString("level")
            } else {
              ""
            }
          }

        val testLevel = {
            testST.setString(1, passId)
            val testRS = testST.executeQuery
            if (testRS.next) {
              testRS.getString("level")
            } else {
              ""
            }
          }

        if (log.isDebugEnabled) {
          log.debug("userId: " + userId)
          log.debug("firstName: " + firstName)
          log.debug("lastName: " + lastName)
          log.debug("passId: " + passId)
          log.debug("al: " + al)
          log.debug("tl: " + tl)
          log.debug("started: " + started)
          log.debug("vsptLevel: " + vsptLevel)
          log.debug("saLevel: " + saLevel)
          log.debug("testLevel: " + testLevel)
        }

        list += ((userId, firstName, lastName, al, tl, vsptLevel, saLevel, testLevel, started))
      }
      passesRS.close()
    } catch {
      case _: Throwable => {
        log.error("Caught exception whilst deleting token.")
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
        } catch { case _ : SQLException => }
      }
    }
    list.toList
  }

  def getLTIUserNames(consumerKey: String) = {

    lazy val conn = ds.getConnection

    lazy val st = conn.prepareStatement("SELECT first_name, last_name, user_id FROM sessions WHERE consumer_key = ?")

    val listBuffer = new ListBuffer[Tuple3[String, String, String]]()

    try {
      st.setString(1, consumerKey)
      val rs = st.executeQuery
      while (rs.next) {
        listBuffer += ((rs.getString("first_name"), rs.getString("last_name"), rs.getString("user_id")))
      }
      rs.close()
    } catch {
      case e: Exception => {
        log.error("Failed to get LTI user names", e)
      }
    } finally {
      if (st != null) {
        try {
          st.close()
        } catch { case _ : SQLException => }
      }

      if (conn != null) {
        try {
          conn.close()
        } catch { case _ : SQLException => }
      }
    }
    listBuffer.toList
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

    log.debug("ageGroup: {}", ageGroup)
    log.debug("gender: {}", gender)
    log.debug("otherGender: {}", otherGender)
    log.debug("firstLanguage: {}", firstLanguage)
    log.debug("nationality: {}", nationality)
    log.debug("institution: {}", institution)
    log.debug("reason: {}", reason)
    log.debug("accuracy: {}", accuracy)
    log.debug("comments: {}", comments)

    lazy val conn = ds.getConnection
    lazy val questionnaireST
      = conn.prepareStatement("INSERT INTO questionnaire VALUES(?,?,?,?,?,?,?,?,?,?)")

    try {
      questionnaireST.setString(1, sessionId)
      questionnaireST.setInt(2, ageGroup.toInt)
      questionnaireST.setString(3, gender)
      questionnaireST.setString(4, otherGender)
      questionnaireST.setString(5, firstLanguage)
      questionnaireST.setString(6, nationality)
      questionnaireST.setString(7, institution)
      questionnaireST.setInt(8, reason.toInt)
      questionnaireST.setInt(9, accuracy.toInt)
      questionnaireST.setString(10, comments)
      questionnaireST.executeUpdate
    } catch {
      case e: Exception => {
        log.error("Caught exception whilst storing questionnaire.", e)
      }
    } finally {
      if (questionnaireST != null) {
        try {
          questionnaireST .close()
        } catch { case _ : SQLException => }
      }

      if (conn != null) {
        try {
          conn.close()
        } catch { case _ : SQLException => }
      }
    }
  }
}
