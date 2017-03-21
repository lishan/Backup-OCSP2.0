"use strict";
let express = require('express');
let router = express.Router();
let multer  = require('multer');
let fs = require('fs');
let unzip = require('unzip2');
let sequelize = require('../sequelize');
let Sequelize = require('sequelize');
let Label = require('../model/STREAM_LABEL_DEFINITION')(sequelize, Sequelize);
let LabelRefer = require('../model/STREAM_LABEL')(sequelize, Sequelize);
let config = require('../config');
let path = require('path');
let trans = config[config.trans || 'zh'];

let storage = multer.diskStorage({
  destination: './uploads/',
  filename: function (req, file, cb) {
    cb(null, "tmpOran.jar");
  }
});
let upload = multer({ storage: storage });

router.get('/', function(req, res){
  let username = req.query.username;
  let user = req.query.user;
  if(user === "admin") {
    Label.findAll().then(function (labels) {
      res.send(labels);
    }, function () {
      res.status(500).send(trans.databaseError);
    });
  }else{
    Label.findAll({where: {$or: [{owner: username}, {owner: "ocspadmin"}]}}).then(function (labels) {
      res.send(labels);
    }, function () {
      res.status(500).send(trans.databaseError);
    });
  }
});

router.post('/', function(req, res){
  let username = req.query.username;
  let labels = req.body.labels;
  let promises = [];
  for(let i in labels){
    if(labels[i].owner === username) {// only owner can change label properties
      promises.push(Label.update(labels[i], {where: {id: labels[i].id}}));
    }
  }
  sequelize.Promise.all(promises).then(function(){
    res.send({success : true});
  },function(){
    res.status(500).send(trans.databaseError);
  }).catch(function(){
    res.status(500).send(trans.databaseError);
  });

});

router.get('/diid/:id', function(req, res){
  LabelRefer.findAll({
    where:{
      diid : req.params.id
    },
    order: '`p_label_id` ASC'
  }).then(function (labels){
    res.send(labels);
  }, function(){
    res.status(500).send(trans.databaseError);
  });
});

router.post('/upload', upload.single('file'), function(req, res){
  let username = req.query.username;
  let result = [];
  let promise = new sequelize.Promise((resolve, reject) => {
    fs.createReadStream('./uploads/tmpOran.jar')
      .pipe(unzip.Parse())
      .once('error', function () {
        reject("Cannot parse file " + req.file.originalname);
      })
      .on('entry', function (entry) {
        let filename = entry.path;
        let filetype = entry.type;
        if (filetype === 'File' && filename.endsWith('.class') && !filename.includes("$")) {
          filename = filename.replace(/\.class/g, "");
          filename = filename.replace(/\//g, "\.");
          let index = filename.lastIndexOf(".");
          if(index > 0) {
            result.push({
              name: filename.substr(index + 1),
              classname: filename
            });
          }else{
            reject("Filename error " + filename);
          }
        }
        entry.autodrain();
      })
      .on('close', ()=> {
        resolve();
      });
  });
  promise.then(() => {
    if(fs.existsSync('./uploads/' + req.file.originalname)){
      sequelize.transaction(function (t) {
        let promises = [];
        for (let i in result) {
          promises.push(Label.findOrCreate({
            where:{name: result[i].name},
            defaults:{class_name: result[i].classname, owner: username},
            transaction: t
          }));
        }
        return sequelize.Promise.all(promises);
      }).then(() => {
        fs.rename('./uploads/tmpOran.jar', './uploads/' + req.file.originalname, (err) => {
          if (err) {
            fs.unlink('./uploads/tmpOran.jar', () => {
              res.status(500).send(trans.uploadError + path.join(__dirname, "../../uploads"));
            });
          } else {
            fs.unlink('./uploads/tmpOran.jar', () => {
              res.status(200).send({success: true});
            });
          }
        });
      }).catch(() => {
        fs.unlink('./uploads/tmpOran.jar', () => {
          res.status(500).send(trans.uploadError + path.join(__dirname, "../../uploads"));
        });
      });
    }else {
      sequelize.transaction(function (t) {
        let promises = [];
        for (let i in result) {
          promises.push(Label.create({
            name: result[i].name, //This is a unique key, cannot insert duplicate
            class_name: result[i].classname,
            owner: username
          }, {
            transaction: t
          }));
        }
        return sequelize.Promise.all(promises);
      }).then(() => {
        fs.rename('./uploads/tmpOran.jar', './uploads/' + req.file.originalname, (err) => {
          if (err) {
            fs.unlink('./uploads/tmpOran.jar', () => {
              res.status(500).send(trans.uploadError + path.join(__dirname, "../../uploads"));
            });
          } else {
            res.status(200).send({success: true});
          }
        });
      }).catch(() => {
        fs.unlink('./uploads/tmpOran.jar', () => {
          res.status(500).send(trans.uploadError + path.join(__dirname, "../../uploads"));
        });
      });
    }
  },() => {
    fs.unlink('./uploads/tmpOran.jar', () => {
      res.status(500).send(trans.uploadError + path.join(__dirname,"../../uploads"));
    });
  });
});

module.exports = router;
