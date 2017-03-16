//app.js
"use strict";
var express = require('express');
var app = express();
var bodyParser = require('body-parser');
var config = require('./config');
var env = config.env || 'dev';
var path = require('path');
var favicon = require('serve-favicon');

if(env === 'dev') {
  app.use(require('connect-livereload')());
  app.use("/fonts",express.static("app/bower_components/bootstrap/fonts"));
}

if(env === "express_only") {
  // Make express server support CORS request in another server
  app.use(function(req, res, next) {
    res.header("Access-Control-Allow-Origin", "*"); //It need port also, like http://localhost:8080
    res.header("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept");
    next();
  });
}else{ //web artifact
  app.use(express.static(config[env].dist));
  app.use(favicon(path.join(__dirname, '../', config[env].dist, '/favicon.ico')));
}

app.use(bodyParser.json());       // to support JSON-encoded bodies
app.use(bodyParser.urlencoded({     // to support URL-encoded bodies
  extended: true
}));

// rest api
app.use('/api/task', require('./api/task'));
app.use('/api/label', require('./api/label'));
app.use('/api/prop', require('./api/properties'));
app.use('/api/datasource', require('./api/datasource'));
app.use('/api/datainterface', require('./api/datainterface'));
app.use('/api/event', require('./api/event'));
app.use('/api/config', require('./api/globalconfig'));
app.use('/api/user', require('./api/user'));
app.use('/api/chart', require('./api/chart'));

app.get('*', function(req, res) {
  res.sendFile(path.join(__dirname, '../',config[env].dist,'/404.html'));// load the single view file (angular will handle the page changes on the front-end)
});

app.listen(config[env].port, function () {
  console.log('App listening on port ' + config[env].port + "!");
});

module.exports = app;
