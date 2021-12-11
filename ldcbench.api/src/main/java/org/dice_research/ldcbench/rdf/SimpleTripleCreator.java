package org.dice_research.ldcbench.rdf;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.ResourceFactory;
import org.dice_research.ldcbench.graph.Graph;

/**
 * A simple {@link TripleCreator} implementation relying on a base graph Id and
 * a list of URI templates of the existing graphs. Node that it makes this class
 * being bound to a single graph.
 *
 * @author Michael R&ouml;der (michael.roeder@uni-paderborn.de)
 *
 */
public class SimpleTripleCreator implements TripleCreator {

    protected int baseGraphId;
    protected String resourceUriTemplates[];
    protected String accessUriTemplates[];

    /**
     * Constructor.
     *
     * @param baseGraphId
     *            the graph Id of nodes that are not external nodes of the graph for
     *            which this triple creator is used.
     * @param resourceUriTemplates
     *            a mapping from graph Ids to resource IRI templates.
     * @param accessUriTemplates
     *            a mapping from graph Ids to access URL templates.
     */
    public SimpleTripleCreator(int baseGraphId, String[] resourceUriTemplates, String[] accessUriTemplates) {
        this.baseGraphId = baseGraphId;
        this.resourceUriTemplates = resourceUriTemplates;
        this.accessUriTemplates = accessUriTemplates;
    }

    @Override
    public Triple createTriple(int sourceId, int propertyId, int targetId, int targetExtId, int targetExtGraphId) {
        return createTriple(sourceId, propertyId, targetId, targetExtId, targetExtGraphId, null);
    }

    @Override
    public Triple createTriple(int sourceId, int propertyId, int targetId, int targetExtId,
            int targetExtGraphId, RDFNodeType targetNodeType) {
        return new Triple(createNode(sourceId, -1, Graph.INTERNAL_NODE_GRAPH_ID, RDFNodeType.IRI),
                createNode(propertyId, -1, Graph.INTERNAL_NODE_GRAPH_ID, RDFNodeType.Property),
                createNode(targetId, targetExtId, targetExtGraphId, targetNodeType));
    }

    /**
     * Create a node of type IRI
     * use this function to create a basic IRI node without having to specify the node type
     * @param nodeId
     * @param externalId
     * @param extGraphId
     * @return
     */
    public Node createNode(int nodeId, int externalId, int extGraphId) {
       return createNode(nodeId, externalId, extGraphId, RDFNodeType.IRI);
    }

    /**
     * Creates a {@link Node} instance based on the given information.
     *
     * @param nodeId
     *            the internal Id of the node
     * @param externalId
     *            the external Id of the node if it belongs to a different graph or
     *            {@code -1} if it is an internal node
     * @param extGraphId
     *            the Id of the graph to which this node belongs to or {@code -1} if
     *            it is an internal node
     * @param isProperty
     *            a flag indicating whether the node is a property
     * @param isBlankNode
     *            a flag indicating whether the node is a blankNode
     * @param isLiteral
     *            a flag indicating whether the node is a literal
     * @return the created {@link Node} instance
     */
    public Node createNode(int nodeId, int externalId, int extGraphId, RDFNodeType nodeType) {
        Node n;
        if (nodeType == RDFNodeType.BlankNode) {
            n = NodeFactory.createBlankNode(String.valueOf(nodeId));
            return n;
        }
        if (nodeType == RDFNodeType.Literal) {
            n = NodeFactory.createLiteral(String.format(UriHelper.LITERAL, nodeId));
            return n;
        }
        String domain;
        if (extGraphId == Graph.INTERNAL_NODE_GRAPH_ID) {
            externalId = nodeId;
            domain = resourceUriTemplates[baseGraphId];
        } else if (extGraphId == -2) {
            externalId = nodeId;
            domain = accessUriTemplates[baseGraphId];
        } else {
            domain = accessUriTemplates[extGraphId];
            // TODO get the datasetId on the other server
        }
        if (nodeType == RDFNodeType.Property) {
            n = ResourceFactory.createProperty(UriHelper.createUri(domain, 0, UriHelper.PROPERTY_NODE_TYPE, externalId))
                    .asNode();
        } else {
            n = ResourceFactory.createResource(UriHelper.createUri(domain, 0, UriHelper.RESOURCE_NODE_TYPE, externalId))
                    .asNode();
        }
        return n;
    }
}
