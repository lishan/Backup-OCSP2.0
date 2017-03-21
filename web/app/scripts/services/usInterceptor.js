'use strict';
angular.module('ocspApp').factory('UsInterceptor', ['$q', 'usSpinnerService', '$injector' ,($q, usSpinnerService, $injector)=>{
  return {
    'request': (config) => {
      let url = config.url;
      if(!url.startsWith("/api/task/status") && !url.startsWith("/api/chart/taskData/")) {
        usSpinnerService.spin('spinner');
      }
      return config;
    },
    'response': (response) => {
      usSpinnerService.stop('spinner');
      return response;
    },
    'responseError': (reason) => {
      //Use $injector to load service dynamically in case of circle dependencies
      let Notification = $injector.get('Notification');
      usSpinnerService.stop('spinner');
      Notification.error(reason.data);
      return $q.reject(reason);
    },
  };
}]);
