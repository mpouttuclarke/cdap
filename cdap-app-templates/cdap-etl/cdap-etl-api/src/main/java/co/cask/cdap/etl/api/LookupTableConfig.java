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

package co.cask.cdap.etl.api;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

import java.util.Map;
import javax.annotation.Nullable;

/**
 * Configuration for a particular {@link Lookup} table.
 */
public class LookupTableConfig {

  /**
   * Type of lookup table.
   */
  public enum TableType {
    DATASET
  }

  private final TableType type;

  @Nullable
  private final Map<String, String> datasetProperties;

  @Nullable
  private final CacheConfig cache;

  public LookupTableConfig(TableType type, @Nullable CacheConfig cache,
                           @Nullable Map<String, String> datasetProperties) {
    this.type = type;
    this.cache = cache;
    this.datasetProperties = datasetProperties;
  }

  public LookupTableConfig(TableType type) {
    this(type, null, null);
  }

  public TableType getType() {
    return type;
  }

  public Map<String, String> getDatasetProperties() {
    Preconditions.checkArgument(type == TableType.DATASET);
    return datasetProperties == null ? ImmutableMap.<String, String>of() : datasetProperties;
  }

  public boolean isCacheEnabled() {
    return cache != null;
  }

  public CacheConfig getCacheConfig() {
    return Preconditions.checkNotNull(cache);
  }
}
