package org.dialang.web.datacapture

import java.sql.{Connection,Statement,PreparedStatement,ResultSet,SQLException}
import java.util.Date
import javax.naming.InitialContext
import javax.sql.DataSource

import org.dialang.web.model.DialangSession

import org.slf4j.LoggerFactory

class DataCaptureImpl(dsUrl: String) {

  private val logger = LoggerFactory.getLogger(classOf[DataCaptureImpl])

  val ctx = new InitialContext
  val ds = ctx.lookup(dsUrl).asInstanceOf[DataSource]

  /*
  def createSession(dialangSession:DialangSession) {

    lazy val conn = ds.getConnection
    lazy val st = conn.prepareStatement("INSERT INTO sessions (id,user_id,consumer_key,ip_address,started) VALUES(?,?,?,?,?)")

    try {
      st.setString(1,dialangSession.sessionId)
      st.setString(2,dialangSession.userId)
      st.setString(3,dialangSession.consumerKey)
      st.setString(4,dialangSession.ipAddress)
      st.setLong(5,dialangSession.started)
      if(st.executeUpdate != 1) {
        logger.error("Failed to log session creation.")
      }
    } catch {
      case e:Exception => {
        logger.error("Caught exception whilst create session", e)
      }
    } finally {

      if(st != null) {
        try {
          st.close()
        } catch { case _ : SQLException => }
      }

      if(conn != null) {
        try {
          conn.close()
        } catch { case _ : SQLException => }
      }
    }
  }
  */

  def createSessionAndPass(dialangSession:DialangSession) {

    lazy val conn = ds.getConnection
    lazy val sessionST = conn.prepareStatement("INSERT INTO sessions (id,user_id,consumer_key,ip_address,started) VALUES(?,?,?,?,?)")
    lazy val passST = conn.prepareStatement("INSERT INTO passes (id,session_id,al,tl,skill,started) VALUES(?,?,?,?,?,?)")

    try {

      conn.setAutoCommit(false)

      sessionST.setString(1,dialangSession.sessionId)
      sessionST.setString(2,dialangSession.userId)
      sessionST.setString(3,dialangSession.consumerKey)
      sessionST.setString(4,dialangSession.ipAddress)
      sessionST.setLong(5,dialangSession.started)
      if(sessionST.executeUpdate != 1) {
        logger.error("Failed to insert session.")
      }

      passST.setString(1,dialangSession.passId)
      passST.setString(2,dialangSession.sessionId)
      passST.setString(3,dialangSession.adminLanguage)
      passST.setString(4,dialangSession.testLanguage)
      passST.setString(5,dialangSession.skill)
      passST.setLong(6,dialangSession.started)
      if(passST.executeUpdate != 1) {
        logger.error("Failed to log pass creation.")
      }

      conn.commit()
    } catch {
      case e:Exception => {
        logger.error("Caught exception whilst creating session and pass.", e)
        conn.rollback()
      }
    } finally {

      if(sessionST != null) {
        try {
          sessionST.close()
        } catch { case _ : SQLException => }
      }

      if(passST != null) {
        try {
          passST.close()
        } catch { case _ : SQLException => }
      }

      if(conn != null) {
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
      if(rs.next) {
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

      if(st != null) {
        try {
          st.close()
        } catch { case _ : SQLException => }
      }

      if(conn != null) {
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
      if(rs.next) {
        val dialangSession = new DialangSession
        dialangSession.passId = passId
        dialangSession.adminLanguage = rs.getString("al")
        dialangSession.testLanguage = rs.getString("tl")
        dialangSession.skill = rs.getString("skill")
        Some(dialangSession)
      } else {
        None
      }
    } finally {

      if(st != null) {
        try {
          st.close()
        } catch { case _ : SQLException => }
      }

      if(conn != null) {
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
      st.setString(3,dialangSession.adminLanguage)
      st.setString(4,dialangSession.testLanguage)
      st.setString(5,dialangSession.skill)
      st.setLong(6,dialangSession.started)
      if(st.executeUpdate != 1) {
        logger.error("Failed to log pass creation.")
      }
    } catch {
      case e:Exception => {
        logger.error("Caught exception whilst create pass", e)
      }
    } finally {

      if(st != null) {
        try {
          st.close()
        } catch { case _ : SQLException => }
      }

      if(conn != null) {
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

      st.setString(1,dialangSession.passId)
      responses.foreach(t => {
        st.setString(2,t._1)
        st.setBoolean(3,t._2)
        if(st.executeUpdate != 1) {
          logger.error("Failed to log vspt word response.")
        }
      })

      conn.commit
    } catch {
      case e:Exception => {
        logger.error("Caught exception whilst logging VSPT responses", e)
      }
    } finally {
      if(st != null) {
        try {
          st.close()
        } catch { case _ : SQLException => }
      }

      if(conn != null) {
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
      if(st.executeUpdate != 1) {
        logger.error("Failed to log vspt scores.")
      }
    } catch {
      case e:Exception => {
        logger.error("Caught exception whilst logging VSPT scores", e)
      }
    } finally {

      if(st != null) {
        try {
          st.close()
        } catch { case _ : SQLException => }
      }

      if(conn != null) {
        try {
          conn.close()
        } catch { case _ : SQLException => }
      }

    }
  }

  def logSAResponses(dialangSession:DialangSession, responses: Map[String,Boolean]) {

    lazy val conn = ds.getConnection
    lazy val st = conn.prepareStatement("INSERT INTO sa_responses (pass_id,statement_id,response) VALUES(?,?,?)")

    try {

      // Insert this pass's sa responses in a transaction
      conn.setAutoCommit(false)

      st.setString(1,dialangSession.passId)
      responses.foreach(t => {
        st.setString(2,t._1)
        st.setBoolean(3,t._2)
        if(st.executeUpdate != 1) {
          logger.error("Failed to log sa response.")
        }
      })

      conn.commit
    } catch {
      case e:Exception => {
        logger.error("Caught exception whilst logging SA responses", e)
      }
    } finally {

      if(st != null) {
        try {
          st.close()
        } catch { case _ : SQLException => }
      }

      if(conn != null) {
        try {
          conn.setAutoCommit(true)
          conn.close()
        } catch { case _ : SQLException => }
      }
    }
  }

  def logSAPPE(dialangSession:DialangSession) {

    lazy val conn = ds.getConnection
    lazy val st = conn.prepareStatement("INSERT INTO sa_ppe (pass_id,ppe) VALUES(?,?)")

    try {

      // Now insert the scores
      st.setString(1,dialangSession.passId)
      st.setDouble(2,dialangSession.saPPE)
      if(st.executeUpdate != 1) {
        logger.error("Failed to log SA PPE.")
      }
    } catch {
      case e:Exception => {
        logger.error("Caught exception whilst logging SA PPE", e)
      }
    } finally {
      if(st != null) {
        try {
          st.close()
        } catch { case _ : SQLException => }
      }

      if(conn != null) {
        try {
          conn.close()
        } catch { case _ : SQLException => }
      }
    }
  }

  def logTestStart(passId:String) {

    lazy val conn = ds.getConnection
    lazy val st = conn.prepareStatement("INSERT INTO test_durations (pass_id,start) VALUES(?,?)")

    try {

      st.setString(1,passId)
      st.setLong(2,((new Date()).getTime()) / 1000L)
      if(st.executeUpdate != 1) {
        logger.error("Failed to log test start time.")
      }
    } catch {
      case e:Exception => {
        logger.error("Caught exception whilst logging test start time", e)
      }
    } finally {

      if(st != null) {
        try {
          st.close()
        } catch { case _ : SQLException => }
      }

      if(conn != null) {
        try {
          conn.close()
        } catch { case _ : SQLException => }
      }
    }
  }

  def logSingleIdResponse(passId: String, basketId: Int, itemId: Int, answerId: Int) {

    if(logger.isDebugEnabled()) {
      logger.debug("PASS ID: " + passId + ". BASKET ID: " + basketId + ". ITEM ID: " + itemId + ". ANSWER ID: " + answerId)
    }

    lazy val conn = ds.getConnection
    lazy val st = conn.prepareStatement("INSERT INTO item_responses (pass_id,basket_id,item_id,answer_id) VALUES(?,?,?,?)")

    try {
      st.setString(1,passId)
      st.setInt(2,basketId)
      st.setInt(3,itemId)
      st.setInt(4,answerId)
      if(st.executeUpdate != 1) {
        logger.error("Failed to log single id response.")
      }
    } catch {
      case e:Exception => {
        logger.error("Caught exception whilst logging single id response.", e)
      }
    } finally {

      if(st != null) {
        try {
          st.close()
        } catch { case _ : SQLException => }
      }

      if(conn != null) {
        try {
          conn.close()
        } catch { case _ : SQLException => }
      }
    }
  }

  def logMultipleTextualResponses(passId: String, basketId: Int, responses: Map[Int,String]) {

    lazy val conn = ds.getConnection
    lazy val st:PreparedStatement = conn.prepareStatement("INSERT INTO item_responses (pass_id,basket_id,item_id,answer_text) VALUES(?,?,?,?)")

    try {

      st.setString(1,passId)
      st.setInt(2,basketId)

      responses.foreach(t => {
        st.setInt(3,t._1)
        st.setString(4,t._2)
        if(st.executeUpdate != 1) {
          logger.error("Failed to log textual response.")
        }
      })
    } catch {
      case e:Exception => {
        logger.error("Caught exception whilst logging multiple textual response.", e)
      }
    } finally {

      if(st != null) {
        try {
          st.close()
        } catch { case _ : SQLException => }
      }

      if(conn != null) {
        try {
          conn.close()
        } catch { case _ : SQLException => }
      }
    }
  }

  def logMultipleIdResponses(passId: String, basketId: Int, responses: Map[Int,Int]) {

    if(logger.isDebugEnabled()) {
      logger.debug("PASS ID: " + passId + ". BASKET ID: " + basketId)
    }

    lazy val conn = ds.getConnection
    lazy val st = conn.prepareStatement("INSERT INTO item_responses (pass_id,basket_id,item_id,answer_id) VALUES(?,?,?,?)")

    try {

      st.setString(1,passId)
      st.setInt(2,basketId)

      responses.foreach(t => {
        st.setInt(3,t._1)
        st.setInt(4,t._2)
        if(st.executeUpdate != 1) {
          logger.error("Failed to log id response.")
        }
      })
    } catch {
      case e:Exception => {
        logger.error("Caught exception whilst logging multiple id response.", e)
      }
    } finally {

      if(st != null) {
        try {
          st.close()
        } catch { case _ : SQLException => }
      }

      if(conn != null) {
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
      if(st.executeUpdate != 1) {
        logger.error("Failed to log test result.")
      }
    } catch {
      case e:Exception => {
        logger.error("Caught exception whilst logging test result.", e)
      }
    } finally {

      if(st != null) {
        try {
          st.close()
        } catch { case _ : SQLException => }
      }

      if(conn != null) {
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
      if(st.executeUpdate != 1) {
        logger.error("Failed to log test finish time.")
      }
    } catch {
      case e:Exception => {
        logger.error("Caught exception whilst logging test finish time.", e)
      }
    } finally {

      if(st != null) {
        try {
          st.close()
        } catch { case _ : SQLException => }
      }

      if(conn != null) {
        try {
          conn.close()
        } catch { case _ : SQLException => }
      }
    }
  }
}
