'use strict';
angular.module('ocspApp').factory('AuthInterceptor', ['$q', '$rootScope', '$filter', ($q, $rootScope, $filter)=> {
  return {
    'request': function(config) {
      function htmlRequest(url){
        return !!(url.endsWith(".html") || url.endsWith(".htm"));
      }
      let url = config.url;
      if(!htmlRequest(url) && config.method !== 'GET') {
        if ($rootScope.isAdmin()) {
          if (url.startsWith("/api/task")) {
            return $q.reject({data: $filter('translate')('ocsp_web_user_manage_008')});
          }
        } else {
          if (url.startsWith("/api/prop") || url.startsWith("/api/datasource")) {
            return $q.reject({data: $filter('translate')('ocsp_web_user_manage_008')});
          }
        }
      }
      if(!htmlRequest(url)) {
        config.url+=`?username=${$rootScope.getUsername()}&usertype=${$rootScope.isAdmin()? "admin": "user"}&token=${$rootScope.getToken()}`;
      }
      return config;
    }
  };
}]);
