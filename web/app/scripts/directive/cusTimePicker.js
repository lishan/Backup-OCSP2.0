'use strict';
/**
 * Confirmation Block Directive
 */
angular.module('ocspApp')
.directive('ocspInputShowTime', function () {
  return {
    scope: {
      bindModel: '=ngModel'
    },
    restrict: 'A',
    require: '^ngModel',
    link: function link($scope, elem, attrs, ngModelCtrl) {
      var get2CharValue = function get2CharValue(orgValue) {
        if (String(orgValue).length === 1) {
          return "0" + String(orgValue);
        } else {
          return String(orgValue);
        }
      };
      ngModelCtrl.$render = function () {
        if($scope.bindModel === undefined || $scope.bindModel===null || !($scope.bindModel instanceof Date)){
          $scope.bindModel = new Date();
        }
        var outputVal = "" + get2CharValue($scope.bindModel.getHours()) + ":" + get2CharValue($scope.bindModel.getMinutes()) + ":" + get2CharValue($scope.bindModel.getSeconds());
        elem.val(outputVal);
      };
    }
  };
})
.directive('ocspTimePicker', function () {
  return {
    scope: {
      bindModel: '=ngModel',
      change:'&'
    },
    restrict: 'AE',
    require: '^ngModel',
    replace: true,
    controller:function($scope){
        $scope.changed = function(){
            $scope.change();
        };
    },
    template: "<div><div class=\"btn-group\" uib-dropdown><p class=\"input-group\"><input id=\"split-button\" ocsp-input-show-time=\"\" type=\"text\" class=\"form-control\" ng-model=\"bindModel\" onfocus=this.blur() required><span class=\"input-group-btn\"><button type=\"button\" class=\"btn btn-default\" uib-dropdown-toggle><i class=\"glyphicon glyphicon-time\"></i></button></span></p><ul class=\"dropdown-menu\" uib-dropdown-menu role=\"menu\" aria-labelledby=\"split-button\" ng-click=\"$event.preventDefault(); $event.stopPropagation();updateTime()\"><div uib-timepicker show-seconds=\"true\" show-meridian=\"false\" ng-change=\"changed()\" ng-model=\"bindModel\"></div></ul></div></div>"
  };
});
