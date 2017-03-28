package com.asiainfo.ocdp.stream.constant

import scala.collection.mutable

/**
  * Created by rainday on 3/8/17.
  */
object ExceptionConstant {

  val MainFramID = "0"

  val EXCEPTION_WARNING = 1
  val EXCEPTION_ERROR = 2
  val EXCEPTION_FATAL = 3

  val ERR_NONE = 0
  val ERR_SPARK_JOB_FINISHED = 1
  val ERR_JOB_EXCEPTION = 2
  val ERR_MAINFRAME_EXCEPTION = 2

  val EXCEPTION_CODE = mutable.Map[Int, String] (
    ERR_NONE -> "no exception",
    ERR_SPARK_JOB_FINISHED -> "ERROR! This spark app already exited!",
    ERR_JOB_EXCEPTION -> "ERROR! This stream had something wrong and exited after exception !",
    ERR_MAINFRAME_EXCEPTION-> "ERROR! MainFrame had exited after exception ! Please check the MainFrame log"
  )

  def getExceptionInfo(exception_code: Int): String = {
    EXCEPTION_CODE.getOrElse(exception_code, "no exception")
  }
}
