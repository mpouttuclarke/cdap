angular.module(PKG.name + '.feature.hydrator')
  .controller('HydratorDetailCanvasController', function(rPipelineDetail, MyAppDAGService) {

    this.nodes = [];
    function initializeDAG() {
      try{
        rPipelineDetail.config = JSON.parse(rPipelineDetail.configuration);
      } catch(e) {
        console.log('ERROR in configuration from backend: ', e);
        return;
      }
      MyAppDAGService.setNodesAndConnectionsFromDraft(rPipelineDetail);
    }

    initializeDAG.call(this);
  });
