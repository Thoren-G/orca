package org.dice_research.ldcbench.benchmark.eval;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.dice_research.ldcbench.graph.Graph;
import org.dice_research.ldcbench.graph.serialization.SerializationHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileBasedGraphSupplier implements GraphSupplier {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileBasedGraphSupplier.class);

    protected String[] graphFiles;
    protected String[] domains;

    public FileBasedGraphSupplier(String[] graphFiles, String[] domains) {
        this.graphFiles = graphFiles;
        this.domains = domains;
    }

    @Override
    public int getNumberOfGraphs() {
        return graphFiles.length;
    }

    @Override
    public Graph getGraph(int id) {
        try {
            return SerializationHelper.deserialize(FileUtils.readFileToByteArray(new File(graphFiles[id])));
        } catch (Exception e) {
            LOGGER.error("Couldn't load graph #" + id + ". Returning null.", e);
        }
        return null;
    }

    @Override
    public String[] getDomains() {
        return domains;
    }

}