/*
 * Copyright © 2015 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

angular.module(PKG.name + '.commons')
  .directive('mySchedulesView', function() {
    return {
      restrict: 'EA',
      scope: {
        schedules: '=',
        isSchedulesDisabled: '@',
        onScheduleAction: '&',
        scheduleContext: '='
      },
      templateUrl: 'schedules-view/schedules-view.html',
      controller: function($scope) {
        $scope.disableSchedule = ($scope.isSchedulesDisabled === 'true');
        $scope.takeScheduleAction = function(schedule, action) {

            var fn = $scope.onScheduleAction();
            if ('undefined' !== typeof fn) {
              fn.call($scope.scheduleContext, schedule, action);
            }

        };
      }
    };
  });
