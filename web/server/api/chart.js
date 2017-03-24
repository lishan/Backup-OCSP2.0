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
  let runtimetimestamps = [];
  promises.push(Record.findAll({
    attributes: ["reserved_records", "dropped_records","timestamp", "application_id"],
    where: {task_id: taskid, archived: 0},
    order: 'timestamp ASC',
    limit: 120
  }).then((data) => {
    if(data !== null && data !== undefined && data.length > 0) {
      let appId = data[data.length-1].dataValues.application_id;
      for (let i in data) {
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
  promises.push(sequelize.query('select task_id, timestamp, batch_running_time_ms  as run_time , STREAM_TASK_MONITOR.application_id from STREAM_TASK_MONITOR, (select application_id from STREAM_TASK_MONITOR where archived=0 and task_id='+ req.params.id +' ORDER BY timestamp DESC limit 1) tmp where archived=0 and tmp.application_id=STREAM_TASK_MONITOR.application_id limit 120;',{
    type: sequelize.QueryTypes.SELECT
    }).then((data) => {
      if(data !== null && data !== undefined && data.length > 0) {
        for(let i in data) {
          let tmp = data[i];
          batchtime[0].push(tmp.run_time);
          runtimetimestamps.push(tmp.timestamp);
        }
      }
      batchtime[0].push(0);
    }
  ));

  sequelize.Promise.all(promises).then(()=>{
    res.status(200).send({result,timestamps,batchtime,runtimetimestamps});
  }, () => {
    res.status(500).send(trans.databaseError);
  });
});

router.get('/status', (req,res) => {
  Task.findAll({attributes: ['id', 'name', 'diid', 'status','start_time','stop_time']}).then((tasks) => {
    let status = [0, 0, 0, 0, 0, 0];
    let names = [];
    let batchtime = [];
    let tasknum = [];
    let running = [];
    let count = [[], []];
    let promises = [];
    let records = [[], []];
    let taskname = [];
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

    //dashboard batch time
    let _getBatchTime = function () {
      sequelize.query('select tmp.task_id, AVG(tmp.batch_running_time_ms) as run_time from (select * from STREAM_TASK_MONITOR where archived=0 ORDER BY timestamp DESC limit 10) tmp, (select application_id from STREAM_TASK_MONITOR where archived=0 ORDER BY timestamp DESC limit 1) tmp2 where tmp.application_id=tmp2.application_id group by tmp.task_id',{
        type: sequelize.QueryTypes.SELECT}
      ).then((data) => {
        for(let i in data) {
          let tmp = data[i];
          batchtime.push(tmp.run_time);
          tasknum.push(tmp.task_id);
        }
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
      for(let i in tasknum) {
        let tmp = tasknum[i]-1;
        taskname.push(tasks[tmp].dataValues.name);
      }
      running.push(0);
      count[0].push(0);
      count[1].push(0);
      records[0].push(0);
      records[1].push(0);
      res.status(200).send({status,names,batchtime,taskname,running,count,records});
    },()=>{
      res.status(500).send(trans.databaseError);
    });
  }, () => {
    res.status(500).send(trans.databaseError);
  });
});

module.exports = router;
