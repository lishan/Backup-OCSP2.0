
"use strict";

import express from 'express';
import sequelize from '../sequelize';
import Sequelize from 'sequelize';
import config from '../config';
let Task = require('../model/STREAM_TASK')(sequelize, Sequelize);
let EventDef = require('../model/STREAM_EVENT')(sequelize, Sequelize);
let Record = require('../model/STREAM_TASK_MONITOR')(sequelize, Sequelize);
let router = express.Router();
let trans = config[config.trans || 'zh'];

let _getRunningTime = function (tasks) {
  if (tasks !== undefined && tasks.length > 0) {
    let date = new Date();
    let sss = date.getTime();
    for (let i = 0; i < tasks.length; i++) {
      if(tasks[i].dataValues !== undefined && tasks[i].dataValues.start_time !== undefined &&
        tasks[i].dataValues.start_time !== null && tasks[i].dataValues.start_time !== "") {
        if(tasks[i].status === 2) {
          tasks[i].dataValues.running_time = parseInt(sss - tasks[i].dataValues.start_time);
        }else if(tasks[i].status === 0 && tasks[i].dataValues.stop_time !== undefined &&
          tasks[i].dataValues.stop_time !== null &&
          tasks[i].dataValues.stop_time !== ""){
          tasks[i].dataValues.running_time = parseInt(tasks[i].dataValues.stop_time - tasks[i].dataValues.start_time);
        }else{
          tasks[i].dataValues.running_time = null;
        }
      }
    }
  }
};

router.get('/taskData/:id',(req,res)=>{
  let taskid = req.params.id;
  let promises = [];
  let timestamps= [];
  let result = [[],[]];
  let batchtime = [[]];//for chart with type 'line' data must be in double array
  let mem_storage=[[],[]];
  let runtimetimestamps = [];
  promises.push(Record.findAll({
    attributes: ["reserved_records","dropped_records","timestamp","application_id"],
    where: {task_id: taskid, archived: 0},
    order: 'timestamp DESC',
    limit: 120
  }).then((data) => {
    if(data !== null && data !== undefined && data.length > 0) {
      let appId = data[data.length-1].dataValues.application_id;
      for (let i = data.length - 1; i >= 0; i--) {
        if(data[i].application_id === appId) {
          result[0].push(data[i].dataValues.reserved_records);
          result[1].push(data[i].dataValues.dropped_records);
          timestamps.push(data[i].dataValues.timestamp);
        }
      }
    }
    result[0].push(0);
    result[1].push(0);
  }));

  //task batch time
  promises.push(sequelize.query(
    'select task_id, timestamp, batch_running_time_ms as run_time, used_storage_memory as use_mem, max_storage_memory as rem_mem,  STREAM_TASK_MONITOR.application_id from STREAM_TASK_MONITOR, (select application_id from STREAM_TASK_MONITOR where archived=0 and task_id=' + req.params.id +' ORDER BY timestamp DESC limit 1) tmp where archived=0 and tmp.application_id=STREAM_TASK_MONITOR.application_id ORDER BY timestamp DESC limit 120;',
    {type: sequelize.QueryTypes.SELECT
  }).then((data) => {
    if(data !== null && data !== undefined && data.length > 0) {
      for(let i in data) {
        let tmp = data[i];
        batchtime[0].push(tmp.run_time?  (Number(tmp.run_time)/ 1000).toFixed(2): 0); //convert ms to s
        mem_storage[0].push(tmp.use_mem?  (tmp.use_mem/ 1024).toFixed(2): 0); //convert B to KB
        mem_storage[1].push(tmp.rem_mem?  (tmp.rem_mem/ 1024).toFixed(2): 0);
        runtimetimestamps.push(tmp.timestamp);
      }
      batchtime[0].reverse();
      mem_storage[0].reverse();
      mem_storage[1].reverse();
      runtimetimestamps.reverse();
    }
    batchtime[0].push(0);
    mem_storage[0].push(0);
    mem_storage[1].push(0);
  }));

  sequelize.Promise.all(promises).then(()=>{
    res.status(200).send({result,timestamps,batchtime,mem_storage,runtimetimestamps});
  }, () => {
    res.status(500).send(trans.databaseError);
  });
});

router.get('/status', (req,res) => {
  Task.findAll({attributes: ['id', 'name', 'diid', 'status','start_time','stop_time']}).then((tasks) => {
    let status = [0, 0, 0, 0, 0, 0];
    let names = [];
    let mem_storage=[[],[]];
    let batchtime = [];
    let running = [];
    let count = [[], []];
    let promises = [];
    let records = [[], []];
    let _getData = function (i) {
      promises.push(EventDef.count({where: {diid: tasks[i].diid, status: 1}}).then((data) => {
        tasks[i].dataValues.count1 = data;
      }));
      promises.push(EventDef.count({where: {diid: tasks[i].diid, status: 0}}).then((data) => {
        tasks[i].dataValues.count2 = data;
      }));
      promises.push(Record.find({
        attributes: ["reserved_records", "dropped_records"],
        where: {task_id: tasks[i].id, archived: 0},
        order: 'timestamp DESC'
      }).then((data) => {
        if (data !== null && data !== undefined && data.dataValues !== undefined) {
          tasks[i].dataValues.reserved = data.dataValues.reserved_records;
          tasks[i].dataValues.dropped = data.dataValues.dropped_records;
        }
      }));
    };
    //batch time & storage memory
    let _getBatchTime = function () {
      sequelize.query('SELECT STREAM_TASK_MONITOR.timestamp,' +
                      'STREAM_TASK_MONITOR.task_id,'+
                      'STREAM_TASK_MONITOR.application_id,'+
                      'STREAM_TASK_MONITOR.batch_running_time_ms as run_time,'+
                      'STREAM_TASK_MONITOR.max_storage_memory as rem_mem,'+
                      'STREAM_TASK_MONITOR.used_storage_memory as use_mem '+
                      'from (SELECT max(timestamp) as maxtimestamp, task_id '+
                      'FROM STREAM_TASK_MONITOR GROUP BY task_id ) a,' +
                      'STREAM_TASK_MONITOR where a.maxtimestamp=STREAM_TASK_MONITOR.timestamp and '+
                      'a.task_id=STREAM_TASK_MONITOR.task_id and '+
                      'STREAM_TASK_MONITOR.archived=0;',{
      type: sequelize.QueryTypes.SELECT}
      ).then((data) => {
        if(data !== null && data !== undefined && data.length > 0) {
          for(let i in data) {
            let tmp = data[i];
            let tmpId = tmp.task_id -1;
            batchtime[tmpId] = tmp.run_time?  (Number(tmp.run_time)/ 1000).toFixed(2): 0; //convert ms to s
            mem_storage[0][tmpId]=tmp.use_mem?  (tmp.use_mem/ 1024).toFixed(2): 0; //convert B to KB
            mem_storage[1][tmpId]=tmp.rem_mem?  (tmp.rem_mem/ 1024).toFixed(2): 0;
          }
        }
        //if no data
        batchtime.push(0);
        mem_storage[0].push(0);
        mem_storage[1].push(0);
      });
    };

    if(tasks !== undefined && tasks.length > 0){
      _getRunningTime(tasks);
      _getBatchTime();
      for(let i in tasks) {
        _getData(i);
      }
    }

    sequelize.Promise.all(promises).then(()=>{
      for(let i in tasks) {
        let tmp = tasks[i].dataValues;
        running.push(tmp.running_time?  (tmp.running_time/ 60000).toFixed(2): 0);
        names.push(tmp.name? tmp.name : 0);
        count[0].push(tmp.count1? tmp.count1 : 0);
        count[1].push(tmp.count2? tmp.count2 : 0);
        records[0].push(tmp.reserved? tmp.reserved: 0);
        records[1].push(tmp.dropped? tmp.dropped: 0);
        if (tmp.status >= 0 && tmp.status < 6) {
          status[tmp.status]++;
        }
      }
      running.push(0);
      count[0].push(0);
      count[1].push(0);
      records[0].push(0);
      records[1].push(0);
      res.status(200).send({status,names,batchtime,running,count,records,mem_storage});
    },()=>{
      res.status(500).send(trans.databaseError);
    });
  }, () => {
    res.status(500).send(trans.databaseError);
  });
});

module.exports = router;

