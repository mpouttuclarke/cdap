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

class NodeConfigController {
  constructor(NodeConfigStore, $scope, $timeout, $state, DetailNonRunsStore, PluginConfigFactory, EventPipe, GLOBALS, ConfigActionsFactory) {

    this.$scope = $scope;
    this.$timeout = $timeout;
    this.$state = $state;
    this.EventPipe = EventPipe;
    this.DetailNonRunsStore = DetailNonRunsStore;
    this.PluginConfigFactory = PluginConfigFactory;
    this.GLOBALS = GLOBALS;
    this.NodeConfigStore = NodeConfigStore;
    this.ConfigActionsFactory = ConfigActionsFactory;

    this.setDefaults({});
    NodeConfigStore.registerOnChangeListener(this.setState.bind(this));
  }
  setState() {
    var appType = this.$state.params.type || this.DetailNonRunsStore.getAppType();
    var nodeState = this.NodeConfigStore.getState();
    nodeState.appType = appType;
    if (angular.isArray(this.state.watchers)) {
      this.state.watchers.forEach(watcher => watcher());
      this.state.watchers = [];
    }
    this.setDefaults(nodeState);
    this.loadNewPlugin();
  }
  setDefaults(config) {
    this.state = {
      configfetched : false,
      properties : [],
      noconfig: null,
      noproperty: true,
      config: {},

      isValidPlugin: config.isValidPlugin || false,
      plugin: config.plugin || {},

      isSource: config.isSource || false,
      isSink: config.isSink || false,
      isTransform: config.isTransform || false,

      type: config.appType || null,
      watchers: []
    };
  }
  loadNewPlugin() {

    this.state.noproperty = Object.keys(
      this.state.plugin._backendProperties || {}
    ).length;

    if (this.state.noproperty) {
      this.PluginConfigFactory.fetch(
        this.state.type,
        this.state.plugin.name
      )
        .then(
          (res) => {

            this.state.groupsConfig = this.PluginConfigFactory.generateNodeConfig(this.state.plugin._backendProperties, res);
            angular.forEach(this.state.groupsConfig.groups, (group) => {
              angular.forEach(group.fields, (field) => {
                if (field.defaultValue) {
                  this.state.plugin.properties[field.name] = this.state.plugin.properties[field.name] || field.defaultValue;
                }
              });
            });
            var configOutputSchema = this.state.groupsConfig.outputSchema;
            // If its an implicit schema, set the output schema to the implicit schema and inform ConfigActionFactory
            if (configOutputSchema.implicitSchema) {
              var keys = Object.keys(configOutputSchema.implicitSchema);
              var formattedSchema = [];
              angular.forEach(keys, (key) => {
                formattedSchema.push({
                  name: key,
                  type: configOutputSchema.implicitSchema[key]
                });
              });

              this.state.plugin.outputSchema = JSON.stringify({ fields: formattedSchema });
              this.ConfigActionsFactory.editPlugin(this.state.plugin.id, this.state.plugin);
            } else {
              // If not an implcit schema check if an schema property exists in the node config.
              // What this means is, has the plugin developer specified a plugin property in 'outputs' array of node config.
              // If yes then set it as output schema and everytime when a user edits the output schema the value has to
              // be transitioned to the respective plugin property.
              if (configOutputSchema.isOutputSchemaExists) {
                if (this.state.plugin.properties[configOutputSchema.outputSchemaProperty[0]] !== this.state.plugin.outputSchema) {
                  this.state.properties[configOutputSchema.outputSchemaProperty[0]] = this.state.plugin.outputSchema;
                }
                this.state.watchers.push(
                  this.$scope.$watch('NodeConfigController.state.plugin.outputSchema', () => {
                    if(this.validateSchema()) {
                      this.state.plugin.properties[configOutputSchema.outputSchemaProperty[0]] = this.state.plugin.outputSchema;
                    }
                  })
                );
              } else if (this.state.plugin.inputSchema) {
                // If there is no information of output schema in the node config then just mantain an output schema for UI purposes.
                configOutputSchema.isOutputSchemaExists = true;
                this.state.plugin.outputSchema = this.state.plugin.outputSchema || this.state.plugin.inputSchema;
                this.ConfigActionsFactory.editPlugin(this.state.plugin.id, this.state.plugin);
              }
            }
            if (!this.$scope.isDisabled) {
              this.state.watchers.push(
                this.$scope.$watch(
                  'NodeConfigController.state.plugin',
                  _.debounce( () => {
                    this.ConfigActionsFactory.editPlugin(this.state.plugin.id, this.state.plugin);
                  }, 1000),
                  true
                )
              );
            }

            // Mark the configfetched to show that configurations have been received.
            this.state.configfetched = true;
            this.state.config = res;
            this.state.noconfig = false;

          },
          (err) => {
            var propertiesFromBackend = Object.keys(this.state.plugin._backendProperties);
            // Didn't receive a configuration from the backend. Fallback to all textboxes.
            switch(err) {
              case 'NO_JSON_FOUND':
                this.state.noConfigMessage = this.GLOBALS.en.hydrator.studio.noConfigMessage;
                break;
              case 'CONFIG_SYNTAX_JSON_ERROR':
                this.state.noConfigMessage = this.GLOBALS.en.hydrator.studio.syntaxConfigJsonError;
                break;
              case 'CONFIG_SEMANTICS_JSON_ERROR':
                this.state.noConfigMessage = this.GLOBALS.en.hydrator.studio.semanticConfigJsonError;
                break;
            }
            this.state.noconfig = true;
            this.state.configfetched = true;
            propertiesFromBackend.forEach( (property) => {
              this.state.plugin.properties[property] = this.state.plugin.properties[property] || '';
            });
            this.state.watchers.push(
              this.$scope.$watch(
                'NodeConfigController.state.plugin.properties',
                _.debounce(() => this.ConfigActionsFactory.editPlugin(this.state.plugin.id, this.state.plugin), 1000),
                true
              )
            );
          }
        );
    } else {
      this.state.configfetched = true;
    }
  }
  schemaClear() {
    this.EventPipe.emit('schema.clear');
  }
  validateSchema() {
    this.state.errors = [];
    var schema;
    try {
      schema = JSON.parse(this.state.plugin.outputSchema);
      schema = schema.fields;
    } catch (e) {
      schema = null;
    }

    var validationRules = [
      this.hasUniqueFields
    ];

    var error = [];
    validationRules.forEach(function (rule) {
      rule.call(this, schema, error);
    });

    if (error.length > 0) {
      this.state.errors = error;
      return false;
    } else {
      return true;
    }
  }
  hasUniqueFields(schema, error) {
    if (!schema) { return true; }

    var fields = schema.map(function (field) { return field.name; });
    var unique = _.uniq(fields);

    if (fields.length !== unique.length) {
      error.push('There are two or more fields with the same name.');
    }
  }
}

NodeConfigController.$inject = ['NodeConfigStore', '$scope', '$timeout', '$state', 'DetailNonRunsStore', 'PluginConfigFactory', 'EventPipe', 'GLOBALS', 'ConfigActionsFactory'];

angular.module(PKG.name + '.feature.hydrator')
  .controller('NodeConfigController', NodeConfigController);
