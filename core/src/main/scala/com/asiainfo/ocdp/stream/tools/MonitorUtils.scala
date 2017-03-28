package com.asiainfo.ocdp.stream.tools

import com.asiainfo.ocdp.stream.common.{JDBCUtil, Logging}
import com.asiainfo.ocdp.stream.constant.TableInfoConstant

/**
  * Created by peng on 2017/3/3.
  */
object MonitorUtils extends Logging{

  def deleteTaskStatisticsHistory(retainSeconds: Long): Unit = {
    val sql = s"delete from ${TableInfoConstant.TaskMonitorName} where archived=0 and timestamp < date_sub(now(), INTERVAL ${retainSeconds} MINUTE);"

    try{
      JDBCUtil.execute(sql)
      logInfo(s"Deleted task statistics history via '${sql}' successfully.")
    }catch{
      case e: Exception=>logError(s"delete task statistics history with '${sql}' failed", e)
    }
  }

  def updateTaskStatisticsHistoryStatus(taskId: String): Unit ={
    val sql = s"update ${TableInfoConstant.TaskMonitorName} a, (select application_id, max(timestamp) as t from ${TableInfoConstant.TaskMonitorName} where task_id=${taskId} group by application_id) b set a.archived=1 where a.task_id=${taskId} and a.application_id=b.application_id and a.timestamp=b.t;"
    try{
      JDBCUtil.execute(sql)
      logInfo(s"Update task statistics status via '${sql}' successfully.")
    }catch{
      case e: Exception=>logError(s"Update task statistics status via '${sql}' failed", e)
    }
  }


  def outputTaskStatistics(taskId: String, batchTime:String, reservedRecordsCounter: Long, droppedRecordsCounter: Long, applicationId: String, maxMem: Long, memUsed: Long, memRemaining: Long, runningTime: Long) = {
    val sql = s"INSERT INTO ${TableInfoConstant.TaskMonitorName} (task_id,timestamp,reserved_records,dropped_records,archived,application_id,batch_running_time_ms,max_storage_memory,used_storage_memory,remaining_storage_memory) VALUES ('${taskId}',from_unixtime(${batchTime}),${reservedRecordsCounter},${droppedRecordsCounter}, 0, '${applicationId}', '${runningTime}', ${maxMem}, ${memUsed}, ${memRemaining});"
    try {
      JDBCUtil.execute(sql)
      logInfo(s"Update task statistics via '${sql}' successfully")
    } catch {
      case e: Exception => logError(s"Update task statistics via '${sql}' failed", e)
    }
  }



}
