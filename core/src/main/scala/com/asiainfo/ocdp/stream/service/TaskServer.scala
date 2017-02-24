package com.asiainfo.ocdp.stream.service

import com.asiainfo.ocdp.stream.common.{Logging, JDBCUtil}
import com.asiainfo.ocdp.stream.config.TaskConf
import com.asiainfo.ocdp.stream.constant.{TaskConstant, TableInfoConstant}

/**
 * Created by leo on 8/28/15.
 */
class TaskServer extends Logging {

  def startTask(id: String) {
    val startTime = System.currentTimeMillis()
    val sql = s"update ${TableInfoConstant.TaskTableName} set status=${TaskConstant.RUNNING}, start_time='${startTime}' where id= '${id}'"
    JDBCUtil.execute(sql)
  }

  def RestartTask(id: String) {
    val sql = "update " + TableInfoConstant.TaskTableName + " set status=" + TaskConstant.PRE_START + " where id= '" + id +"'"
    JDBCUtil.execute(sql)
  }

  def RetryTask(id: String) {
    val sql = "update " + TableInfoConstant.TaskTableName + " set status=" + TaskConstant.RETRY+ " where id= '" + id +"'"
    JDBCUtil.execute(sql)
  }

  def stopTask(id: String) {
    val stopTime = System.currentTimeMillis()
    val sql = s"update ${TableInfoConstant.TaskTableName}  set status=${TaskConstant.STOP}, stop_time='${stopTime}', cur_retry=0 where id='${id}'"
    JDBCUtil.execute(sql)
  }

  def checkTaskStatus(id: String): Int = {
    val sql = "select status from " + TableInfoConstant.TaskTableName + " where id= '" + id +"'"
    val data = JDBCUtil.query(sql)
    data.head.get("status").get.toInt
  }

  def getStartTime(id: String): Long = {
    val sql = "select start_time from " + TableInfoConstant.TaskTableName + " where id= '" + id +"'"
    val data = JDBCUtil.query(sql)
    data.head.get("start_time").get.toLong
  }

  def updateRetry(id: String, retry: Int) {
    val sql = "update " + TableInfoConstant.TaskTableName + " set cur_retry=" + retry + " where id= '" + id +"'"
    JDBCUtil.execute(sql)
  }

  def checkMaxRetry(id: String): Int = {
    val sql = "select retry from " + TableInfoConstant.TaskTableName + " where id= '" + id +"'"
    val data = JDBCUtil.query(sql)
    data.head.get("retry").get.toInt
  }

  def checkTaskRetry(id: String): Int = {
    val sql = "select cur_retry from " + TableInfoConstant.TaskTableName + " where id= '" + id +"'"
    val data = JDBCUtil.query(sql)
    data.head.get("cur_retry").get.toInt
  }

  def getAllTaskInfos(): Array[TaskConf] = {

    val sql = "select id,type,status,num_executors,executor_memory,total_executor_cores,queue,retry,diid,owner from " + TableInfoConstant.TaskTableName
    val data = JDBCUtil.query(sql)
    data.map(x => {
      val taskConf = new TaskConf()
      taskConf.setId(x.get("id").get)
      taskConf.setTask_type(x.get("type").get.toInt)
      taskConf.setStatus(x.get("status").get.toInt)
      taskConf.setNum_executors(x.get("num_executors").get)
      taskConf.setExecutor_memory(x.get("executor_memory").get)
      taskConf.setTotal_executor_cores(x.get("total_executor_cores").get)
      taskConf.setQueue(x.get("queue").get)
      taskConf.setRetry(x.get("retry").get.toInt)
      taskConf.setDiid(x.get("diid").get)
      taskConf.setOwner(x.getOrElse("owner", ""))
      taskConf
    })
  }

  def getTaskInfoById(id: String): TaskConf = {
    val sql = "select id,type,name,receive_interval,retry,diid, owner from " + TableInfoConstant.TaskTableName + " where id= '" + id +"'"
    val data = JDBCUtil.query(sql).head
    val taskConf = new TaskConf()
    taskConf.setId(data.get("id").get)
    taskConf.setTask_type(data.get("type").get.toInt)
    taskConf.setName(data.get("name").get)
    taskConf.setReceive_interval(data.get("receive_interval").get.toInt)
    taskConf.setRetry(data.get("retry").get.toInt)
    taskConf.setDiid(data.get("diid").get)
    taskConf.setOwner(data.getOrElse("owner", ""))
    taskConf
  }

}
