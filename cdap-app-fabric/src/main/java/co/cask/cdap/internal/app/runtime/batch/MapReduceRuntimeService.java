/*
 * Copyright © 2014-2015 Cask Data, Inc.
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
package co.cask.cdap.internal.app.runtime.batch;

import co.cask.cdap.api.Resources;
import co.cask.cdap.api.data.batch.DatasetOutputCommitter;
import co.cask.cdap.api.data.batch.InputFormatProvider;
import co.cask.cdap.api.data.batch.OutputFormatProvider;
import co.cask.cdap.api.dataset.DataSetException;
import co.cask.cdap.api.flow.flowlet.StreamEvent;
import co.cask.cdap.api.mapreduce.MapReduce;
import co.cask.cdap.api.mapreduce.MapReduceContext;
import co.cask.cdap.api.mapreduce.MapReduceSpecification;
import co.cask.cdap.api.stream.StreamEventDecoder;
import co.cask.cdap.common.conf.CConfiguration;
import co.cask.cdap.common.conf.ConfigurationUtil;
import co.cask.cdap.common.conf.Constants;
import co.cask.cdap.common.io.Locations;
import co.cask.cdap.common.lang.ClassLoaders;
import co.cask.cdap.common.lang.CombineClassLoader;
import co.cask.cdap.common.lang.WeakReferenceDelegatorClassLoader;
import co.cask.cdap.common.logging.LoggingContextAccessor;
import co.cask.cdap.common.twill.HadoopClassExcluder;
import co.cask.cdap.common.twill.LocalLocationFactory;
import co.cask.cdap.common.utils.DirUtils;
import co.cask.cdap.data.stream.StreamInputFormat;
import co.cask.cdap.data.stream.StreamInputFormatProvider;
import co.cask.cdap.data2.metadata.lineage.AccessType;
import co.cask.cdap.data2.registry.UsageRegistry;
import co.cask.cdap.data2.transaction.Transactions;
import co.cask.cdap.data2.transaction.stream.StreamAdmin;
import co.cask.cdap.data2.util.hbase.HBaseTableUtilFactory;
import co.cask.cdap.internal.app.runtime.LocalizationUtils;
import co.cask.cdap.internal.app.runtime.batch.dataset.MultipleOutputs;
import co.cask.cdap.internal.app.runtime.batch.dataset.MultipleOutputsMainOutputWrapper;
import co.cask.cdap.internal.app.runtime.batch.dataset.UnsupportedOutputFormat;
import co.cask.cdap.internal.app.runtime.batch.distributed.ContainerLauncherGenerator;
import co.cask.cdap.internal.app.runtime.batch.distributed.MapReduceContainerHelper;
import co.cask.cdap.internal.app.runtime.batch.distributed.MapReduceContainerLauncher;
import co.cask.cdap.internal.app.runtime.distributed.LocalizeResource;
import co.cask.cdap.proto.Id;
import co.cask.cdap.proto.ProgramType;
import co.cask.tephra.Transaction;
import co.cask.tephra.TransactionContext;
import co.cask.tephra.TransactionFailureException;
import co.cask.tephra.TransactionSystemClient;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import com.google.common.reflect.TypeToken;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.inject.Injector;
import com.google.inject.ProvisionException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapreduce.InputFormat;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.MRJobConfig;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.OutputFormat;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.security.Credentials;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.twill.api.ClassAcceptor;
import org.apache.twill.filesystem.Location;
import org.apache.twill.filesystem.LocationFactory;
import org.apache.twill.internal.ApplicationBundler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarOutputStream;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Performs the actual execution of mapreduce job.
 *
 * Service start -> Performs job setup, beforeSubmit and submit job
 * Service run -> Poll for job completion
 * Service triggerStop -> kill job
 * Service stop -> Commit/abort transaction, onFinish, cleanup
 */
final class MapReduceRuntimeService extends AbstractExecutionThreadService {

  private static final Logger LOG = LoggerFactory.getLogger(MapReduceRuntimeService.class);

  /**
   * Do not remove: we need this variable for loading MRClientSecurityInfo class required for communicating with
   * AM in secure mode.
   */
  @SuppressWarnings("unused")
  private org.apache.hadoop.mapreduce.v2.app.MRClientSecurityInfo mrClientSecurityInfo;

  // Regex pattern for configuration source if it is set programmatically. This constant is not defined in Hadoop
  // Hadoop 2.3.0 and before has a typo as 'programatically', while it is fixed later as 'programmatically'.
  private static final Pattern PROGRAMATIC_SOURCE_PATTERN = Pattern.compile("program{1,2}atically");

  private final Injector injector;
  private final CConfiguration cConf;
  private final Configuration hConf;
  private final MapReduce mapReduce;
  private final MapReduceSpecification specification;
  private final Location programJarLocation;
  private final BasicMapReduceContext context;
  private final LocationFactory locationFactory;
  private final StreamAdmin streamAdmin;
  private final TransactionSystemClient txClient;
  private final UsageRegistry usageRegistry;

  private Job job;
  private Transaction transaction;
  private Runnable cleanupTask;

  // This needs to keep as a field.
  // We need to hold a strong reference to the ClassLoader until the end of the MapReduce job.
  private ClassLoader classLoader;
  private volatile boolean stopRequested;

  MapReduceRuntimeService(Injector injector, CConfiguration cConf, Configuration hConf,
                          MapReduce mapReduce, MapReduceSpecification specification,
                          BasicMapReduceContext context,
                          Location programJarLocation, LocationFactory locationFactory,
                          StreamAdmin streamAdmin, TransactionSystemClient txClient,
                          UsageRegistry usageRegistry) {
    this.injector = injector;
    this.cConf = cConf;
    this.hConf = hConf;
    this.mapReduce = mapReduce;
    this.specification = specification;
    this.programJarLocation = programJarLocation;
    this.locationFactory = locationFactory;
    this.streamAdmin = streamAdmin;
    this.txClient = txClient;
    this.context = context;
    this.usageRegistry = usageRegistry;
  }

  @Override
  protected String getServiceName() {
    return "MapReduceRunner-" + specification.getName();
  }

  @Override
  protected void startUp() throws Exception {
    // Creates a temporary directory locally for storing all generated files.
    File tempDir = createTempDirectory();
    cleanupTask = createCleanupTask(tempDir);

    try {
      Job job = createJob(new File(tempDir, "mapreduce"));
      Configuration mapredConf = job.getConfiguration();

      classLoader = new MapReduceClassLoader(injector, cConf, mapredConf, context.getProgram().getClassLoader(),
                                             context.getPlugins(),
                                             context.getPluginInstantiator());
      cleanupTask = createCleanupTask(cleanupTask, classLoader);

      mapredConf.setClassLoader(new WeakReferenceDelegatorClassLoader(classLoader));
      ClassLoaders.setContextClassLoader(mapredConf.getClassLoader());

      context.setJob(job);

      beforeSubmit(job);

      // Localize additional resources that users have requested via BasicMapReduceContext.localize methods
      Map<String, String> localizedUserResources = localizeUserResources(job, tempDir);

      // Override user-defined job name, since we set it and depend on the name.
      // https://issues.cask.co/browse/CDAP-2441
      String jobName = job.getJobName();
      if (!jobName.isEmpty()) {
        LOG.warn("Job name {} is being overridden.", jobName);
      }
      job.setJobName(getJobName(context));

      // Create a temporary location for storing all generated files through the LocationFactory.
      Location tempLocation = createTempLocationDirectory();
      cleanupTask = createCleanupTask(cleanupTask, tempLocation);

      // For local mode, everything is in the configuration classloader already, hence no need to create new jar
      if (!MapReduceTaskContextProvider.isLocal(mapredConf)) {
        // After calling beforeSubmit, we know what plugins are needed for the program, hence construct the proper
        // ClassLoader from here and use it for setting up the job
        Location pluginArchive = createPluginArchive(tempLocation);
        if (pluginArchive != null) {
          job.addCacheArchive(Locations.toURI(pluginArchive));
          mapredConf.set(Constants.Plugin.ARCHIVE, pluginArchive.getName());
        }
      }

      setOutputClassesIfNeeded(job);
      setMapOutputClassesIfNeeded(job);

      // set resources for the job
      TaskType.MAP.setResources(mapredConf, context.getMapperResources());
      TaskType.REDUCE.setResources(mapredConf, context.getReducerResources());

      // replace user's Mapper & Reducer's with our wrappers in job config
      MapperWrapper.wrap(job);
      ReducerWrapper.wrap(job);

      // packaging job jar which includes cdap classes with dependencies
      File jobJar = buildJobJar(job, tempDir);
      job.setJar(jobJar.toURI().toString());

      Location programJar = programJarLocation;
      if (!MapReduceTaskContextProvider.isLocal(mapredConf)) {
        // Copy and localize the program jar in distributed mode
        programJar = copyProgramJar(tempLocation);
        job.addCacheFile(Locations.toURI(programJar));

        // Generate and localize the launcher jar to control the classloader of MapReduce containers processes
        List<String> paths = new ArrayList<>();
        paths.add("job.jar/lib/*");
        paths.add("job.jar/classes");
        Location launcherJar = createLauncherJar(
          Joiner.on(",").join(MapReduceContainerHelper.getMapReduceClassPath(mapredConf, paths)), tempLocation);
        job.addCacheFile(Locations.toURI(launcherJar));

        // The only thing in the container classpath is the launcher.jar
        // The MapReduceContainerLauncher inside the launcher.jar will creates a MapReduceClassLoader and launch
        // the actual MapReduce AM/Task from that
        // We explicitly localize the mr-framwork, but not use it with the classpath
        URI frameworkURI = MapReduceContainerHelper.getFrameworkURI(mapredConf);
        if (frameworkURI != null) {
          job.addCacheArchive(frameworkURI);
        }

        mapredConf.unset(MRJobConfig.MAPREDUCE_APPLICATION_FRAMEWORK_PATH);
        mapredConf.set(MRJobConfig.MAPREDUCE_APPLICATION_CLASSPATH, launcherJar.getName());
        mapredConf.set(YarnConfiguration.YARN_APPLICATION_CLASSPATH, launcherJar.getName());
      }

      MapReduceContextConfig contextConfig = new MapReduceContextConfig(mapredConf);
      // We start long-running tx to be used by mapreduce job tasks.
      Transaction tx = txClient.startLong();
      try {
        // We remember tx, so that we can re-use it in mapreduce tasks
        CConfiguration cConfCopy = cConf;
        contextConfig.set(context, cConfCopy, tx, Locations.toURI(programJar), localizedUserResources);

        LOG.info("Submitting MapReduce Job: {}", context);
        // submits job and returns immediately. Shouldn't need to set context ClassLoader.
        job.submit();

        this.job = job;
        this.transaction = tx;
      } catch (Throwable t) {
        Transactions.invalidateQuietly(txClient, tx);
        throw t;
      }
    } catch (Throwable t) {
      LOG.error("Exception when submitting MapReduce Job: {}", context, t);
      cleanupTask.run();
      throw t;
    }
  }

  @Override
  protected void run() throws Exception {
    MapReduceMetricsWriter metricsWriter = new MapReduceMetricsWriter(job, context);

    // until job is complete report stats
    while (!job.isComplete()) {
      metricsWriter.reportStats();

      // we report to metrics backend every second, so 1 sec is enough here. That's mapreduce job anyways (not
      // short) ;)
      TimeUnit.SECONDS.sleep(1);
    }

    LOG.info("MapReduce Job is complete, status: {}, job: {}", job.isSuccessful(), context);
    // NOTE: we want to report the final stats (they may change since last report and before job completed)
    metricsWriter.reportStats();
    // If we don't sleep, the final stats may not get sent before shutdown.
    TimeUnit.SECONDS.sleep(2L);

    // If the job is not successful, throw exception so that this service will terminate with a failure state
    // Shutdown will still get executed, but the service will notify failure after that.
    // However, if it's the job is requested to stop (via triggerShutdown, meaning it's a user action), don't throw
    if (!stopRequested) {
      Preconditions.checkState(job.isSuccessful(), "MapReduce execution failure: %s", job.getStatus());
    }
  }

  @Override
  protected void shutDown() throws Exception {
    boolean success = job.isSuccessful();

    try {
      if (success) {
        LOG.info("Committing MapReduce Job transaction: {}", context);
        // committing long running tx: no need to commit datasets, as they were committed in external processes
        // also no need to rollback changes if commit fails, as these changes where performed by mapreduce tasks
        // NOTE: can't call afterCommit on datasets in this case: the changes were made by external processes.
        if (!txClient.commit(transaction)) {
          LOG.warn("MapReduce Job transaction failed to commit");
          throw new TransactionFailureException("Failed to commit transaction for MapReduce " + context.toString());
        }
      } else {
        // invalids long running tx. All writes done by MR cannot be undone at this point.
        txClient.invalidate(transaction.getWritePointer());
      }
    } finally {
      // whatever happens we want to call this
      try {
        onFinish(success);
      } finally {
        context.close();
        cleanupTask.run();
      }
    }
  }

  @Override
  protected void triggerShutdown() {
    try {
      stopRequested = true;
      if (job != null && !job.isComplete()) {
        job.killJob();
      }
    } catch (IOException e) {
      LOG.error("Failed to kill MapReduce job {}", context, e);
      throw Throwables.propagate(e);
    }
  }

  @Override
  protected Executor executor() {
    // Always execute in new daemon thread.
    return new Executor() {
      @Override
      public void execute(@Nonnull final Runnable runnable) {
        final Thread t = new Thread(new Runnable() {

          @Override
          public void run() {
            // note: this sets logging context on the thread level
            LoggingContextAccessor.setLoggingContext(context.getLoggingContext());
            runnable.run();
          }
        });
        t.setDaemon(true);
        t.setName(getServiceName());
        t.start();
      }
    };
  }

  /**
   * Creates a MapReduce {@link Job} instance.
   *
   * @param hadoopTmpDir directory for the "hadoop.tmp.dir" configuration
   */
  private Job createJob(File hadoopTmpDir) throws IOException {
    Job job = Job.getInstance(new Configuration(hConf));
    Configuration jobConf = job.getConfiguration();

    if (MapReduceTaskContextProvider.isLocal(jobConf)) {
      // Set the MR framework local directories inside the given tmp directory.
      // Setting "hadoop.tmp.dir" here has no effect due to Explore Service need to set "hadoop.tmp.dir"
      // as system property for Hive to work in local mode. The variable substitution of hadoop conf
      // gives system property the highest precedence.
      jobConf.set("mapreduce.cluster.local.dir", new File(hadoopTmpDir, "local").getAbsolutePath());
      jobConf.set("mapreduce.jobtracker.system.dir", new File(hadoopTmpDir, "system").getAbsolutePath());
      jobConf.set("mapreduce.jobtracker.staging.root.dir", new File(hadoopTmpDir, "staging").getAbsolutePath());
      jobConf.set("mapreduce.cluster.temp.dir", new File(hadoopTmpDir, "temp").getAbsolutePath());
    }

    if (UserGroupInformation.isSecurityEnabled()) {
      // If runs in secure cluster, this program runner is running in a yarn container, hence not able
      // to get authenticated with the history.
      jobConf.unset("mapreduce.jobhistory.address");
      jobConf.setBoolean(Job.JOB_AM_ACCESS_DISABLED, false);

      Credentials credentials = UserGroupInformation.getCurrentUser().getCredentials();
      LOG.info("Running in secure mode; adding all user credentials: {}", credentials.getAllTokens());
      job.getCredentials().addAll(credentials);
    }
    return job;
  }

  /**
   * Creates a local temporary directory for this MapReduce run.
   */
  private File createTempDirectory() {
    Id.Program programId = context.getProgram().getId();
    File tempDir = new File(cConf.get(Constants.CFG_LOCAL_DATA_DIR),
                            cConf.get(Constants.AppFabric.TEMP_DIR)).getAbsoluteFile();
    File runtimeServiceDir = new File(tempDir, "runner");
    File dir = new File(runtimeServiceDir, String.format("%s.%s.%s.%s.%s",
                                                         programId.getType().name().toLowerCase(),
                                                         programId.getNamespaceId(), programId.getApplicationId(),
                                                         programId.getId(), context.getRunId().getId()));
    dir.mkdirs();
    return dir;
  }

  /**
   * Creates a temporary directory through the {@link LocationFactory} provided to this class.
   */
  private Location createTempLocationDirectory() throws IOException {
    Id.Program programId = context.getProgram().getId();

    String tempLocationName = String.format("%s/%s.%s.%s.%s.%s", cConf.get(Constants.AppFabric.TEMP_DIR),
                                            programId.getType().name().toLowerCase(),
                                            programId.getNamespaceId(), programId.getApplicationId(),
                                            programId.getId(), context.getRunId().getId());
    Location location = locationFactory.create(tempLocationName);
    location.mkdirs();
    return location;
  }

  /**
   * Calls the {@link MapReduce#beforeSubmit(MapReduceContext)} method and
   * also setup the Input/Output dataset within the same transaction.
   */
  private void beforeSubmit(final Job job) throws TransactionFailureException {
    TransactionContext txContext = context.getTransactionContext();
    Transactions.execute(txContext, "beforeSubmit", new Callable<Void>() {
      @Override
      public Void call() throws Exception {
        ClassLoader oldClassLoader = setContextCombinedClassLoader(context);
        try {
          mapReduce.beforeSubmit(context);

          // set input/output datasets info
          setInputDatasetIfNeeded(job);
          setOutputDatasetsIfNeeded(job);

          return null;
        } finally {
          ClassLoaders.setContextClassLoader(oldClassLoader);
        }
      }
    });
  }

  /**
   * Commit a single output after the MR has finished, if it is an OutputFormatCommitter.
   * @param succeeded whether the run was successful
   * @param datasetName the name of the dataset
   * @param outputFormatProvider the output format provider to commit
   * @return whether the action was successful (it did not throw an exception)
   */
  private boolean commitOutput(boolean succeeded, String datasetName, OutputFormatProvider outputFormatProvider) {
    if (outputFormatProvider instanceof DatasetOutputCommitter) {
      try {
        if (succeeded) {
          ((DatasetOutputCommitter) outputFormatProvider).onSuccess();
        } else {
          ((DatasetOutputCommitter) outputFormatProvider).onFailure();
        }
      } catch (Throwable t) {
        LOG.error(String.format("Error from %s method of output dataset %s.",
                                succeeded ? "onSuccess" : "onFailure", datasetName), t);
        return false;
      }
    }
    return true;
  }

  /**
   * Calls the {@link MapReduce#onFinish(boolean, co.cask.cdap.api.mapreduce.MapReduceContext)} method.
   */
  private void onFinish(final boolean succeeded) throws TransactionFailureException {
    TransactionContext txContext = context.getTransactionContext();
    Transactions.execute(txContext, "onFinish", new Callable<Void>() {
      @Override
      public Void call() throws Exception {
        ClassLoader oldClassLoader = setContextCombinedClassLoader(context);
        try {
          // TODO (CDAP-1952): this should be done in the output committer, to make the M/R fail if addPartition fails
          // TODO (CDAP-1952): also, should failure of an output committer change the status of the program run?
          boolean success = succeeded;
          for (Map.Entry<String, OutputFormatProvider> dsEntry : context.getOutputFormatProviders().entrySet()) {
            if (!commitOutput(succeeded, dsEntry.getKey(), dsEntry.getValue())) {
              success = false;
            }
          }
          mapReduce.onFinish(success, context);
          return null;
        } finally {
          ClassLoaders.setContextClassLoader(oldClassLoader);
        }
      }
    });
  }

  private void setInputDatasetIfNeeded(Job job) throws IOException {
    InputFormatProvider provider = context.getInputFormatProvider();

    // If input format is not set during beforeSubmit, use the one from the spec if it exists.
    if (provider == null && specification.getInputDataSet() != null) {
      context.setInput(specification.getInputDataSet());
      provider = context.getInputFormatProvider();
    }

    if (provider != null) {
      Configuration jobConf = job.getConfiguration();
      jobConf.set(Job.INPUT_FORMAT_CLASS_ATTR, provider.getInputFormatClassName());
      ConfigurationUtil.setAll(provider.getInputFormatConfiguration(), jobConf);

      // A bit hacky for stream.
      // For stream, we need to do two extra steps.
      // 1. stream usage registration since it only happens on client side.
      // 2. Infer the stream event decoder from Mapper/Reducer
      if (provider instanceof StreamInputFormatProvider) {
        StreamInputFormatProvider streamProvider = (StreamInputFormatProvider) provider;
        Type inputValueType = getInputValueType(jobConf, StreamEvent.class);
        ConfigurationUtil.setAll(streamProvider.setDecoderType(new HashMap<String, String>(), inputValueType), jobConf);

        Id.Stream streamId = streamProvider.getStreamId();
        try {
          usageRegistry.register(context.getProgram().getId(), streamId);
          streamAdmin.addAccess(new Id.Run(context.getProgram().getId(), context.getRunId().getId()),
                                streamId, AccessType.READ);
        } catch (Exception e) {
          LOG.warn("Failed to register usage {} -> {}", context.getProgram().getId(), streamId, e);
        }
      }
    }
  }

  /**
   * Sets the configurations for Datasets used for output.
   */
  private void setOutputDatasetsIfNeeded(Job job) {
    Map<String, OutputFormatProvider> outputFormatProviders = context.getOutputFormatProviders();
    LOG.debug("Using Datasets as output for MapReduce Job: {}", outputFormatProviders.keySet());

    if (outputFormatProviders.isEmpty()) {
      // user is not going through our APIs to add output; leave the job's output format to user
      return;
    } else if (outputFormatProviders.size() == 1) {
      // If only one output is configured through the context, then set it as the root OutputFormat
      Map.Entry<String, OutputFormatProvider> next = outputFormatProviders.entrySet().iterator().next();
      OutputFormatProvider outputFormatProvider = next.getValue();
      job.getConfiguration().set(Job.OUTPUT_FORMAT_CLASS_ATTR, outputFormatProvider.getOutputFormatClassName());
      for (Map.Entry<String, String> entry : outputFormatProvider.getOutputFormatConfiguration().entrySet()) {
        job.getConfiguration().set(entry.getKey(), entry.getValue());
      }
      return;
    }
    // multiple output formats configured via the context. We should use a RecordWriter that doesn't support writing
    // as the root output format in this case to disallow writing directly on the context
    MultipleOutputsMainOutputWrapper.setRootOutputFormat(job, UnsupportedOutputFormat.class.getName(),
                                                         new HashMap<String, String>());
    job.setOutputFormatClass(MultipleOutputsMainOutputWrapper.class);

    for (Map.Entry<String, OutputFormatProvider> entry : outputFormatProviders.entrySet()) {
      String outputDatasetName = entry.getKey();
      OutputFormatProvider outputFormatProvider = entry.getValue();

      String outputFormatClassName = outputFormatProvider.getOutputFormatClassName();
      if (outputFormatClassName == null) {
        throw new DataSetException("Output dataset '" + outputDatasetName + "' provided null as the output format");
      }

      Map<String, String> outputConfig = outputFormatProvider.getOutputFormatConfiguration();
      MultipleOutputs.addNamedOutput(job, outputDatasetName, outputFormatClassName,
                                     job.getOutputKeyClass(), job.getOutputValueClass(), outputConfig);

    }
  }

  /**
   * Returns the input value type of the MR job based on the job Mapper/Reducer type.
   * It does so by inspecting the Mapper/Reducer type parameters to figure out what the input type is.
   * If the job has Mapper, then it's the Mapper IN_VALUE type, otherwise it would be the Reducer IN_VALUE type.
   * If the cannot determine the input value type, then return the given default type.
   */
  @VisibleForTesting
  Type getInputValueType(Configuration hConf, Type defaultType) {
    // Try to see if there is mapper
    TypeToken<?> type = resolveClass(hConf, MRJobConfig.MAP_CLASS_ATTR, Mapper.class);
    if (type == null) {
      // If there is no Mapper, it's a Reducer only job, hence get the value type from Reducer class
      type = resolveClass(hConf, MRJobConfig.REDUCE_CLASS_ATTR, Reducer.class);
    }
    Preconditions.checkArgument(type != null, "No Mapper and Reducer for the MapReduce job.");

    if (!(type.getType() instanceof ParameterizedType)) {
      return defaultType;
    }

    // The super type Mapper/Reducer must be a parametrized type with <IN_KEY, IN_VALUE, OUT_KEY, OUT_VALUE>
    Type inputValueType = ((ParameterizedType) type.getType()).getActualTypeArguments()[1];

    // If the concrete Mapper/Reducer class is not parameterized (meaning not extends with parameters),
    // then assume use the default type.
    // We need to check if the TypeVariable is the same as the one in the parent type.
    // This avoid the case where a subclass that has "class InvalidMapper<I, O> extends Mapper<I, O>"
    if (inputValueType instanceof TypeVariable && inputValueType.equals(type.getRawType().getTypeParameters()[1])) {
      inputValueType = defaultType;
    }
    return inputValueType;
  }

  private String getJobName(BasicMapReduceContext context) {
    Id.Program programId = context.getProgram().getId();
    // MRJobClient expects the following format (for RunId to be the first component)
    return String.format("%s.%s.%s.%s.%s",
                         context.getRunId().getId(), ProgramType.MAPREDUCE.name().toLowerCase(),
                         programId.getNamespaceId(), programId.getApplicationId(), programId.getId());
  }

  /**
   * Creates a jar that contains everything that are needed for running the MapReduce program by Hadoop.
   *
   * @return a new {@link File} containing the job jar
   */
  private File buildJobJar(Job job, File tempDir) throws IOException, URISyntaxException {
    File jobJar = new File(tempDir, "job.jar");
    LOG.debug("Creating Job jar: {}", jobJar);

    // For local mode, nothing is needed in the job jar since we use the classloader in the configuration object.
    if (MapReduceTaskContextProvider.isLocal(job.getConfiguration())) {
      JarOutputStream output = new JarOutputStream(new FileOutputStream(jobJar));
      output.close();
      return jobJar;
    }

    // Excludes libraries that are for sure not needed.
    // Hadoop - Available from the cluster
    // Spark - MR never uses Spark
    final HadoopClassExcluder hadoopClassExcluder = new HadoopClassExcluder();
    ApplicationBundler appBundler = new ApplicationBundler(new ClassAcceptor() {
      @Override
      public boolean accept(String className, URL classUrl, URL classPathUrl) {
        if (className.startsWith("org.apache.spark") || classPathUrl.toString().contains("spark-assembly")) {
          return false;
        }
        return hadoopClassExcluder.accept(className, classUrl, classPathUrl);
      }
    });
    Set<Class<?>> classes = Sets.newHashSet();
    classes.add(MapReduce.class);
    classes.add(MapperWrapper.class);
    classes.add(ReducerWrapper.class);

    // We only need to trace the Input/OutputFormat class due to MAPREDUCE-5957 so that those classes are included
    // in the job.jar and be available in the MR system classpath before our job classloader (ApplicationClassLoader)
    // take over the classloading.
    if (cConf.getBoolean(Constants.AppFabric.MAPREDUCE_INCLUDE_CUSTOM_CLASSES)) {
      try {
        Class<? extends InputFormat<?, ?>> inputFormatClass = job.getInputFormatClass();
        LOG.info("InputFormat class: {} {}", inputFormatClass, inputFormatClass.getClassLoader());
        classes.add(inputFormatClass);

        // If it is StreamInputFormat, also add the StreamEventCodec class as well.
        if (StreamInputFormat.class.isAssignableFrom(inputFormatClass)) {
          Class<? extends StreamEventDecoder> decoderType =
            StreamInputFormat.getDecoderClass(job.getConfiguration());
          if (decoderType != null) {
            classes.add(decoderType);
          }
        }
      } catch (Throwable t) {
        LOG.info("InputFormat class not found: {}", t.getMessage(), t);
        // Ignore
      }
      try {
        Class<? extends OutputFormat<?, ?>> outputFormatClass = job.getOutputFormatClass();
        LOG.info("OutputFormat class: {} {}", outputFormatClass, outputFormatClass.getClassLoader());
        classes.add(outputFormatClass);
      } catch (Throwable t) {
        LOG.info("OutputFormat class not found: {}", t.getMessage(), t);
        // Ignore
      }
    }
    // End of MAPREDUCE-5957.

    try {
      Class<?> hbaseTableUtilClass = HBaseTableUtilFactory.getHBaseTableUtilClass();
      classes.add(hbaseTableUtilClass);
    } catch (ProvisionException e) {
      LOG.warn("Not including HBaseTableUtil classes in submitted Job Jar since they are not available");
    }

    // Add the logback.xml as a resource while creating the MapReduce Job JAR
    Set<URI> logbackURI = Sets.newHashSet();
    URL logback = getClass().getResource("/logback.xml");
    if (logback != null) {
      logbackURI.add(logback.toURI());
    } else {
      LOG.warn("Could not find logback.xml while building MapReduce Job JAR!");
    }

    ClassLoader oldCLassLoader = ClassLoaders.setContextClassLoader(job.getConfiguration().getClassLoader());
    appBundler.createBundle(new LocalLocationFactory().create(jobJar.toURI()), classes, logbackURI);
    ClassLoaders.setContextClassLoader(oldCLassLoader);

    LOG.info("Built MapReduce Job Jar at {}", jobJar.toURI());
    return jobJar;
  }

  /**
   * Returns a resolved {@link TypeToken} of the given super type by reading a class from the job configuration that
   * extends from super type.
   *
   * @param conf the job configuration
   * @param typeAttr The job configuration attribute for getting the user class
   * @param superType Super type of the class to get from the configuration
   * @param <V> Type of the super type
   * @return A resolved {@link TypeToken} or {@code null} if no such class in the job configuration
   */
  @SuppressWarnings("unchecked")
  private <V> TypeToken<V> resolveClass(Configuration conf, String typeAttr, Class<V> superType) {
    Class<? extends V> userClass = conf.getClass(typeAttr, null, superType);
    if (userClass == null) {
      return null;
    }

    return (TypeToken<V>) TypeToken.of(userClass).getSupertype(superType);
  }

  /**
   * Sets the output key and value classes in the job configuration by inspecting the {@link Mapper} and {@link Reducer}
   * if it is not set by the user.
   *
   * @param job the MapReduce job
   */
  private void setOutputClassesIfNeeded(Job job) {
    Configuration conf = job.getConfiguration();

    // Try to get the type from reducer
    TypeToken<?> type = resolveClass(conf, MRJobConfig.REDUCE_CLASS_ATTR, Reducer.class);

    if (type == null) {
      // Map only job
      type = resolveClass(conf, MRJobConfig.MAP_CLASS_ATTR, Mapper.class);
    }

    // If not able to detect type, nothing to set
    if (type == null || !(type.getType() instanceof ParameterizedType)) {
      return;
    }

    Type[] typeArgs = ((ParameterizedType) type.getType()).getActualTypeArguments();

    // Set it only if the user didn't set it in beforeSubmit
    // The key and value type are in the 3rd and 4th type parameters
    if (!isProgrammaticConfig(conf, MRJobConfig.OUTPUT_KEY_CLASS)) {
      Class<?> cls = TypeToken.of(typeArgs[2]).getRawType();
      LOG.debug("Set output key class to {}", cls);
      job.setOutputKeyClass(cls);
    }
    if (!isProgrammaticConfig(conf, MRJobConfig.OUTPUT_VALUE_CLASS)) {
      Class<?> cls = TypeToken.of(typeArgs[3]).getRawType();
      LOG.debug("Set output value class to {}", cls);
      job.setOutputValueClass(cls);
    }
  }

  /**
   * Sets the map output key and value classes in the job configuration by inspecting the {@link Mapper}
   * if it is not set by the user.
   *
   * @param job the MapReduce job
   */
  private void setMapOutputClassesIfNeeded(Job job) {
    Configuration conf = job.getConfiguration();

    int keyIdx = 2;
    int valueIdx = 3;
    TypeToken<?> type = resolveClass(conf, MRJobConfig.MAP_CLASS_ATTR, Mapper.class);

    if (type == null) {
      // Reducer only job. Use the Reducer input types as the key/value classes.
      type = resolveClass(conf, MRJobConfig.REDUCE_CLASS_ATTR, Reducer.class);
      keyIdx = 0;
      valueIdx = 1;
    }

    // If not able to detect type, nothing to set.
    if (type == null || !(type.getType() instanceof ParameterizedType)) {
      return;
    }

    Type[] typeArgs = ((ParameterizedType) type.getType()).getActualTypeArguments();

    // Set it only if the user didn't set it in beforeSubmit
    // The key and value type are in the 3rd and 4th type parameters
    if (!isProgrammaticConfig(conf, MRJobConfig.MAP_OUTPUT_KEY_CLASS)) {
      Class<?> cls = TypeToken.of(typeArgs[keyIdx]).getRawType();
      LOG.debug("Set map output key class to {}", cls);
      job.setMapOutputKeyClass(cls);
    }
    if (!isProgrammaticConfig(conf, MRJobConfig.MAP_OUTPUT_VALUE_CLASS)) {
      Class<?> cls = TypeToken.of(typeArgs[valueIdx]).getRawType();
      LOG.debug("Set map output value class to {}", cls);
      job.setMapOutputValueClass(cls);
    }
  }

  private boolean isProgrammaticConfig(Configuration conf, String name) {
    String[] sources = conf.getPropertySources(name);
    return sources != null && sources.length > 0 &&
      PROGRAMATIC_SOURCE_PATTERN.matcher(sources[sources.length - 1]).matches();
  }

  /**
   * Copies a plugin archive jar to the target location.
   *
   * @param targetDir directory where the archive jar should be created
   * @return {@link Location} to the plugin archive or {@code null} if no plugin archive is available from the context.
   */
  @Nullable
  private Location createPluginArchive(Location targetDir) throws IOException {
    File pluginArchive = context.getPluginArchive();
    if (pluginArchive == null) {
      return null;
    }
    Location pluginLocation = targetDir.append(pluginArchive.getName()).getTempFile(".jar");
    Files.copy(pluginArchive, Locations.newOutputSupplier(pluginLocation));
    return pluginLocation;
  }

  /**
   * Creates a temp copy of the program jar.
   *
   * @return a new {@link Location} which contains the same content as the program jar
   */
  private Location copyProgramJar(Location targetDir) throws IOException {
    Location programJarCopy = targetDir.append("program.jar");

    ByteStreams.copy(Locations.newInputSupplier(programJarLocation), Locations.newOutputSupplier(programJarCopy));
    LOG.info("Copied Program Jar to {}, source: {}", programJarCopy, programJarLocation);
    return programJarCopy;
  }

  /**
   * Creates a launcher jar.
   *
   * @see MapReduceContainerLauncher
   * @see ContainerLauncherGenerator
   */
  private Location createLauncherJar(String applicationClassPath, Location targetDir) throws IOException {
    Location launcherJar = targetDir.append("launcher.jar");
    ContainerLauncherGenerator.generateLauncherJar(applicationClassPath, MapReduceClassLoader.class.getName(),
                                                   Locations.newOutputSupplier(launcherJar));
    return launcherJar;
  }

  private Runnable createCleanupTask(final Object...resources) {
    return new Runnable() {

      @Override
      public void run() {
        for (Object resource : resources) {
          if (resource == null) {
            continue;
          }

          try {
            if (resource instanceof File) {
              if (((File) resource).isDirectory()) {
                DirUtils.deleteDirectoryContents((File) resource);
              } else {
                ((File) resource).delete();
              }
            } else if (resource instanceof Location) {
              Locations.deleteQuietly((Location) resource, true);
            } else if (resource instanceof AutoCloseable) {
              ((AutoCloseable) resource).close();
            } else if (resource instanceof Runnable) {
              ((Runnable) resource).run();
            }
          } catch (Throwable t) {
            LOG.warn("Exception when cleaning up resource {}", resource, t);
          }
        }
      }
    };
  }

  private enum TaskType {
    MAP(Job.MAP_MEMORY_MB, Job.MAP_JAVA_OPTS),
    REDUCE(Job.REDUCE_MEMORY_MB, Job.REDUCE_JAVA_OPTS);

    private final String memoryConfKey;
    private final String javaOptsKey;
    private final String vcoreConfKey;

    TaskType(String memoryConfKey, String javaOptsKey) {
      this.memoryConfKey = memoryConfKey;
      this.javaOptsKey = javaOptsKey;

      String vcoreConfKey = null;
      try {
        String fieldName = name() + "_CPU_VCORES";
        Field field = Job.class.getField(fieldName);
        vcoreConfKey = field.get(null).toString();
      } catch (Exception e) {
        // OK to ignore
        // Some older version of hadoop-mr-client doesn't has the VCORES field as vcores was not supported in YARN.
      }
      this.vcoreConfKey = vcoreConfKey;
    }

    /**
     * Sets up resources usage for the task represented by this task type.
     *
     * @param conf configuration to modify
     * @param resources resources information or {@code null} if nothing to set
     */
    public void setResources(Configuration conf, @Nullable Resources resources) {
      if (resources == null) {
        return;
      }

      conf.setInt(memoryConfKey, resources.getMemoryMB());
      // Also set the Xmx to be smaller than the container memory.
      conf.set(javaOptsKey, "-Xmx" + (int) (resources.getMemoryMB() * 0.8) + "m");

      if (vcoreConfKey != null) {
        conf.setInt(vcoreConfKey, resources.getVirtualCores());
      }
    }
  }

  private ClassLoader setContextCombinedClassLoader(BasicMapReduceContext context) {
    return ClassLoaders.setContextClassLoader(new CombineClassLoader(
      null, ImmutableList.of(context.getProgram().getClassLoader(), getClass().getClassLoader())));
  }

  /**
   * Localizes resources requested by users in the MapReduce Program's beforeSubmit phase.
   * In Local mode, also copies resources to a temporary directory.
   *
   * @param job the {@link Job} for this MapReduce program
   * @param targetDir in local mode, a temporary directory to copy the resources to
   * @return a {@link Map} of resource name to the resource path. The resource path will be absolute in local mode,
   * while it will just contain the file name in distributed mode.
   */
  private Map<String, String> localizeUserResources(Job job, File targetDir) throws IOException {
    Map<String, String> localizedResources = new HashMap<>();
    Map<String, LocalizeResource> resourcesToLocalize = context.getResourcesToLocalize();
    for (Map.Entry<String, LocalizeResource> entry : resourcesToLocalize.entrySet()) {
      String localizedFilePath;
      String name = entry.getKey();
      Configuration mapredConf = job.getConfiguration();
      if (MapReduceTaskContextProvider.isLocal(mapredConf)) {
        // in local mode, also add localize resources in a temporary directory
        localizedFilePath =
          LocalizationUtils.localizeResource(entry.getKey(), entry.getValue(), targetDir).getAbsolutePath();
      } else {
        URI uri = entry.getValue().getURI();
        // in distributed mode, use the MapReduce Job object to localize resources
        URI actualURI;
        try {
          actualURI = new URI(uri.getScheme(), uri.getAuthority(), uri.getPath(), uri.getQuery(), name);
        } catch (URISyntaxException e) {
          // Most of the URI is constructed from the passed URI. So ideally, this should not happen.
          // If it does though, there is nothing that clients can do to recover, so not propagating a checked exception.
          throw Throwables.propagate(e);
        }
        if (entry.getValue().isArchive()) {
          job.addCacheArchive(actualURI);
        } else {
          job.addCacheFile(actualURI);
        }
        localizedFilePath = name;
      }
      localizedResources.put(name, localizedFilePath);
    }
    return localizedResources;
  }
}
