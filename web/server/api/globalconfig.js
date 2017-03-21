"use strict";
let express = require('express');
let config = require('../config');
let router = express.Router();

router.get('/links', function(req, res){
  res.send(config.quickLinks);
});

module.exports = router;
