package org.opendaylight.unimgr.mef.notification.es.example;

import org.opendaylight.controller.md.sal.dom.api.DOMNotificationPublishService;
import org.opendaylight.controller.messagebus.spi.EventSourceRegistry;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.unimgr.mef.notification.impl.ExampleEventSourceBIProvider;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;

/**
 * Created by root on 24.11.16.
 */
public class ExampleEventSourceGenerator {

    private final DOMNotificationPublishService domPublish;

    public ExampleEventSourceGenerator(Broker domBroker){
        ExampleEventSourceBIProvider providerBI = new ExampleEventSourceBIProvider();
        Broker.ProviderSession domCtx = domBroker.registerProvider(providerBI);
        this.domPublish = domCtx.getService(DOMNotificationPublishService.class);
    }

    public ExampleEventSource generateExampleEventSource(String nodeName,EventSourceRegistry eventSourceRegistry){
        Node node = getNewNode(nodeName);
        ExampleEventSource exampleEventSource = new ExampleEventSource(domPublish,node);
        eventSourceRegistry.registerEventSource(exampleEventSource);
        return exampleEventSource;

    }

    private Node getNewNode(String nodeIdent){
        NodeId nodeId = new NodeId(nodeIdent);
        NodeBuilder nb = new NodeBuilder();
        nb.setKey(new NodeKey(nodeId));
        return nb.build();
    }
}
