package org.dialang.web.db

import java.sql.{DriverManager,Connection,Statement,PreparedStatement,SQLException}

import scala.collection.JavaConversions._
import scala.collection.mutable.{ConcurrentMap,ListBuffer,HashMap,ArrayBuffer}

import org.dialang.web.model._
import org.dialang.web.util.DialangLogger

import javax.naming.InitialContext
import javax.sql.DataSource
import java.util.concurrent.ConcurrentHashMap

import org.dialang.common.model.{Answer,Item}

object DB extends DialangLogger {

  private val ds = (new InitialContext).lookup("java:comp/env/jdbc/dialang").asInstanceOf[DataSource];

  private def terminalDBError(t:Throwable):Nothing = {
    error("A terminal error occurred. The JVM will exit.",t)
    exit(1)
  }

  private val testLanguageCache:List[String] = {

      debug("Caching test languages ...")

      var conn:Connection = null
      var st:Statement = null
      try {
        val buffer = new ListBuffer[String]
        conn = ds.getConnection
        st = conn.createStatement
        val rs = st.executeQuery("SELECT locale FROM test_languages")
        while(rs.next) {
          buffer += rs.getString(1)
        }
        rs.close()
        buffer.toList
      } catch {
        case t:Throwable => terminalDBError(t)
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

        debug("Test languages cached.")
      }
    }

  private val vsptWordCache: Map[String,List[VSPTWord]] = {

      debug("Caching VSPT words ...")

      var conn:Connection = null
      var st:Statement = null
      try {
        val tmp = new HashMap[String,List[VSPTWord]]
        conn = ds.getConnection
        st = conn.createStatement
        testLanguageCache.foreach(tl => {
          val rs = st.executeQuery("SELECT words.word_id,words.word,words.valid,words.weight FROM vsp_test_word,words WHERE locale = '" + tl + "' AND vsp_test_word.word_id = words.word_id")
          val words = new ListBuffer[VSPTWord]
          while(rs.next) {
            words += new VSPTWord(rs)
          }
          rs.close
          tmp += ((tl,words.toList))
        })
        tmp.toMap
      } catch {
        case t:Throwable => terminalDBError(t)
      } finally {
        if(st != null) {
          try {
            st.close()
          } catch { case e:SQLException => }
        }

        if(conn != null) {
          try {
            conn.close
          } catch { case e:SQLException => }
        }

        debug("VSPT words cached.")
      }
    }

  def getVSPTWords(tl:String): Option[List[VSPTWord]] = {
    vsptWordCache.get(tl)
  }

  private val preestAssignCache:PreestAssign = {

      debug("Caching pre-estimation assignments ...")

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
      } catch {
        case t:Throwable => terminalDBError(t)
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

        debug("Pre-estimation assignments cached.")
      }
    }

  def getPreestAssign:PreestAssign = preestAssignCache

  private val preestWeightsCache:PreestWeights = {

      debug("Caching pre-estimation weights ...")

      var conn:Connection = null
      var st:Statement = null
      try {
        conn = ds.getConnection
        st = conn.createStatement
        val rs = st.executeQuery("SELECT * FROM preest_weights")
        val preestWeights = new PreestWeights(rs)
        rs.close
        preestWeights
      } catch {
        case t:Throwable => terminalDBError(t)
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

        debug("Pre-estimation weights cached ...")
      }
    }

  def getPreestWeights:PreestWeights = preestWeightsCache

  private val vsptBandsCache:Map[String,Vector[(String,Int,Int)]] = {

      debug("Caching VSPT bands ...")

      var conn:Connection = null
      var st:Statement = null
      try {
        conn = ds.getConnection
        st = conn.createStatement
        var rs = st.executeQuery("SELECT * FROM vsp_bands")
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

        // Make it all immutable, assign it to the cache and return that
        val levels = new HashMap[String,Vector[(String,Int,Int)]]
        temp.foreach(t => levels += (t._1 -> t._2.toVector))
        levels.toMap
      } catch {
        case t:Throwable => terminalDBError(t)
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

        debug("VSPT bands cached.")
      }
    }

  def getVSPTBands:Map[String,Vector[(String,Int,Int)]] = vsptBandsCache

  private val saWeightsCache:Map[String,Map[String,Int]] = {

      debug("Caching SA weights ...")

      var conn:Connection = null
      var st:Statement = null
      try {
        conn = ds.getConnection
        val temp = new HashMap[String,HashMap[String,Int]]
        st = conn.createStatement
        val rs = st.executeQuery("SELECT * FROM sa_weights")
        while(rs.next) {
          val skill = rs.getString("skill")
          if(!temp.contains(skill)) {
            temp += (skill -> new HashMap[String,Int])
          }
          temp.get(skill).get += (rs.getString("wid") -> rs.getInt("weight"))
        }
        rs.close

        // Make it all immutable, assign it to the cache and return that
        val saWeightsMap = new HashMap[String,Map[String,Int]]
        temp.foreach(t => saWeightsMap += (t._1 -> t._2.toMap))
        saWeightsMap.toMap
      } catch {
        case t:Throwable => terminalDBError(t)
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

        debug("SA weights cached.")
      }
    }

  /**
   * Returns a Map[SKILL[ID: WEIGHT]] with the sa statement weights
   */
  def getSAWeights:Map[String,Map[String,Int]] = saWeightsCache

  private val saGradesCache:SAGrades = {

      debug("Caching SA grades ...")

      var conn:Connection = null
      var st:Statement = null
      try {
        conn = ds.getConnection
        st = conn.createStatement
        val rs = st.executeQuery("SELECT * FROM sa_grading")
        val saGrades = new SAGrades(rs)
        rs.close
        saGrades
      } catch {
        case t:Throwable => terminalDBError(t)
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

        debug("SA grades cached.")
      }
    }

  def getSAGrades:SAGrades = saGradesCache

  private val skillCache:List[String] = {

      debug("Caching skills ...")

      var conn:Connection = null
      var st:Statement = null
      try {
        val buffer = new ListBuffer[String]
        conn = ds.getConnection
        st = conn.createStatement
        val rs = st.executeQuery("SELECT name FROM skills")
        while(rs.next) {
          buffer += rs.getString(1)
        }
        rs.close()
        buffer.toList
      } catch {
        case t:Throwable => terminalDBError(t)
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

        debug("Skills cached.")
      }
    }

  private val bookletIdCache:List[Int] = {

      debug("Caching booklet ids ...")

      var conn:Connection = null
      var st:Statement = null
      try {
        val buffer = new ListBuffer[Int]
        conn = ds.getConnection
        st = conn.createStatement
        val rs = st.executeQuery("SELECT id FROM booklets")
        while(rs.next) {
          buffer += rs.getInt(1)
        }
        rs.close()
        buffer.toList
      } catch {
        case t:Throwable => terminalDBError(t)
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

        debug("Booklet ids cached.")
      }
    }

  private val itemGradesCache:Map[String,Map[String,Map[Int,ItemGrades]]] = {

      debug("Caching item grades ...")

      var conn:Connection = null
      var st:Statement = null
      try {
        conn = ds.getConnection
        st = conn.createStatement
        val map = new HashMap[String,Map[String,Map[Int,ItemGrades]]]
        testLanguageCache.foreach(tl => {
          val skillMap = new HashMap[String,Map[Int,ItemGrades]]
          skillCache.foreach(skill => {
            val bookletMap = new HashMap[Int,ItemGrades]
            bookletIdCache.foreach(bookletId => {
              val rs = st.executeQuery("SELECT rsc,ppe,se,grade FROM item_grading WHERE tl = '" + tl + "' AND skill = '" + skill + "' AND booklet_id = " + bookletId)
              val itemGrades = new ItemGrades(tl,skill,bookletId,rs)
              rs.close()
              bookletMap += (bookletId -> itemGrades)
            })
            skillMap += (skill -> bookletMap.toMap)
          })
          map += (tl -> skillMap.toMap)
        })
        map.toMap
      } catch {
        case t:Throwable => terminalDBError(t)
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

        debug("Item grades cached.")
      }
    }

  def getItemGrades(tl:String,skill:String,bookletId:Int): ItemGrades = {

    var cacheHit = false
    var itemGrades:ItemGrades = null

    if(itemGradesCache.contains(tl)) {
      val skillMap = itemGradesCache.get(tl).get
      if(skillMap.contains(skill)) {
        val bookletMap = skillMap.get(skill).get
        if(bookletMap.contains(bookletId)) {
          cacheHit = true
          itemGrades = bookletMap.get(bookletId).get
        }
      }
    }

    if(cacheHit) {
      debug("itemGradesCache hit")
      itemGrades
    } else {
      debug("itemGradesCache miss")
      itemGrades
    }
  }

  def getSecret(consumerKey: String):Option[String] = {
    var conn:Connection = null
    var st:PreparedStatement = null
    try {
      conn = ds.getConnection
      st = conn.prepareStatement("SELECT secret FROM lti_consumers WHERE consumer_key = ?")
      st.setString(1,consumerKey)
      val rs = st.executeQuery
      if(rs.next) {
        Some(rs.getString("secret"))
      } else {
        None
      }
    } catch {
      case t:Throwable => {
        error("Caught exception whilst getting LTI secret for '" + consumerKey + "'. Returning None ...",t)
        None
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

  private val bookletLengthCache:Map[Int,Int] = {

      debug("Caching booklet lengths ...")

      var conn:Connection = null
      var st1:PreparedStatement = null
      var st2:PreparedStatement = null
      var st3:PreparedStatement = null
      try {
        conn = ds.getConnection
        // This counts the number of non tabbedpane baskets in a booklet
        st1 = conn.prepareStatement("SELECT count(bb.booklet_id) FROM booklet_basket bb,baskets b WHERE bb.booklet_id = ? AND bb.basket_id = b.id AND b.type != 'tabbedpane'")
        // This counts the number of tabbedpane baskets in a booklet
        st2 = conn.prepareStatement("SELECT bb.basket_id FROM booklet_basket bb,baskets b WHERE bb.booklet_id = ? AND bb.basket_id = b.id AND b.type = 'tabbedpane'")
        // This counts the number of children of a given basket (tabbedpane of course)
        st3 = conn.prepareStatement("SELECT count(*) FROM baskets WHERE parent_testlet_id = ?")

        val temp = new HashMap[Int,Int]

        bookletIdCache.foreach(bookletId => {
          var total = 0

          // Count the non testlet baskets
          st1.setInt(1,bookletId)
          val rs = st1.executeQuery
          if(rs.next) {
            total += rs.getInt(1)
          }
          rs.close()

          // Select the testlet baskets in this booklet
          st2.setInt(1,bookletId)
          val rs2 = st2.executeQuery
          while(rs2.next) {
            // Add one for the testlet
            total += 1
            // Now count the child baskets
            st3.setInt(1,rs2.getInt("basket_id"))
            val rs3 = st3.executeQuery
            if(rs3.next) {
              total += rs3.getInt(1)
            }
            rs3.close()
          }
          rs2.close()
          temp += (bookletId -> total)
        })
        temp.toMap
      } catch {
        case t:Throwable => terminalDBError(t)
      } finally {
        if(st3 != null) {
          try {
            st3.close()
          } catch { case e:SQLException => }
        }

        if(st2 != null) {
          try {
            st2.close()
          } catch { case e:SQLException => }
        }

        if(st1 != null) {
          try {
            st1.close()
          } catch { case e:SQLException => }
        }

        if(conn != null) {
          try {
            conn.close()
          } catch { case e:SQLException => }
        }

        debug("Booklet lengths cached.")
      }
    }

  def getBookletLength(bookletId:Int):Int = {
    bookletLengthCache.get(bookletId) match {
        case Some(length) => length
        case None => {
          error("No booklet length for booklet " + bookletId + ". Returning zero ...")
          0
        }
      }
  }

  private val basketIdCache:Map[Int,List[Int]] = {

      debug("Caching basket ids ...")

      var conn:Connection = null
      var st:PreparedStatement = null
      try {
        conn = ds.getConnection
        st = conn.prepareStatement("SELECT basket_id FROM booklet_basket WHERE booklet_id = ?")
        val temp = new HashMap[Int,List[Int]]
        bookletIdCache.foreach(bookletId => {

          val basketIds = new ListBuffer[Int]

          st.setInt(1,bookletId)
          val rs = st.executeQuery
          while(rs.next) {
            basketIds += rs.getInt(1)
          }
          rs.close()
          temp += (bookletId -> basketIds.toList)
        })

        temp.toMap
      } catch {
        case t:Throwable => terminalDBError(t)
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

        debug("Basket ids cached.")
      }
    }

  def getBasketIdsForBooklet(bookletId:Int):List[Int] = {
    basketIdCache.get(bookletId) match {
        case Some(ids) => ids
        case None => {
          debug("No basket ids for booklet " + bookletId + ". Returning an empty list ...")
          List[Int]()
        }
      }
  }

  private val itemCache:Map[Int,Item] = {

      debug("Caching items ...")

      var conn:Connection = null
      var st:Statement = null
      try {
        conn = ds.getConnection
        st = conn.createStatement
        val temp = new HashMap[Int,Item]
        val rs = st.executeQuery("SELECT * FROM items")
        while(rs.next) {
            temp += (rs.getInt("id") -> new Item(rs))
        }
        rs.close()
        temp.toMap
      } catch {
        case t:Throwable => terminalDBError(t)
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
        debug("Items cached ...")
      }
    }

  def getItem(itemId:Int): Option[Item] = itemCache.get(itemId)

  private val (answerCache:Map[Int,Answer],itemAnswerCache:Map[Int,List[Answer]]) = {

      debug("Caching answers ...")

      var conn:Connection = null
      var st:Statement = null
      try {
        val answers = new HashMap[Int,Answer]
        val itemAnswers = new HashMap[Int,ListBuffer[Answer]]
        conn = ds.getConnection
        st = conn.createStatement
        val rs = st.executeQuery("SELECT * FROM answers")
        while(rs.next) {
          val answer = new Answer(rs)
          val id = rs.getInt("id")
          answers += (id -> answer)
          val itemId = rs.getInt("item_id")
          if(!itemAnswers.contains(itemId)) {
            itemAnswers += (itemId -> new ListBuffer[Answer])
          }
          itemAnswers.get(itemId).get += answer
        }
        rs.close()
        ( answers.toMap,itemAnswers.toMap.map(t => ((t._1,t._2.toList))) )
      } catch {
        case t:Throwable => terminalDBError(t)
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

        debug("Answers cached.")
      }
    }

  def getAnswer(answerId:Int): Option[Answer] = {

    if(isDebugEnabled) {
      if(answerCache.contains(answerId)) {
        debug("answerCache hit")
      } else {
        debug("answerCache miss")
      }
    }

    answerCache.get(answerId)
  }

  def getAnswers(itemId:Int): Option[List[Answer]] = {

    if(isDebugEnabled) {
      if(itemAnswerCache.contains(itemId)) {
        debug("itemAnswerCache hit")
      } else {
        debug("itemAnswerCache miss")
      }
    }

    itemAnswerCache.get(itemId)
  }

  private val punctuationCache:List[String] = {

      debug("Caching punctuation ...")

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
        chars.toList
      } catch {
        case t:Throwable => terminalDBError(t)
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

        debug("Punctuation cached.")
      }
    }

  def getPunctuationCharacters = punctuationCache

  private val levelCache: Map[Int,String] = {

      debug("Caching levels ...")

      var conn:Connection = null
      var st: Statement = null
      try {
        conn = ds.getConnection
        st = conn.createStatement
        val rs = st.executeQuery("SELECT grade,level FROM item_levels")

        val map = new HashMap[Int,String]

        while(rs.next) {
          map += ((rs.getInt(1),rs.getString(2)))
        }

        rs.close()
        map.toMap
      } catch {
        case t:Throwable => terminalDBError(t)
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

        debug("Levels cached.")
      }
    }

  /**
   * Returns a mapping of textual level onto numeric grade, eg: 1 -&gt; A1
   */
  def getLevels = levelCache
}
