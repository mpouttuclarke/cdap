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

package co.cask.cdap.cli.command.app;

import co.cask.cdap.cli.CLIConfig;
import co.cask.cdap.client.ApplicationClient;
import co.cask.cdap.proto.BatchProgram;
import co.cask.common.cli.Arguments;
import com.google.common.base.Joiner;

import java.io.PrintStream;

/**
 * Base class for stop/start/status commands that work on multiple programs in an application.
 *
 * @param <T> the type of input object for the batch command
 */
public abstract class BatchLifecycleCommand<T extends BatchProgram> extends BaseBatchCommand<T> {

  protected BatchLifecycleCommand(ApplicationClient appClient, CLIConfig cliConfig) {
    super(appClient, cliConfig);
  }

  @Override
  public void perform(Arguments arguments, PrintStream output) throws Exception {
    Args<T> args = readArgs(arguments);
    if (args.programs.isEmpty()) {
      output.printf(String.format("application '%s' contains no programs of type '%s'",
                                  args.appId.getId(), Joiner.on(',').join(args.programTypes)));
      return;
    }

    runBatchCommand(output, args);
  }

  /**
   * Run the batch command on all programs.
   */
  protected abstract void runBatchCommand(PrintStream printStream, Args<T> args) throws Exception;

}
