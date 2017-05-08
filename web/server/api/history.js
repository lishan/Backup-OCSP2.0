"use strict";
let express = require('express');
let sequelize = require('../sequelize');
let Sequelize = require('sequelize');
let History = require('../model/STREAM_HISTORY_CONFIG')(sequelize, Sequelize);
let config = require('../config');
let trans = config[config.trans || 'zh'];

let router = express.Router();

router.post('/event', function(req, res){
  let usertype = req.query.usertype;
  let username = req.query.username;
  if(usertype === "admin") {
    let event = req.body.event;
    event.component_name = "event";
    event.user_name = username;
    event.config_data = JSON.stringify(event.config_data);
    History.create(event).then(function(){
      res.send({success: true});
    }, function(){
      res.status(500).send(trans.databaseError);
    });
  }else{
    res.status(500).send(trans.authError);
  }
});

module.exports = router;
