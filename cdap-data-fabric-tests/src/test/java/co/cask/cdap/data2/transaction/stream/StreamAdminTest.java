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

package co.cask.cdap.data2.transaction.stream;

import co.cask.cdap.api.flow.flowlet.StreamEvent;
import co.cask.cdap.common.conf.CConfiguration;
import co.cask.cdap.common.namespace.NamespacedLocationFactory;
import co.cask.cdap.data.file.FileWriter;
import co.cask.cdap.data.stream.StreamFileWriterFactory;
import co.cask.cdap.data.stream.StreamUtils;
import co.cask.cdap.proto.Id;
import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import org.apache.twill.filesystem.Location;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

public abstract class StreamAdminTest {
  protected static CConfiguration cConf = CConfiguration.create();
  protected static final String FOO_NAMESPACE = "fooNamespace";
  protected static final String OTHER_NAMESPACE = "otherNamespace";

  protected abstract StreamAdmin getStreamAdmin();

  protected abstract StreamFileWriterFactory getFileWriterFactory();

  protected static void setupNamespaces(NamespacedLocationFactory namespacedLocationFactory) throws IOException {
    namespacedLocationFactory.get(Id.Namespace.from(FOO_NAMESPACE)).mkdirs();
    namespacedLocationFactory.get(Id.Namespace.from(OTHER_NAMESPACE)).mkdirs();
  }

  @Test
  public void testCreateExist() throws Exception {
    StreamAdmin streamAdmin = getStreamAdmin();

    String streamName = "streamName";
    Id.Stream streamId = Id.Stream.from(FOO_NAMESPACE, streamName);
    Id.Stream otherStreamId = Id.Stream.from(OTHER_NAMESPACE, streamName);

    Assert.assertFalse(streamAdmin.exists(streamId));
    Assert.assertFalse(streamAdmin.exists(otherStreamId));

    streamAdmin.create(streamId);
    // Even though both streams have the same name, {@code otherStreamId} does not exist because it is in a different
    // namespace than the one created above.
    Assert.assertTrue(streamAdmin.exists(streamId));
    Assert.assertFalse(streamAdmin.exists(otherStreamId));

    streamAdmin.create(otherStreamId);
    Assert.assertTrue(streamAdmin.exists(otherStreamId));
  }

  @Test
  public void testDropAllInNamespace() throws Exception {
    StreamAdmin streamAdmin = getStreamAdmin();

    Id.Stream otherStream = Id.Stream.from(OTHER_NAMESPACE, "otherStream");

    List<Id.Stream> fooStreams = Lists.newArrayList();
    for (int i = 0; i < 4; i++) {
      fooStreams.add(Id.Stream.from(FOO_NAMESPACE, "stream" + i));
    }

    List<Id.Stream> allStreams = Lists.newArrayList();
    allStreams.addAll(fooStreams);
    allStreams.add(otherStream);

    for (Id.Stream stream : allStreams) {
      streamAdmin.create(stream);
      writeEvent(stream);
      // all of the streams should have data in it after writing to them
      Assert.assertNotEquals(0, getStreamSize(stream));
    }

    streamAdmin.dropAllInNamespace(Id.Namespace.from(FOO_NAMESPACE));

    // All of the streams within the default namespace should no longer exist
    for (Id.Stream defaultStream : fooStreams) {
      Assert.assertFalse(streamAdmin.exists(defaultStream));
    }
    // otherStream isn't in the foo namespace so its data is not deleted in the above call to dropAllInNamespace.
    Assert.assertNotEquals(0, getStreamSize(otherStream));

    // truncate should also delete all the data of a stream
    streamAdmin.truncate(otherStream);
    Assert.assertEquals(0, getStreamSize(otherStream));
  }

  private long getStreamSize(Id.Stream streamId) throws IOException {
    StreamAdmin streamAdmin = getStreamAdmin();
    StreamConfig config = streamAdmin.getConfig(streamId);

    Location generationLocation = StreamUtils.createGenerationLocation(config.getLocation(),
                                                                       StreamUtils.getGeneration(config));
    return StreamUtils.fetchStreamFilesSize(generationLocation);
  }

  // simply writes a static string to a stream
  private void writeEvent(Id.Stream streamId) throws IOException {
    StreamConfig streamConfig = getStreamAdmin().getConfig(streamId);
    FileWriter<StreamEvent> streamEventFileWriter = getFileWriterFactory().create(streamConfig, 0);
    streamEventFileWriter.append(new StreamEvent(Charsets.UTF_8.encode("EVENT")));
  }

}
