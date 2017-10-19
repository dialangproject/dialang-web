package org.dialang.web.datacapture

import java.sql.{Connection, Statement, PreparedStatement, ResultSet, SQLException}
import javax.naming.InitialContext
import javax.sql.DataSource
import java.util.UUID

import org.dialang.common.model.DialangSession

import grizzled.slf4j.Logging

class Saver(dsUrl: String) extends Logging {

  val ctx = new InitialContext
  val ds = ctx.lookup(dsUrl).asInstanceOf[DataSource]

  def save(dialangSession: DialangSession): Option[String] = {

    val token = UUID.randomUUID.toString

    lazy val conn = ds.getConnection
    lazy val tokenST = conn.prepareStatement(
      "INSERT INTO tokens (token,pass_id,current_basket,vspt_skipped,sa_skipped) VALUES(?,?,?,?,?)")

    try {
      tokenST.setString(1, token)
      tokenST.setString(2, dialangSession.passId)
      tokenST.setInt(3, dialangSession.currentBasketNumber)
      tokenST.setBoolean(4, dialangSession.vsptSkipped)
      tokenST.setBoolean(5, dialangSession.saSkipped)
      if (tokenST.executeUpdate == 1) {
        Some(token)
      } else {
        None
      }
    } catch {
      case e: Exception => {
        logger.error("Caught exception whilst creating pass token.", e)
        None
      }
    } finally {
      if (tokenST != null) {
        try {
          tokenST.close()
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
