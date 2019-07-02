package org.dice_research.ldcbench;

import org.hobbit.sdk.docker.builders.hobbit.*;
import org.hobbit.sdk.utils.ModelsHandler;

import java.io.IOException;
import static org.apache.jena.datatypes.xsd.XSDDatatype.*;
import org.apache.jena.rdf.model.*;
import org.dice_research.ldcbench.builders.*;
import org.dice_research.ldcbench.vocab.LDCBench;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hobbit.sdk.Constants.BENCHMARK_URI;
import static org.hobbit.sdk.Constants.GIT_USERNAME;
import static org.dice_research.ldcbench.Constants.*;

/**
 * @author Pavel Smirnov
 */

public class BenchmarkTest extends BenchmarkTestBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(BenchmarkTest.class);

    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    BenchmarkDockerBuilder benchmarkBuilder;
    DataGenDockerBuilder dataGeneratorBuilder;
    SystemAdapterDockerBuilder systemAdapterBuilder;
    EvalModuleDockerBuilder evalModuleBuilder;
    SimpleHttpNodeBuilder httpNodeBuilder;
    CkanNodeBuilder ckanNodeBuilder;

    @Test
    public void executeBenchmark() throws Exception {
        executeBenchmark(false);
    }

    //Flush a queue of a locally running platform
    @Test
    @Ignore
    public void flushQueue(){
        QueueClient queueClient = new QueueClient(GIT_USERNAME);
        queueClient.flushQueue();
    }

    //Submit benchmark to a queue of a locally running platform
    @Test
    @Ignore
    public void submitToQueue() throws Exception {
        QueueClient queueClient = new QueueClient(GIT_USERNAME);
        queueClient.submitToQueue(BENCHMARK_URI, SYSTEM_URI, createBenchmarkParameters());
    }

    @Override
    public Model createBenchmarkParameters() throws IOException {
        return ModelsHandler.readModelFromFile("test-benchmark-parameters.ttl");
    }
}
