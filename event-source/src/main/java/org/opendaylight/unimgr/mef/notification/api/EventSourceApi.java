package org.opendaylight.unimgr.mef.notification.api;

/**
 * @author marek.ryznar@amartus.com
 */
public interface EventSourceApi {

    void generateExampleEventSource(String nodeName);
    void generateOvsEventSource(String nodeName);
}
