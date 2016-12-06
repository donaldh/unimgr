package org.opendaylight.unimgr.mef.notification.es.example;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationPublishService;
import org.opendaylight.controller.messagebus.spi.EventSource;
import org.opendaylight.unimgr.mef.notification.impl.TopicDOMNotification;
import org.opendaylight.unimgr.mef.notification.impl.Util;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventaggregator.rev141202.NotificationPattern;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventaggregator.rev141202.TopicId;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventaggregator.rev141202.TopicNotification;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.eventsource.api.rev150408.ExampleEventSourceNotification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.eventsource.api.rev150408.ExampleEventSourceNotificationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.eventsource.api.rev150408.SourceIdentifier;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.AnyXmlNode;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMSource;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static com.google.common.util.concurrent.Futures.immediateFuture;

/**
 * @author marek.ryznar@amartus.com
 */
public class ExampleEventSource implements EventSource {
    private static final Logger LOG = LoggerFactory.getLogger(ExampleEventSource.class);
    public static final QName sample_notification_QNAME = QName.create("urn:cisco:params:xml:ns:yang:messagebus:sample","2015-03-16","example-notification").intern();
    public static final String XMLNS_ATTRIBUTE_KEY = "xmlns";
    public static final String XMLNS_URI = "http://www.w3.org/2000/xmlns/";

    private static final YangInstanceIdentifier.NodeIdentifier TOPIC_NOTIFICATION_ARG = new YangInstanceIdentifier.NodeIdentifier(TopicNotification.QNAME);
    private static final YangInstanceIdentifier.NodeIdentifier EVENT_SOURCE_ARG = new YangInstanceIdentifier.NodeIdentifier(QName.create(TopicNotification.QNAME, "node-id").intern());
    private static final YangInstanceIdentifier.NodeIdentifier TOPIC_ID_ARG = new YangInstanceIdentifier.NodeIdentifier(QName.create(TopicNotification.QNAME, "topic-id").intern());
    private static final YangInstanceIdentifier.NodeIdentifier PAYLOAD_ARG = new YangInstanceIdentifier.NodeIdentifier(QName.create(TopicNotification.QNAME, "payload").intern());

    private final Short messageGeneratePeriod = 2;
    private final ScheduledExecutorService scheduler;
    private final DOMNotificationPublishService domPublish;
    private final List<SchemaPath> listSchemaPaths = new ArrayList<>();
    private final List<TopicId> listAcceptedTopics = new ArrayList<>();

    private final Node sourceNode;
    private final String messageText = "Example text message!";

    public ExampleEventSource(DOMNotificationPublishService domPublish, Node node){
        LOG.info("ExampleEventSource constructor started.");
        sourceNode = node;
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.domPublish = domPublish;

        setAvailableNotifications();

        startMessageGenerator();
        LOG.info("ExampleEventSource constructor finished.");
    }

    private void startMessageGenerator(){
        // message generator is started as scheduled task
        scheduler.scheduleAtFixedRate(new MessageGenerator(sourceNode.getNodeId().getValue(), this.messageText), messageGeneratePeriod, messageGeneratePeriod, TimeUnit.SECONDS);
    }

    /*
     * This method internally set list of SchemaPath(s) that represents all types of notification that event source can produce.
     * In actual implementation event source can set this list same way as this example code or it can obtain it from other sources
     * (e.g. configuration parameters, device capabilities etc.)
     */
    private void setAvailableNotifications(){
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(0);
        cal.set(2015, 4, 8, 0, 0, 0);
        Date revisionDate = cal.getTime();

        URI uriSample = null;
        URI uriTest = null;
        URI uriOVS = null;
        try {
            uriSample = new URI("urn:opendaylight:unimgr:mef:notification:es:example:notification");
            uriTest = new URI("urn:opendaylight:unimgr:mef:notification:es:test:notification");
            uriOVS = new URI("ovs");
        } catch (URISyntaxException e) {
            throw new RuntimeException("Bad URI for notification", e);
        }

        QName qnSample = QName.create(uriSample,revisionDate,"example-message");
        QName qnTest = QName.create(uriTest,revisionDate,"example-message");
        QName qnOvs = QName.create(uriOVS,revisionDate,"example-message");

        SchemaPath spSample = SchemaPath.create(true, qnSample);
        SchemaPath spTest = SchemaPath.create(true, qnTest);
        SchemaPath spOvs = SchemaPath.create(true, qnOvs);

        listSchemaPaths.add(spSample);
        listSchemaPaths.add(spTest);
        listSchemaPaths.add(spOvs);
    }

    @Override
    public NodeKey getSourceNodeKey() {
        return sourceNode.getKey();
    }

    @Override
    public List<SchemaPath> getAvailableNotifications() {
        LOG.info("getAvailableNotifications: {}",this.listSchemaPaths.toString());
        return Collections.unmodifiableList(this.listSchemaPaths);
    }

    @Override
    public void close() throws Exception {
        this.scheduler.shutdown();
    }

    @Override
    public Future<RpcResult<JoinTopicOutput>> joinTopic(JoinTopicInput joinTopicInput) {
        LOG.info("Start join Topic {} {}",getSourceNodeKey().getNodeId().getValue(), joinTopicInput.getTopicId().getValue());
        final NotificationPattern notificationPattern = joinTopicInput.getNotificationPattern();
        // obtaining list of SchamePath of notifications which match with notification pattern
        final List<SchemaPath> matchingNotifications = getMatchingNotifications(notificationPattern);
        LOG.info("JoinTopic matching notifications: {}",matchingNotifications.toString());
        JoinTopicStatus joinTopicStatus = JoinTopicStatus.Down;
        if(Util.isNullOrEmpty(matchingNotifications) == false){
            // if there is at least one SchemaPath matched with NotificationPattern then topic is add into the list
            LOG.info("Node {} Join topic {}", sourceNode.getNodeId().getValue(), joinTopicInput.getTopicId().getValue());
            listAcceptedTopics.add(joinTopicInput.getTopicId());
            joinTopicStatus = JoinTopicStatus.Up;
        }
        final JoinTopicOutput output = new JoinTopicOutputBuilder().setStatus(joinTopicStatus).build();
        return immediateFuture(RpcResultBuilder.success(output).build());
    }

    @Override
    public Future<RpcResult<Void>> disJoinTopic(DisJoinTopicInput disJoinTopicInput) {
        listAcceptedTopics.remove(disJoinTopicInput.getTopicId());
        return immediateFuture(RpcResultBuilder.success((Void) null).build());
    }

    /*
 * This private class is responsible to generate messages in given interval and publish notification if an topic has been joined
 * Text of message is composed by constructor parameter String messageText and time. Time is added to simulate
 * changes of message content.
 */
    private class MessageGenerator implements Runnable {

        private final String messageText;
        private final String eventSourceIdent;

        public MessageGenerator(String EventSourceIdent, String messageText) {
            this.messageText = messageText;
            this.eventSourceIdent = EventSourceIdent;
            LOG.info("MessageGenerator constructor with values: {} {}",messageText, eventSourceIdent);
        }

        /*
         * Method is run periodically (see method startMessageGenerator in parent class)
         * Create messages and publish notification
         * @see java.lang.Runnable#run()
         */
        // Eventually here, this thread will read state (by registered listener) of the device (ie OVS or cisco xr)
        @Override
        public void run() {
            // message is generated every run of method
            String message = this.messageText + " [" + Calendar.getInstance().getTime().toString() +"]";
            LOG.info("MessageGenerator.run: {} {}",messageText, message);
            LOG.info("MessageGenerator.run acceptedTopics: {} ",listAcceptedTopics.toString());
            LOG.debug("Sample message generated: {}",message);

            for(TopicId jointTopic : listAcceptedTopics){
                // notification is published for each accepted topic
                // if there is no accepted topic, no notification will publish

                // notification is created by builder and contain identification of eventSource and text of message
                // SampleEventSourceNotification has been defined for this example purposes only
                // Actual implementation should define own / suitable notification
                ExampleEventSourceNotificationBuilder builder = new ExampleEventSourceNotificationBuilder();
                builder.setMessage(message);
                builder.setSourceId(new SourceIdentifier(this.eventSourceIdent));
                ExampleEventSourceNotification notification = builder.build();

                final String topicId = jointTopic.getValue();

                // notification is encapsulated into TopicDOMNotification and publish via DOMNotificationPublisherService
                TopicDOMNotification topicNotification = createNotification(notification,this.eventSourceIdent,topicId);

                ListenableFuture<? extends Object> notifFuture;
                try {
                    notifFuture = domPublish.putNotification(topicNotification);
                    Futures.addCallback(notifFuture, new FutureCallback<Object>(){

                        @Override
                        public void onSuccess(Object result) {
                            LOG.info("Sample message published for topic [TopicId: {}]",topicId);
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            LOG.error("Sample message has not published for topic [TopicId: {}], Exception: {}",topicId,t);
                        }
                    });
                } catch (InterruptedException e) {
                    LOG.error("Sample message has not published for topic [TopicId: {}], Exception: {}",topicId,e);
                }

            }
        }

        /*
         * Method encapsulates specific SampleEventSourceNotification into TopicDOMNotification
         * TopicDOMNotification carries next informations
         *   - TopicId
         *   - identifier of event source
         *   - SampleEventSourceNotification encapsulated into XML form (see AnyXmlNode encapsulate(...))
         */
        private TopicDOMNotification createNotification(ExampleEventSourceNotification notification, String eventSourceIdent, String topicId){

            final ContainerNode topicNotification = Builders.containerBuilder()
                    .withNodeIdentifier(TOPIC_NOTIFICATION_ARG)
                    .withChild(ImmutableNodes.leafNode(TOPIC_ID_ARG, new TopicId(topicId)))
                    .withChild(ImmutableNodes.leafNode(EVENT_SOURCE_ARG, eventSourceIdent))
                    .withChild(encapsulate(notification))
                    .build();
            return new TopicDOMNotification(topicNotification);

        }

        /*
         * Result of this method is encapsulated SampleEventSourceNotification into AnyXMLNode
         * SampleEventSourceNotification is XML fragment in output
         */
        private AnyXmlNode encapsulate(ExampleEventSourceNotification notification){

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder;

            try {
                docBuilder = docFactory.newDocumentBuilder();
            } catch (ParserConfigurationException e) {
                throw new IllegalStateException("Can not create XML DocumentBuilder");
            }

            Document doc = docBuilder.newDocument();

            final Optional<String> namespace = Optional.of(PAYLOAD_ARG.getNodeType().getNamespace().toString());
            final Element rootElement = createElement(doc , "payload", namespace);

            final Element notifElement = doc.createElement("SampleEventSourceNotification");
            rootElement.appendChild(notifElement);

            final Element sourceElement = doc.createElement("Source");
            sourceElement.appendChild(doc.createTextNode(notification.getSourceId().getValue()));
            notifElement.appendChild(sourceElement);

            final Element messageElement = doc.createElement("Message");
            messageElement.appendChild(doc.createTextNode(notification.getMessage()));
            notifElement.appendChild(messageElement);

            return Builders.anyXmlBuilder().withNodeIdentifier(PAYLOAD_ARG)
                    .withValue(new DOMSource(rootElement))
                    .build();

        }

        // Helper to create root XML element with correct namespace and attribute
        private Element createElement(final Document document, final String qName, final Optional<String> namespaceURI) {
            if(namespaceURI.isPresent()) {
                final Element element = document.createElementNS(namespaceURI.get(), qName);
                String name = XMLNS_ATTRIBUTE_KEY;
                if(element.getPrefix() != null) {
                    name += ":" + element.getPrefix();
                }
                element.setAttributeNS(XMLNS_URI, name, namespaceURI.get());
                return element;
            }
            return document.createElement(qName);
        }
    }

    /*
 * Method return list of SchemaPath matched by notificationPattern
 */
    private List<SchemaPath> getMatchingNotifications(NotificationPattern notificationPattern){
        // wildcard notification pattern is converted into regex pattern
        // notification pattern could be changed into regex syntax in the future
        LOG.info("getMatchingNotifications notification: {}",notificationPattern.getValue());
        final String regex = Util.wildcardToRegex(notificationPattern.getValue());
        LOG.info("getMatchingNotifications regex: {}",regex);
        final Pattern pattern = Pattern.compile(regex);

        return Util.selectSchemaPath(getAvailableNotifications(), pattern);
    }

}
