/*
 * Copyright (c) 2017 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.nrp.common;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.unimgr.mef.nrp.api.TapiConstants;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.Context;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.LayerProtocolName;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.ServiceInterfacePointRef;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.tapi.context.ServiceInterfacePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180307.tapi.context.ServiceInterfacePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.ConnectionEndPointRef;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.OwnedNodeEdgePoint1;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.OwnedNodeEdgePoint1Builder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.cep.list.ConnectionEndPoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.cep.list.ConnectionEndPointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.connection.ConnectionEndPointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.connectivity.context.Connection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.connectivity.context.ConnectionKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.connectivity.context.ConnectivityService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.connectivity.context.ConnectivityServiceKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.Context1;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.OwnedNodeEdgePointRef;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.node.OwnedNodeEdgePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.node.OwnedNodeEdgePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.node.OwnedNodeEdgePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.topology.context.Topology;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.topology.context.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.topology.Node;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev180307.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

/**
 * @author bartosz.michalik@amartus.com
 */
public class NrpDao  {
    private static final Logger LOG = LoggerFactory.getLogger(NrpDao.class);
    private final ReadWriteTransaction tx;
    private final ReadTransaction rtx;


    public NrpDao(ReadWriteTransaction tx) {
        if(tx == null) throw new NullPointerException();
        this.tx = tx;
        this.rtx = tx;
    }
    public NrpDao(ReadOnlyTransaction tx) {
        this.rtx = tx;
        this.tx =  null;
    }

    public Node createNode(String topologyId, String nodeId, LayerProtocolName name, List<OwnedNodeEdgePoint> neps) {
        verifyTx();
        assert tx != null;
        Uuid uuid = new Uuid(nodeId);

        Node node = new NodeBuilder()
                .setKey(new NodeKey(uuid))
                .setUuid(uuid)
                .setLayerProtocolName(Collections.singletonList(name))
                .setOwnedNodeEdgePoint(neps)
                .build();
        tx.put(LogicalDatastoreType.OPERATIONAL, node(new Uuid(topologyId), new Uuid(nodeId)), node);
        return node;
    }

    private void verifyTx() {
        if (tx == null) {
            throw new IllegalStateException("To perform write operation read write transaction is needed");
        }
    }

    /**
     * Update nep or add if it does not exist.
     * @param nodeId node id
     * @param nep nep to update
     */
    public void updateNep(String nodeId, OwnedNodeEdgePoint nep) {
        updateNep(new Uuid(nodeId), nep);
    }

    public void updateNep(Uuid nodeId, OwnedNodeEdgePoint nep) {
        verifyTx();
        InstanceIdentifier<OwnedNodeEdgePoint> nodeIdent = node(nodeId).child(OwnedNodeEdgePoint.class, new OwnedNodeEdgePointKey(nep.getUuid()));
        assert tx != null;
        tx.put(LogicalDatastoreType.OPERATIONAL, nodeIdent, nep);
    }

    public void removeNep(String nodeId, String nepId, boolean removeSips) {
        verifyTx();
        assert tx != null;
        InstanceIdentifier<OwnedNodeEdgePoint> nepIdent = node(nodeId).child(OwnedNodeEdgePoint.class, new OwnedNodeEdgePointKey(new Uuid(nepId)));
        try {
            Optional<OwnedNodeEdgePoint> opt = rtx.read(LogicalDatastoreType.OPERATIONAL, nepIdent).checkedGet();
            if (opt.isPresent()) {
                tx.delete(LogicalDatastoreType.OPERATIONAL,nepIdent);
                if (removeSips) {
                    Stream<Uuid> sips = opt.get().getMappedServiceInterfacePoint().stream().map(ServiceInterfacePointRef::getServiceInterfacePointId);
                    removeSips(sips);
                }
            }
        } catch (ReadFailedException e) {
            LOG.error("Cannot read {} with id {}",OwnedNodeEdgePoint.class, nodeId);
        }
    }

    public void addSip(ServiceInterfacePoint sip) {
        verifyTx();
        assert tx != null;
        tx.put(LogicalDatastoreType.OPERATIONAL,
            ctx().child(ServiceInterfacePoint.class, new ServiceInterfacePointKey(sip.getUuid())),
                sip);
    }

    private Function<OwnedNodeEdgePointRef, KeyedInstanceIdentifier<OwnedNodeEdgePoint, OwnedNodeEdgePointKey>> toPath = ref -> topo(ref.getTopologyId())
            .child(Node.class, new NodeKey(new Uuid(ref.getNodeId())))
            .child(OwnedNodeEdgePoint.class, new OwnedNodeEdgePointKey(ref.getOwnedNodeEdgePointId()));

    public org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.connection.ConnectionEndPoint addConnectionEndPoint(OwnedNodeEdgePointRef ref, ConnectionEndPoint cep) {
        verifyTx();
        assert tx != null;
        OwnedNodeEdgePoint nep = null;
        try {
            nep = readNep(ref);
        } catch (ReadFailedException e) {
            LOG.warn("Error while reading NEP", e);
        }
        if(nep == null) throw new IllegalArgumentException("Cannot find NEP for " + ref);

        OwnedNodeEdgePoint1Builder builder;

        OwnedNodeEdgePoint1 aug = nep.getAugmentation(OwnedNodeEdgePoint1.class);
        if(aug == null) {
            builder = new OwnedNodeEdgePoint1Builder();
        } else {
            builder = new OwnedNodeEdgePoint1Builder(aug);
        }

        List<ConnectionEndPoint> cepList = builder.getConnectionEndPoint();
        if(cepList == null) {
            cepList = new LinkedList<>();
        }

        cepList.add(cep);
        builder.setConnectionEndPoint(cepList);

        nep = new OwnedNodeEdgePointBuilder(nep).addAugmentation(OwnedNodeEdgePoint1.class, builder.build()).build();
        tx.merge(LogicalDatastoreType.OPERATIONAL, toPath.apply(ref), nep);

        return new ConnectionEndPointBuilder(ref).setConnectionEndPointId(cep.getUuid()).build();
    }

    public OwnedNodeEdgePoint readNep(OwnedNodeEdgePointRef ref) throws ReadFailedException {

        KeyedInstanceIdentifier<OwnedNodeEdgePoint, OwnedNodeEdgePointKey> nepKey = toPath.apply(ref);

        return rtx.read(LogicalDatastoreType.OPERATIONAL, nepKey).checkedGet().orNull();
    }

    public OwnedNodeEdgePoint readNep(String nodeId, String nepId) throws ReadFailedException {
        KeyedInstanceIdentifier<OwnedNodeEdgePoint, OwnedNodeEdgePointKey> nepKey = node(nodeId).child(OwnedNodeEdgePoint.class, new OwnedNodeEdgePointKey(new Uuid(nepId)));
        return rtx.read(LogicalDatastoreType.OPERATIONAL, nepKey).checkedGet().orNull();
    }

    public boolean hasSip(String nepId) {
        Uuid universalId = new Uuid("sip:" + nepId);
        try {
            return rtx.read(LogicalDatastoreType.OPERATIONAL,
                    ctx().child(ServiceInterfacePoint.class, new ServiceInterfacePointKey(universalId))).checkedGet().isPresent();
        } catch (ReadFailedException e) {
            LOG.error("Cannot read sip with id {}", universalId.getValue());
        }
        return false;
    }

    public boolean hasNep(String nodeId, String nepId) throws ReadFailedException {
        return readNep(nodeId, nepId) != null;
    }

    public Topology getTopology(String uuid) throws ReadFailedException {
        Optional<Topology> topology = rtx.read(LogicalDatastoreType.OPERATIONAL, topo(uuid)).checkedGet();
        return topology.orNull();
    }

    public Node getNode(String uuidTopo, String uuidNode) throws ReadFailedException {
        Optional<Node> topology = rtx.read(LogicalDatastoreType.OPERATIONAL, node(new Uuid(uuidTopo), new Uuid(uuidNode))).checkedGet();
        return topology.orNull();
    }

    public static InstanceIdentifier<Context> ctx() {
        return InstanceIdentifier.create(Context.class);
    }

    public static InstanceIdentifier<Topology> topo(String topoId) {
        return topo(new Uuid(topoId));
    }

    public static InstanceIdentifier<Topology> topo(Uuid topoId) {
        return ctx()
                .augmentation(Context1.class)
                .child(Topology.class, new TopologyKey(topoId));
    }

    public static InstanceIdentifier<Node> node(String nodeId) {
        return node(new Uuid(nodeId));
    }

    public static InstanceIdentifier<Node> node(Uuid topologyId, Uuid nodeId) {
        return topo(topologyId).child(Node.class, new NodeKey(nodeId));
    }

    public static InstanceIdentifier<Node> node(Uuid nodeId) {
        return node(new Uuid(TapiConstants.PRESTO_SYSTEM_TOPO), nodeId);
    }

    public static InstanceIdentifier<Node> abstractNode() {
        return topo(TapiConstants.PRESTO_EXT_TOPO).child(Node.class, new NodeKey(new Uuid(TapiConstants.PRESTO_ABSTRACT_NODE)));
    }

    public void removeSip(Uuid uuid) {
        removeSips(Stream.of(uuid));
    }

    public void removeSips(Stream<Uuid>  uuids) {
        verifyTx();
        assert tx != null;
        if (uuids == null) {
            return;
        }
        uuids.forEach(sip -> {
            LOG.debug("removing ServiceInterfacePoint with id {}", sip);
            tx.delete(LogicalDatastoreType.OPERATIONAL, ctx().child(ServiceInterfacePoint.class, new ServiceInterfacePointKey(sip)));
        });
    }

    public void removeNode(String nodeId, boolean removeSips) {
        verifyTx();
        if (removeSips) {
            try {
                Optional<Node> opt = rtx.read(LogicalDatastoreType.OPERATIONAL, node(nodeId)).checkedGet();
                if (opt.isPresent()) {
                    List<OwnedNodeEdgePoint> neps = opt.get().getOwnedNodeEdgePoint();
                    if(neps != null)
                    removeSips(neps.stream().flatMap(nep -> nep.getMappedServiceInterfacePoint() == null
                                                                                  ? Stream.empty()
                                                                                  : nep.getMappedServiceInterfacePoint()
                            .stream().map(ServiceInterfacePointRef::getServiceInterfacePointId)
                    ));
                }
            } catch (ReadFailedException e) {
                LOG.error("Cannot read node with id {}", nodeId);
            }
        }
        assert tx != null;
        tx.delete(LogicalDatastoreType.OPERATIONAL, node(nodeId));
    }

    public void updateAbstractNep(OwnedNodeEdgePoint nep) {
        verifyTx();
        assert tx != null;
        InstanceIdentifier<OwnedNodeEdgePoint> nodeIdent = abstractNode().child(OwnedNodeEdgePoint.class, new OwnedNodeEdgePointKey(nep.getUuid()));
        tx.merge(LogicalDatastoreType.OPERATIONAL, nodeIdent, nep);
    }

    public void deleteAbstractNep(OwnedNodeEdgePoint nep) {
        verifyTx();
        assert tx != null;
        InstanceIdentifier<OwnedNodeEdgePoint> nodeIdent = abstractNode().child(OwnedNodeEdgePoint.class, new OwnedNodeEdgePointKey(nep.getUuid()));
        tx.delete(LogicalDatastoreType.OPERATIONAL, nodeIdent);
    }

    public List<ConnectivityService> getConnectivityServiceList() {
        try {
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.Context1 connections = rtx.read(LogicalDatastoreType.OPERATIONAL,
                    ctx().augmentation(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.Context1.class))
                    .checkedGet().orNull();
            return connections == null ? null : connections.getConnectivityService();
        } catch (ReadFailedException e) {
            LOG.warn("reading connectivity services failed", e);
            return null;
        }
    }

    public ConnectivityService getConnectivityService(Uuid id) {
        try {
            return rtx.read(LogicalDatastoreType.OPERATIONAL, ctx().augmentation(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.Context1.class).child(ConnectivityService.class, new ConnectivityServiceKey(id)))
                    .checkedGet().orNull();

        } catch (ReadFailedException e) {
            LOG.warn("reading connectivity service failed", e);
            return null;
        }
    }

    public OwnedNodeEdgePoint getNepByCep(ConnectionEndPointRef ref) {
        KeyedInstanceIdentifier<OwnedNodeEdgePoint, OwnedNodeEdgePointKey> nepPath = node(ref.getTopologyId(), ref.getNodeId()).child(OwnedNodeEdgePoint.class, new OwnedNodeEdgePointKey(ref.getOwnedNodeEdgePointId()));

        try {
            return rtx.read(LogicalDatastoreType.OPERATIONAL, nepPath)
                    .checkedGet().orNull();

        } catch (ReadFailedException e) {
            LOG.warn("reading NEP for ref " +  ref + " failed", e);
            return null;
        }
    }

    public ServiceInterfacePoint getSip(String sipId) throws ReadFailedException {
        KeyedInstanceIdentifier<ServiceInterfacePoint, ServiceInterfacePointKey> key = ctx().child(ServiceInterfacePoint.class, new ServiceInterfacePointKey(new Uuid(sipId)));
        return rtx.read(LogicalDatastoreType.OPERATIONAL, key).checkedGet().orNull();
    }

    public ConnectivityService getConnectivityService(String id) {
        return getConnectivityService(new Uuid(id));
    }

    public Connection getConnection(Uuid connectionId) {
        try {
            return rtx.read(LogicalDatastoreType.OPERATIONAL, ctx().augmentation(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.Context1.class).child(Connection.class, new ConnectionKey(connectionId)))
                    .checkedGet().orNull();

        } catch (ReadFailedException e) {
            LOG.warn("reading connectivity service failed", e);
            return null;
        }
    }

    public void removeConnection(Uuid connectionId) {
        Objects.requireNonNull(connectionId);
        verifyTx();
        assert tx != null;
        Connection connection = getConnection(connectionId);
        if(connection == null) {
            return;
        }

        for (org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.connection.ConnectionEndPoint cepRef : connection.getConnectionEndPoint()) {
            KeyedInstanceIdentifier<ConnectionEndPoint, ConnectionEndPointKey> cepKey = node(cepRef.getTopologyId(), cepRef.getNodeId())
                    .child(OwnedNodeEdgePoint.class, new OwnedNodeEdgePointKey(cepRef.getOwnedNodeEdgePointId()))
                    .augmentation(OwnedNodeEdgePoint1.class).child(ConnectionEndPoint.class, new ConnectionEndPointKey(cepRef.getConnectionEndPointId()));
            tx.delete(LogicalDatastoreType.OPERATIONAL,cepKey);
        }
        LOG.debug("removing connection {}", connectionId.getValue());
        tx.delete(LogicalDatastoreType.OPERATIONAL, ctx().augmentation(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.Context1.class)
                .child(Connection.class, new ConnectionKey(connectionId)));
    }
}
