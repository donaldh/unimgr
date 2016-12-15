package org.opendaylight.unimgr.mef.notification.api;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationPublishService;
import org.opendaylight.controller.messagebus.spi.EventSource;
import org.opendaylight.controller.messagebus.spi.EventSourceRegistry;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.unimgr.mef.notification.es.example.ExampleEventSource;
import org.opendaylight.unimgr.mef.notification.impl.Util;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventaggregator.rev141202.*;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.JoinTopicInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.JoinTopicInputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Future;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * @author marek.ryznar@amartus.com
 */
@RunWith(PowerMockRunner.class)
public class EventSourceApiImplTest {
    private EventSourceApiImpl eventSourceApi;
    private Broker brokerMock;
    private EventSourceRegistry eventSourceRegistry;
    private BindingAwareBroker bindingAwareBrokerMock;
    private EventAggregatorService eventAggregatorServiceMock;
    private DOMNotificationPublishService domPublishMock;
    private RpcProviderRegistry rpcProviderRegistryMock;
    private static final String nodeName = "testNodeName";

    @Before
    public void setUp(){
        //given
        brokerMock = mock(Broker.class);
        eventSourceRegistry = mock(EventSourceRegistry.class);
        bindingAwareBrokerMock = mock(BindingAwareBroker.class);

        BindingAwareBroker.ProviderContext bindingCtxMock = mock(BindingAwareBroker.ProviderContext.class);
        rpcProviderRegistryMock = mock(RpcProviderRegistry.class);
        eventAggregatorServiceMock = mock(EventAggregatorService.class);
        Broker.ProviderSession domCtxMock = mock(Broker.ProviderSession.class);
        domPublishMock = mock(DOMNotificationPublishService.class);

        when(rpcProviderRegistryMock.getRpcService(EventAggregatorService.class)).thenReturn(eventAggregatorServiceMock);
        when(bindingCtxMock.getSALService(RpcProviderRegistry.class)).thenReturn(rpcProviderRegistryMock);
        when(bindingAwareBrokerMock.registerProvider(any())).thenReturn(bindingCtxMock);
        when(domCtxMock.getService(DOMNotificationPublishService.class)).thenReturn(domPublishMock);
        when(brokerMock.registerProvider(any())).thenReturn(domCtxMock);

        eventSourceApi = new EventSourceApiImpl(brokerMock, eventSourceRegistry,bindingAwareBrokerMock);
    }

    @Test
    public void testGenerateAndDeleteExampleEventSource(){
        //Test generate:
        //when
        ExampleEventSource exampleEventSource = eventSourceApi.generateExampleEventSource(nodeName);

        //then
        List<EventSource> eventSources = eventSourceApi.getEventSourceList();
        assertEquals(1,eventSources.size());
        checkExampleEventSource((ExampleEventSource) eventSources.get(0),nodeName);

        //Test delete:
        //given
        eventSourceApi.deleteEventSource(exampleEventSource);

        //then
        assertFalse(eventSourceApi.getEventSourceList().contains(exampleEventSource));
    }

    @Test
    public void testCreateAndDestroyTopicToEventSource(){
        //given
        ExampleEventSource exampleEventSource = eventSourceApi.generateExampleEventSource(nodeName);
        when(eventAggregatorServiceMock.createTopic(any()))
                .thenReturn(createTopicMock())
                .thenReturn(createTopicMock())
                .thenReturn(createTopicMock());

        //Test create:
        //when
        eventSourceApi.createTopicToEventSource(nodeName);
        joinTopicsToEventSource(exampleEventSource);
        try {
            //sleep because notification is send every 2 seconds since join the topic (if topic was joined)
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //then
        Map<TopicId, SchemaPath> topics = eventSourceApi.getTopics();
        assertEquals(3,topics.size());
        exampleEventSource.getAvailableNotifications().stream()
                .forEach(schemaPath -> assertTrue(topics.values().contains(schemaPath)));
        try {
            verify(domPublishMock).putNotification(any());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //Test destroy:
        //when
        eventSourceApi.destroyEventSourceTopics(exampleEventSource.getSourceNodeKey().getNodeId().getValue());

        //then
        assertEquals(0,eventSourceApi.getTopics().size());
        assertFalse(eventSourceApi.getEventSourceList().contains(exampleEventSource));
    }

    /**
     * Method made to call {@link org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.EventSourceService#joinTopic(JoinTopicInput)} manually.
     * In normal situation it is called automatically when topic is created, but in test @see org.opendaylight.controller.md.sal.dom.api.DOMNotificationPublishService is not mocked enough.
     *
     * @param eventSource
     */
    private void joinTopicsToEventSource(EventSource eventSource){
        eventSourceApi.getTopics().entrySet()
                .stream()
                .filter(topic -> eventSource.getAvailableNotifications().contains(topic.getValue()))
                .forEach(topic -> eventSource.joinTopic(createJoinTopicInput(topic)));
    }

    private JoinTopicInput createJoinTopicInput(Map.Entry<TopicId, SchemaPath> topic){
        JoinTopicInputBuilder joinTopicInputBuilder = new JoinTopicInputBuilder();
        joinTopicInputBuilder.setTopicId(topic.getKey());

        NotificationPattern notificationPattern = new NotificationPattern(eventSourceApi.getSchemaPathString(topic.getValue()));
        joinTopicInputBuilder.setNotificationPattern(notificationPattern);

        return joinTopicInputBuilder.build();
    }

    private Future<RpcResult<CreateTopicOutput>> createTopicMock(){
        CreateTopicOutputBuilder createTopicOutputBuilder = new CreateTopicOutputBuilder();
        Random generator = new Random();
        String id = Integer.toString(generator.nextInt(Integer.SIZE - 1));
        while (checkUniqueness(id)){
            id = Integer.toString(generator.nextInt(Integer.SIZE - 1));
        }
        TopicId topicId = new TopicId(id);
        createTopicOutputBuilder.setTopicId(topicId);
        final CreateTopicOutput cto = createTopicOutputBuilder.build();
        Future<RpcResult<CreateTopicOutput>> result = Util.resultRpcSuccessFor(cto);
        return result;
    }

    private boolean checkUniqueness(String nodeName){
        return eventSourceApi.getTopics().keySet().stream()
                .anyMatch(topicId -> topicId.getValue().equals(nodeName));
    }

    private void checkExampleEventSource(ExampleEventSource exampleEventSource,String nodeName){
        NodeKey nodeKey = exampleEventSource.getSourceNodeKey();
        assertEquals(nodeName,nodeKey.getNodeId().getValue());

        List<SchemaPath> notifications = exampleEventSource.getAvailableNotifications();
        checkNotifications(notifications);
    }

    private void checkNotifications(List<SchemaPath> notifications){
        assertEquals("urn:opendaylight:unimgr:mef:notification:es:example:notification",eventSourceApi.getSchemaPathString(notifications.get(0)));
        assertEquals("urn:opendaylight:unimgr:mef:notification:es:test:notification",eventSourceApi.getSchemaPathString(notifications.get(1)));
        assertEquals("ovs",eventSourceApi.getSchemaPathString(notifications.get(2)));
    }
}