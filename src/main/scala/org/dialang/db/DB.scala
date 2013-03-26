package org.dialang.db

import java.sql.{DriverManager,Connection,Statement,PreparedStatement,SQLException}

import scala.collection.JavaConversions._
import scala.collection.mutable.{ListBuffer,HashMap,ArrayBuffer}

import org.dialang.model._

import javax.naming.InitialContext
import javax.sql.DataSource

object DB {

  val ctx = new InitialContext
  val ds = ctx.lookup("java:comp/env/jdbc/dialang").asInstanceOf[DataSource];

  def getVSPTWords(tl:String): List[Tuple4[String,String,Boolean,Int]] = {

    val tuples = new ListBuffer[Tuple4[String,String,Boolean,Int]]

    var conn:Connection = null
    var st:Statement = null
    try {
      conn = ds.getConnection
      st = conn.createStatement
      val rs = st.executeQuery("SELECT words.word_id,words.word,words.valid,words.weight FROM vsp_test_word,words WHERE locale = '" + tl + "' AND vsp_test_word.word_id = words.word_id")
      while(rs.next) {
        val id = rs.getString("WORD_ID")
        val word = rs.getString("WORD")
        val valid = rs.getBoolean("VALID")
        val weight = rs.getInt("WEIGHT")
        tuples += ((id,word,valid,weight))
      }
      rs.close
    } finally {
      if(st != null) {
        try {
          st.close
        } catch {
          case e:SQLException =>
        }
      }

      if(conn != null) {
        try {
          conn.close
        } catch {
          case e:SQLException =>
        }
      }
    }

    tuples.toList
  }

  def getPreestAssign = {
    var conn:Connection = null
    var st:Statement = null
    try {
      conn = ds.getConnection
      st = conn.createStatement
      val rs = st.executeQuery("SELECT * FROM preest_assignments")

      val temp = new HashMap[String,ArrayBuffer[(Float,Int)]]

      while(rs.next) {
        val tl = rs.getString("tl")
        val skill = rs.getString("skill")
        val pe = rs.getFloat("pe")
        val bookletId = rs.getInt("booklet_id")
        val key = tl + "#" + skill
        if(!temp.contains(key)) {
          temp += (key -> new ArrayBuffer[(Float,Int)])
        }
        val array = temp.get(key).get
        array += ((pe,bookletId))
      }

      rs.close

      val assign = new HashMap[String,Vector[(Float,Int)]]

      val ordering = Ordering.by[(Float,Int), Float](_._1)

      temp.foreach(t => {
        assign += (t._1 -> t._2.sortWith(_._1 < _._1).toVector)
      })

      new PreestAssign(assign.toMap)
    } finally {
      if(st != null) {
        try {
          st.close()
        } catch {
          case e:SQLException =>
        }
      }

      if(conn != null) {
        try {
          conn.close()
        } catch {
          case e:SQLException =>
        }
      }
    }
  }

  def getPreestWeights = {
    var conn:Connection = null
    var st:Statement = null
    try {
      conn = ds.getConnection
      st = conn.createStatement
      val rs = st.executeQuery("SELECT * FROM preest_weights")

      val preestWeights = new PreestWeights

      while(rs.next) {
        preestWeights.add( rs.getString("tl")
                          ,rs.getString("skill")
                          ,rs.getBoolean("vspttaken")
                          ,rs.getBoolean("sataken")
                          ,rs.getFloat("vspt")
                          ,rs.getFloat("sa")
                          ,rs.getFloat("coe") )
      }

      rs.close

      preestWeights
    } finally {
      if(st != null) {
        try {
          st.close()
        } catch {
          case e:SQLException =>
        }
      }

      if(conn != null) {
        try {
          conn.close()
        } catch {
          case e:SQLException =>
        }
      }
    }
  }

  def getVSPTLevels:Map[String,Vector[(String,Int,Int)]] = {
    var conn:Connection = null
    var st:Statement = null
    try {
      conn = ds.getConnection
      st = conn.createStatement
      var rs = st.executeQuery("SELECT * FROM vsp_levels")
      val temp = new HashMap[String,ArrayBuffer[(String,Int,Int)]]
      while(rs.next) {
        val locale = rs.getString("locale")
        val level = rs.getString("level")
        val low = rs.getInt("low")
        val high = rs.getInt("high")
        if(!temp.contains(locale)) {
          temp += (locale -> new ArrayBuffer[(String,Int,Int)])
        }
        val list = temp.get(locale).get
        list += ((level,low,high))
      }

      rs.close

      // Make it all immutable and return it
      val levels = new HashMap[String,Vector[(String,Int,Int)]]
      temp.foreach(t => levels += (t._1 -> t._2.toVector))
      levels.toMap
    } finally {
      if(st != null) {
        try {
          st.close()
        } catch {
          case e:SQLException =>
        }
      }

      if(conn != null) {
        try {
          conn.close()
        } catch {
          case e:SQLException =>
        }
      }
    }
  }

  /**
   * Returns a Map[SKILL[ID: WEIGHT]] with the sa statement weights
   */
  def getSAWeights:Map[String,HashMap[String,Int]] = {
    var conn:Connection = null
    var st:Statement = null
    try {
      conn = ds.getConnection
      val skillMap = new HashMap[String,HashMap[String,Int]]
      st = conn.createStatement
      val rs = st.executeQuery("SELECT * FROM sa_weights")
      while(rs.next) {
        val skill = rs.getString("skill")
        if(!skillMap.contains(skill)) {
          //println("Adding " + skill)
          skillMap += (skill -> new HashMap[String,Int])
        }
        skillMap.get(skill).get += (rs.getString("wid") -> rs.getInt("weight"))
      }
      rs.close
      skillMap.toMap
    } finally {
      if(st != null) {
        try {
          st.close()
        } catch {
          case e:SQLException =>
        }
      }

      if(conn != null) {
        try {
          conn.close()
        } catch {
          case e:SQLException =>
        }
      }
    }
  }

  def getSAGrades:SAGrades = {
    var conn:Connection = null
    var st:Statement = null
    try {
      conn = ds.getConnection
      val saGrades = new SAGrades
      st = conn.createStatement
      val rs = st.executeQuery("SELECT * FROM sa_grading")

      while(rs.next) {
        saGrades += ( rs.getString("skill")
                          ,rs.getInt("rsc")
                          ,rs.getFloat("ppe")
                          ,rs.getFloat("se")
                          ,rs.getInt("grade") )
      }
      rs.close
      saGrades
    } finally {
      if(st != null) {
        try {
          st.close()
        } catch {
          case e:SQLException =>
        }
      }

      if(conn != null) {
        try {
          conn.close()
        } catch {
          case e:SQLException =>
        }
      }
    }
  }

  def getSecret(consumerKey: String) = {
    var conn:Connection = null
    var st:PreparedStatement = null
    try {
      conn = ds.getConnection
      st = conn.prepareStatement("SELECT secret FROM lti_consumers WHERE consumer_key = ?")
      st.setString(1,consumerKey)
      val rs = st.executeQuery
      if(rs.next) {
        rs.getString("secret")
      } else {
        ""
      }
    } finally {
      if(st != null) {
        try {
          st.close()
        } catch {
          case e:SQLException =>
        }
      }

      if(conn != null) {
        try {
          conn.close()
        } catch {
          case e:SQLException =>
        }
      }
    }
  }
}
