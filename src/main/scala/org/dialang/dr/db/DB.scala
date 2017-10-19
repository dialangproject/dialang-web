package org.dialang.dr.db

import java.sql.{ResultSet, DriverManager, Connection, Statement, PreparedStatement, SQLException}

import scala.collection.mutable.ListBuffer

import javax.naming.InitialContext
import javax.sql.DataSource

import org.dialang.dr.model._

//import org.dialang.dr.util.DialangLogger
import grizzled.slf4j.Logging

object DB extends Logging {

  private val dialangDS = (new InitialContext).lookup("java:comp/env/jdbc/dialang").asInstanceOf[DataSource];
  private val dialangDCDS = (new InitialContext).lookup("java:comp/env/jdbc/dialangdatacapture").asInstanceOf[DataSource];

  if (dialangDS == null || dialangDCDS == null) {
    throw new IllegalStateException("Data sources not configured")
  }

  def getSecret(consumerKey: String):Option[String] = {

    var conn: Connection = null
    var st: PreparedStatement = null

    try {
      conn = dialangDS.getConnection
      st = conn.prepareStatement("SELECT secret FROM lti_consumers WHERE consumer_key = ?")
      st.setString(1,consumerKey)
      val rs = st.executeQuery
      if (rs.next) {
        Some(rs.getString("secret"))
      } else {
        None
      }
    } catch {
      case t: Throwable => {
        logger.error("Caught exception whilst getting LTI secret for '" + consumerKey + "'. Returning None ...",t)
        None
      }
    } finally {
      if (st != null) {
        try {
          st.close()
        } catch {
          case e: SQLException =>
        }
      }

      if (conn != null) {
        try {
          conn.close()
        } catch {
          case e: SQLException =>
        }
      }
    }
  }

  def getSessionsForUserId(userId: String, consumerKey: String): Option[List[Session]] = {

    var conn: Connection = null
    var pst1: PreparedStatement = null
    var pst2: PreparedStatement = null

    try {

      val sessions = new ListBuffer[Session]

      conn = dialangDCDS.getConnection
      pst1 = conn.prepareStatement("SELECT * FROM sessions where user_id = ? AND consumer_key = ?")

      pst1.setString(1, userId)
      pst1.setString(2, consumerKey)
      val sessionRS = pst1.executeQuery
      while (sessionRS.next) {
        val id = sessionRS.getString("id")
        val ipAddress = sessionRS.getString("ip_address")
        val started = sessionRS.getLong("started")
        val session = new Session(id, ipAddress, started)
        session.passes = getPassesForSessionId(session.id, conn) match {
            case Some(l: List[Pass]) => l
            case None => List[Pass]()
          }
        sessions += session
      }
      sessionRS.close()

      Some(sessions.toList)
    } catch {
      case t: Throwable => {
          logger.error("Error while retrieving sessions for user_id  '" + userId + "' and consumer_key '" + consumerKey + "'",t)
          None
      }
    } finally {
      if (pst1 != null) {
        try {
          pst1.close()
        } catch { case e: SQLException => }
      }
      if (conn != null) {
        try {
          conn.close()
        } catch { case e:SQLException => }
      }
    }
  }

  private def getPassesForSessionId(sessionId:String, conn:Connection): Option[List[Pass]] = {

    var ps:PreparedStatement = null

    try {
      val passes = new ListBuffer[Pass]
      ps = conn.prepareStatement("SELECT * FROM passes where session_id = ?")

      ps.setString(1,sessionId)
      val rs = ps.executeQuery

     while (rs.next) {
      val id = rs.getString("id")
      val al = rs.getString("al")
      val tl = rs.getString("tl")
      val skill = rs.getString("skill")

      val pass = new Pass(id, al, tl, skill)
      pass.vsptResponseData = getVSPTResponseDataForPassId(pass.id, conn)
      pass.saResponseData = getSAResponseDataForPassId(pass.id, conn)
      pass.itemResponseData = getItemResponseDataForPassId(pass.id, conn)
      passes += pass
     }

     rs.close

     Some(passes.toList)
    } catch {
      case t: Throwable => {
          logger.error("Error while retrieving passes for session id  '" + sessionId + "'",t)
          None
      }
    } finally {
      if (ps != null) {
        try {
          ps.close()
        } catch { case e: SQLException => }
      }
    }
  }

  private def getVSPTResponseDataForPassId(passId: String, conn: Connection): Option[VSPTResponseData] = {

    var ps: PreparedStatement = null

    try {
      ps = conn.prepareStatement("SELECT * FROM vsp_test_scores WHERE pass_id = ?")

      ps.setString(1,passId)
      val rs = ps.executeQuery

      if (rs.next) {
        val zScore = rs.getFloat("z_score")
        val mearaScore = rs.getInt("meara_score")
        val level = rs.getString("level")
        val vsptResponseData = new VSPTResponseData(zScore,mearaScore,level)
        Some(vsptResponseData)
      } else {
        None
      }
    } catch {
      case t: Throwable => {
          logger.error("Error while retrieving vspt response data for pass id  '" + passId + "'",t)
          None
      }
    } finally {
      if (ps != null) {
        try {
          ps.close()
        } catch { case e: SQLException => }
      }
    }
  }
  
  private def getSAResponseDataForPassId(passId: String, conn: Connection): Option[SAResponseData] = {

    var ps: PreparedStatement = null

    try {
      ps = conn.prepareStatement("SELECT * FROM sa_scores WHERE pass_id = ?")

      ps.setString(1, passId)
      val rs = ps.executeQuery

      if (rs.next) {
        val ppe = rs.getFloat("ppe")
        val saResponseData = new SAResponseData(ppe)
        Some(saResponseData)
      } else {
        None
      }
    } catch {
      case t: Throwable => {
          logger.error("Error while retrieving sa response data for pass id  '" + passId + "'",t)
          None
      }
    } finally {
      if (ps != null) {
        try {
          ps.close()
        } catch { case e:SQLException => }
      }
    }
  }
  
  private def getItemResponseDataForPassId(passId: String, conn: Connection): Option[ItemResponseData] = {

    var ps: PreparedStatement = null

    try {
      ps = conn.prepareStatement("SELECT * FROM test_results,test_durations WHERE test_results.pass_id = ? AND test_results.pass_id = test_durations.pass_id")

      ps.setString(1,passId)
      val rs = ps.executeQuery

      if (rs.next) {
        val grade = rs.getInt("grade")
        val level = rs.getString("level")
        val start = rs.getLong("start")
        val finish = rs.getLong("finish")
        val itemResponseData = new ItemResponseData(grade,level,start,finish)
        Some(itemResponseData)
      } else {
        None
      }
    } catch {
      case t: Throwable => {
          logger.error("Error while retrieving item response data for pass id  '" + passId + "'",t)
          None
      }
    } finally {
      if (ps != null) {
        try {
          ps.close()
        } catch { case e:SQLException => }
      }
    }
  }
}
