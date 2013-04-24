package org.dialang.datacapture

import javax.naming.InitialContext
import javax.sql.DataSource

import java.sql.{Connection,Statement,PreparedStatement,ResultSet,SQLException}
import java.util.Date

class DataCaptureImpl {

  val ctx = new InitialContext
  val ds = ctx.lookup("java:comp/env/jdbc/dialangdatacapture").asInstanceOf[DataSource]

  def createSession(sessionId:String, userId:String, consumerKey:String, al:String, tl:String, skill:String, ipAddress:String) {

    var conn:Connection = null
    var st:PreparedStatement = null

    try {
      conn = ds.getConnection
      st = conn.prepareStatement("INSERT INTO sessions (session_id,user_id,consumer_key,al,tl,skill,ip_address,started) VALUES(?,?,?,?,?,?,?,?)")
      st.setString(1,sessionId)
      st.setString(2,userId)
      st.setString(3,consumerKey)
      st.setString(4,al)
      st.setString(5,tl)
      st.setString(6,skill)
      st.setString(7,ipAddress)
      st.setLong(8,new Date().getTime)
      if(st.executeUpdate != 1) {
        // TODO: LOGGING
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

  def logVSPTResponsesAndScores(sessionId: String, responses: Map[String,Boolean],zScore: Double,mearaScore: Int,level: String) {

    var conn:Connection = null
    var st1:PreparedStatement = null
    var st2:PreparedStatement = null

    try {
      conn = ds.getConnection

      // Insert this session's vspt responses in a transaction
      conn.setAutoCommit(false)

      st1 = conn.prepareStatement("INSERT INTO vsp_test_responses (session_id,word_id,response) VALUES(?,?,?)")

      st1.setString(1,sessionId)
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
      st2.setString(1,sessionId)
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

  def logSAResponsesAndPPE(sessionId: String, responses: Map[String,Boolean],ppe: Float) {

    var conn:Connection = null
    var st1:PreparedStatement = null
    var st2:PreparedStatement = null

    try {
      conn = ds.getConnection

      // Insert this session's vspt responses in a transaction
      conn.setAutoCommit(false)

      st1 = conn.prepareStatement("INSERT INTO sa_responses (session_id,statement_id,response) VALUES(?,?,?)")

      st1.setString(1,sessionId)
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
      st2.setString(1,sessionId)
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
