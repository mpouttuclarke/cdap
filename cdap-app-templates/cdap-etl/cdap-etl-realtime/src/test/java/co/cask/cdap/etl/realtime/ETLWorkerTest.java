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

package co.cask.cdap.etl.realtime;

import co.cask.cdap.api.common.Bytes;
import co.cask.cdap.api.data.schema.Schema;
import co.cask.cdap.api.dataset.lib.KeyValueTable;
import co.cask.cdap.api.dataset.table.Row;
import co.cask.cdap.api.dataset.table.Table;
import co.cask.cdap.api.flow.flowlet.StreamEvent;
import co.cask.cdap.common.conf.Constants;
import co.cask.cdap.common.utils.Tasks;
import co.cask.cdap.etl.api.LookupConfig;
import co.cask.cdap.etl.api.LookupTableConfig;
import co.cask.cdap.etl.common.ETLStage;
import co.cask.cdap.etl.common.Plugin;
import co.cask.cdap.etl.common.Properties;
import co.cask.cdap.etl.realtime.config.ETLRealtimeConfig;
import co.cask.cdap.etl.realtime.source.DataGeneratorSource;
import co.cask.cdap.etl.realtime.source.KafkaSource;
import co.cask.cdap.proto.Id;
import co.cask.cdap.proto.artifact.AppRequest;
import co.cask.cdap.test.ApplicationManager;
import co.cask.cdap.test.DataSetManager;
import co.cask.cdap.test.SlowTests;
import co.cask.cdap.test.StreamManager;
import co.cask.cdap.test.TestConfiguration;
import co.cask.cdap.test.WorkerManager;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.gson.Gson;
import org.apache.twill.internal.kafka.EmbeddedKafkaServer;
import org.apache.twill.internal.kafka.client.ZKKafkaClientService;
import org.apache.twill.internal.utils.Networks;
import org.apache.twill.internal.zookeeper.InMemoryZKServer;
import org.apache.twill.kafka.client.Compression;
import org.apache.twill.kafka.client.KafkaClientService;
import org.apache.twill.kafka.client.KafkaPublisher;
import org.apache.twill.zookeeper.ZKClientService;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * Tests for {@link ETLRealtimeApplication}.
 */
public class ETLWorkerTest extends ETLRealtimeBaseTest {

  private static final Gson GSON = new Gson();

  private static ZKClientService zkClient;
  private static KafkaClientService kafkaClient;

  @ClassRule
  public static final TemporaryFolder TMP_FOLDER = new TemporaryFolder();

  @ClassRule
  public static final TestConfiguration CONFIG = new TestConfiguration(Constants.Explore.EXPLORE_ENABLED, true);

  protected static final int PARTITIONS = 1;

  protected static InMemoryZKServer zkServer;
  protected static EmbeddedKafkaServer kafkaServer;
  protected static int kafkaPort;

  @Test
  public void testEmptyProperties() throws Exception {
    // Set properties to null to test if ETLTemplate can handle it.
    Plugin sourceConfig = new Plugin("DataGenerator", null);
    Plugin sinkConfig =
      new Plugin("Stream", ImmutableMap.of(Properties.Stream.NAME, "testS"));
    ETLStage source = new ETLStage("source", sourceConfig);
    ETLStage sink = new ETLStage("sink", sinkConfig);
    ETLRealtimeConfig etlConfig = new ETLRealtimeConfig(2, source, sink, Lists.<ETLStage>newArrayList());

    Id.Application appId = Id.Application.from(Id.Namespace.DEFAULT, "testAdap");
    AppRequest<ETLRealtimeConfig> appRequest = new AppRequest<>(APP_ARTIFACT, etlConfig);
    ApplicationManager appManager = deployApplication(appId, appRequest);
    Assert.assertNotNull(appManager);
    WorkerManager workerManager = appManager.getWorkerManager(ETLWorker.NAME);
    workerManager.start();
    workerManager.waitForStatus(true, 10, 1);
    Assert.assertEquals(2, workerManager.getInstances());
    workerManager.stop();
    workerManager.waitForStatus(false, 10, 1);
  }

  @Test
  @Category(SlowTests.class)
  public void testStreamSinks() throws Exception {
    Plugin sourceConfig = new Plugin(
      "DataGenerator", ImmutableMap.of(DataGeneratorSource.PROPERTY_TYPE,
      DataGeneratorSource.STREAM_TYPE));

    ETLStage source = new ETLStage("source", sourceConfig);
    List<ETLStage> sinks = Lists.newArrayList(
      new ETLStage("sink1", new Plugin("Stream", ImmutableMap.of(Properties.Stream.NAME, "streamA"))),
      new ETLStage("sink2", new Plugin("Stream", ImmutableMap.of(Properties.Stream.NAME, "streamB"))),
      new ETLStage("sink3", new Plugin("Stream", ImmutableMap.of(Properties.Stream.NAME, "streamC")))
    );
    ETLRealtimeConfig etlConfig = new ETLRealtimeConfig(source, sinks,
                                                        new ArrayList<ETLStage>(),
                                                        new ArrayList<co.cask.cdap.etl.common.Connection>());

    Id.Application appId = Id.Application.from(Id.Namespace.DEFAULT, "testToStream");
    AppRequest<ETLRealtimeConfig> appRequest = new AppRequest<>(APP_ARTIFACT, etlConfig);
    ApplicationManager appManager = deployApplication(appId, appRequest);

    long startTime = System.currentTimeMillis();
    WorkerManager workerManager = appManager.getWorkerManager(ETLWorker.NAME);
    workerManager.start();

    List<StreamManager> streamManagers = Lists.newArrayList(
      getStreamManager(Id.Namespace.DEFAULT, "streamA"),
      getStreamManager(Id.Namespace.DEFAULT, "streamB"),
      getStreamManager(Id.Namespace.DEFAULT, "streamC")
    );

    int retries = 0;
    boolean succeeded = false;
    while (retries < 10) {
      succeeded = checkStreams(streamManagers, startTime);
      if (succeeded) {
        break;
      }
      retries++;
      TimeUnit.SECONDS.sleep(1);
    }

    workerManager.stop();
    Assert.assertTrue(succeeded);
  }

  @Test
  public void testScriptLookup() throws Exception {
    addDatasetInstance(KeyValueTable.class.getName(), "lookupTable");
    DataSetManager<KeyValueTable> lookupTable = getDataset("lookupTable");
    lookupTable.get().write("Bob".getBytes(Charsets.UTF_8), "123".getBytes(Charsets.UTF_8));
    lookupTable.flush();

    Schema.Field idField = Schema.Field.of("id", Schema.nullableOf(Schema.of(Schema.Type.INT)));
    Schema.Field nameField = Schema.Field.of("name", Schema.of(Schema.Type.STRING));
    Schema.Field scoreField = Schema.Field.of("score", Schema.of(Schema.Type.DOUBLE));
    Schema.Field graduatedField = Schema.Field.of("graduated", Schema.of(Schema.Type.BOOLEAN));
    Schema.Field binaryNameField = Schema.Field.of("binary", Schema.of(Schema.Type.BYTES));
    Schema.Field timeField = Schema.Field.of("time", Schema.of(Schema.Type.LONG));
    Schema schema =  Schema.recordOf("tableRecord", idField, nameField, scoreField, graduatedField,
                                     binaryNameField, timeField);

    Plugin source = new Plugin("DataGenerator", ImmutableMap.of(DataGeneratorSource.PROPERTY_TYPE,
                                                                    DataGeneratorSource.TABLE_TYPE));
    Plugin transform = new Plugin("Script", ImmutableMap.of(
      "script", "function transform(x, ctx) { " +
        "x.name = x.name + '..hi..' + ctx.getLookup('lookupTable').lookup(x.name); return x; }",
      "lookup", GSON.toJson(new LookupConfig(ImmutableMap.of(
        "lookupTable", new LookupTableConfig(LookupTableConfig.TableType.DATASET)
      )))
    ));
    Plugin sink =
      new Plugin("Table", ImmutableMap.of(Properties.Table.NAME, "testScriptLookup_table1",
                                                       Properties.Table.PROPERTY_SCHEMA_ROW_FIELD, "binary",
                                                       Properties.Table.PROPERTY_SCHEMA, schema.toString()));

    ETLRealtimeConfig etlConfig = new ETLRealtimeConfig(new ETLStage("source", source),
                                                        new ETLStage("sink", sink),
                                                        Lists.newArrayList(new ETLStage("transform", transform)));

    Id.Application appId = Id.Application.from(Id.Namespace.DEFAULT, "testToStream");
    AppRequest<ETLRealtimeConfig> appRequest = new AppRequest<>(APP_ARTIFACT, etlConfig);
    ApplicationManager appManager = deployApplication(appId, appRequest);

    WorkerManager workerManager = appManager.getWorkerManager(ETLWorker.NAME);

    workerManager.start();
    DataSetManager<Table> tableManager = getDataset("testScriptLookup_table1");
    waitForTableToBePopulated(tableManager);
    workerManager.stop();

    // verify
    Table table = tableManager.get();
    Row row = table.get("Bob".getBytes(Charsets.UTF_8));

    Assert.assertEquals("Bob..hi..123", row.getString("name"));

    Connection connection = getQueryClient();
    ResultSet results = connection.prepareStatement("select name from dataset_testScriptLookup_table1").executeQuery();
    Assert.assertTrue(results.next());
    Assert.assertEquals("Bob..hi..123", results.getString(1));
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testTableSink() throws Exception {
    Schema.Field idField = Schema.Field.of("id", Schema.nullableOf(Schema.of(Schema.Type.INT)));
    Schema.Field nameField = Schema.Field.of("name", Schema.of(Schema.Type.STRING));
    Schema.Field scoreField = Schema.Field.of("score", Schema.of(Schema.Type.DOUBLE));
    Schema.Field graduatedField = Schema.Field.of("graduated", Schema.of(Schema.Type.BOOLEAN));
    // nullable row key field to test cdap-3239
    Schema.Field binaryNameField = Schema.Field.of("binary", Schema.nullableOf(Schema.of(Schema.Type.BYTES)));
    Schema.Field timeField = Schema.Field.of("time", Schema.of(Schema.Type.LONG));
    Schema schema =  Schema.recordOf("tableRecord", idField, nameField, scoreField, graduatedField,
                                     binaryNameField, timeField);

    Plugin source = new Plugin("DataGenerator", ImmutableMap.of(DataGeneratorSource.PROPERTY_TYPE,
                                                           DataGeneratorSource.TABLE_TYPE));
    Plugin sink = new Plugin("Table", ImmutableMap.of(Properties.Table.NAME, "table1",
                                                          Properties.Table.PROPERTY_SCHEMA_ROW_FIELD, "binary",
                                                          Properties.Table.PROPERTY_SCHEMA, schema.toString()));
    ETLRealtimeConfig etlConfig = new ETLRealtimeConfig(new ETLStage("source", source),
                                                        new ETLStage("sink", sink), Lists.<ETLStage>newArrayList());

    Id.Application appId = Id.Application.from(Id.Namespace.DEFAULT, "testToStream");
    AppRequest<ETLRealtimeConfig> appRequest = new AppRequest<>(APP_ARTIFACT, etlConfig);
    ApplicationManager appManager = deployApplication(appId, appRequest);

    WorkerManager workerManager = appManager.getWorkerManager(ETLWorker.NAME);

    workerManager.start();
    DataSetManager<Table> tableManager = getDataset("table1");
    waitForTableToBePopulated(tableManager);
    workerManager.stop();

    // verify
    Table table = tableManager.get();
    Row row = table.get("Bob".getBytes(Charsets.UTF_8));

    Assert.assertEquals(1, (int) row.getInt("id"));
    Assert.assertEquals("Bob", row.getString("name"));
    Assert.assertEquals(3.4, row.getDouble("score"), 0.000001);
    // binary field was the row key and thus shouldn't be present in the columns
    Assert.assertNull(row.get("binary"));
    Assert.assertNotNull(row.getLong("time"));

    Connection connection = getQueryClient();
    ResultSet results = connection.prepareStatement("select binary,name,score from dataset_table1").executeQuery();
    Assert.assertTrue(results.next());
    Assert.assertArrayEquals("Bob".getBytes(Charsets.UTF_8), results.getBytes(1));
    Assert.assertEquals("Bob", results.getString(2));
    Assert.assertEquals(3.4, results.getDouble(3), 0.000001);
  }



  @Test
  @SuppressWarnings("ConstantConditions")
  public void testDAG() throws Exception {

    Schema.Field idField = Schema.Field.of("id", Schema.nullableOf(Schema.of(Schema.Type.INT)));
    Schema.Field nameField = Schema.Field.of("name", Schema.of(Schema.Type.STRING));
    Schema.Field scoreField = Schema.Field.of("score", Schema.of(Schema.Type.DOUBLE));
    Schema.Field graduatedField = Schema.Field.of("graduated", Schema.of(Schema.Type.BOOLEAN));
    // nullable row key field to test cdap-3239
    Schema.Field binaryNameField = Schema.Field.of("binary", Schema.nullableOf(Schema.of(Schema.Type.BYTES)));
    Schema.Field timeField = Schema.Field.of("time", Schema.of(Schema.Type.LONG));
    Schema schema =  Schema.recordOf("tableRecord", idField, nameField, scoreField, graduatedField,
                                     binaryNameField, timeField);

    Plugin source = new Plugin("DataGenerator", ImmutableMap.of(DataGeneratorSource.PROPERTY_TYPE,
                                                                DataGeneratorSource.TABLE_TYPE));

    Plugin sinkConfig1 = new Plugin("Table", ImmutableMap.of(Properties.Table.NAME, "table1",
                                                      Properties.Table.PROPERTY_SCHEMA_ROW_FIELD, "binary",
                                                      Properties.Table.PROPERTY_SCHEMA, schema.toString()));

    Plugin sinkConfig2 = new Plugin("Table", ImmutableMap.of(Properties.Table.NAME, "table2",
                                                             Properties.Table.PROPERTY_SCHEMA_ROW_FIELD, "binary",
                                                             Properties.Table.PROPERTY_SCHEMA, schema.toString()));

    List<ETLStage> sinks = ImmutableList.of(new ETLStage("sink1", sinkConfig1),
                                            new ETLStage("sink2", sinkConfig2));


    String script = "function transform(x, context) {  " +
      "x.name = \"Rob\";" +
      "x.id = 2;" +
      "return x;" +
      "};";
    Plugin transformConfig = new Plugin("Script", ImmutableMap.of("script", script));

    List<ETLStage> transformList = Lists.newArrayList(new ETLStage("transform", transformConfig));

    List<co.cask.cdap.etl.common.Connection> connections = new ArrayList<>();
    connections.add(new co.cask.cdap.etl.common.Connection("source", "sink1"));
    connections.add(new co.cask.cdap.etl.common.Connection("source", "transform"));
    connections.add(new co.cask.cdap.etl.common.Connection("transform", "sink2"));

    ETLRealtimeConfig etlConfig = new ETLRealtimeConfig(new ETLStage("source", source),
                                                        sinks, transformList, connections);

    Id.Application appId = Id.Application.from(Id.Namespace.DEFAULT, "testToStream");
    AppRequest<ETLRealtimeConfig> appRequest = new AppRequest<>(APP_ARTIFACT, etlConfig);
    ApplicationManager appManager = deployApplication(appId, appRequest);

    WorkerManager workerManager = appManager.getWorkerManager(ETLWorker.NAME);

    workerManager.start();
    DataSetManager<Table> tableManager1 = getDataset("table1");
    waitForTableToBePopulated(tableManager1);
    DataSetManager<Table> tableManager2 = getDataset("table2");
    waitForTableToBePopulated(tableManager2);
    workerManager.stop();

    // verify
    Table table = tableManager1.get();
    Row row = table.get("Bob".getBytes(Charsets.UTF_8));

    Assert.assertEquals("Bob", row.getString("name"));
    Assert.assertEquals(1, (int) row.getInt("id"));
    Assert.assertEquals(3.4, row.getDouble("score"), 0.000001);
    // binary field was the row key and thus shouldn't be present in the columns
    Assert.assertNull(row.get("binary"));
    Assert.assertNotNull(row.getLong("time"));

    // verify that table2 doesn't have these records
    tableManager2 = getDataset("table2");
    table = tableManager2.get();

    row = table.get("Bob".getBytes(Charsets.UTF_8));

    Assert.assertEquals(2, (int) row.getInt("id"));
    // transformed
    Assert.assertEquals("Rob", row.getString("name"));
    Assert.assertEquals(3.4, row.getDouble("score"), 0.000001);
    // binary field was the row key and thus shouldn't be present in the columns
    Assert.assertNull(row.get("binary"));
    Assert.assertNotNull(row.getLong("time"));
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void testKafkaSource() throws Exception {
    Schema schema = Schema.recordOf("student",
                                    Schema.Field.of("NAME", Schema.of(Schema.Type.STRING)),
                                    Schema.Field.of("ID", Schema.of(Schema.Type.INT)),
                                    Schema.Field.of("AGE", Schema.of(Schema.Type.INT)));
    setUp();
    Plugin source = new Plugin("Kafka", ImmutableMap.<String, String>builder()
      .put(KafkaSource.KAFKA_TOPIC, "MyTopic")
      .put(KafkaSource.KAFKA_ZOOKEEPER, zkServer.getConnectionStr())
      .put(KafkaSource.FORMAT, "csv")
      .put(KafkaSource.SCHEMA, schema.toString())
      .put(KafkaSource.KAFKA_PARTITIONS, Integer.toString(PARTITIONS))
      .build()
    );

    Plugin sink = new Plugin("Table", ImmutableMap.of(
      "name", "outputTable",
      Properties.Table.PROPERTY_SCHEMA, schema.toString(),
      Properties.Table.PROPERTY_SCHEMA_ROW_FIELD, "NAME"));

    Map<String, String> message = Maps.newHashMap();
    message.put("1", "Bob,1,3");
    sendMessage("MyTopic", message);

    ETLRealtimeConfig etlConfig = new ETLRealtimeConfig(new ETLStage("source", source), new ETLStage("sink", sink));

    Id.Application appId = Id.Application.from(Id.Namespace.DEFAULT, "testToStream");
    AppRequest<ETLRealtimeConfig> appRequest = new AppRequest<>(APP_ARTIFACT, etlConfig);
    ApplicationManager appManager = deployApplication(appId, appRequest);

    WorkerManager workerManager = appManager.getWorkerManager(ETLWorker.NAME);

    workerManager.start();
    DataSetManager<Table> tableManager = getDataset("outputTable");
    waitForTableToBePopulated(tableManager);
    workerManager.stop();

    // verify. We need to get the table fresh because it doesn't update otherwise
    Table table = tableManager.get();
    Row row = table.get("Bob".getBytes(Charsets.UTF_8));

    Assert.assertEquals(1, (int) row.getInt("ID"));
    Assert.assertEquals(3, (int) row.getInt("AGE"));

    Connection connection = getQueryClient();
    ResultSet results = connection.prepareStatement("select NAME,ID,AGE from dataset_outputTable").executeQuery();
    Assert.assertTrue(results.next());
    Assert.assertEquals("Bob", results.getString(1));
    Assert.assertEquals(1, results.getInt(2));
    Assert.assertEquals(3, results.getInt(3));
  }

  private void waitForTableToBePopulated(final DataSetManager<Table> tableManager) throws Exception {
    Tasks.waitFor(true, new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        tableManager.flush();
        Table table = tableManager.get();
        Row row = table.get("Bob".getBytes(Charsets.UTF_8));
        // need to wait for information to get to the table, not just for the row to be created
        return row.getColumns().size() != 0;
      }
    }, 10, TimeUnit.SECONDS);
  }

  public static void setUp() throws IOException {
    zkServer = InMemoryZKServer.builder().setDataDir(TMP_FOLDER.newFolder()).build();
    zkServer.startAndWait();

    kafkaPort = Networks.getRandomPort();
    kafkaServer = new EmbeddedKafkaServer(generateKafkaConfig(zkServer.getConnectionStr(),
                                                              kafkaPort, TMP_FOLDER.newFolder()));
    kafkaServer.startAndWait();

    zkClient = ZKClientService.Builder.of(zkServer.getConnectionStr()).build();
    zkClient.startAndWait();

    kafkaClient = new ZKKafkaClientService(zkClient);
    kafkaClient.startAndWait();
  }

  private static java.util.Properties generateKafkaConfig(String zkConnectStr, int port, File logDir) {
    // Note: the log size properties below have been set so that we can have log rollovers
    // and log deletions in a minute.
    java.util.Properties prop = new java.util.Properties();
    prop.setProperty("log.dir", logDir.getAbsolutePath());
    prop.setProperty("port", Integer.toString(port));
    prop.setProperty("broker.id", "1");
    prop.setProperty("socket.send.buffer.bytes", "1048576");
    prop.setProperty("socket.receive.buffer.bytes", "1048576");
    prop.setProperty("socket.request.max.bytes", "104857600");
    prop.setProperty("num.partitions", Integer.toString(PARTITIONS));
    prop.setProperty("log.retention.hours", "24");
    prop.setProperty("log.flush.interval.messages", "10");
    prop.setProperty("log.flush.interval.ms", "1000");
    prop.setProperty("log.segment.bytes", "100");
    prop.setProperty("zookeeper.connect", zkConnectStr);
    prop.setProperty("zookeeper.connection.timeout.ms", "1000000");
    prop.setProperty("default.replication.factor", "1");
    prop.setProperty("log.retention.bytes", "1000");
    prop.setProperty("log.retention.check.interval.ms", "60000");

    return prop;
  }

  protected void sendMessage(String topic, Map<String, String> messages) {
    // Publish a message to Kafka, the flow should consume it
    KafkaPublisher publisher = kafkaClient.getPublisher(KafkaPublisher.Ack.ALL_RECEIVED, Compression.NONE);

    // If publish failed, retry up to 20 times, with 100ms delay between each retry
    // This is because leader election in Kafka 08 takes time when a topic is being created upon publish request.
    int count = 0;
    do {
      KafkaPublisher.Preparer preparer = publisher.prepare(topic);
      for (Map.Entry<String, String> entry : messages.entrySet()) {
        preparer.add(Charsets.UTF_8.encode(entry.getValue()), entry.getKey());
      }
      try {
        preparer.send().get();
        break;
      } catch (Exception e) {
        // Backoff if send failed.
        Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
      }
    } while (count++ < 20);
  }

  private boolean checkStreams(Collection<StreamManager> streamManagers, long startTime) throws IOException {
    try {
      long currentDiff = System.currentTimeMillis() - startTime;
      for (StreamManager streamManager : streamManagers) {
        List<StreamEvent> streamEvents = streamManager.getEvents("now-" + Long.toString(currentDiff) + "ms", "now",
          Integer.MAX_VALUE);
        // verify that some events were sent to the stream
        Assert.assertTrue(streamEvents.size() > 0);
        // since we sent all identical events, verify the contents of just one of them
        Random random = new Random();
        StreamEvent event = streamEvents.get(random.nextInt(streamEvents.size()));
        ByteBuffer body = event.getBody();
        Map<String, String> headers = event.getHeaders();
        if (headers != null && !headers.isEmpty()) {
          // check h1 header has value v1
          if (!"v1".equals(headers.get("h1"))) {
            return false;
          }
        }
        // check body has content "Hello"
        if (!"Hello".equals(Bytes.toString(body, Charsets.UTF_8))) {
          return false;
        }
      }
      return true;
    } catch (Exception e) {
      // streamManager.getEvents() can throw an exception if there is nothing in the stream
      return false;
    }
  }
}
