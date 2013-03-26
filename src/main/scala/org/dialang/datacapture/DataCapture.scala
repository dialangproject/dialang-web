package org.dialang.datacapture

import javax.naming.InitialContext
import javax.sql.DataSource

import java.sql.{Connection,Statement,PreparedStatement,ResultSet,SQLException}
import java.util.Date

class DataCapture {

  val ctx = new InitialContext
  val ds = ctx.lookup("java:comp/env/jdbc/dialangdatacapture").asInstanceOf[DataSource]

  @throws[Exception]
  def createSession(userId:String, al:String, tl:String, skill:String, ipAddress:String) = {

    var conn:Connection = null
    var st:PreparedStatement = null
    var lastIdSt:Statement = null
    var lastIdRS:ResultSet = null

    try {
      conn = ds.getConnection
      lastIdSt = conn.createStatement
      st = conn.prepareStatement("INSERT INTO sessions (user_id,al,tl,skill,ip_address,started) VALUES(?,?,?,?,?,?)")
      st.setString(1,userId)
      st.setString(2,al)
      st.setString(3,tl)
      st.setString(4,skill)
      st.setString(5,ipAddress)
      st.setLong(6,new Date().getTime)
      if(st.executeUpdate == 1) {
        lastIdRS = lastIdSt.executeQuery("SELECT currval('sessions_session_id_seq')")
        if(lastIdRS.next()) {
          lastIdRS.getInt(1)
        } else {
          throw new Exception("Failed to create data capture session")
        }
      } else {
        throw new Exception("Failed to create data capture session")
      }
    } finally {
      if(lastIdRS != null) {
        try {
          lastIdRS.close
        } catch { case _ : SQLException => }
      }

      if(lastIdSt != null) {
        try {
          lastIdSt.close()
        } catch { case _ : SQLException => }
      }

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

  def logVSPTResponsesAndScores(sessionId: Int, responses: Map[String,Boolean],zScore: Double,mearaScore: Int,level: String) {

    var conn:Connection = null
    var st1:PreparedStatement = null
    var st2:PreparedStatement = null

    try {
      conn = ds.getConnection

      // Insert this session's vspt responses in a transaction
      conn.setAutoCommit(false)

      st1 = conn.prepareStatement("INSERT INTO vsp_test_responses (session_id,word_id,response) VALUES(?,?,?)")


      st1.setInt(1,sessionId)
      responses.foreach(t => {
        st1.setString(2,t._1)
        st1.setBoolean(3,t._2)
        if(st1.executeUpdate != 1) {
          // TODO: LOGGING
        }
      })

      conn.commit

      // Now insert the scores
      st2 = conn.prepareStatement("INSERT INTO vsp_test_scores (session_id,z_score,meara_score,level) VALUES(?,?,?,?)")
      st2.setInt(1,sessionId)
      st2.setDouble(2,zScore)
      st2.setInt(3,mearaScore)
      st2.setString(4,level)
      if(st2.executeUpdate != 1) {
        // TODO: LOGGING
      }
      conn.commit
    } catch {
      case e:Exception => {
        // TODO: LOGGING
        e.printStackTrace
      }
    } finally {
      if(st1 != null) {
        try {
          st1.close()
        } catch { case _ : SQLException => }
      }

      if(st2 != null) {
        try {
          st2.close()
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

  def logSAResponsesAndPPE(sessionId: Int, responses: Map[String,Boolean],ppe: Float) {

    var conn:Connection = null
    var st1:PreparedStatement = null
    var st2:PreparedStatement = null

    try {
      conn = ds.getConnection

      // Insert this session's vspt responses in a transaction
      conn.setAutoCommit(false)

      st1 = conn.prepareStatement("INSERT INTO sa_responses (session_id,statement_id,response) VALUES(?,?,?)")

      st1.setInt(1,sessionId)
      responses.foreach(t => {
        st1.setString(2,t._1)
        st1.setBoolean(3,t._2)
        if(st1.executeUpdate != 1) {
          // TODO: LOGGING
        }
      })

      conn.commit

      // Now insert the scores
      st2 = conn.prepareStatement("INSERT INTO sa_ppe (session_id,ppe) VALUES(?,?)")
      st2.setInt(1,sessionId)
      st2.setDouble(2,ppe)
      if(st2.executeUpdate != 1) {
        // TODO: LOGGING
      }
      conn.commit
    } catch {
      case e:Exception => {
        // TODO: LOGGING
        e.printStackTrace
      }
    } finally {
      if(st1 != null) {
        try {
          st1.close()
        } catch { case _ : SQLException => }
      }

      if(st2 != null) {
        try {
          st2.close()
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
