package org.dialang.web.datacapture

import javax.naming.InitialContext
import javax.sql.DataSource

import java.sql.{Connection,Statement,PreparedStatement,ResultSet,SQLException}
import java.util.Date

import org.dialang.web.model.DialangSession

class DataCaptureImpl {

  val ctx = new InitialContext
  val ds = ctx.lookup("java:comp/env/jdbc/dialangdatacapture").asInstanceOf[DataSource]

  def createSession(dialangSession:DialangSession,ipAddress:String) {

    var conn:Connection = null
    var st:PreparedStatement = null

    try {
      conn = ds.getConnection
      st = conn.prepareStatement("INSERT INTO sessions (session_id,user_id,consumer_key,al,tl,skill,ip_address,started) VALUES(?,?,?,?,?,?,?,?)")
      st.setString(1,dialangSession.sessionId)
      st.setString(2,dialangSession.userId)
      st.setString(3,dialangSession.consumerKey)
      st.setString(4,dialangSession.adminLanguage)
      st.setString(5,dialangSession.testLanguage)
      st.setString(6,dialangSession.skill)
      st.setString(7,ipAddress)
      st.setLong(8,new Date().getTime)
      if(st.executeUpdate != 1) {
        // TODO: LOGGING
      }
    } catch {
      case e:Exception => {
        // TODO: LOGGING
        e.printStackTrace
      }
    }finally {

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

    var conn:Connection = null
    var st:PreparedStatement = null

    try {
      conn = ds.getConnection

      // Insert this session's vspt responses in a transaction
      conn.setAutoCommit(false)

      st = conn.prepareStatement("INSERT INTO vsp_test_responses (session_id,word_id,response) VALUES(?,?,?)")

      st.setString(1,dialangSession.sessionId)
      responses.foreach(t => {
        st.setString(2,t._1)
        st.setBoolean(3,t._2)
        if(st.executeUpdate != 1) {
          // TODO: LOGGING
        }
      })

      conn.commit
    } catch {
      case e:Exception => {
        // TODO: LOGGING
        e.printStackTrace
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

    var conn:Connection = null
    var st:PreparedStatement = null

    try {
      conn = ds.getConnection

      // Insert this session's vspt responses in a transaction
      conn.setAutoCommit(false)

      // Now insert the scores
      st = conn.prepareStatement("INSERT INTO vsp_test_scores (session_id,z_score,meara_score,level) VALUES(?,?,?,?)")
      st.setString(1,dialangSession.sessionId)
      st.setDouble(2,dialangSession.vsptZScore)
      st.setInt(3,dialangSession.vsptMearaScore)
      st.setString(4,dialangSession.vsptLevel)
      if(st.executeUpdate != 1) {
        // TODO: LOGGING
      }
      conn.commit
    } catch {
      case e:Exception => {
        // TODO: LOGGING
        e.printStackTrace
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

  def logSAResponses(dialangSession:DialangSession, responses: Map[String,Boolean]) {

    var conn:Connection = null
    var st:PreparedStatement = null

    try {
      conn = ds.getConnection

      // Insert this session's vspt responses in a transaction
      conn.setAutoCommit(false)

      st = conn.prepareStatement("INSERT INTO sa_responses (session_id,statement_id,response) VALUES(?,?,?)")

      st.setString(1,dialangSession.sessionId)
      responses.foreach(t => {
        st.setString(2,t._1)
        st.setBoolean(3,t._2)
        if(st.executeUpdate != 1) {
          // TODO: LOGGING
        }
      })

      conn.commit
    } catch {
      case e:Exception => {
        // TODO: LOGGING
        e.printStackTrace
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

    var conn:Connection = null
    var st:PreparedStatement = null

    try {
      conn = ds.getConnection

      // Insert this session's vspt responses in a transaction
      conn.setAutoCommit(false)

      // Now insert the scores
      st = conn.prepareStatement("INSERT INTO sa_ppe (session_id,ppe) VALUES(?,?)")
      st.setString(1,dialangSession.sessionId)
      st.setDouble(2,dialangSession.saPPE)
      if(st.executeUpdate != 1) {
        // TODO: LOGGING
      }
      conn.commit
    } catch {
      case e:Exception => {
        // TODO: LOGGING
        e.printStackTrace
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

  def logSingleIdResponse(sessionId: String, basketId: Int, itemId: Int, answerId: Int) {

    var conn:Connection = null
    var st:PreparedStatement = null

    try {
      conn = ds.getConnection
      st = conn.prepareStatement("INSERT INTO item_responses (session_id,basket_id,item_id,answer_id) VALUES(?,?,?,?)")
      st.setString(1,sessionId)
      st.setInt(2,basketId)
      st.setInt(3,itemId)
      st.setInt(4,answerId)
      if(st.executeUpdate != 1) {
        // TODO: LOGGING
      }
    } catch {
      case e:Exception => {
        // TODO: LOGGING
        e.printStackTrace
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

  def logMultipleTextualResponses(sessionId: String, basketId: Int, responses: Map[Int,String]) {

    var conn:Connection = null
    var st:PreparedStatement = null

    try {
      conn = ds.getConnection
      st = conn.prepareStatement("INSERT INTO item_responses (session_id,basket_id,item_id,answer_text) VALUES(?,?,?,?)")
      st.setString(1,sessionId)
      st.setInt(2,basketId)

      responses.foreach(t => {
        st.setInt(3,t._1)
        st.setString(4,t._2)
        if(st.executeUpdate != 1) {
          // TODO: LOGGING
        }
      })
    } catch {
      case e:Exception => {
        // TODO: LOGGING
        e.printStackTrace
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

  def logMultipleIdResponses(sessionId: String, basketId: Int, responses: Map[Int,Int]) {

    var conn:Connection = null
    var st:PreparedStatement = null

    try {
      conn = ds.getConnection
      st = conn.prepareStatement("INSERT INTO item_responses (session_id,basket_id,item_id,answer_id) VALUES(?,?,?,?)")
      st.setString(1,sessionId)
      st.setInt(2,basketId)

      responses.foreach(t => {
        st.setInt(3,t._1)
        st.setInt(4,t._2)
        if(st.executeUpdate != 1) {
          // TODO: LOGGING
        }
      })
    } catch {
      case e:Exception => {
        // TODO: LOGGING
        e.printStackTrace
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

    var conn:Connection = null
    var st:PreparedStatement = null

    try {
      conn = ds.getConnection

      // Insert this session's vspt responses in a transaction
      conn.setAutoCommit(false)

      // Now insert the scores
      st = conn.prepareStatement("INSERT INTO test_results (session_id,grade,level) VALUES(?,?,?)")
      st.setString(1,dialangSession.sessionId)
      st.setInt(2,dialangSession.itemGrade)
      st.setString(3,dialangSession.itemLevel)
      if(st.executeUpdate != 1) {
        // TODO: LOGGING
      }
      conn.commit
    } catch {
      case e:Exception => {
        // TODO: LOGGING
        e.printStackTrace
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
}
