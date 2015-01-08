package org.dialang.web.db

import java.sql.{DriverManager,Connection,Statement,PreparedStatement,SQLException}

import scala.collection.JavaConversions._
import scala.collection.mutable.{ListBuffer,HashMap,ArrayBuffer}

import org.dialang.web.model._
import org.dialang.web.util.DialangLogger

import javax.naming.InitialContext
import javax.sql.DataSource
import java.util.concurrent.ConcurrentHashMap

import org.dialang.common.model.{Answer,Item}
import org.dialang.util.ResultSetImplicits._

class DB(datasourceUrl: String) extends DialangLogger {

  private val ds = (new InitialContext).lookup(datasourceUrl).asInstanceOf[DataSource];

  val adminLanguages: List[String] = {
      debug("Caching admin languages ...")
      lazy val conn = ds.getConnection
      lazy val st = conn.createStatement

      try {
        val rs = st.executeQuery("SELECT locale FROM admin_languages")
        rs.map(_.getString(1)).toList
      } finally {
        if (st != null) {
          try {
            st.close()
          } catch { case e:SQLException => }
        }

        if (conn != null) {
          try {
            conn.close()
          } catch { case e:SQLException => }
        }

        debug("Admin languages cached.")
      }
    }

  val ltiLocaleLookup: Map[String, String] = {
      debug("Caching LTI locale lookup ...")
      lazy val conn = ds.getConnection
      lazy val st = conn.createStatement

      try {
        val rs = st.executeQuery("SELECT * FROM lti_locale_lookup")
        rs.map( (row) => {
            (row.getString(1) -> row.getString(2))
          }).toMap
      } finally {
        if (st != null) {
          try {
            st.close()
          } catch { case e:SQLException => }
        }

        if (conn != null) {
          try {
            conn.close()
          } catch { case e:SQLException => }
        }

        debug("LTI locale lookups cached.")
      }
    }

  val testLanguages: List[String] = {

      debug("Caching test languages ...")

      lazy val conn = ds.getConnection
      lazy val st = conn.createStatement

      try {
        val rs = st.executeQuery("SELECT locale FROM test_languages")
        rs.map(_.getString(1)).toList
      } finally {
        if (st != null) {
          try {
            st.close()
          } catch { case e:SQLException => }
        }

        if (conn != null) {
          try {
            conn.close()
          } catch { case e:SQLException => }
        }

        debug("Test languages cached.")
      }
    }

  private val vsptWordCache: Map[String, List[VSPTWord]] = {
      debug("Caching VSPT words ...")

      lazy val conn = ds.getConnection
      lazy val st = conn.createStatement

      try {
        testLanguages.map(tl => {
            val rs = st.executeQuery("SELECT words.word_id,words.word,words.valid,words.weight FROM vsp_test_word,words WHERE locale = '" + tl + "' AND vsp_test_word.word_id = words.word_id")
            (tl -> (rs.map(new VSPTWord(_)).toList))
          }).toMap
      } finally {
        if (st != null) {
          try {
            st.close()
          } catch { case e:SQLException => }
        }

        if (conn != null) {
          try {
            conn.close
          } catch { case e:SQLException => }
        }

        debug("VSPT words cached.")
      }
    }

  def getVSPTWords(tl: String): Option[List[VSPTWord]] = vsptWordCache.get(tl)

  val preestAssign: PreestAssign = {
      debug("Caching pre-estimation assignments ...")

      lazy val conn = ds.getConnection
      lazy val st = conn.createStatement

      try {
        val rs = st.executeQuery("SELECT * FROM preest_assignments")

        val temp = rs.foldLeft(new HashMap[String, ArrayBuffer[(Float, Int)]]())((map,r) => {
            val key = r.getString("tl") + "#" + r.getString("skill")
            if (!map.contains(key)) {
              map += (key -> new ArrayBuffer[(Float,Int)])
            }
            map.get(key).get += ((r.getFloat("pe"),r.getInt("booklet_id")))
            map
          }).toMap

        val assign = temp.map(t => {
            (t._1 -> t._2.sortWith(_._1 < _._1).toVector)
          })

        new PreestAssign(assign)
      } finally {
        if (st != null) {
          try {
            st.close()
          } catch { case e:SQLException => }
        }

        if (conn != null) {
          try {
            conn.close()
          } catch { case e:SQLException => }
        }

        debug("Pre-estimation assignments cached.")
      }
    }

  val preestWeights: PreestWeights = {
      debug("Caching pre-estimation weights ...")

      lazy val conn = ds.getConnection
      lazy val st = conn.createStatement

      try {
        val rs = st.executeQuery("SELECT * FROM preest_weights")
        val preestWeights = new PreestWeights(rs)
        rs.close
        preestWeights
      } finally {
        if (st != null) {
          try {
            st.close()
          } catch { case e:SQLException => }
        }

        if (conn != null) {
          try {
            conn.close()
          } catch { case e:SQLException => }
        }

        debug("Pre-estimation weights cached ...")
      }
    }

  val vsptBands: Map[String, Vector[(String, Int, Int)]] = {
      debug("Caching VSPT bands ...")

      lazy val conn = ds.getConnection
      lazy val st = conn.createStatement

      try {
        var rs = st.executeQuery("SELECT * FROM vsp_bands")

        val temp = rs.foldLeft(new HashMap[String, ArrayBuffer[(String, Int, Int)]]())((map,r) => {
            val locale = r.getString("locale")
            val level = r.getString("level")
            val low = r.getInt("low")
            val high = r.getInt("high")
            if (!map.contains(locale)) {
              map += (locale -> new ArrayBuffer[(String, Int, Int)])
            }
            map.get(locale).get += ((level,low,high))
            map
          }).toMap

        // Make it all immutable and return it
        temp.map(t => (t._1 -> t._2.toVector)).toMap
      } finally {
        if (st != null) {
          try {
            st.close()
          } catch { case e:SQLException => }
        }

        if (conn != null) {
          try {
            conn.close()
          } catch { case e:SQLException => }
        }

        debug("VSPT bands cached.")
      }
    }

  /**
   * Returns a Map[SKILL[ID: WEIGHT]] with the sa statement weights
   */
  val saWeights: Map[String, Map[String, Int]] = {
      debug("Caching SA weights ...")

      lazy val conn = ds.getConnection
      lazy val st = conn.createStatement

      try {
        val rs = st.executeQuery("SELECT * FROM sa_weights")
        val temp = rs.foldLeft(new HashMap[String,HashMap[String,Int]]())((map,r) => {
            val skill = r.getString("skill")
            if (!map.contains(skill)) {
              map += (skill -> new HashMap[String,Int])
            }
            map.get(skill).get += (r.getString("wid") -> r.getInt("weight"))
            map
          }).toMap

        // Make it all immutable and return it
        temp.map(t => (t._1 -> t._2.toMap))
      } finally {
        if (st != null) {
          try {
            st.close()
          } catch {
            case e:SQLException =>
          }
        }

        if (conn != null) {
          try {
            conn.close()
          } catch {
            case e:SQLException =>
          }
        }

        debug("SA weights cached.")
      }
    }


  val saGrades: SAGrades = {
      debug("Caching SA grades ...")

      lazy val conn = ds.getConnection
      lazy val st = conn.createStatement

      try {
        val rs = st.executeQuery("SELECT * FROM sa_grading")
        val saGrades = new SAGrades(rs)
        rs.close
        saGrades
      } finally {
        if (st != null) {
          try {
            st.close()
          } catch { case e:SQLException => }
        }

        if (conn != null) {
          try {
            conn.close()
          } catch { case e:SQLException => }
        }

        debug("SA grades cached.")
      }
    }

  val skills: List[String] = {
      debug("Caching skills ...")

      lazy val conn = ds.getConnection
      lazy val st = conn.createStatement

      try {
        val rs = st.executeQuery("SELECT name FROM skills")
        rs.map(_.getString(1)).toList
      } finally {
        if (st != null) {
          try {
            st.close()
          } catch { case e:SQLException => }
        }

        if (conn != null) {
          try {
            conn.close()
          } catch { case e:SQLException => }
        }

        debug("Skills cached.")
      }
    }

  private val bookletIdCache: List[Int] = {
      debug("Caching booklet ids ...")

      lazy val conn = ds.getConnection
      lazy val st = conn.createStatement

      try {
        val rs = st.executeQuery("SELECT id FROM booklets")
        rs.map(_.getInt(1)).toList
      } finally {
        if (st != null) {
          try {
            st.close()
          } catch { case e:SQLException => }
        }

        if (conn != null) {
          try {
            conn.close()
          } catch { case e:SQLException => }
        }

        debug("Booklet ids cached.")
      }
    }

  private val itemGradesCache: Map[String, Map[String, Map[Int, ItemGrades]]] = {
      debug("Caching item grades ...")

      lazy val conn = ds.getConnection
      lazy val st = conn.createStatement

      try {
        val map = new HashMap[String,Map[String,Map[Int,ItemGrades]]]
        testLanguages.foreach(tl => {
          val skillMap = new HashMap[String,Map[Int,ItemGrades]]
          skills.foreach(skill => {
            val bookletMap = new HashMap[Int,ItemGrades]
            bookletIdCache.foreach(bookletId => {
              val rs = st.executeQuery("SELECT rsc,ppe,se,grade FROM item_grading WHERE tl = '" + tl + "' AND skill = '" + skill + "' AND booklet_id = " + bookletId)
              val itemGrades = new ItemGrades(tl, skill, bookletId, rs)
              rs.close()
              bookletMap += (bookletId -> itemGrades)
            })
            skillMap += (skill -> bookletMap.toMap)
          })
          map += (tl -> skillMap.toMap)
        })
        map.toMap
      } finally {
        if (st != null) {
          try {
            st.close()
          } catch { case e:SQLException => }
        }

        if (conn != null) {
          try {
            conn.close()
          } catch { case e:SQLException => }
        }

        debug("Item grades cached.")
      }
    }

  def getItemGrades(tl: String, skill: String, bookletId: Int): ItemGrades = {

    var cacheHit = false
    var itemGrades:ItemGrades = null

    if (itemGradesCache.contains(tl)) {
      val skillMap = itemGradesCache.get(tl).get
      if (skillMap.contains(skill)) {
        val bookletMap = skillMap.get(skill).get
        if (bookletMap.contains(bookletId)) {
          cacheHit = true
          itemGrades = bookletMap.get(bookletId).get
        }
      }
    }

    if (cacheHit) {
      debug("itemGradesCache hit")
      itemGrades
    } else {
      debug("itemGradesCache miss")
      itemGrades
    }
  }

  def getSecret(consumerKey: String): Option[String] = {

    lazy val conn = ds.getConnection
    lazy val st = conn.prepareStatement("SELECT secret FROM lti_consumers WHERE consumer_key = ?")

    try {
      st.setString(1, consumerKey)
      val rs = st.executeQuery
      if (rs.next) {
        Some(rs.getString("secret"))
      } else {
        None
      }
    } finally {
      if (st != null) {
        try {
          st.close()
        } catch {
          case e:SQLException =>
        }
      }

      if (conn != null) {
        try {
          conn.close()
        } catch {
          case e:SQLException =>
        }
      }
    }
  }

  private val bookletLengthCache: Map[Int, Int] = {
      debug("Caching booklet lengths ...")

      lazy val conn:Connection = ds.getConnection
      lazy val st1:PreparedStatement = conn.prepareStatement(
        "SELECT baskets.* FROM baskets, booklet_basket WHERE booklet_basket.booklet_id = ? AND booklet_basket.basket_id = baskets.id")
      lazy val st2:PreparedStatement = conn.prepareStatement(
        "SELECT * FROM baskets WHERE parent_testlet_id = ?")
      lazy val st3:PreparedStatement = conn.prepareStatement(
        "SELECT count(*) as num_items FROM basket_item WHERE basket_id = ?")

      try {

        val temp = new HashMap[Int,Int]

        bookletIdCache.foreach(bookletId => {

          var total = 0

          // Get all the baskets for this booklet.

          st1.setInt(1,bookletId)
          val basketsRS = st1.executeQuery

          while (basketsRS.next) {
            val basketId = basketsRS.getInt("id")
            basketsRS.getString("type") match {
              case "tabbedpane" => {
                st2.setInt(1, basketId)
                val childBasketsRS = st2.executeQuery
                while (childBasketsRS.next) {
                  val childBasketId = childBasketsRS.getInt("id")
                  st3.setInt(1,childBasketId)
                  val itemCountRS = st3.executeQuery
                  if (itemCountRS.next) {
                    total += itemCountRS.getInt("num_items")
                  }
                }
              }
              case _ => {
                st3.setInt(1,basketId)
                val itemCountRS = st3.executeQuery
                if (itemCountRS.next) {
                  total += itemCountRS.getInt("num_items")
                }
              }
            }
          }

          temp += (bookletId -> total)
        })
        temp.toMap
      } finally {
        if (st3 != null) {
          try {
            st3.close()
          } catch { case e:SQLException => }
        }

        if (st2 != null) {
          try {
            st2.close()
          } catch { case e:SQLException => }
        }

        if (st1 != null) {
          try {
            st1.close()
          } catch { case e:SQLException => }
        }

        if (conn != null) {
          try {
            conn.close()
          } catch { case e:SQLException => }
        }

        debug("Booklet lengths cached.")
      }
    }

  def getBookletLength(bookletId: Int): Int = {

    bookletLengthCache.get(bookletId) match {
        case Some(length) => length
        case None => {
          error("No booklet length for booklet " + bookletId + ". Returning zero ...")
          0
        }
      }
  }

  private val basketIdCache: Map[Int, List[Int]] = {

      debug("Caching basket ids ...")

      lazy val conn = ds.getConnection
      lazy val st = conn.prepareStatement("SELECT basket_id FROM booklet_basket WHERE booklet_id = ?")

      try {
        val temp = new HashMap[Int,List[Int]]
        bookletIdCache.foreach(bookletId => {

          st.setInt(1,bookletId)
          val rs = st.executeQuery
          temp += (bookletId -> rs.map(_.getInt(1)).toList)
          rs.close()
        })
        temp.toMap
      } finally {
        if (st != null) {
          try {
            st.close()
          } catch { case e:SQLException => }
        }

        if (conn != null) {
          try {
            conn.close()
          } catch { case e:SQLException => }
        }

        debug("Basket ids cached.")
      }
    }

  def getBasketIdsForBooklet(bookletId: Int): List[Int] = {

    basketIdCache.get(bookletId) match {
        case Some(ids) => ids
        case None => {
          debug("No basket ids for booklet " + bookletId + ". Returning an empty list ...")
          List[Int]()
        }
      }
  }

  val items: Map[Int, Item] = {
      debug("Caching items ...")

      lazy val conn = ds.getConnection
      lazy val st = conn.createStatement

      try {
        val rs = st.executeQuery("SELECT * FROM items")
        rs.map(r => {
            val item = new Item(r)
            (item.id -> item)
          }).toMap
      } finally {
        if (st != null) {
          try {
            st.close()
          } catch { case e: SQLException => }
        }

        if (conn != null) {
          try {
            conn.close()
          } catch { case e: SQLException => }
        }
        debug("Items cached ...")
      }
    }

  private val (answerCache: Map[Int, Answer], itemAnswerCache: Map[Int, List[Answer]]) = {
      debug("Caching answers ...")

      lazy val conn = ds.getConnection
      lazy val st = conn.createStatement

      try {
        val answers = new HashMap[Int,Answer]
        val itemAnswers = new HashMap[Int,ListBuffer[Answer]]
        val rs = st.executeQuery("SELECT * FROM answers")
        while (rs.next) {
          val answer = new Answer(rs)
          val id = rs.getInt("id")
          answers += (id -> answer)
          val itemId = rs.getInt("item_id")
          if (!itemAnswers.contains(itemId)) {
            itemAnswers += (itemId -> new ListBuffer[Answer])
          }
          itemAnswers.get(itemId).get += answer
        }
        rs.close()
        ( answers.toMap,itemAnswers.toMap.map(t => ((t._1,t._2.toList))) )
      } finally {
        if (st != null) {
          try {
            st.close()
          } catch { case e:SQLException => }
        }

        if (conn != null) {
          try {
            conn.close()
          } catch { case e:SQLException => }
        }

        debug("Answers cached.")
      }
    }

  def getAnswer(answerId: Int): Option[Answer] = {

    if (isDebugEnabled) {
      if (answerCache.contains(answerId)) {
        debug("answerCache hit")
      } else {
        debug("answerCache miss")
      }
    }

    answerCache.get(answerId)
  }

  def getAnswers(itemId: Int): Option[List[Answer]] = {

    if (isDebugEnabled) {
      if (itemAnswerCache.contains(itemId)) {
        debug("itemAnswerCache hit")
      } else {
        debug("itemAnswerCache miss")
      }
    }

    itemAnswerCache.get(itemId)
  }

  val punctuation:List[String] = {

      debug("Caching punctuation ...")

      lazy val conn = ds.getConnection
      lazy val st = conn.createStatement

      try {
        val rs = st.executeQuery("SELECT unicode_hex FROM punctuation")
        rs.map(_.getString(1).replaceFirst("^0*","")).toList
      } finally {
        if (st != null) {
          try {
            st.close()
          } catch { case e:SQLException => }
        }

        if (conn != null) {
          try {
            conn.close()
          } catch { case e:SQLException => }
        }

        debug("Punctuation cached.")
      }
    }

  /**
   * Returns a mapping of textual level onto numeric grade, eg: 1 -&gt; A1
   */
  val levels: Map[Int,String] = {
      debug("Caching levels ...")

      lazy val conn = ds.getConnection
      lazy val st = conn.createStatement

      try {
        val rs = st.executeQuery("SELECT grade,level FROM item_levels")
        rs.map( r => (r.getInt(1) -> r.getString(2)) ).toMap
      } finally {
        if (st != null) {
          try {
            st.close()
          } catch { case e:SQLException => }
        }

        if (conn != null) {
          try {
            conn.close()
          } catch { case e:SQLException => }
        }

        debug("Levels cached.")
      }
    }
}
