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

package co.cask.cdap.api.schedule;

import java.util.Objects;
import javax.annotation.Nullable;

/**
 * Defines constraints that must be met at runtime in order for the scheduler to launch a run.
 *
 * Currently only contains the maximum number of concurrent runs. In the future, other checks may be added,
 * such as the amount of available memory in the YARN cluster.
 */
public class RunConstraints {
  public static final RunConstraints NONE = new RunConstraints(null);
  private final Integer maxConcurrentRuns;

  RunConstraints(Integer maxConcurrentRuns) {
    this.maxConcurrentRuns = maxConcurrentRuns;
  }

  /**
   * @return the maximum number of concurrent runs for a schedule.
   *         When a schedule is triggered, the scheduler will look up all active runs for the scheduled program.
   *         If that number is equal to or greater than the max, the run will be skipped.
   */
  @Nullable
  public Integer getMaxConcurrentRuns() {
    return maxConcurrentRuns;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (other == null || getClass() != other.getClass()) {
      return false;
    }

    RunConstraints that = (RunConstraints) other;
    return Objects.equals(maxConcurrentRuns, that.maxConcurrentRuns);
  }

  @Override
  public int hashCode() {
    return Objects.hash(maxConcurrentRuns);
  }
}
