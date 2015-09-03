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
  .factory('MyDAGFactory', function() {
    var defaultSettings = {
      Connector : [ 'Flowchart', {gap: 7} ],
      ConnectionsDetachable: true
    };
    var connectorStyle = {
      strokeStyle: '#666e82',
      fillStyle: '#666e82',
      radius: 7,
      lineWidth: 2
    };

    var commonSettings = {
      endpoint:'Dot',
      maxConnections: 1,
      paintStyle: {
        strokeStyle: 'white',
        fillStyle: '#666e82',
        radius: 7,
        lineWidth: 3
      },
      connectorOverlays: [
        [ 'Arrow', { location: 1, length: 12, width: 12, height: 10, foldback: 1 } ],
        [ 'Custom', {
          create: function() {
            return angular.element('<div><div class="label-container text-center"><i class="icon-SchemaEdge"></i></div></div>');
          },
          location: 0.5,
          id: 'label'
        }]
      ],
      anchors: [ 'Perimeter', {shape: 'Circle'}]
    };
    var sourceSettings = angular.extend({
      isSource: true,
      anchor: 'Right',
      connectorStyle: connectorStyle
    }, commonSettings);
    var sinkSettings = angular.extend({
      isTarget: true,
      anchor: 'Left',
      connectorStyle: connectorStyle
    }, commonSettings);

    function getSettings() {
      return {
        default: defaultSettings,
        common: commonSettings,
        source: sourceSettings,
        sink: sinkSettings
      };
    }

    function getIcon(plugin) {
      var iconMap = {
        'script': 'fa-code',
        'scriptfilter': 'fa-code',
        'twitter': 'fa-twitter',
        'cube': 'fa-cubes',
        'data': 'fa-database',
        'database': 'fa-database',
        'table': 'fa-table',
        'kafka': 'icon-kafka',
        'stream': 'icon-plugin-stream',
        'tpfsavro': 'icon-avro',
        'jms': 'icon-jms',
        'projection': 'icon-projection'
      };

      var pluginName = plugin.toLowerCase();
      var icon = iconMap[pluginName] ? iconMap[pluginName]: 'fa-plug';
      return icon;
    }

    function generateStyles(name, nodes, xmargin, ymargin) {
      var styles = {};
      var nodeStylesFromDagre = nodes.filter(function(node) {
        return node.label === name;
      });
      if (nodeStylesFromDagre.length) {
        styles = {
          'top': (nodeStylesFromDagre[0].x + xmargin) + 'px',
          'left': (nodeStylesFromDagre[0].y + ymargin) + 'px'
        };
      }
      return styles;
    }

    // Using Dagre here to generate x and y co-ordinates for each node.
    // When we fork and branch and have complex connections this will be useful for us.
    // Right now this returns a pretty simple straight linear graph.
     function getGraph(plugins) {
      var graph = new dagre.graphlib.Graph();
      graph.setGraph({
        nodesep: 90,
        ranksep: 100,
        rankdir: 'LR',
        marginx: 30,
        marginy: 30
      });
      plugins.forEach(function(plugin) {
        graph.setNode(plugin.id, {label: plugin.id, width: 100, height: 100});
      });
      dagre.layout(graph);
      return graph;
    }

    return {
      getSettings: getSettings,
      getIcon: getIcon,
      generateStyles: generateStyles,
      getGraph: getGraph
    };

  });
