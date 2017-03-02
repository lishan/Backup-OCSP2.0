package com.asiainfo.ocdp.stream.config

import com.asiainfo.ocdp.stream.common.JDBCUtil
import com.asiainfo.ocdp.stream.constant.TableInfoConstant
import com.asiainfo.ocdp.stream.tools.Json4sUtils
import org.apache.spark.sql.types.StructType
import org.slf4j.LoggerFactory

import scala.collection.mutable

/**
 * Created by leo on 8/12/15.
 */

object MainFrameConf {

  val logger = LoggerFactory.getLogger(this.getClass.getSimpleName.replace("$", ""))

  val systemProps = new SystemProps()
  val codisProps = new DataSourceConf()

  initMainFrameConf()

  def initMainFrameConf(): Unit = {
    initSystemProps()
    initCodisProps()

    println("= = " * 20 +" finish initMainFrameConf")
  }

  /**
   * init SystemProps config
   */
  def initSystemProps() {
    val sql = "select name,value from " + TableInfoConstant.SystemPropTableName
    val sysdata = JDBCUtil.query(sql)
    sysdata.foreach(x => {
      systemProps.set(x.get("name").get, x.get("value").get)
    })
  }

  /**
    * flush SystemProps config
    */
  def flushSystemProps() {
    initSystemProps()
  }

  def initCodisProps() {
    val sql = "select properties " +
      "from " + TableInfoConstant.DataSourceTableName +
      " where type='codis'"

    val datasource = JDBCUtil.query(sql).head

    val propsJsonStr = datasource.get("properties").getOrElse("")
    val propsArrMap = Json4sUtils.jsonStr2ArrMap(propsJsonStr)
    propsArrMap.foreach { kvMap =>
      if (!kvMap.isEmpty) codisProps.set(kvMap.get("pname").get, kvMap.get("pvalue").get)
    }
  }

}
