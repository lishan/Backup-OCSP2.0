'use strict';

angular.module('ocspApp')
  .controller('DashboardCtrl',['$scope', 'loginService', '$http', 'Notification', 'usSpinnerService', '$filter', ($scope, loginService, $http, Notification, usSpinnerService, $filter)=>{
    loginService.init('dashboard');

    usSpinnerService.spin('spinner');
    $scope.labels = [
      $filter('translate')('ocsp_web_streams_manage_032'),
      $filter('translate')('ocsp_web_streams_manage_033'),
      $filter('translate')('ocsp_web_streams_manage_034'),
      $filter('translate')('ocsp_web_streams_manage_035'),
      $filter('translate')('ocsp_web_streams_manage_036'),
      $filter('translate')('ocsp_web_streams_manage_044')
    ];
    $http.get('/api/chart/status').success((data)=>{
      $scope.status = data.status;
      $scope.names = data.names;
      $scope.running = data.running;
      $scope.count = data.count;
      $scope.records = data.records;
      $scope.series = [$filter('translate')('ocsp_web_dashboard_reserved'), $filter('translate')('ocsp_web_dashboard_dropped')];
      usSpinnerService.stop('spinner');
    }, (err)=> {
      Notification.error(err.data);
      usSpinnerService.stop('spinner');
    });

    $scope.test = 123;
  }]);
