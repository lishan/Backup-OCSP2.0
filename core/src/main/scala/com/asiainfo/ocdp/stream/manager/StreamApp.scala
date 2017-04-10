package com.asiainfo.ocdp.stream.manager

import com.asiainfo.ocdp.stream.common.{Logging, SscManager}
import com.asiainfo.ocdp.stream.constant.{ExceptionConstant, TaskConstant}
import com.asiainfo.ocdp.stream.service.TaskServer
import org.apache.spark.streaming.{Seconds, StreamingContext, StreamingContextState}
import org.apache.spark.{SparkConf, SparkContext}

import scala.util.{Failure, Success, Try}
/**
  * Created by leo on 9/16/15.
  */

object StreamApp extends Logging {
  val taskServer = new TaskServer

  def main(args: Array[String]) {

    if (args.length < 1) {
      //      System.err.println("usage:  ./bin/spark-class com.asiainfo.ocdc.stream.mainframe [taskId]")
      System.exit(1)
    }

    val taskId = args(0)

    val taskConf = taskServer.getTaskInfoById(taskId)

    //1 初始化 streamingContext

    val sparkConf = new SparkConf()
    sparkConf.set("spark.scheduler.mode", "FAIR")

    sparkConf.setAppName(s"OCSP_${taskConf.getName}")

    val sc = new SparkContext(sparkConf)

    //2 启动 streamingContext
    val ssc = new StreamingContext(sc, Seconds(taskConf.getReceive_interval))
    //    ssc.addStreamingListener(new ReceiveRecordNumListener())
    new TaskStopManager(ssc, taskConf.getId)

    //将ssc存放到sscManager中
    SscManager.initSsc(ssc)

    try {
      StreamTaskFactory.getStreamTask(taskConf).process(ssc)
      ssc.start()
      //4 update task status in db
      if (StreamingContextState.ACTIVE == ssc.getState()) {
        taskServer.updateHeartbeat(taskConf.getId)
        taskServer.startTask(taskConf.getId, sc.applicationId)
        logInfo("Start task " + taskConf.getId + " success !")
      }
      ssc.awaitTermination()
    } catch {
      case e: Exception => {
        e.printStackTrace()
        val res = Try(taskServer.checkMaxRetry(taskConf.getId))
        res match {
          case Success(maxRetry) => {
            if (maxRetry > 0) {
              logInfo("task : " + taskConf.getId + " got exception, task will retry")
              taskServer.RetryTask(taskConf.getId)
            }
            val exception_code = ExceptionConstant.ERR_JOB_EXCEPTION
            taskServer.insertExcepiton(taskId, taskConf.appID, exception_code, ExceptionConstant.getExceptionInfo(exception_code))
          }
          case Failure(e) => {
            logError("error to get task info from db " + e.getStackTrace)
          }
        }
     }
      case err: Error =>  {
        err.printStackTrace()
        logError("stream task goes wrong!" + err.getStackTrace)
      }
    } finally {
      ssc.stop()
      //若task的状态是PRE_RESTART 则将数据库中的task status设为1,准备启动;
      //否则将数据库中task status设为0,停止状态
      val res = Try(taskServer.checkTaskStatus(taskConf.getId))
      res match {
        case Success(status) => {
          if (TaskConstant.PRE_START == status || TaskConstant.PRE_RESTART == status ){
            taskServer.RestartTask(taskConf.getId)
            logInfo("Restarting task " + taskConf.getId + " ...")
          } else if (TaskConstant.RETRY != status ) {
            taskServer.stopTask(taskConf.getId)
            logInfo("Stop task " + taskConf.getId + " successfully !")
          }
        }
        case Failure(e) => {
          //do nothing when can not connect db
          logError("error to get task status from db " + e.getStackTrace)
        }
      }

      sys.exit()
    }

  }

}
