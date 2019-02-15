package org.dice_research.ldcbench.nodes.http.simple;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.Semaphore;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.dice_research.ldcbench.ApiConstants;
import org.dice_research.ldcbench.data.NodeMetadata;
import org.dice_research.ldcbench.graph.Graph;
import org.dice_research.ldcbench.nodes.rabbit.GraphHandler;
import org.dice_research.ldcbench.rdf.UriHelper;
import org.hobbit.core.components.AbstractCommandReceivingComponent;
import org.hobbit.core.components.Component;
import org.hobbit.core.rabbit.DataReceiver;
import org.hobbit.core.rabbit.DataReceiverImpl;
import org.hobbit.core.Commands;
import org.hobbit.utils.EnvVariables;
import org.simpleframework.http.core.Container;
import org.simpleframework.http.core.ContainerServer;
import org.simpleframework.transport.Server;
import org.simpleframework.transport.connect.Connection;
import org.simpleframework.transport.connect.SocketConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

public class SimpleHttpServerComponent extends AbstractCommandReceivingComponent implements Component {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleHttpServerComponent.class);

    private static final int DEFAULT_PORT = 80;

    protected Semaphore dataGenerationFinished = new Semaphore(0);
    protected Semaphore domainNamesReceived = new Semaphore(0);
    protected Container container;
    protected Server server;
    protected Connection connection;
    protected Channel bcBroadcastChannel;
    protected DataReceiver receiver;
    protected String domainNames[];

    @Override
    public void init() throws Exception {
        super.init();

        int domainId = EnvVariables.getInt(ApiConstants.ENV_NODE_ID_KEY, LOGGER);

        // initialize exchange with BC
        String exchangeName = EnvVariables.getString(ApiConstants.ENV_BENCHMARK_EXCHANGE_KEY);
        bcBroadcastChannel = cmdQueueFactory.getConnection().createChannel();
        String queueName = bcBroadcastChannel.queueDeclare().getQueue();
        bcBroadcastChannel.exchangeDeclare(exchangeName, "fanout", false, true, null);
        bcBroadcastChannel.queueBind(queueName, exchangeName, "");

        Consumer consumer = new DefaultConsumer(bcBroadcastChannel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
                    byte[] body) throws IOException {
                try {
                    handleBCMessage(body);
                } catch (Exception e) {
                    LOGGER.error("Exception while trying to handle incoming command.", e);
                }
            }
        };
        bcBroadcastChannel.basicConsume(queueName, true, consumer);

        // initialize graph queue
        queueName = EnvVariables.getString(ApiConstants.ENV_DATA_QUEUE_KEY);
        GraphHandler graphHandler = new GraphHandler();
        receiver = DataReceiverImpl.builder().dataHandler(graphHandler).queue(this.incomingDataQueueFactory, queueName)
                .build();

        // Wait for the data generation to finish
        dataGenerationFinished.acquire();
        receiver.closeWhenFinished();

        if (graphHandler.encounteredError()) {
            throw new IllegalStateException("Encountered an error while receiving graphs.");
        }
        List<Graph> graphs = graphHandler.getGraphs();
        if (graphs.isEmpty()) {
            throw new IllegalStateException("Didn't received a single graph.");
        }
        if (domainNames == null) {
            throw new IllegalStateException("Didn't received the domain names from the benchmark controller.");
        }

        // Create the container based on the information that has been received
        container = new CrawleableResourceContainer(new GraphBasedResource(domainId, domainNames,
                graphs.toArray(new Graph[graphs.size()]), (r -> r.getTarget().contains(UriHelper.DATASET_KEY_WORD)
                        && r.getTarget().contains(UriHelper.RESOURCE_NODE_TYPE)),
                new String[] {
                // "application/rdf+xml", "text/plain", "*/*"
                }));
        // Start server
        server = new ContainerServer(container);
        connection = new SocketConnection(server);
        SocketAddress address = new InetSocketAddress(
                EnvVariables.getInt(ApiConstants.ENV_HTTP_PORT_KEY, DEFAULT_PORT, LOGGER));
        connection.connect(address);

        LOGGER.info("HTTP server initialized.");
        // Inform the BC that this node is ready
        sendToCmdQueue(ApiConstants.NODE_READY_SIGNAL);
    }

    protected Model readModel(String modelFile, String modelLang) {
        Model model = ModelFactory.createDefaultModel();
        FileInputStream fin = null;
        try {
            fin = new FileInputStream(modelFile);
            model.read(fin, "", modelLang);
        } catch (Exception e) {
            LOGGER.error("Couldn't read model file. Returning null.", e);
            return null;
        } finally {
            IOUtils.closeQuietly(fin);
        }
        return model;
    }

    @Override
    public void receiveCommand(byte command, byte[] data) {
        switch (command) {
        case Commands.DATA_GENERATION_FINISHED:
            LOGGER.debug("Received DATA_GENERATION_FINISHED");
            dataGenerationFinished.release();
        }
    }

    protected void handleBCMessage(byte[] body) {
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(body))) {
            NodeMetadata[] nodeMetadata = (NodeMetadata[]) ois.readObject();
            domainNames = new String[nodeMetadata.length];
            for (int i = 0; i < nodeMetadata.length; ++i) {
                domainNames[i] = nodeMetadata[i].getHostname();
            }
        } catch (Exception e) {
            LOGGER.error("Couldn't parse node metadata received from benchmark controller.", e);
            domainNames = null;
        }
        // In any case, we should release the semaphore. Otherwise, this component would
        // get stuck and wait forever for an additional message.
        domainNamesReceived.release();
    }

    @Override
    public void run() throws Exception {
        synchronized (this) {
            this.wait();
        }
    }

    @Override
    public void close() throws IOException {
        IOUtils.closeQuietly(connection);
        try {
            if (server != null) {
                server.stop();
            }
        } catch (IOException e) {
            LOGGER.error("Exception while closing server. It will be ignored.", e);
        }
        IOUtils.closeQuietly(receiver);
        if (bcBroadcastChannel != null) {
            try {
                bcBroadcastChannel.close();
            } catch (Exception e) {
            }
        }
        super.close();
    }
}
