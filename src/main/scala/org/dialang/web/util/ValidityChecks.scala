package org.dialang.web.util

import org.dialang.db.DBFactory

object ValidityChecks {

  val db = DBFactory.get()

  def adminLanguageExists(al: String): Boolean = db.adminLanguages.contains(al)
  def testLanguageExists(tl: String): Boolean = db.testLanguages.contains(tl)
  def skillExists(skill: String): Boolean = db.skills.contains(skill)
}
