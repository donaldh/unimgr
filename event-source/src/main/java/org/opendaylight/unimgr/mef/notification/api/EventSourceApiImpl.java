package org.opendaylight.unimgr.mef.notification.api;

import org.opendaylight.controller.messagebus.spi.EventSource;
import org.opendaylight.controller.messagebus.spi.EventSourceRegistry;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.unimgr.mef.notification.es.example.ExampleEventSource;
import org.opendaylight.unimgr.mef.notification.es.example.ExampleEventSourceGenerator;
import org.opendaylight.unimgr.mef.notification.impl.Providers;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventaggregator.rev141202.*;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * @author marek.ryznar@amartus.com
 */
public class EventSourceApiImpl implements EventSourceApi{
    private static final Logger LOG = LoggerFactory.getLogger(ExampleEventSource.class);
    private final Broker domBroker;
    private final EventSourceRegistry eventSourceRegistry;
    private final BindingAwareBroker broker;
    private EventAggregatorService eventAggregatorService;
    private List<EventSource> eventSourceList;

    public EventSourceApiImpl(Broker domBroker, EventSourceRegistry eventSourceRegistry,BindingAwareBroker broker) { //EventAggregatorService eventAggregatorService){
        LOG.info("EventSourceApiImpl constructor reached.");
        this.domBroker = domBroker;
        this.eventSourceRegistry = eventSourceRegistry;
        this.broker = broker;
        initEventAggregatorService();
    }

    @Override
    public void generateExampleEventSource(String nodeName){
        LOG.info(" generateExampleEventSource() has started.");
        ExampleEventSourceGenerator exampleEventSourceGenerator = new ExampleEventSourceGenerator(domBroker);
        ExampleEventSource exampleEventSource = exampleEventSourceGenerator.generateExampleEventSource(nodeName,eventSourceRegistry);
        eventSourceList.add(exampleEventSource);
        addTopicForNotification(exampleEventSource,nodeName);
        LOG.info(" generateExampleEventSource() has finished.");
    }

    @Override
    public void generateOvsEventSource(String nodeName) {
        //TODO: implement
    }

    /**
     * Method to initialize EventAggregatorService needed to create and destroy topics.
     */
    public void initEventAggregatorService(){
        final BindingAwareBroker.ProviderContext bindingCtx = broker.registerProvider(new Providers.BindingAware());
        final RpcProviderRegistry rpcRegistry = bindingCtx.getSALService(RpcProviderRegistry.class);
        eventAggregatorService = rpcRegistry.getRpcService(EventAggregatorService.class);
    }

    /**
     * Method create topic corresponding to event source (created for given nodeName).
     *
     * @param nodeName Name of the node included in EventSource.
     */
    public void createTopicToEventSource(String nodeName){
        List<EventSource> resultList = eventSourceList.stream()
                .filter(event -> nodeName.equals(event.getSourceNodeKey().getNodeId().getValue()))
                .collect(Collectors.toList());

        resultList.stream()
                .forEach(eventSource -> addTopicForNotification(eventSource,nodeName));
    }

    private CreateTopicInput createTopicInput(String nodeName, String notificationPatternName){
        CreateTopicInputBuilder createTopicInputBuilder = new CreateTopicInputBuilder();
        Pattern pattern = new Pattern(nodeName);
        createTopicInputBuilder.setNodeIdPattern(pattern);
        NotificationPattern notificationPattern = new NotificationPattern(notificationPatternName);
        createTopicInputBuilder.setNotificationPattern(notificationPattern);
        return createTopicInputBuilder.build();
    }

    private void addTopicForNotification(EventSource eventSource,String nodeName){
        LOG.info("addTopicForNotification");
        List<SchemaPath> notifications = eventSource.getAvailableNotifications();
        String notificationPattern;
        for(SchemaPath notification:notifications){
            LOG.info("addTopicForNotification for loop");
            notificationPattern = notification.getLastComponent().getNamespace().toString();
            CreateTopicInput topicInput = createTopicInput(nodeName,notificationPattern);
            Future<RpcResult<CreateTopicOutput>> topicOutput = eventAggregatorService.createTopic(topicInput);
            try {
                TopicId topicId = topicOutput.get().getResult().getTopicId();
                LOG.info("Topic for node: {} and notification pattern: {} created with id: {}",nodeName,notificationPattern,topicId.getValue());
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    public void startUp(){
        LOG.info("Bundle EventSourceApiImpl has started.");
        eventSourceList = Collections.synchronizedList(new ArrayList<EventSource>());
        this.generateExampleEventSource("EventSource3000");
    }

    public List<EventSource> getEventSourceList() {
        return eventSourceList;
    }
}
