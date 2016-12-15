package org.opendaylight.unimgr.mef.notification.api;

import org.opendaylight.controller.messagebus.spi.EventSource;
import org.opendaylight.unimgr.mef.notification.es.example.ExampleEventSource;

/**
 * @author marek.ryznar@amartus.com
 */
public interface EventSourceApi {

    ExampleEventSource generateExampleEventSource(String nodeName);
    void generateOvsEventSource(String nodeName);
    void createTopicToEventSource(String nodeName);
    void deleteEventSource(String nodeName);
    void deleteEventSource(EventSource eventSource);
    void destroyEventSourceTopics(String nodeName);
    void destroyTopic(String topicId);
}
