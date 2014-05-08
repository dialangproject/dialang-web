package org.dialang.util

import java.sql.ResultSet

class ResultSetIterator(rs: ResultSet) extends Iterator[ResultSet] {

  def hasNext(): Boolean = rs.next
  def next(): ResultSet = rs
}

object ResultSetImplicits {

  implicit def ResultSet2ResultSetIterator(rs: ResultSet) = {
    new ResultSetIterator(rs)
  }
}
