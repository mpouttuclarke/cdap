

class SearchController {
  constructor(myTagsApi, myAppsApi, myDatasetApi, myStreamApi, $stateParams) {
    this.myTagsApi = myTagsApi;
    this.myAppsApi = myAppsApi;
    this.myDatasetApi = myDatasetApi;
    this.myStreamApi = myStreamApi;
    this.$stateParams = $stateParams;
    this.tags = [];

    this.getAppsTags();
    this.getDatasetsTags();
    this.getStreamsTags();
  }

  getAppsTags() {
    let params = {
      namespace: this.$stateParams.namespace
    };
    this.myAppsApi
      .list(params)
      .$promise
      .then(
        (apps) => {
          apps.forEach((app) => {
            this.fetchAppTags(app.id);
            this.getProgramTags(app.id)
          });
        },
        () => { console.error('Apps Fetch errored'); }
      );
  }
  fetchAppTags(appId) {
    let params = {
      namespaceId: this.$stateParams.namespace,
      appId: appId
    };
    this.myTagsApi
      .getAppTags(params)
      .$promise
      .then(
        (appTags) => {
          this.tags = this.tags.concat(appTags);
        },
        () => { console.error('Error on fetching tags for app: ', appId); }
      );
  }
  getProgramTags(appId) {
    let params = {
      namespace: this.$stateParams.namespace,
      appId: appId
    };
    this.myAppsApi
      .get(params)
      .$promise
      .then(
        (app) => {
          app.programs.forEach( (program) => {
            this.fetchProgramTags(appId, program.id, program.type.toLowerCase());
          });
        },
        () => {
          console.error('Error fetching tags for programs');
        }
      );
  }
  getDatasetsTags() {
    let params = {
      namespace: this.$stateParams.namespace
    };
    this.myDatasetApi
      .list(params)
      .$promise
      .then(
        (datasets) => {
          datasets.forEach( (dataset) => { this.fetchDatasetTags(dataset.name); } );
        },
        () => { console.error('Error on fetching datasets tags '); }
      );
  }
  fetchDatasetTags(datasetId) {
    let params = {
      namespaceId: this.$stateParams.namespace,
      datasetId: datasetId
    };
    this.myTagsApi
      .getDatasetTags(params)
      .$promise
      .then(
        (datasetTags) => {
          this.tags = this.tags.concat(datasetTags);
        },
        () => { console.error('Fetching tags for dataset failed: ', datasetId); }
      );
  }
  getStreamsTags() {
    let params = {
      namespace: this.$stateParams.namespace
    };
    this.myStreamApi
      .list(params)
      .$promise
      .then(
        (streams) => { debugger; streams.forEach((stream) => { this.fetchStreamTags(stream.name); } ); },
        () => { console.error('Fetching streams tags failed'); }
      );
  }
  fetchStreamTags(streamId) {
    let params = {
      namespaceId: this.$stateParams.namespace,
      streamId: streamId
    };
    this.myTagsApi
      .getStreamTags(params)
      .$promise
      .then(
        (streamTags) => { this.tags = this.tags.concat(streamTags); },
        () => { console.error('Fetching stream tags failed for: ', streamId); }
      );
  }
  fetchProgramTags(appId, programId, programType) {
    let params = {
      namespaceId: this.$stateParams.namespace,
      appId: appId,
      programType: (programType !== 'mapreduce')? programType + 's': programType,
      programId: programId
    };
    this.myTagsApi
      .getProgramTags(params)
      .$promise
      .then(
        (programTags) => { this.tags = this.tags.concat(programTags); },
        () => { console.error('Fetching tags'); }
      );
  }
}

SearchController.$inject = ['myTagsApi', 'myAppsApi', 'myDatasetApi', 'myStreamApi', '$stateParams'];

angular.module(`${PKG.name}.feature.search`)
  .controller('SearchController', SearchController);
