angular.module(PKG.name + '.feature.hydrator')
  .controller('HydratorDetailTopPanelController', function(MyPipelineDetailTopPanelService, rPipelineDetail, GLOBALS, $alert, $state) {
    this.runOnceLoading = false;
    this.app = rPipelineDetail;
    this.GLOBALS = GLOBALS;

    this.do = function (action) {
      switch (action) {
        case 'Start':
          this.appStatus = 'STARTING';
          MyPipelineDetailTopPanelService
            .start()
            .then(
              function success(type) {
                if (type === 'Batch') {
                  this.appStatus = 'SCHEDULED';
                  this.scheduleStatus = 'SCHEDULED';
                } else if (type === 'Realtime') {
                  this.appStatus = 'RUNNING';
                }
              }.bind(this),
              function error(err) {
                $alert({
                  type: 'danger',
                  content: err.data
                });
                this.appStatus = 'FAILED';
              }.bind(this)
            );
          break;
        case 'Stop':
          this.appStatus = 'STOPPING';
          MyPipelineDetailTopPanelService
            .stop()
            .then(
              function sucess(type) {
                if (type === 'Batch') {
                  this.scheduleStatus = 'SUSPENDED';
                }
                this.appStatus = 'SUSPENDED';
              }
            );
          break;
        case 'Run Once':
          this.runOnceLoading = true;
          MyPipelineDetailTopPanelService
            .start('once')
            .then(
              function success() {
                this.appStatus = 'RUNNING';
                this.runOnceLoading = false;
              }.bind(this),
              function error(err) {
                $alert({
                  type: 'danger',
                  content: err.data
                });
                this.runOnceLoading = false;
              }.bind(this)
            );
          break;
        case 'Delete':
          MyPipelineDetailTopPanelService
            .delete()
            .then(
              function success() {
                console.log('Successfully Deleted Hydrator App');
                $state.go('hydrator.list');
             },
             function error(err) {
               $alert({
                 type: 'danger',
                 content: (err && err.data)? err.data: err
               });
             }
           );
          break;
      }
    };

    MyPipelineDetailTopPanelService.registerForAppStatusChange(function(status) {
      this.appStatus = status;
    }.bind(this));
  });
