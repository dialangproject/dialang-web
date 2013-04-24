package org.dialang.db

import java.sql.{DriverManager,Connection,Statement,PreparedStatement,SQLException}

import scala.collection.JavaConversions._
import scala.collection.mutable.{ListBuffer,HashMap,ArrayBuffer}

import org.dialang.model._

import javax.naming.InitialContext
import javax.sql.DataSource

import org.dialang.common.model.{Answer,Item}

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

  def getItemGrades(tl:String,skill:String,bookletId:Int): ItemGrades = {

    var conn:Connection = null
    var st:Statement = null
    try {
      conn = ds.getConnection
      st = conn.createStatement
      val rs = st.executeQuery("SELECT rsc,ppe,se,grade FROM item_grading WHERE tl = '" + tl + "' AND skill = '" + skill + "' AND booklet_id = " + bookletId)
      val itemGrades = new ItemGrades(tl,skill,bookletId,rs)
      rs.close()
      itemGrades
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

  def getBookletLength(bookletId:Int) = {

    var conn:Connection = null
    var st:PreparedStatement = null
    var st2:PreparedStatement = null
    try {
      conn = ds.getConnection

      var total = 0

      // Count the non testlet baskets
      st = conn.prepareStatement("SELECT count(booklet_id) FROM booklet_basket,baskets WHERE booklet_id = ? AND basket_id = baskets.id AND type != 'tabbedpane'")
      st.setInt(1,bookletId)
      val rs = st.executeQuery
      if(rs.next) {
        total += rs.getInt(1)
      }
      rs.close()
      st.close()

      // Select the testlet baskets in this booklet
      st = conn.prepareStatement("SELECT basket_id FROM booklet_basket,baskets WHERE booklet_id = ? AND basket_id = baskets.id AND type = 'tabbedpane'")
      st.setInt(1,bookletId)
      val rs2 = st.executeQuery
      while(rs2.next) {
        // Add one for the testlet
        total += 1
        // Now count the child baskets
        st2 = conn.prepareStatement("SELECT count(*) FROM baskets WHERE parent_basket_id = ?")
        st2.setInt(1,rs2.getInt("basket_id"))
        val rs3 = st2.executeQuery
        if(rs3.next) {
          total += rs3.getInt(1)
        }
        rs3.close()
      }
      rs2.close()
      total
    } finally {
      if(st2 != null) {
        try {
          st2.close()
        } catch { case e:SQLException => }
      }

      if(st != null) {
        try {
          st.close()
        } catch { case e:SQLException => }
      }

      if(conn != null) {
        try {
          conn.close()
        } catch { case e:SQLException => }
      }
    }
  }

  def getBasketIdsForBooklet(bookletId:Int) = {

    var conn:Connection = null
    var st:PreparedStatement = null
    try {
      conn = ds.getConnection

      val basketIds = new ListBuffer[Int]

      // Count the non testlet baskets
      st = conn.prepareStatement("SELECT basket_id FROM booklet_basket WHERE booklet_id = ?")
      st.setInt(1,bookletId)
      val rs = st.executeQuery
      while(rs.next) {
        basketIds += rs.getInt(1)
      }
      rs.close()

      basketIds
    } finally {
      if(st != null) {
        try {
          st.close()
        } catch { case e:SQLException => }
      }

      if(conn != null) {
        try {
          conn.close()
        } catch { case e:SQLException => }
      }
    }
  }

  def getItem(itemId:Int): Option[Item] = {

    var conn:Connection = null
    var st:PreparedStatement = null
    try {
      conn = ds.getConnection

      st = conn.prepareStatement("SELECT * FROM items WHERE id = ?")
      st.setInt(1,itemId)
      val rs = st.executeQuery
      if(rs.next) {
        Some(new Item(rs))
      } else {
        None
      }
    } catch {
      case e:SQLException => None
    } finally {
      if(st != null) {
        try {
          st.close()
        } catch { case e:SQLException => }
      }

      if(conn != null) {
        try {
          conn.close()
        } catch { case e:SQLException => }
      }
    }
  }

  def getAnswer(answerId:Int): Option[Answer] = {

    var conn:Connection = null
    var st:PreparedStatement = null
    try {
      conn = ds.getConnection

      st = conn.prepareStatement("SELECT * FROM answers WHERE id = ?")
      st.setInt(1,answerId)
      val rs = st.executeQuery
      if(rs.next) {
        Some(new Answer(rs))
      } else {
        None
      }
    } catch {
      case e:SQLException => None
    } finally {
      if(st != null) {
        try {
          st.close()
        } catch { case e:SQLException => }
      }

      if(conn != null) {
        try {
          conn.close()
        } catch { case e:SQLException => }
      }
    }
  }

  def getAnswers(answerId:Int): Option[List[Answer]] = {

    var conn:Connection = null
    var st:PreparedStatement = null
    try {
      conn = ds.getConnection

      st = conn.prepareStatement("SELECT * FROM answers WHERE id = ?")
      st.setInt(1,answerId)
      val rs = st.executeQuery
      val answers = new ListBuffer[Answer]
      while(rs.next) {
        answers += new Answer(rs)
      }
      rs.close()
      return Some(answers.toList)
    } catch {
      case e:SQLException => None
    } finally {
      if(st != null) {
        try {
          st.close()
        } catch { case e:SQLException => }
      }

      if(conn != null) {
        try {
          conn.close()
        } catch { case e:SQLException => }
      }
    }
  }

  def getPunctuationCharacters: Option[List[String]] = {

    var conn:Connection = null
    var st: Statement = null
    try {
      conn = ds.getConnection

      st = conn.createStatement
      val chars = new ListBuffer[String]
      val rs = st.executeQuery("SELECT unicode_hex FROM punctuation")
      while(rs.next) {
        // Strip leading zeros
        chars += rs.getString(1).replaceFirst("^0*","")
      }
      Some(chars.toList)
    } catch {
      case e:SQLException => None
    } finally {
      if(st != null) {
        try {
          st.close()
        } catch { case e:SQLException => }
      }

      if(conn != null) {
        try {
          conn.close()
        } catch { case e:SQLException => }
      }
    }
  }

  /**
   * Returns a mapping of textual level onto numeric grade, eg: 1 -&gt; A1
   */
  def getLevels:Map[Int,String] = {

    var conn:Connection = null
    var st: Statement = null
    try {
      conn = ds.getConnection
      st = conn.createStatement
      val rs = st.executeQuery("SELECT grade,level FROM levels")

      val map = new HashMap[Int,String]

      while(rs.next) {
        map += ((rs.getInt(1),rs.getString(2)))
      }

      rs.close()
      map.toMap
    } catch {
      case e:SQLException => {
        println("Failed to get levels")
        Map()
      }
    } finally {
      if(st != null) {
        try {
          st.close()
        } catch { case e:SQLException => }
      }

      if(conn != null) {
        try {
          conn.close()
        } catch { case e:SQLException => }
      }
    }
  }
}
