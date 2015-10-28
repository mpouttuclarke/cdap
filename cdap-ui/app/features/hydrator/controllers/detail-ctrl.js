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

angular.module(PKG.name + '.feature.hydrator')
  .controller('HydratorDetailController', function(MyPipelineProgramService, rPipelineDetail) {
    MyPipelineProgramService.init(rPipelineDetail);
    // HydratorDetail.initialize(rPipelineDetail, $state);
    // Ideally this guy should set the service with config parameters so that the service can be used by all the three sections.
    // This will determine whether its etlbatch or realtime and will decide what to use (workflows, mapreduce, spark or workers);
  });
