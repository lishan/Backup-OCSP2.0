"use strict";
let express = require('express');
let sequelize = require('../sequelize');
let Sequelize = require('sequelize');
let EventDef = require('../model/STREAM_EVENT')(sequelize, Sequelize);
let CEP = require('../model/STREAM_EVENT_CEP')(sequelize, Sequelize);
let config = require('../config');
let trans = config[config.trans || 'zh'];
let moment = require('moment');

let router = express.Router();
EventDef.hasOne(CEP, {foreignKey:'event_id', targetKey:'id'});

router.get('/diid/:id', function(req, res){
  EventDef.findAll({
    where:{
      diid : req.params.id
    }
  }).then(function (events){
    res.send(events);
  }, function(){
    res.status(500).send(trans.databaseError);
  });
});

function _createEvent(event, diid, status) {
  event.p_event_id = 0;
  //Only event contains PROPERTIES instead pf properties
  event.PROPERTIES = {"props": [], "output_dis": []};
  event.diid = event.task.diid;
  event.status = status;
  if(event.select_expr !== undefined && event.select_expr !== "") {
    event.select_expr = event.select_expr.replace(/\s/g, '');
  }
  if(event.delim === undefined){
    event.delim = "";
  }
  event.PROPERTIES.props.push({
    "pname" : "userKeyIdx",
    "pvalue" : 2
  });
  if(event.output !== undefined && event.output.id !== undefined) {
    event.PROPERTIES.output_dis.push({
      "diid": event.output.id,
      "interval" : event.interval,
      "delim": event.delim
    });
  }
  //Add data audit function
  if(event.audit !== undefined && event.audit.type !== undefined && event.audit.type!== "always" && event.audit.periods !== undefined && event.audit.periods.length > 0){
    let result = {
      period: event.audit.type,
      time:[]
    };
    for(let j = 0 ; j < event.audit.periods.length; j++){
      let sd = "0";
      let ed = "0";
      if(event.audit.type === 'none') {
        sd = moment(event.audit.periods[j].start).format("YYYY-MM-DD");
        ed = moment(event.audit.periods[j].end).format("YYYY-MM-DD");
      }else if(event.audit.type === 'week' || event.audit.type === 'month'){
        sd = event.audit.periods[j].s;
        ed = event.audit.periods[j].d;
      }
      let sh = moment(event.audit.periods[j].start).format("HH:mm:ss");
      let eh = moment(event.audit.periods[j].end).format("HH:mm:ss");
      result.time.push({
        begin:{
          d: sd,
          h: sh
        },
        end:{
          d: ed,
          h: eh
        }
      });
    }
    event.PROPERTIES.props.push({
      "pname" : "period",
      "pvalue" : JSON.stringify(result)
    });
  }
  event.PROPERTIES = JSON.stringify(event.PROPERTIES);
}

router.put('/:id', function(req, res){
  let username = req.query.username;
  let event = req.body.event;
  _createEvent(event, event.output.id, event.status ? 1 : 0);
  sequelize.transaction(function (t) {
    return sequelize.Promise.all([
      EventDef.update(event, {where: {id: event.id}, transaction: t}),
      CEP.update(event.cep, {where: {event_id: event.id}, transaction: t}),
    ])
  }).then(function(){
    res.send({success: true});
  }, function(){
    res.status(500).send(trans.databaseError);
  }).catch(function () {
    res.status(500).send(trans.databaseError);
  });
});

router.get('/findId/:id', function(req, res){
  let username = req.query.username;
  let usertype = req.query.usertype;
  if(usertype === "admin"){
    EventDef.find({
      where:{
        id: req.params.id
      },
      include: {
        model: CEP
      }
    }).then(function(event){
      res.send(event);
    }, function(){
      res.status(500).send(trans.databaseError);
    });
  }else{
    EventDef.find({
      where:{
        id: req.params.id,
        owner: username
      },
      include: {
        model: CEP
      }
    }).then(function(event){
      res.send(event);
    }, function(){
      res.status(500).send(trans.databaseError);
    });
  }
});

router.get('/all', function(req, res){
  let username = req.query.username;
  let usertype = req.query.usertype;
  if(usertype === "admin") {
    EventDef.findAll({
      include: {
        model: CEP
      }
    }).then(function(events){
      res.send(events);
    }, function(){
      res.status(500).send(trans.databaseError);
    });
  }else{
    EventDef.findAll({
      where:{
        owner: username
      },
      include: {
        model: CEP
      }
    }).then(function(events){
      res.send(events);
    }, function(){
      res.status(500).send(trans.databaseError);
    });
  }
});


module.exports = router;
