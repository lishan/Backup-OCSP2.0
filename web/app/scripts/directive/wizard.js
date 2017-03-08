'use strict';
/**
 * Wizard directive
 */
angular.module('ocspApp').directive('wizard',['$filter', function($filter){
  return {
    restrict: 'E',
    templateUrl: 'views/directive/wizard.html',
    transclude: true,
    replace: true,
    scope:{
      submitMethod: "&",
      task: "="
    },
    link : function(scope, element, attrs) {
      var options = {
        contentWidth : attrs.width,
        contentHeight : 600,
        backdrop: 'static',
        buttons: {
          cancelText: $filter('translate')('ocsp_web_common_020'),
          nextText: $filter('translate')('ocsp_web_common_007'),
          backText: $filter('translate')('ocsp_web_common_008'),
          submitText: $filter('translate')('ocsp_web_common_021'),
          submittingText: $filter('translate')('ocsp_web_common_022')
        }
      };
      let wizardel = element.find(".wizard");
      wizardel.attr("data-title",attrs.title);
      wizardel.find("div.wizard-card").each(function(){
        var cardname = $(this).attr("data-cardname");
        $(this).prepend("<h3 style='display: none'>" + $filter('translate')(cardname) + "</h3>");
      });
      element.find("button.wizard-button").text($filter('translate')(attrs.name));
      var wizardModal = wizardel.wizard(options);
      scope.showModal = function(){
        wizardModal.show();
      };
      scope.$on('openModal',function(){
        wizardModal.show();
      });
      wizardModal.on("closed", function(){
        wizardModal.reset();
      });
      wizardModal.on("validate", function(wizard){
        let flag = true;
        wizard.el.find("div.popover.error-popover.fade.right.in").remove();
        wizard.el.find("input.ng-invalid").each(function(){
          $(this).addClass("ng-touched");
          wizardModal.errorPopover($(this), $filter('translate')('ocsp_web_common_035'));
          flag = false;
        });
        wizard.el.find("div.ng-invalid").each(function(){
          $(this).addClass("ng-touched");
          wizardModal.errorPopover($(this), $filter('translate')('ocsp_web_common_035'));
          flag = false;
        });
        return flag;
      });
      wizardModal.on("submit", function() {
        var promise = scope.submitMethod();
        promise.then(function () {
          wizardModal.submitSuccess();
          wizardModal.reset();
          wizardModal.close();
        }, function () {
          wizardModal.submitFailure();
          wizardModal.reset();
        });
      });
    }
  };
}]);
