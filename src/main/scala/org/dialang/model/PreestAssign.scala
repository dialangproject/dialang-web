package org.dialang.model

/**
 * We're representing records (mappings of booket id onto personal estimation pe), by Tuple2 instances.
 */
class PreestAssign(assign: Map[String,Vector[(Float,Int)]]) {

  def getBookletId(tl: String,skill: String) = {
    val list = assign.get(tl + "#" + skill).get

    // obtain the  element at (set.size () +1) / 2.
    list( (list.length + 1) / 2 )._2
  }

  def getBookletId(tl: String,skill: String,pe: Float) = {

    println("PE: " + pe)

    // Make our compound key and get our list of records
    val list = assign.get(tl + "#" + skill).get

    println(list)

    try {
      println(list.filter(t => pe <= t._1))
      list.filter(t => pe <= t._1)(0)._2
    } catch {
      case e:NoSuchElementException => {
        println("PreestAssign/get: max return forced for " + pe);
        list.last._2
      }
    }
  }
}
