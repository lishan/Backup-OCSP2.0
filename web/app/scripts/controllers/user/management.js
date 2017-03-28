'use strict';

/**
 * For label management main page controller
 */
angular.module('ocspApp')
  .controller('UserManagementCtrl', ['$scope', '$http', '$rootScope', '$filter', 'Notification', 'hotkeys', function ($scope, $http, $rootScope, $filter, Notification, hotkeys) {
    $rootScope.init('user');
    $scope.message = null;
    $scope.styles = null;
    $scope.user = {
      name: $rootScope.username
    };
    $scope.checkPassword = function(){
      if($scope.user.password !== undefined && $scope.user.password2 !== undefined && $scope.user.password === $scope.user.password2){
        $scope.message = null;
        $scope.styles = null;
        return true;
      }else{
        $scope.message = $filter('translate')('ocsp_web_user_manage_006');
        $scope.styles = "redBlock";
        return false;
      }
    };
    hotkeys.bindTo($scope).add({
      combo: 'enter',
      allowIn: ['INPUT', 'SELECT', 'TEXTAREA'],
      callback: function() {
        $scope.save();
      }
    });
    $scope.save = function(){
      if ($scope.checkPassword() && !$scope.mainForm.$invalid) {
        $http.post('/api/user/change', {"user": $scope.user}).success(function (data) {
          if(data.status) {
            $scope.user = {
              name: $rootScope.username
            };
            Notification.success($filter('translate')('ocsp_web_common_026'));
          }else{
            Notification.error($filter('translate')('ocsp_web_common_030'));
          }
        });
      }
    };
  }]);
