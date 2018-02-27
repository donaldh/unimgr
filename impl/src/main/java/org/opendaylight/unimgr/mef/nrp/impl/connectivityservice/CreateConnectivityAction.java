/*
 * Copyright (c) 2017 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.nrp.impl.connectivityservice;

import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.unimgr.mef.nrp.api.ActivationDriver;
import org.opendaylight.unimgr.mef.nrp.api.EndPoint;
import org.opendaylight.unimgr.mef.nrp.api.FailureResult;
import org.opendaylight.unimgr.mef.nrp.api.RequestValidator;
import org.opendaylight.unimgr.mef.nrp.api.Subrequrest;
import org.opendaylight.unimgr.mef.nrp.api.TapiConstants;
import org.opendaylight.unimgr.mef.nrp.common.NrpDao;
import org.opendaylight.unimgr.mef.nrp.impl.ActivationTransaction;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev171221.EndPoint2;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev180216.*;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180216.*;
//import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180216.connection.ConnectionEndPoint;
//import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180216.connection.ConnectionEndPointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180216.cep.list.ConnectionEndPoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180216.cep.list.ConnectionEndPointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180216.connection.RouteBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180216.connectivity.context.Connection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180216.connectivity.context.ConnectionBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180216.connectivity.context.ConnectionKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180216.connectivity.context.ConnectivityService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180216.connectivity.context.ConnectivityServiceBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180216.connectivity.context.ConnectivityServiceKey;
//import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180216.connectivity.service.ConnConstraint;
//import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180216.connectivity.service.ConnConstraintBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180216.connectivity.service.EndPointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180216.create.connectivity.service.output.ServiceBuilder;
import org.opendaylight.yangtools.yang.common.OperationFailedException;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author bartosz.michalik@amartus.com
 */
class CreateConnectivityAction implements Callable<RpcResult<CreateConnectivityServiceOutput>> {
    private static final Logger LOG = LoggerFactory.getLogger(CreateConnectivityAction.class);

    private TapiConnectivityServiceImpl service;
    private final CreateConnectivityServiceInput input;
    private List<Subrequrest> decomposedRequest;
    private List<EndPoint> endpoints;

    public CreateConnectivityAction(TapiConnectivityServiceImpl tapiConnectivityService, CreateConnectivityServiceInput input) {
        Objects.requireNonNull(tapiConnectivityService);
        Objects.requireNonNull(input);
        this.service = tapiConnectivityService;
        this.input = input;
    }

    @Override
    public RpcResult<CreateConnectivityServiceOutput> call() throws Exception {
        LOG.debug("running CreateConnectivityService task");

        try {
            RequestValidator.ValidationResult validationResult = validateInput();
            if (!validationResult.isValid()) {
                RpcResultBuilder<CreateConnectivityServiceOutput> res = RpcResultBuilder.failed();
                validationResult.getProblems().forEach(p -> res.withError(RpcError.ErrorType.APPLICATION, p));
                return res.build();

            }

            endpoints = input.getEndPoint().stream().map(ep -> {
                EndPoint2 nrpAttributes = ep.getAugmentation(EndPoint2.class);
                return new EndPoint(ep, nrpAttributes);
            }).collect(Collectors.toList());

            String uniqueStamp = service.getServiceIdPool().getServiceId();

            ActivationTransaction tx = prepareTransaction(toCsId(uniqueStamp));
            if (tx != null) {
                ActivationTransaction.Result txResult = tx.activate();
                if (txResult.isSuccessful()) {
                    LOG.info("ConnectivityService construct activated successfully, request = {} ", input);

                    // XXX [bm] when createConnectivityModel methods throws an exception we have desync
                    // (devices are configured but no data stored in MD-SAL. How should we address that?
                    ConnectivityService service = createConnectivityModel(uniqueStamp);
                    CreateConnectivityServiceOutput result = new CreateConnectivityServiceOutputBuilder()
                            .setService(new ServiceBuilder(service).build()).build();
                    return RpcResultBuilder.success(result).build();
                } else {
                    LOG.warn("CreateConnectivityService failed, reason = {}, request = {}", txResult.getMessage(), input);
                }
            }
            throw new IllegalStateException("no transaction created for create connectivity request");


        } catch (Exception e) {
            LOG.warn("Exception in create connectivity service", e);
            return RpcResultBuilder
                    .<CreateConnectivityServiceOutput>failed()
                    .withError(ErrorType.APPLICATION, e.getMessage())
                    .build();
        }
    }

    private ActivationTransaction prepareTransaction(String serviceId) throws FailureResult {
        LOG.debug("decompose request");
        decomposedRequest = service.getDecomposer().decompose(endpoints, null);

        ActivationTransaction tx = new ActivationTransaction();

        decomposedRequest.stream().map(s -> {
            Optional<ActivationDriver> driver = service.getDriverRepo().getDriver(s.getNodeUuid());
            if (!driver.isPresent()) {
                throw new IllegalStateException(MessageFormat.format("driver {} cannot be created", s.getNodeUuid()));
            }
            driver.get().initialize(s.getEndpoints(), serviceId, null);
            LOG.debug("driver {} added to activation transaction", driver.get());
            return driver.get();
        }).forEach(tx::addDriver);

        return tx;
    }

    private RequestValidator.ValidationResult validateInput() {
        return service.getValidator().checkValid(input);
    }

    private String toCsId(String uniqueStamp) {
        return "cs:" + uniqueStamp;
    }

    private class ConnectionData {
        final Uuid sysNodeUuid;
        final Map<Uuid, ConnectionEndPoint> nepToCep;

        private ConnectionData(Uuid sysNodeUuid, Map<Uuid, ConnectionEndPoint> nepToCep) {
            this.sysNodeUuid = sysNodeUuid;
            this.nepToCep = nepToCep;
        }
    }

    private ConnectivityService createConnectivityModel(String uniqueStamp) throws OperationFailedException, TimeoutException {
        assert decomposedRequest != null : "this method can be only run after request was successfuly decomposed";
        //sort of unique ;)

        LOG.debug("Preparing connectivity related model for {}", uniqueStamp);

        final ReadWriteTransaction tx = service.getBroker().newReadWriteTransaction();

//        final NrpDao nrpDao = new NrpDao(tx);

        List<ConnectionData> systemConnectionsData = decomposedRequest.stream().map(s -> {
            final LayerProtocolName layerProtocolName = s.getEndpoints().stream().filter(e -> e.getEndpoint() != null && e.getEndpoint().getLayerProtocolName() != null)
                    .map(e -> e.getEndpoint().getLayerProtocolName()).findFirst().orElse(null);


            Map<Uuid, ConnectionEndPoint> ceps = s.getEndpoints().stream().map(ep -> {
                String cepUuid = Integer.toString(Objects.hash("cep", ep.getSystemNepUuid(), s.getNodeUuid(), uniqueStamp), 16);
                ConnectivityServiceEndPoint sep = ep.getEndpoint();

                ConnectionEndPointBuilder cepB = new ConnectionEndPointBuilder();
                cepB.setUuid(new Uuid(cepUuid))
                        .setLayerProtocolName(layerProtocolName);
                if (sep != null && sep.getServiceInterfacePoint() != null) {
                    cepB.setConnectivityServiceEndPoint(sep.getServiceInterfacePoint().getValue());
                    //set others as well

                }
                return new Map.Entry<Uuid, ConnectionEndPoint>() {

                    @Override
                    public Uuid getKey() {
                        return ep.getSystemNepUuid();
                    }

                    @Override
                    public ConnectionEndPoint getValue() {
                        return cepB.build();
                    }

                    @Override
                    public ConnectionEndPoint setValue(ConnectionEndPoint value) {
                        return null;
                    }
                };
            }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            return new ConnectionData(s.getNodeUuid(), ceps);
        }).collect(Collectors.toList());


        List<Connection> systemConnections = decomposedRequest.stream().map(s -> new ConnectionBuilder()
                .setUuid(new Uuid("conn:" + s.getNodeUuid().getValue() + ":" + uniqueStamp))
//                        .setState()
                .setDirection(ForwardingDirection.BIDIRECTIONAL)
                .setLayerProtocolName(LayerProtocolName.ETH)
//                .setContainerNode(s.getNodeUuid())
//                .setConnectionEndPoint(toConnectionPoints(s.getEndpoints(), uniqueStamp))
                .build()).collect(Collectors.toList());

        Connection globalConnection = new ConnectionBuilder()
                .setUuid(new Uuid("conn:" + TapiConstants.PRESTO_ABSTRACT_NODE + ":" + uniqueStamp))
//                        .setState()
                .setDirection(ForwardingDirection.BIDIRECTIONAL)
                .setLayerProtocolName(LayerProtocolName.ETH)
//                .setContainerNode(new Uuid(TapiConstants.PRESTO_ABSTRACT_NODE))
//                .setConnectionEndPoint(toConnectionPoints(endpoints, uniqueStamp))
                .setRoute(Collections.singletonList(new RouteBuilder()
                        .setLocalId("route")
                        .setConnectionEndPoint(systemConnections.stream()
                                .map(GlobalClass::getUuid)
                                .collect(Collectors.toList()))
                        .build())
                ).build();


        ConnectivityServiceBuilder builder = new ConnectivityServiceBuilder(input.getConnConstraint());

        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180216.connectivity.context.ConnectivityService cs =
                builder
                .setUuid(new Uuid(toCsId(uniqueStamp)))
//                    .setState()
                .setConnection(Collections.singletonList(globalConnection.getUuid()))
                .setEndPoint(toConnectionServiceEndpoints(endpoints, uniqueStamp))
                .build();

//        final WriteTransaction tx = service.getBroker().newWriteOnlyTransaction();
        systemConnections.forEach(c -> {
            tx.put(LogicalDatastoreType.OPERATIONAL, TapiConnectivityServiceImpl.connectivityCtx.child(Connection.class, new ConnectionKey(c.getUuid())), c);
        });
        tx.put(LogicalDatastoreType.OPERATIONAL,
                TapiConnectivityServiceImpl.connectivityCtx.child(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180216.connectivity.context.ConnectivityService.class,
                        new ConnectivityServiceKey(cs.getUuid())), cs);

        tx.put(LogicalDatastoreType.OPERATIONAL, TapiConnectivityServiceImpl.connectivityCtx.child(Connection.class, new ConnectionKey(globalConnection.getUuid())), globalConnection);

        LOG.debug("Storing connectivity related model for {} to operational data store", uniqueStamp);


        try {
            tx.submit().checkedGet(500, TimeUnit.MILLISECONDS);
            LOG.info("Success with serializing Connections and Connectivity Service for {}", uniqueStamp);
        } catch (TimeoutException e) {
            LOG.error("Error with commiting Connections and Connectivity Service for {} within {} ms", uniqueStamp, 500);
            throw e;
        } catch (TransactionCommitFailedException e) {
            LOG.error("Error with commiting Connections and Connectivity Service for " + uniqueStamp, e);
            throw e;
        }

        return new ConnectivityServiceBuilder(cs).build();
    }

    private List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180216.connectivity.service.EndPoint> toConnectionServiceEndpoints(List<EndPoint> endpoints, String uniqueStamp) {
        return endpoints.stream().map(ep -> new EndPointBuilder()
                .setLocalId("sep:" + ep.getSystemNepUuid() + ":" + uniqueStamp)
                .setServiceInterfacePoint(ep.getEndpoint().getServiceInterfacePoint())
                .setDirection(PortDirection.BIDIRECTIONAL)
// TODO donaldh .setLayerProtocolName(LayerProtocolName.ETH)
                .setRole(PortRole.SYMMETRIC)
                .build()
        ).collect(Collectors.toList());
    }

//    private List<ConnectionEndPoint> toConnectionPoints(List<EndPoint> endpoints, String uniqueStamp) {
//        return endpoints.stream().map(ep -> new ConnectionEndPointBuilder()
//                        .setUuid(new Uuid("cep:" + ep.getSystemNepUuid() + ":" + uniqueStamp))
////                    .setState()
//                        .setConnectionPortDirection(PortDirection.BIDIRECTIONAL)
//                        .setConnectionPortRole(PortRole.SYMMETRIC)
//                        .setServerNodeEdgePoint(ep.getSystemNepUuid())
//                        .setLayerProtocolName(LayerProtocolName.ETH)
//
//                                .setTerminationDirection(TerminationDirection.BIDIRECTIONAL)
//                                .setLayerProtocolName(LayerProtocolName.ETH).build()))
//                        .build()
//        ).collect(Collectors.toList());
//
//
//    }
}
