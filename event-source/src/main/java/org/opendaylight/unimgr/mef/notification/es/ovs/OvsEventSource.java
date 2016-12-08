package org.opendaylight.unimgr.mef.notification.es.ovs;

import org.opendaylight.controller.messagebus.spi.EventSource;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.DisJoinTopicInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.JoinTopicInput;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.messagebus.eventsource.rev141202.JoinTopicOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

import java.util.List;
import java.util.concurrent.Future;

/**
 * Created by root on 23.11.16.
 */
public class OvsEventSource implements EventSource {

    @Override
    public NodeKey getSourceNodeKey() {
        return null;
    }

    @Override
    public List<SchemaPath> getAvailableNotifications() {
        return null;
    }

    @Override
    public void close() throws Exception {

    }

    @Override
    public Future<RpcResult<JoinTopicOutput>> joinTopic(JoinTopicInput joinTopicInput) {
        return null;
    }

    @Override
    public Future<RpcResult<Void>> disJoinTopic(DisJoinTopicInput disJoinTopicInput) {
        return null;
    }
}
