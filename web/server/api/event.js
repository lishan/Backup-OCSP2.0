"use strict";
let express = require('express');
let sequelize = require('../sequelize');
let Sequelize = require('sequelize');
let EventDef = require('../model/STREAM_EVENT')(sequelize, Sequelize);
let config = require('../config');
let trans = config[config.trans || 'zh'];

let router = express.Router();

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


module.exports = router;
