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

import co.cask.cdap.cli.ArgumentName;
import co.cask.cdap.cli.CLIConfig;
import co.cask.cdap.client.ApplicationClient;
import co.cask.cdap.client.ProgramClient;
import co.cask.cdap.common.utils.Tasks;
import co.cask.cdap.proto.BatchProgram;
import co.cask.cdap.proto.BatchProgramStart;
import co.cask.cdap.proto.BatchProgramStatus;
import co.cask.cdap.proto.ProgramRecord;
import co.cask.common.cli.Arguments;
import com.google.inject.Inject;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Starts one or more programs in an application.
 */
public class RestartProgramsCommand extends BaseBatchCommand<BatchProgram> {
  private final ProgramClient programClient;

  @Inject
  public RestartProgramsCommand(ApplicationClient appClient, ProgramClient programClient, CLIConfig cliConfig) {
    super(appClient, cliConfig);
    this.programClient = programClient;
  }

  @Override
  public void perform(Arguments arguments, final PrintStream printStream) throws Exception {
    final Args<BatchProgram> args = readArgs(arguments);
    long timeoutSecs = arguments.getLongOptional(ArgumentName.TIMEOUT_SECONDS.getName(), 300L);

    printStream.print("Stopping programs...\n");
    programClient.stop(args.appId.getNamespace(), args.programs);

    printStream.print("Waiting for programs to enter STOPPED state...\n");
    try {
      Tasks.waitFor(true, new Callable<Boolean>() {
        @Override
        public Boolean call() throws Exception {
          List<BatchProgramStatus> statusList = programClient.getStatus(args.appId.getNamespace(), args.programs);
          for (BatchProgramStatus status : statusList) {
            if (!"STOPPED".equals(status.getStatus())) {
              printStream.print(".");
              return false;
            }
          }
          return true;
        }
      }, timeoutSecs, TimeUnit.SECONDS, 1, TimeUnit.SECONDS);
    } catch (TimeoutException e) {
      printStream.print("Timed out waiting for programs to enter STOPPED state.\n");
      return;
    }

    printStream.print("Starting programs...\n");
    List<BatchProgramStart> startList = new ArrayList<>(args.programs.size());
    for (BatchProgram program : args.programs) {
      startList.add(new BatchProgramStart(program));
    }
    programClient.start(args.appId.getNamespace(), startList);
  }

  @Override
  protected BatchProgram createProgram(ProgramRecord programRecord) {
    return new BatchProgram(programRecord.getApp(), programRecord.getType(), programRecord.getName());
  }

  @Override
  public String getPattern() {
    return String.format("restart app programs <%s> [timeout <%s>] [types <%s>]",
      ArgumentName.APP, ArgumentName.TIMEOUT_SECONDS, ArgumentName.PROGRAM_TYPES);
  }

  @Override
  public String getDescription() {
    return getDescription("restart") + " After issuing a stop to all programs, " +
      "the command will wait for some specified number of seconds for all programs to enter the STOPPED state. " +
      "If programs did not enter the STOPPED state after the time allotted, " +
      "the command will exit without starting any programs. " +
      "A default of 300 seconds will be used if none is specified.";
  }
}
