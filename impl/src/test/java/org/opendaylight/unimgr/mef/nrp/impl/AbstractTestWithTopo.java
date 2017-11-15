/*
 * Copyright (c) 2017 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.nrp.impl;

import static org.opendaylight.unimgr.mef.nrp.api.TapiConstants.PRESTO_SYSTEM_TOPO;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Before;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.unimgr.mef.nrp.api.EndPoint;
import org.opendaylight.unimgr.mef.nrp.common.NrpDao;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapi.common.rev170712.*;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapi.common.rev170712.context.attrs.ServiceInterfacePointBuilder;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapi.connectivity.rev170712.ConnectivityServiceEndPoint;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapi.connectivity.rev170712.create.connectivity.service.input.EndPointBuilder;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapi.topology.rev170712.link.StateBuilder;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapi.topology.rev170712.node.OwnedNodeEdgePointBuilder;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapi.topology.rev170712.topology.Link;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapi.topology.rev170712.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapi.topology.rev170712.topology.LinkKey;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapi.topology.rev170712.topology.Node;

/**
 * @author bartosz.michalik@amartus.com
 */
public abstract class AbstractTestWithTopo extends AbstractDataBrokerTest {


    protected DataBroker dataBroker;

    @Before
    public void setupBroker() throws Exception {
        dataBroker = getDataBroker();
        new NrpInitializer(dataBroker).init();
    }

    protected EndPoint ep(String nepId) {
        ConnectivityServiceEndPoint ep = new EndPointBuilder()
                .setLocalId("ep_" + nepId)
                .setServiceInterfacePoint(new Uuid("sip:" + nepId))
                .build();

        return new EndPoint(ep, null);
    }

    protected void l(ReadWriteTransaction tx, String nA, String nepA, String nB, String nepB, OperationalState state) {
        l(tx, nA, nepA, nB, nepB, state, ForwardingDirection.Bidirectional);
    }

    protected void l(ReadWriteTransaction tx, String nA, String nepA, String nB, String nepB, OperationalState state, ForwardingDirection dir) {
        Uuid uuid = new Uuid(nepA + "-" + nepB);
        Link link = new LinkBuilder()
                .setUuid(uuid)
                .setKey(new LinkKey(uuid))
                .setDirection(dir)
                .setLayerProtocolName(Collections.singletonList(Eth.class))
                .setNode(toIds(nA, nB).collect(Collectors.toList()))
                .setNodeEdgePoint(toIds(nepA, nepB).collect(Collectors.toList()))
                .setState(new StateBuilder().setOperationalState(state).build())
                .build();

        tx.put(LogicalDatastoreType.OPERATIONAL, NrpDao.topo(PRESTO_SYSTEM_TOPO).child(Link.class, new LinkKey(uuid)), link);
    }

    protected Stream<Uuid> toIds(String ... uuids) {
        return toIds(Arrays.stream(uuids));
    }

    protected Stream<Uuid> toIds(Stream<String> uuids) {
        return uuids.map(Uuid::new);
    }

    protected Node n(ReadWriteTransaction tx, boolean addSips, String node, String ... endpoints) {
        return n(tx, addSips, node, Arrays.asList(endpoints).stream().map(i -> new Pair(i, PortDirection.Bidirectional)));
    }

    protected Node n(ReadWriteTransaction tx, boolean addSips, String node, Stream<Pair> endpoints) {
        List<Pair> eps = endpoints.collect(Collectors.toList());
        NrpDao nrpDao = new NrpDao(tx);
        if (addSips) {
            eps.stream().map(e -> new ServiceInterfacePointBuilder()
                .setUuid(new Uuid("sip:" + e))
                .build())
                .forEach(nrpDao::addSip);
        }

        return nrpDao.createSystemNode(node, eps.stream()
                .map(e-> {
                    OwnedNodeEdgePointBuilder builder = new OwnedNodeEdgePointBuilder()
                            .setLinkPortDirection(e.getDir())
                            .setUuid(new Uuid(e.getId()));
                    if (addSips) {
                        builder.setMappedServiceInterfacePoint(Collections.singletonList(new Uuid("sip:" + e)));
                    }
                    return builder.build();
                }).collect(Collectors.toList()));
    }


    static class Pair {
        private String id;
        private PortDirection dir;

        public Pair(String id, PortDirection dir) {
            this.id = id;
            this.dir = dir;
        }

        public String getId() {
            return id;
        }

        public PortDirection getDir() {
            return dir;
        }
    }

    protected Node n(ReadWriteTransaction tx, String node, String ... endpoints) {
        return n(tx,true, node, endpoints);
    }
}
