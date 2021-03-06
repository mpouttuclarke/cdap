/*
 * Copyright © 2014 Cask Data, Inc.
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
package co.cask.cdap.data.stream;

/**
 * An enum to distinguish different file types in stream data file.
 */
public enum StreamFileType {

  EVENT("dat"),
  INDEX("idx");

  private final String suffix;

  private StreamFileType(String suffix) {
    this.suffix = suffix;
  }

  public boolean isMatched(String fileName) {
    return fileName.endsWith("." + suffix);
  }

  public String getSuffix() {
    return suffix;
  }

  public static boolean isMatched(String fileName, StreamFileType type) {
    return type.isMatched(fileName);
  }

  public static StreamFileType getType(String fileName) {
    for (StreamFileType type : StreamFileType.values()) {
      if (type.isMatched(fileName)) {
        return type;
      }
    }

    throw new IllegalArgumentException("Unknown stream file type for " + fileName);
  }
}
