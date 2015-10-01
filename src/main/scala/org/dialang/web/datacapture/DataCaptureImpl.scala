package org.dialang.web.datacapture

import java.sql.{Connection,Statement,PreparedStatement,ResultSet,SQLException}
import java.util.Date
import javax.naming.InitialContext
import javax.sql.DataSource

import net.oauth.OAuth

import org.dialang.common.model.ImmutableItem
import org.dialang.web.model.DialangSession

import scala.collection.mutable.ListBuffer

import org.slf4j.LoggerFactory

class DataCaptureImpl(dsUrl: String) {

  private val logger = LoggerFactory.getLogger(classOf[DataCaptureImpl])

  private val itemResponseSql =
    """INSERT INTO item_responses (pass_id,basket_id,item_id,answer_id,score,correct,pass_order)
          VALUES(?,?,?,?,?,?,?)"""

  private val basketSql = "INSERT INTO baskets (pass_id,basket_id,basket_number) VALUES(?,?,?)"

  val ctx = new InitialContext
  val ds = ctx.lookup(dsUrl).asInstanceOf[DataSource]

  def createSessionAndPass(dialangSession:DialangSession) {

    lazy val conn = ds.getConnection
    lazy val sessionST
      = conn.prepareStatement("INSERT INTO sessions (id,user_id,consumer_key,ip_address,started) VALUES(?,?,?,?,?)")
    lazy val passST
      = conn.prepareStatement("INSERT INTO passes (id,session_id,al,tl,skill,started) VALUES(?,?,?,?,?,?)")

    try {

      conn.setAutoCommit(false)

      sessionST.setString(1, dialangSession.sessionId)
      sessionST.setString(2, dialangSession.userId)
      sessionST.setString(3, dialangSession.consumerKey)
      sessionST.setString(4, dialangSession.ipAddress)
      sessionST.setLong(5, dialangSession.started)
      if (sessionST.executeUpdate != 1) {
        logger.error("Failed to insert session.")
      }

      passST.setString(1, dialangSession.passId)
      passST.setString(2, dialangSession.sessionId)
      passST.setString(3, dialangSession.tes.al)
      passST.setString(4, dialangSession.tes.tl)
      passST.setString(5, dialangSession.tes.skill)
      passST.setLong(6, dialangSession.started)
      if (passST.executeUpdate != 1) {
        logger.error("Failed to log pass creation.")
      }

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
        } catch { case _ : SQLException => }
      }

      if (passST != null) {
        try {
          passST.close()
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

  def createPass(dialangSession: DialangSession) {

    lazy val conn = ds.getConnection
    lazy val st = conn.prepareStatement("INSERT INTO passes (id,session_id,al,tl,skill,started) VALUES(?,?,?,?,?,?)")

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
        logger.error("Caught exception whilst create pass", e)
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

    if (logger.isDebugEnabled()) {
      logger.debug("logBasket(" + passId + "," + basketId + "," + basketNumber + ")")
    }

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

    if (logger.isDebugEnabled()) {
      logger.debug("PASS ID: " + passId + ". BASKET ID: " + item.basketId
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

    if (logger.isDebugEnabled()) {
      logger.debug("PASS ID: " + passId + ". BASKET ID: " + items.last.basketId)
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
        } catch { case _ : SQLException => }
      }

      if (conn != null) {
        try {
          conn.close()
        } catch { case _ : SQLException => }
      }
    }
  }

  def getScores(consumerKey: String, fromDate: String, toDate: String) = {

    lazy val conn = ds.getConnection

    val vsptQuery = "SELECT level FROM vsp_test_scores WHERE pass_id = ?"

    lazy val vsptST = conn.prepareStatement(vsptQuery)

    val saQuery = "SELECT level FROM sa_scores WHERE pass_id = ?"

    lazy val saST = conn.prepareStatement(saQuery)

    val testQuery = "SELECT level FROM test_results WHERE pass_id = ?"

    lazy val testST = conn.prepareStatement(testQuery)

    var passesQuery = 
      """SELECT s.user_id,p.*,s.ip_address FROM sessions as s, passes as p
            WHERE s.id = p.session_id
              AND s.consumer_key = ?"""

    if (fromDate != "") {
      passesQuery += "AND p.started > ?"
    }

    if (toDate != "") {
      passesQuery += "AND p.started < ?"
    }

    lazy val passesST = conn.prepareStatement(passesQuery)

    val list = new ListBuffer[Tuple5[String, String, String, String, String]]

    try {
      passesST.setString(1, consumerKey)
      if (fromDate != "") {
        passesST.setDouble(2, fromDate.toDouble)
      }
      if (toDate != "") {
        passesST.setDouble(3, toDate.toDouble)
      }
      val passesRS = passesST.executeQuery

      while (passesRS.next) {
        val userId = passesRS.getString("user_id")
        logger.debug("userId: " + userId)

        val passId = passesRS.getString("id")
        logger.debug("passId: " + passId)

        val vsptLevel = {
            vsptST.setString(1, passId)
            val vsptRS = vsptST.executeQuery
            if (vsptRS.next) {
              vsptRS.getString("level")
            } else {
              ""
            }
          }

        logger.debug("vsptLevel: " + vsptLevel)

        val saLevel = {
            saST.setString(1, passId)
            val saRS = saST.executeQuery
            if (saRS.next) {
              saRS.getString("level")
            } else {
              ""
            }
          }

        logger.debug("saLevel: " + saLevel)

        val testLevel = {
            testST.setString(1, passId)
            val testRS = testST.executeQuery
            if (testRS.next) {
              testRS.getString("level")
            } else {
              ""
            }
          }

        logger.debug("testLevel: " + testLevel)

        /*
        list += Map("passId" -> passId,
                      "userId" -> userId,
                      "vsptLevel" -> vsptLevel,
                      "saLevel" -> saLevel,
                      "testLevel" -> testLevel)
        */

        list += ((passId, userId, vsptLevel, saLevel, testLevel))
      }
      passesRS.close()
    } catch {
      case _: Throwable => {
        logger.error("Caught exception whilst deleting token.")
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
}
