package org.dice_research.ldcbench.benchmark;

import java.io.File;
import java.util.Arrays;
import java.util.concurrent.Future;

import org.dice_research.ldcbench.Constants;
import org.dice_research.ldcbench.benchmark.cloud.NodeManager;
import org.dice_research.ldcbench.vocab.LDCBench;
import org.hobbit.utils.rdf.RdfHelper;

public class LemmingBasedBenchmarkController extends BenchmarkController {

    // private static final Logger LOGGER =
    // LoggerFactory.getLogger(LemmingBasedBenchmarkController.class);

    private static final String DATA_GEN_CLASS_NAME = "org.dice_research.ldcbench.benchmark.FileBasedRDFGraphGenerator";
    private static final String DATA_DIRECTORY = "/usr/src/app/data";

    private int fileId = 0;
    private File files[];

    @Override
    public void init() throws Exception {
        super.init();
        // Read dataset name
        String datasetName = RdfHelper.getStringValue(benchmarkParamModel, null, LDCBench.lemmingDatasetName);
        if (datasetName == null) {
            throw new IllegalArgumentException("The lemming dataset name is missing!");
        }
        // Get all files of a dataset
        File dataDir = new File(DATA_DIRECTORY + File.separator + datasetName);
        if (!dataDir.exists() || !dataDir.isDirectory()) {
            throw new IllegalArgumentException(
                    "The given dataset directory (" + dataDir.getAbsolutePath() + ") does not exist!");
        }
        files = dataDir.listFiles();
        if (files.length == 0) {
            throw new IllegalArgumentException(
                    "The given dataset directory (" + dataDir.getAbsolutePath() + ") does not contain any files!");
        }
    }

    protected void createDataGenerator(NodeManager nodeManager, String[] envVariables) {
        String[] variables;
        String imageName = nodeManager.getDataGeneratorImageName();
        if (Constants.DATAGEN_IMAGE_NAME.equals(imageName)) {
            variables = envVariables != null ? Arrays.copyOf(envVariables, envVariables.length + 4) : new String[4];
            variables[variables.length - 4] = "CLASS=" + DATA_GEN_CLASS_NAME;
            variables[variables.length - 3] = FileBasedRDFGraphGenerator.RDF_FILE_LOCATION_KEY + "=" + files[fileId];
            variables[variables.length - 2] = FileBasedRDFGraphGenerator.RDF_FILE_LANG_KEY + "=TTL";
            ++fileId;
            // If we have reached the end of the files, start again
            if (fileId == files.length) {
                fileId = 0;
            }
        } else {
            variables = envVariables != null ? Arrays.copyOf(envVariables, envVariables.length + 1) : new String[1];
        }
        variables[variables.length - 1] = org.hobbit.core.Constants.GENERATOR_ID_KEY + "="
                + (dataGenContainers.size() + 1);
        Future<String> container = createContainerAsync(imageName, org.hobbit.core.Constants.CONTAINER_TYPE_BENCHMARK,
                variables);
        dataGenContainers.add(container);
    }
}
