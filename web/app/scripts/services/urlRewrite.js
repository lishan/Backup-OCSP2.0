'use strict';
angular.module('ocspApp').factory('UrlRewriteInterceptor', function(){
  return {
    'request': function(config) {
      // let restServer = "http://localhost:9000";
      let restServer = "";
      if(!config.url.endsWith('.html') && restServer !== "") {
        config.url = restServer + config.url;
      }
      return config;
    }
  };
});
