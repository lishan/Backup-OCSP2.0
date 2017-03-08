package com.asiainfo.ocdp.stream.tools

import com.asiainfo.ocdp.stream.common.{JDBCUtil, Logging}
import com.asiainfo.ocdp.stream.constant.TableInfoConstant

/**
  * Created by peng on 2017/3/3.
  */
object MonitorUtils extends Logging{

  def outputRecordsCorrectness(taskId: String, reservedRecordsCounter: Long, droppedRecordsCounter: Long, applicationId: String) = {

    val sql = s"INSERT INTO ${TableInfoConstant.MonitorRecordsCorrectnessName} (task_id,reserved_records,dropped_records,archived,application_id) VALUES ('${taskId}',${reservedRecordsCounter},${droppedRecordsCounter}, 0, '${applicationId}');"
    try{
      JDBCUtil.execute(sql)
      logError(s"Update records correctness via '${sql}' successfully")
    }catch{
      case e: Exception=>logError(s"Update records correctness via '${sql}' failed", e)
    }
  }

  def deleteRecordsCorrectnessHistory(retainSeconds: Long): Unit = {
    val sql = s"delete from ${TableInfoConstant.MonitorRecordsCorrectnessName} where archived=0 and timestamp < date_sub(now(), INTERVAL ${retainSeconds} MINUTE);"

    try{
      JDBCUtil.execute(sql)
      logInfo(s"Deleted records correctness history via '${sql}' successfully.")
    }catch{
      case e: Exception=>logError(s"delete records correctness history with '${sql}' failed", e)
    }
  }

  def updateRecordsCorrectnessHistoryStatus(taskId: String): Unit ={
    val sql = s"update ${TableInfoConstant.MonitorRecordsCorrectnessName} a, (select application_id, max(timestamp) as t from ${TableInfoConstant.MonitorRecordsCorrectnessName} where task_id=${taskId} group by application_id) b set a.archived=1 where a.task_id=${taskId} and a.application_id=b.application_id and a.timestamp=b.t;"
    try{
      JDBCUtil.execute(sql)
      logInfo(s"Update records correctness status via '${sql}' successfully.")
    }catch{
      case e: Exception=>logError(s"Update records correctness status via '${sql}' failed", e)
    }
  }

}
