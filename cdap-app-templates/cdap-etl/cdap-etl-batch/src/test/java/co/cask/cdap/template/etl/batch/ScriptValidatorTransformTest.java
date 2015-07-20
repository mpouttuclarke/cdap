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

package co.cask.cdap.template.etl.batch;

import co.cask.cdap.api.common.Bytes;
import co.cask.cdap.api.data.schema.Schema;
import co.cask.cdap.api.dataset.lib.KeyValueTable;
import co.cask.cdap.api.dataset.table.Row;
import co.cask.cdap.api.dataset.table.Scanner;
import co.cask.cdap.api.dataset.table.Table;
import co.cask.cdap.proto.AdapterConfig;
import co.cask.cdap.proto.Id;
import co.cask.cdap.template.etl.batch.config.ETLBatchConfig;
import co.cask.cdap.template.etl.common.ETLStage;
import co.cask.cdap.template.etl.common.Properties;
import co.cask.cdap.test.AdapterManager;
import co.cask.cdap.test.DataSetManager;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 *
 */
public class ScriptValidatorTransformTest extends BaseETLBatchTest {
  private static final String script =
    "function validate(x, ctx) {return x.key % 2 == 0 ? { result : true } : { result : false }; }";

  @Test
  public void testValidator() throws Exception {
    // kv table to kv table pipeline
    String invalidDataset = "invalidData";
    Schema schema = Schema.recordOf("keyvalue",
                                    Schema.Field.of("key", Schema.of(Schema.Type.STRING)),
                                    Schema.Field.of("value", Schema.of(Schema.Type.STRING)));
    ETLStage source = new ETLStage("KVTable", ImmutableMap.of(Properties.BatchReadableWritable.NAME, "table1"));
    ETLStage sink = new ETLStage("KVTable", ImmutableMap.of(Properties.BatchReadableWritable.NAME, "table2"));
    ETLStage transform = new ETLStage("Validator",
                                      ImmutableMap.of("script", script,
                                                      "errorDataset", invalidDataset,
                                                      "schema", schema.toString(),
                                                      "schema.row.field", "key"));
    List<ETLStage> transformList = Lists.newArrayList(transform);
    ETLBatchConfig etlConfig = new ETLBatchConfig("* * * * *", source, sink, transformList);
    AdapterConfig adapterConfig = new AdapterConfig("", TEMPLATE_ID.getId(), GSON.toJsonTree(etlConfig));

    Id.Adapter adapterId = Id.Adapter.from(NAMESPACE, "testValidator");
    AdapterManager manager = createAdapter(adapterId, adapterConfig);

    // add some data to the input table
    DataSetManager<KeyValueTable> table1 = getDataset("table1");
    KeyValueTable inputTable = table1.get();
    for (int i = 0; i < 10; i++) {
      inputTable.write(String.valueOf(i), "world" + i);
    }
    table1.flush();

    manager.start();
    manager.waitForOneRunToFinish(5, TimeUnit.MINUTES);
    manager.stop();

    DataSetManager<Table> invalids = getDataset(invalidDataset);
    Table errorTable = invalids.get();
    Scanner scanner = errorTable.scan(null, null);
    try {
      Row row = scanner.next();
      while (row != null) {
        System.out.println("Error = " + row.getColumns());
        row = scanner.next();
      }
    } finally {
      scanner.close();
    }

    DataSetManager<KeyValueTable> table2 = getDataset("table2");
    KeyValueTable outputTable = table2.get();
    for (int i = 0; i < 10; i += 2) {
      Assert.assertEquals("world" + i, Bytes.toString(outputTable.read(String.valueOf(i))));
    }
  }
}