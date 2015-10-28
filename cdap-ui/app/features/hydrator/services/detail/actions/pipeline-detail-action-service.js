angular.module(PKG.name + '.feature.hydrator')
  .service('MyPipelineDetailActionService', function(GLOBALS, myWorkFlowApi, myMapreduceApi, $state, myWorkersApi, $q, myAppsApi) {

    var appLevelParams;
    var scheduleLevelParams;
    var logsLevelParams;
    this.appStatusChangeListeners = [];
    this.init = function(app) {
      try {
        this.configJSON = JSON.parse(app.configuration);
      } catch(e) {
        this.configJSON = e;
        console.log('ERROR cannot parse configuration');
        return;
      }

      this.app = app;
      appLevelParams = {
        namespace: $state.params.namespace,
        appId: this.app.name
      };

      scheduleLevelParams = angular.extend({scheduleId: 'etlWorkflow'}, appLevelParams);
      logsLevelParams = angular.copy(appLevelParams);

      this.programType = app.artifact.name === GLOBALS.etlBatch ? 'WORKFLOWS' : 'WORKER';
      if (this.programType === 'WORKFLOWS') {
        this.api = myWorkFlowApi;
        this.logsApi = myMapreduceApi;
        this.schedule = this.configJSON.schedule;

        angular.forEach(app.programs, function (program) {
          if (program.type === 'Workflow') {
            appLevelParams.workflowId = program.id;
          } else if (program.type === 'Mapreduce') {
            logsLevelParams.programId = program.id;
            logsLevelParams.programType = 'mapreduce';
          }
        });
      } else {
        this.api = myWorkersApi;
        this.logsApi = myWorkersApi;
        this.instances = this.configJSON.instances;

        angular.forEach(app.programs, function (program) {
          if (program.type === 'Worker') {
            appLevelParams.workerId = program.id;
            logsLevelParams.programType = 'workers';
            logsLevelParams.programId = program.id;
          }
        });
      }
      this.pollStatus();
    };

    this.doStart = function() {
      var prom;
      if (this.programType === 'WORKFLOWS') {
        prom = myWorkFlowApi.scheduleResume(scheduleLevelParams, {}).$promise;
      } else {
        prom = myWorkersApi.doAction(angular.extend(appLevelParams, { action: 'start' }), {}).$promise;
      }
      return prom;
    };
    this.doRunOnce = function() {
      return myWorkFlowApi.doAction(angular.extend(appLevelParams, { action: 'start' }), {}).$promise;
    };
    this.start = function(type) {
      var defer;
      function success() {
        if (this.programType === 'WORKFLOWS') { defer.resolve('Batch');}
        else if (this.programType === 'WORKER') { defer.reject('Realtime');}
      }
      function error() {
        if (this.prograpType === 'WORKFLOWS') { defer.resolve('Batch');}
        else if (this.programType === 'WORKER') { defer.reject('Realtime');}
      }

      if (!type) {
        defer = $q.defer();
        this.doStart()
          .then(success, error);
      } else if (type === 'once') {
        defer = {
          promise: this.doRunOnce()
        };
      }
      return defer.promise;
    };
    this.stop = function() {
      var defer = $q.defer();
      function success() {
        if (this.programType === 'WORKFLOWS') { defer.resolve('Batch');}
        else if (this.programType === 'WORKER') { defer.reject('Realtime');}
      }
      function error() {
        if (this.prograpType === 'WORKFLOWS') { defer.resolve('Batch');}
        else if (this.programType === 'WORKER') { defer.reject('Realtime');}
      }

      if (this.programType === 'WORKFLOWS') {
        myWorkFlowApi
          .scheduleSuspend(scheduleLevelParams, {})
          .$promise
          .then(success, error);
      } else {
        myWorkersApi
          .doAction(angular.extend(appLevelParams, { action: 'stop' }), {})
          .$promise
          .then(success, error);
      }
      return defer.promise;
    };
    this.delete = function() {
      return myAppsApi.delete(appLevelParams).$promise;
    };


    this.pollStatus = function() {
      if (this.programType === 'WORKFLOWS') {
        myWorkFlowApi.pollStatus(appLevelParams)
        .$promise
        .then(function (res) {
          if (res.status === 'RUNNING') {
            this.setAppStatus(res.status);
          } else {
            myWorkFlowApi.getScheduleStatus({
              namespace: $state.params.namespace,
              appId: this.app.name,
              scheduleId: 'etlWorkflow'
            })
              .$promise
              .then(function (schedule) {
                this.setAppStatus(schedule.status);
                this.scheduleStatus = schedule.status;
              }.bind(this));
          }
        }.bind(this));
      } else if (this.programType === 'WORKER') {
        myWorkersApi.pollStatus(appLevelParams)
          .$promise
          .then(function (res) {
            this.setAppStatus(res.status === 'RUNNING' ? 'RUNNING' : 'SUSPENDED');
          }.bind(this));
      }
    };
    this.registerForAppStatusChange = function (callback) {
      this.appStatusChangeListeners.push(callback);
    };
    this.notifyAppStatusChange = function() {
      this.appStatusChangeListeners.forEach(function(listener) {
        listener(this.appStatus);
      }.bind(this));
    };
    this.setAppStatus = function (status) {
      this.appStatus = status;
      this.notifyAppStatusChange();
    };
    this.getAppStatus = function() {
      return this.appStatus;
    };

  });
