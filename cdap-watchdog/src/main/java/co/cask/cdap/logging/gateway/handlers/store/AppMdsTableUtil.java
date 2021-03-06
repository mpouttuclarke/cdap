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

package co.cask.cdap.logging.gateway.handlers.store;

import co.cask.cdap.common.conf.CConfiguration;
import co.cask.cdap.data2.dataset2.DatasetFramework;
import co.cask.cdap.data2.dataset2.lib.table.MetaTableUtil;
import com.google.inject.Inject;

/**
 * Helper class for fetching dataset used by {@link AppMetadataStore}.
 */
public class AppMdsTableUtil extends MetaTableUtil {
  public static final String APP_META_TABLE = "app.meta";

  @Inject
  public AppMdsTableUtil(DatasetFramework framework, CConfiguration conf) {
    super(framework, conf);
  }

  @Override
  public String getMetaTableName() {
    return APP_META_TABLE;
  }
}
