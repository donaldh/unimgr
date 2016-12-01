package org.opendaylight.unimgr.mef.notification.api;

import org.opendaylight.controller.messagebus.spi.EventSourceRegistry;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.unimgr.mef.notification.es.example.ExampleEventSource;
import org.opendaylight.unimgr.mef.notification.es.example.ExampleEventSourceGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by root on 24.11.16.
 */
public class EventSourceApiImpl implements EventSourceApi{
    private static final Logger LOG = LoggerFactory.getLogger(ExampleEventSource.class);
    //private final BindingAwareBroker broker;
    private final Broker domBroker;
    private final EventSourceRegistry eventSourceRegistry;

    public EventSourceApiImpl(Broker domBroker, EventSourceRegistry eventSourceRegistry){
        LOG.info("EventSourceApiImpl constructor reached.");
        //this.broker = broker;
        this.domBroker = domBroker;
        this.eventSourceRegistry = eventSourceRegistry;
    }

    @Override
    public void generateExampleEventSource(String nodeName){
        LOG.info(" generateExampleEventSource() has started.");
        ExampleEventSourceGenerator exampleEventSourceGenerator = new ExampleEventSourceGenerator(domBroker);
        exampleEventSourceGenerator.generateExampleEventSource(nodeName,eventSourceRegistry);
        LOG.info(" generateExampleEventSource() has finished.");
    }

    public void startUp(){
        LOG.info("Bundle EventSourceApiImpl has started.");
        this.generateExampleEventSource("EventSource3000");
    }

    public void generateOvsEventSource(){
        //
    }
}
