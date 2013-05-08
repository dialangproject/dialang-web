package org.dialang.web.model

import java.sql.ResultSet

class VSPTWord(rs:ResultSet) {

  val id = rs.getString("word_id")
  val word = rs.getString("word")
  val valid = rs.getBoolean("valid")
  val weight = rs.getInt("weight")
}
