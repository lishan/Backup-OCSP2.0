package com.asiainfo.ocdp.stream.service

import java.text.SimpleDateFormat
import java.util.Date

import com.asiainfo.ocdp.stream.common.{JDBCUtil, Logging}
import com.asiainfo.ocdp.stream.config.TaskConf
import com.asiainfo.ocdp.stream.constant.{DataSourceConstant, ExceptionConstant, TableInfoConstant, TaskConstant}

import scala.util.{Failure, Success, Try}

/**
 * Created by leo on 8/28/15.
 */
class UserSecurityServer extends Logging {

  private def ExecuteQuery(sql: String) {
    try {
      JDBCUtil.execute(sql)
    } catch {
      case e : Throwable => logError("failed to exectue sql:" + sql + " exception: " + e.getStackTrace)
    }
  }

  def getSparkKeytab(name: String): String = {
    val sql = "select spark_keytab from " + TableInfoConstant.UserSecurityTableName + " where name= '" + name +"'"
    val data = JDBCUtil.query(sql)
    data.head.get("spark_keytab").get
  }

  def getSparkPrincipal(name: String): String = {
    val sql = "select spark_principal from " + TableInfoConstant.UserSecurityTableName + " where name= '" + name +"'"
    val data = JDBCUtil.query(sql)
    data.head.get("spark_principal").get
  }

  def getKafkaKeytab(name: String): String = {
    val sql = "select kafka_keytab from " + TableInfoConstant.UserSecurityTableName + " where name= '" + name +"'"
    val data = JDBCUtil.query(sql)
    data.head.get("kafka_keytab").get
  }

  def getKafkaPrincipal(name: String): String = {
    val sql = "select kafka_principal from " + TableInfoConstant.UserSecurityTableName + " where name= '" + name +"'"
    val data = JDBCUtil.query(sql)
    data.head.get("kafka_principal").get
  }


}
