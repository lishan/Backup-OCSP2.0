'use strict';

/**
 * For label management main page controller
 */
angular.module('ocspApp')
  .controller('LabelManagementCtrl', ['$scope', '$http', 'Upload', 'Notification', '$timeout', '$rootScope', '$filter', function ($scope, $http, Upload, Notification, $timeout, $rootScope, $filter) {
    $rootScope.init('label');
    function init() {
      $scope.message = null;
      $http.get('/api/label').success(function (data) {
        $scope.labels = data;
      });
    }

    init();

    $scope.upload = function(){
      if($scope.file !== undefined && $scope.file !== "") {
        $scope.uploadFile($scope.file);
      }
    };

    $scope.save = function(){
      $http.post("/api/label", {labels: $scope.labels}).success(function(){
        Notification.success($filter('translate')('ocsp_web_common_026'));
      });
    };

    $scope.uploadFile = function (file) {
      Upload.upload({
        url:'/api/label/upload',
        data: {file: file, username: 'jar'}
      }).then(function () {
          $timeout(function(){
            init();
            Notification.success($filter('translate')('ocsp_web_common_028'));
          }, 1000);
        }, function (err) {
          $scope.message = err.data;
        });
    };

    $scope.owner = (label) => {
      return label.owner !== $rootScope.getUsername();

    };

  }]);
