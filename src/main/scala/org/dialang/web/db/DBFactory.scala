package org.dialang.web.db

object DBFactory {

  private var instance:DB = null

  def get(datasourceUrl: String = "java:comp/env/jdbc/dialang"): DB = {

    if(instance == null) {
      instance = new DB(datasourceUrl)
    }

    instance
  }
}
