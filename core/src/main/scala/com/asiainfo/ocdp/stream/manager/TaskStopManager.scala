package com.asiainfo.ocdp.stream.manager

import java.util.{Timer, TimerTask}

import com.asiainfo.ocdp.stream.common.Logging
import com.asiainfo.ocdp.stream.config.MainFrameConf
import com.asiainfo.ocdp.stream.constant.TaskConstant
import com.asiainfo.ocdp.stream.service.TaskServer
import org.apache.spark.streaming.StreamingContext

import scala.util.{Failure, Success, Try}

/**
 * Created by leo on 8/30/15.
 */
class TaskStopManager(ssc: StreamingContext, taskId: String, stopGracefully: Boolean) extends Logging {

  val delaySeconds = MainFrameConf.systemProps.getInt("delaySeconds", 10)
  val periodSeconds = MainFrameConf.systemProps.getInt("periodSeconds", 60)
  val taskServer = new TaskServer()
  val timer = new Timer("Task stop timer", true)
  val task = new TimerTask {
    override def run() {
      try {
        checkTaskStop(ssc, taskId)
      } catch {
        case e: Exception => logError(s"Stop task ${taskId} failed.", e)
      }
    }
  }

  if (delaySeconds > 0) {
    logInfo(
      "Starting check task list status with delay of " + delaySeconds + " secs " +
        "and period of " + periodSeconds + " secs")
    timer.schedule(task, delaySeconds * 1000, periodSeconds * 1000)
  }

  //检测到任务状态为准备停止或准备重启时,均终止ssc
  def checkTaskStop(ssc: StreamingContext, id: String) {
    val res = Try(taskServer.checkTaskStatus(id))
    res match {
      case Success(status) => {
        if ((TaskConstant.PRE_STOP == status || TaskConstant.PRE_RESTART == status) && !ssc.sparkContext.isStopped) {
          ssc.stop(true, stopGracefully)
        }
      }
      case Failure(t) => logError("Failed to get task status from database! " + t.getStackTrace.toString())
    }
  }

}
