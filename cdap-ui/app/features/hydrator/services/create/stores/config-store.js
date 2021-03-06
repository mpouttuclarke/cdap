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

class ConfigStore {
  constructor(ConfigDispatcher, CanvasFactory, GLOBALS, mySettings, ConsoleActionsFactory){
    this.state = {};
    this.mySettings = mySettings;
    this.ConsoleActionsFactory = ConsoleActionsFactory;
    this.CanvasFactory = CanvasFactory;
    this.GLOBALS = GLOBALS;
    this.changeListeners = [];
    this.setDefaults();
    this.configDispatcher = ConfigDispatcher.getDispatcher();
    this.configDispatcher.register('onArtifactSave', this.setArtifact.bind(this));
    this.configDispatcher.register('onMetadataInfoSave', this.setMetadataInformation.bind(this));
    this.configDispatcher.register('onPluginAdd', this.setConfig.bind(this));
    this.configDispatcher.register('onPluginEdit', this.editNodeProperties.bind(this));
    this.configDispatcher.register('onSetSchedule', this.setSchedule.bind(this));
    this.configDispatcher.register('onSetInstance', this.setInstance.bind(this));
    this.configDispatcher.register('onSaveAsDraft', this.saveAsDraft.bind(this));
    this.configDispatcher.register('onInitialize', this.init.bind(this));
  }
  registerOnChangeListener(callback) {
    this.changeListeners.push(callback);
  }
  emitChange() {
    this.changeListeners.forEach( callback => callback() );
  }
  setDefaults(config) {

    this.state = {
      artifact: {
        name: '',
        scope: 'SYSTEM',
        version: ''
      },
      config: {
        source: {
          name: '',
          plugin: {}
        },
        sinks: [],
        transforms: [],
        connections: []
      },
      __ui__: {
        nodes: [],
        isEditing: true
      },
      description: '',
      name: ''
    };
    // This will be eventually used when we just pass on a config to the store to draw the dag.
    if (config) {
      angular.extend(this.state, config);
    }
  }
  init(config) {
    this.setDefaults(config);
  }
  getDefaultConfig() {
    return {
      source: {
        name: '',
        plugin: {}
      },
      sinks: [],
      transforms: [],
      connections: []
    };
  }

  setState(state) {
    this.state = state;
  }
  getState() {
    return angular.copy(this.state);
  }
  getArtifact() {
    return this.getState().artifact;
  }
  getAppType() {
    return this.getState().artifact.name;
  }
  getConnections() {
    return this.getConfig().connections;
  }
  getConfig() {
    return this.getState().config;
  }
  generateConfigFromState() {
    var config = this.getDefaultConfig();
    var artifactTypeExtension = this.GLOBALS.pluginTypes[this.state.artifact.name];
    var nodesMap = {};
    this.state.__ui__.nodes.forEach(function(n) {
      nodesMap[n.id] = n;
    });

    function addPluginToConfig(plugin, id) {
      var pluginConfig =  {
        // Solely adding id and _backendProperties for validation.
        // Should be removed while saving it to backend.
        id: plugin.id,
        name: plugin.name,
        label: plugin.label,
        properties: plugin.properties,
        _backendProperties: plugin._backendProperties,
        outputSchema: plugin.outputSchema
      };

      if (plugin.type === artifactTypeExtension.source) {
        config['source'] = {
          name: pluginConfig.id,
          plugin: pluginConfig
        };
      } else if (plugin.type === 'transform') {
        if (plugin.errorDatasetName && plugin.errorDatasetName.length > 0) {
          pluginConfig.errorDatasetName = plugin.errorDatasetName;
        }
        if (plugin.validationFields) {
          pluginConfig.validationFields = plugin.validationFields;
        }
        pluginConfig = {
          name: pluginConfig.id,
          plugin: pluginConfig
        };
        config['transforms'].push(pluginConfig);
      } else if (plugin.type === artifactTypeExtension.sink) {
        pluginConfig = {
          name: pluginConfig.id,
          plugin: pluginConfig
        };
        config['sinks'].push(pluginConfig);
      }
      delete nodesMap[id];
    }
    var connections = this.CanvasFactory.orderConnections(
      angular.copy(this.state.config.connections),
      this.state.artifact.name,
      this.state.__ui__.nodes
    );

    connections.forEach( connection => {
      if (nodesMap[connection.from]) {
          addPluginToConfig(nodesMap[connection.from], connection.from);
      }
      if (nodesMap[connection.to]) {
        addPluginToConfig(nodesMap[connection.to], connection.to);
      }
    });
    let appType = this.getAppType();
    if ( appType=== this.GLOBALS.etlBatch) {
      config.schedule = this.state.config.schedule;
    } else if (appType === this.GLOBALS.etlRealtime) {
      config.instance = this.state.config.instance;
    }
    config.connections = connections.map(conn => {
      delete conn.visited;
      return conn;
    });
    return config;
  }
  getConfigForExport() {
    var config = angular.copy(this.generateConfigFromState());
    this.CanvasFactory.pruneProperties(config);
    this.state.config = angular.copy(config);
    return angular.copy(this.state);
  }
  getDisplayConfig() {
    var stateCopy = this.getConfigForExport();
    var source = stateCopy.config.source;
    var sinks = stateCopy.config.sinks;
    var transforms = stateCopy.config.transforms;

    if (source.plugin) {
      delete source.plugin.outputSchema;
    }
    angular.forEach(sinks, (sink) => {
      if (sink.plugin) {
        delete sink.plugin.outputSchema;
      }
    });
    angular.forEach(transforms, (transform) =>  {
      if (transform.plugin) {
        delete transform.plugin.outputSchema;
      }
    });
    delete stateCopy.__ui__;
    return stateCopy;
  }
  getDescription() {
    return this.getState().description;
  }
  getName() {
    return this.getState().name;
  }
  setName(name) {
    this.state.name = name;
    this.emitChange();
  }
  setDescription(description) {
    this.state.description = description;
    this.emitChange();
  }
  setMetadataInformation(name, description) {
    this.state.name = name;
    this.state.description = description;
    this.emitChange();
  }
  setConfig(config, type) {
    switch(type) {
      case 'source':
        this.state.config.source = config;
        break;
      case 'sink':
        this.state.config.sinks.push(config);
        break;
      case 'transform':
        this.state.config.transforms.push(config);
        break;
    }
    this.emitChange();
  }
  setArtifact(artifact) {
    this.state.artifact.name = artifact.name;
    this.state.artifact.version = artifact.version;
    this.state.artifact.scope = artifact.scope;

    if (artifact.name === this.GLOBALS.etlBatch) {
      this.state.config.schedule = '* * * * *';
    } else if (artifact.name === this.GLOBALS.etlRealtime) {
      this.state.config.instance = 1;
    }

    this.emitChange();
  }
  setNodes(nodes) {
    this.state.__ui__.nodes = nodes;
  }
  setConnections(connections) {
    this.state.config.connections = connections;
  }
  addNode(node) {
    this.state.__ui__.nodes.push(node);
  }
  getNodes() {
    return this.getState().__ui__.nodes;
  }
  getNode(nodeId) {
    let nodes = this.state.__ui__.nodes;
    let match = nodes.filter( node => node.id === nodeId);
    if (match.length) {
      match = match[0];
    } else {
      match = null;
    }
    return angular.copy(match);
  }
  editNodeProperties(nodeId, nodeConfig) {
    let nodes = this.state.__ui__.nodes;
    let match = nodes.filter( node => node.id === nodeId);
    if (match.length) {
      match = match[0];
      angular.forEach(nodeConfig, (pValue, pName) => match[pName] = pValue);
    }
  }
  getSchedule() {
    return this.getState().config.schedule;
  }
  setSchedule(schedule) {
    this.state.config.schedule = schedule;
  }

  getInstance() {
    return this.getState().config.instance;
  }
  setInstance(instance) {
    this.state.config.instance = instance;
  }

  saveAsDraft(config) {
    this.state.__ui__.isEditing = false;
    this.mySettings.get('adapterDrafts')
       .then(res => {
         if (!angular.isObject(res)) {
           res = {};
         }
         res[config.name] = config;
         return this.mySettings.set('adapterDrafts', res);
       })
       .then(
          () => {
            this.ConsoleActionsFactory.addMessage({
              type: 'success',
              content: `Draft ${config.name} saved successfully.`
            });
          },
          err => {
            this.state.__ui__.isEditing = true;
            this.ConsoleActionsFactory.addMessage({
              type: 'error',
              content: err
            });
          }
        );
  }
}

ConfigStore.$inject = ['ConfigDispatcher', 'CanvasFactory', 'GLOBALS', 'mySettings', 'ConsoleActionsFactory'];
angular.module(`${PKG.name}.feature.hydrator`)
  .service('ConfigStore', ConfigStore);
