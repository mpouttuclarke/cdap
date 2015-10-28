angular.module(PKG.name + '.feature.hydrator')
  // .controller('HydratorDetailBottomPanelController', function(HydratorDetail, MyAppDAGService, $timeout, MyNodeConfigService, $state, rPipelineDetail, $scope, myWorkFlowApi, myWorkersApi) {
  .controller('HydratorDetailBottomPanelController', function() {
    // MyAppDAGService.registerEditPropertiesCallback(viewProperties.bind(this));
    // this.tabs = [
    //   {
    //     title: 'Status',
    //     template: '/assets/features/hydrator/templates/detail/tabs/status.html'
    //   },
    //   {
    //     title: 'History',
    //     template: '/assets/features/hydrator/templates/detail/tabs/history.html'
    //   },
    //   {
    //     title: 'Log',
    //     template: '/assets/features/hydrator/templates/detail/tabs/log.html'
    //   },
    //   {
    //     title: 'Metrics',
    //     template: '/assets/features/hydrator/templates/detail/tabs/metrics.html'
    //   },
    //   {
    //     title: 'Configuration',
    //     template: '/assets/features/hydrator/templates/detail/tabs/configuration.html'
    //   },
    //   {
    //     title: 'Datasets',
    //     template: '/assets/features/hydrator/templates/detail/tabs/datasets.html'
    //   },
    //   {
    //     title: 'Node Configuration',
    //     template: '/assets/features/hydrator/templates/detail/tabs/node-configuration.html'
    //   }
    // ];
    //
    // function checkCron(cron) {
    //   var pattern = /^[0-9\*\s]*$/g;
    //   var parse = cron.split('');
    //   for (var i = 0; i < parse.length; i++) {
    //     if (!parse[i].match(pattern)) {
    //       return false;
    //     }
    //   }
    //   return true;
    // }
    //
    // if (HydratorDetail.programType === 'WORKFLOWS') {
    //   this.schedule = HydratorDetail.schedule;
    //   this.isBasic = checkCron(this.schedule);
    //   this.tabs.push({
    //     title: 'Schedule',
    //     template: '/assets/features/hydrator/templates/tabs/schedule.html'
    //   });
    // } else {
    //   this.instances = HydratorDetail.instances;
    //   this.tabs.push({
    //     title: 'Instance',
    //     template: '/assets/features/hydrator/templates/tabs/instance.html'
    //   });
    // }
    //
    // this.activeTab = this.tabs[0];
    // this.selectTab = function(tab) {
    //   this.activeTab = tab;
    // };
    //
    // function viewProperties(plugin) {
    //   this.selectTab(this.tabs[6]);
    //   // Giving 100ms to load the template and then set the plugin
    //   // For this service to work the controller has to register a callback
    //   // with the service. The callback will not be called if plugin assignment happens
    //   // before controller initialization. Hence the 100ms delay.
    //   $timeout(function() {
    //     MyNodeConfigService.setPlugin(plugin);
    //   }, 100);
    // }
    // var params = {
    //   namespace: $state.params.namespace,
    //   appId: rPipelineDetail.name,
    //   scope: $scope
    // };
    //
    // if (HydratorDetail.programType === 'WORKFLOWS') {
    //   angular.forEach(rPipelineDetail.programs, function (program) {
    //     if (program.type === 'Workflow') {
    //       params.workflowId = program.id;
    //     }
    //   });
    //
    //   myWorkFlowApi.pollStatus(params)
    //     .$promise
    //     .then(function (res) {
    //       if (res.status === 'RUNNING') {
    //         this.appStatus = res.status;
    //       } else {
    //         myWorkFlowApi.getScheduleStatus({
    //           namespace: $state.params.namespace,
    //           appId: rPipelineDetail.name,
    //           scheduleId: 'etlWorkflow',
    //           scope: $scope
    //         })
    //           .$promise
    //           .then(function (schedule) {
    //             this.appStatus = schedule.status;
    //             this.scheduleStatus = schedule.status;
    //           });
    //       }
    //     });
    //
    // } else {
    //
    //   angular.forEach(rPipelineDetail.programs, function (program) {
    //     if (program.type === 'Worker') {
    //       params.workerId = program.id;
    //     }
    //   });
    //
    //   myWorkersApi.pollStatus(params)
    //     .$promise
    //     .then(function (res) {
    //       this.appStatus = res.status === 'RUNNING' ? 'RUNNING' : 'SUSPENDED';
    //     });
    // }
    //
    // this.datasets = [];
    // this.datasets = this.datasets.concat(
    //   rPipelineDetail.datasets.map(function (dataset) {
    //     dataset.type = 'Dataset';
    //     return dataset;
    //   }),
    //   rPipelineDetail.streams.map(function (stream) {
    //     stream.type = 'Stream';
    //     return stream;
    //   }));

  });
