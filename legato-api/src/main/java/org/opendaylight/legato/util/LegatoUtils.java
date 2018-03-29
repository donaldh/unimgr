/*
 * Copyright (c) 2018 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.legato.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.common.types.rev171221.ColorMode;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.common.types.rev171221.NaturalNumber;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.common.types.rev171221.PositiveInteger;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.legato.services.rev171215.mef.services.carrier.ethernet.subscriber.services.Evc;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.legato.services.rev171215.mef.services.carrier.ethernet.subscriber.services.evc.end.points.EndPoint;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.types.rev171215.VlanIdType;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrm.connectivity.rev171221.carrier.eth.connectivity.end.point.resource.CeVlanIdListAndUntagBuilder;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrm.connectivity.rev171221.carrier.eth.connectivity.end.point.resource.IngressBwpFlow;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrm.connectivity.rev171221.carrier.eth.connectivity.end.point.resource.IngressBwpFlowBuilder;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrm.connectivity.rev171221.vlan.id.list.and.untag.VlanId;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrm.connectivity.rev171221.vlan.id.list.and.untag.VlanIdBuilder;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev171221.CreateConnectivityServiceInput1;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev171221.CreateConnectivityServiceInput1Builder;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev171221.EndPoint2;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev171221.EndPoint2Builder;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev171221.EndPoint7;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev171221.EndPoint7Builder;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev171221.UpdateConnectivityServiceInput1;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev171221.UpdateConnectivityServiceInput1Builder;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev171221.nrp.connectivity.service.attrs.NrpCarrierEthConnectivityResource;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev171221.nrp.connectivity.service.attrs.NrpCarrierEthConnectivityResourceBuilder;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev171221.nrp.connectivity.service.end.point.attrs.NrpCarrierEthConnectivityEndPointResource;
import org.opendaylight.yang.gen.v1.urn.mef.yang.nrp._interface.rev171221.nrp.connectivity.service.end.point.attrs.NrpCarrierEthConnectivityEndPointResourceBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.common.rev171113.ETH;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.common.rev171113.PortDirection;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.common.rev171113.PortRole;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.common.rev171113.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.connectivity.rev171113.CreateConnectivityServiceInput;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.connectivity.rev171113.CreateConnectivityServiceInputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.connectivity.rev171113.ServiceType;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.connectivity.rev171113.UpdateConnectivityServiceInput;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.connectivity.rev171113.UpdateConnectivityServiceInputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.connectivity.rev171113.connectivity.service.end.point.LayerProtocol;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.connectivity.rev171113.connectivity.service.end.point.LayerProtocolBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.connectivity.rev171113.create.connectivity.service.input.ConnConstraintBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.connectivity.rev171113.create.connectivity.service.input.EndPointBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

/**
 * @author santanu.de@xoriant.com
 */

public class LegatoUtils {

    private static final Logger LOG = LoggerFactory.getLogger(LegatoUtils.class);

    public static final Map<String, Object> parseNodes(Evc evc) {
        Map<String, Object> evcMap;
        List<String> uniList;
        String vlanId;

        assert evc != null;

        uniList = new ArrayList<String>();
        evcMap = new HashMap<String, Object>();

        assert evc.getEndPoints().getEndPoint() != null && evc.getEndPoints().getEndPoint().size() > 0;
        for (EndPoint endPoint : evc.getEndPoints().getEndPoint()) {
            vlanId = "";
            assert endPoint.getCeVlans().getCeVlan() != null;
            for (VlanIdType vlanIdType : endPoint.getCeVlans().getCeVlan()) {
                vlanId = vlanIdType.getValue().toString();
            }

            uniList.add(endPoint.getUniId().getValue().toString() + "#" + vlanId);

        }

        evcMap.put(LegatoConstants.EVC_ID, evc.getEvcId().getValue());
        evcMap.put(LegatoConstants.EVC_MAX_FRAME,
                (evc.getMaxFrameSize().getValue() != null) ? evc.getMaxFrameSize().getValue() : "");
        evcMap.put(LegatoConstants.EVC_STATUS, (evc.getStatus() != null) ? evc.getStatus() : "");
        evcMap.put(LegatoConstants.EVC_CON_TYPE,
                (evc.getConnectionType().getName() != null) ? evc.getConnectionType().getName() : "");
        evcMap.put(LegatoConstants.EVC_UNI_LIST, uniList);

        return evcMap;
    }

    public static final EndPoint2 buildCreateEthConnectivityEndPointAugmentation(String vlanId) {
        return new EndPoint2Builder()
                .setNrpCarrierEthConnectivityEndPointResource(buildNrpCarrierEthConnectivityEndPointResource(vlanId))
                .build();
    }

    public static final EndPoint7 buildUpdateEthConnectivityEndPointAugmentation(String vlanId) {
        return new EndPoint7Builder()
                .setNrpCarrierEthConnectivityEndPointResource(buildNrpCarrierEthConnectivityEndPointResource(vlanId))
                .build();
    }

    public static final CreateConnectivityServiceInput1 buildCreateConServiceAugmentation(String maxFrameSize) {
        CreateConnectivityServiceInput1 createConServiceAugmentation = new CreateConnectivityServiceInput1Builder()
                .setNrpCarrierEthConnectivityResource(buildNrpCarrierEthConnectivityResource(maxFrameSize)).build();
        return createConServiceAugmentation;
    }

    public static final UpdateConnectivityServiceInput1 buildUpdateConServiceAugmentation(String maxFrameSize) {
        UpdateConnectivityServiceInput1 updateConServiceAugmentation = new UpdateConnectivityServiceInput1Builder()
                .setNrpCarrierEthConnectivityResource(buildNrpCarrierEthConnectivityResource(maxFrameSize)).build();
        return updateConServiceAugmentation;
    }

    public static final NrpCarrierEthConnectivityEndPointResource buildNrpCarrierEthConnectivityEndPointResource(
            String vlanId) {

        NrpCarrierEthConnectivityEndPointResourceBuilder nrpCarrierEthConnectivityEndPointResourceBuilder = new NrpCarrierEthConnectivityEndPointResourceBuilder();

        CeVlanIdListAndUntagBuilder ceVlanIdListAndUntagBuilder = new CeVlanIdListAndUntagBuilder();
        List<VlanId> vlanList = new ArrayList<VlanId>();
        VlanIdBuilder vlanIdBuilder = new VlanIdBuilder().setVlanId(new PositiveInteger(Long.parseLong(vlanId)));
        vlanList.add(vlanIdBuilder.build());

        ceVlanIdListAndUntagBuilder.setVlanId(vlanList);

        nrpCarrierEthConnectivityEndPointResourceBuilder.setCeVlanIdListAndUntag(ceVlanIdListAndUntagBuilder.build());
        nrpCarrierEthConnectivityEndPointResourceBuilder.setIngressBwpFlow(buildIngressBwFlow());

        return nrpCarrierEthConnectivityEndPointResourceBuilder.build();

    }

    public static final IngressBwpFlow buildIngressBwFlow() {
        IngressBwpFlowBuilder ingressBwpFlowBuilder = new IngressBwpFlowBuilder()
                .setRank(new PositiveInteger(LegatoConstants.LONG_VAL))
                .setCir(new NaturalNumber(LegatoConstants.LONG_VAL))
                .setCirMax(new NaturalNumber(LegatoConstants.LONG_VAL))
                .setCbs(new NaturalNumber(LegatoConstants.LONG_VAL)).setEir(new NaturalNumber(LegatoConstants.LONG_VAL))
                .setEirMax(new NaturalNumber(LegatoConstants.LONG_VAL))
                .setEbs(new NaturalNumber(LegatoConstants.LONG_VAL)).setCouplingFlag(true)
                .setColorMode(ColorMode.COLORAWARE).setTokenRequestOffset(new NaturalNumber(LegatoConstants.LONG_VAL));

        return ingressBwpFlowBuilder.build();
    }

    public static final NrpCarrierEthConnectivityResource buildNrpCarrierEthConnectivityResource(String maxFrameSize) {
        NrpCarrierEthConnectivityResourceBuilder nrpCarrierEthConnectivityResourceBuilder = new NrpCarrierEthConnectivityResourceBuilder();
        return nrpCarrierEthConnectivityResourceBuilder
                .setMaxFrameSize(new PositiveInteger(Long.parseLong(maxFrameSize))).build();
    }

    public static final CreateConnectivityServiceInput buildCreateConnectivityServiceInput(Map<String, Object> evcMap,
            String connType) {

        CreateConnectivityServiceInputBuilder createConnServiceInputBuilder = new CreateConnectivityServiceInputBuilder();
        List<org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.connectivity.rev171113.create.connectivity.service.input.EndPoint> endpointList;
        List<String> uniList = new ArrayList<String>();
        List<LayerProtocol> layerList = new ArrayList<LayerProtocol>();

        switch (connType.replace("-", "").toUpperCase()) {
        case LegatoConstants.POINTTOPOINT:
            createConnServiceInputBuilder
                    .setConnConstraint(new ConnConstraintBuilder().setServiceLevel(LegatoConstants.BEST_EFFORT)
                            .setServiceType(ServiceType.POINTTOPOINTCONNECTIVITY).build());
            break;
        case LegatoConstants.MULTIPOINTTOMULTIPOINT:
            createConnServiceInputBuilder
                    .setConnConstraint(new ConnConstraintBuilder().setServiceLevel(LegatoConstants.BEST_EFFORT)
                            .setServiceType(ServiceType.MULTIPOINTCONNECTIVITY).build());
            break;
        default:
            break;
        }

        layerList.add(new LayerProtocolBuilder().setLocalId("eth").setLayerProtocolName(ETH.class).build());

        uniList = (List<String>) evcMap.get(LegatoConstants.EVC_UNI_LIST);

        // build end points
        assert uniList != null && uniList.size() > 0;
        endpointList = buildCreateEndpoints(uniList, layerList);

        createConnServiceInputBuilder.setEndPoint(endpointList);

        createConnServiceInputBuilder.addAugmentation(CreateConnectivityServiceInput1.class,
                LegatoUtils.buildCreateConServiceAugmentation(evcMap.get(LegatoConstants.EVC_MAX_FRAME).toString()));

        return createConnServiceInputBuilder.build();
    }

    public static final UpdateConnectivityServiceInput buildUpdateConnectivityServiceInput(Map<String, Object> evcMap,
            String uniStr, String uuid, String connType) {

        UpdateConnectivityServiceInputBuilder updateConnServiceInputBuilder = new UpdateConnectivityServiceInputBuilder();
        List<LayerProtocol> layerList = new ArrayList<LayerProtocol>();

        switch (connType.replace("-", "").toUpperCase()) {
        case LegatoConstants.POINTTOPOINT:
            updateConnServiceInputBuilder.setConnConstraint(
                    new org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.connectivity.rev171113.update.connectivity.service.input.ConnConstraintBuilder()
                            .setServiceLevel(LegatoConstants.BEST_EFFORT)
                            .setServiceType(ServiceType.POINTTOPOINTCONNECTIVITY).build());
            break;
        case LegatoConstants.MULTIPOINTTOMULTIPOINT:
            updateConnServiceInputBuilder.setConnConstraint(
                    new org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.connectivity.rev171113.update.connectivity.service.input.ConnConstraintBuilder()
                            .setServiceLevel(LegatoConstants.BEST_EFFORT)
                            .setServiceType(ServiceType.MULTIPOINTCONNECTIVITY).build());
            break;
        default:
            break;
        }

        layerList.add(new LayerProtocolBuilder().setLocalId("eth").setLayerProtocolName(ETH.class).build());

        updateConnServiceInputBuilder.setEndPoint(buildUpdateEndpoints(uniStr, layerList));
        updateConnServiceInputBuilder.addAugmentation(UpdateConnectivityServiceInput1.class,
                LegatoUtils.buildUpdateConServiceAugmentation(evcMap.get(LegatoConstants.EVC_MAX_FRAME).toString()));
        updateConnServiceInputBuilder.setServiceIdOrName(uuid);

        return updateConnServiceInputBuilder.build();
    }

    private static List<org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.connectivity.rev171113.create.connectivity.service.input.EndPoint> buildCreateEndpoints(
            List<String> uniList, List<LayerProtocol> layerList) {
        List<org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.connectivity.rev171113.create.connectivity.service.input.EndPoint> endpointList = new ArrayList<org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.connectivity.rev171113.create.connectivity.service.input.EndPoint>();

        EndPointBuilder endPointBuilder;
        String[] uniArr;

        for (String uniStr : uniList) {
            uniArr = uniStr.split("#");
            endPointBuilder = new EndPointBuilder().setRole(PortRole.SYMMETRIC).setLocalId("e:" + uniArr[0])
                    .setServiceInterfacePoint(new Uuid(uniArr[0])).setDirection(PortDirection.BIDIRECTIONAL)
                    .setLayerProtocol(layerList).addAugmentation(EndPoint2.class,
                            LegatoUtils.buildCreateEthConnectivityEndPointAugmentation(uniArr[1]));

            endpointList.add(endPointBuilder.build());
        }

        endPointBuilder = null;
        uniArr = null;

        return endpointList;
    }

    private static org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.connectivity.rev171113.update.connectivity.service.input.EndPoint buildUpdateEndpoints(
            String uniStr, List<LayerProtocol> layerList) {
        org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.connectivity.rev171113.update.connectivity.service.input.EndPointBuilder endPointBuilder = new org.opendaylight.yang.gen.v1.urn.onf.params.xml.ns.yang.tapi.connectivity.rev171113.update.connectivity.service.input.EndPointBuilder();
        String[] uniArr;

        if (StringUtils.isNotBlank(uniStr)) {
            uniArr = uniStr.split("#");
            endPointBuilder.setRole(PortRole.SYMMETRIC).setLocalId("e:" + uniArr[0])
                    .setServiceInterfacePoint(new Uuid(uniArr[0])).setDirection(PortDirection.BIDIRECTIONAL)
                    .setLayerProtocol(layerList).addAugmentation(EndPoint7.class,
                            LegatoUtils.buildUpdateEthConnectivityEndPointAugmentation(uniArr[1]));
        }

        uniArr = null;

        return endPointBuilder.build();
    }

    public static final Optional<Evc> readEvc(DataBroker dataBroker, LogicalDatastoreType store,
            InstanceIdentifier<?> evcNode) {
        final ReadTransaction read = dataBroker.newReadOnlyTransaction();
        final InstanceIdentifier<Evc> evcId = evcNode.firstIdentifierOf(Evc.class);
        final CheckedFuture<Optional<Evc>, ReadFailedException> linkFuture = read.read(store, evcId);
        try {
            return linkFuture.checkedGet();
        } catch (final ReadFailedException e) {
            LOG.error("Unable to read node with EVC Id {}, err: {} ", evcId, e);
        }
        return Optional.absent();
    }

    @SuppressWarnings("deprecation")
    public static Optional<?> readProfile(String string, DataBroker dataBroker, LogicalDatastoreType store,
            InstanceIdentifier<?> child) {
        final ReadTransaction read = dataBroker.newReadOnlyTransaction();
        try {

            switch (string) {
            case LegatoConstants.SLS_PROFILES:
                final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.sls.profiles.Profile> profileId = child
                        .firstIdentifierOf(
                                org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.sls.profiles.Profile.class);
                final CheckedFuture<Optional<org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.sls.profiles.Profile>, ReadFailedException> profileFuture = read
                        .read(store, profileId);
                return profileFuture.checkedGet();

            case LegatoConstants.COS_PROFILES:
                final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.cos.profiles.Profile> cosProfileId = child
                        .firstIdentifierOf(
                                org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.cos.profiles.Profile.class);
                final CheckedFuture<Optional<org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.cos.profiles.Profile>, ReadFailedException> cosProfileFuture = read
                        .read(store, cosProfileId);
                return cosProfileFuture.checkedGet();

            case LegatoConstants.BWP_PROFILES:
                final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.bwp.flow.parameter.profiles.Profile> bwpProfileId = child
                        .firstIdentifierOf(
                                org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.bwp.flow.parameter.profiles.Profile.class);
                final CheckedFuture<Optional<org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.bwp.flow.parameter.profiles.Profile>, ReadFailedException> bwpProfileFuture = read
                        .read(store, bwpProfileId);
                return bwpProfileFuture.checkedGet();

            case LegatoConstants.l2CP_EEC_PROFILES:
                final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.l2cp.eec.profiles.Profile> l2cpEec_ProfileId = child
                        .firstIdentifierOf(
                                org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.l2cp.eec.profiles.Profile.class);
                final CheckedFuture<Optional<org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.l2cp.eec.profiles.Profile>, ReadFailedException> l2cpEecProfileFuture = read
                        .read(store, l2cpEec_ProfileId);
                return l2cpEecProfileFuture.checkedGet();

            case LegatoConstants.L2CP_PEERING_PROFILES:
                final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.l2cp.peering.profiles.Profile> l2cpPeering_ProfileId = child
                        .firstIdentifierOf(
                                org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.l2cp.peering.profiles.Profile.class);
                final CheckedFuture<Optional<org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.l2cp.peering.profiles.Profile>, ReadFailedException> l2cpPeeringProfileFuture = read
                        .read(store, l2cpPeering_ProfileId);
                return l2cpPeeringProfileFuture.checkedGet();

            case LegatoConstants.EEC_PROFILES:
                final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.eec.profiles.Profile> eecProfileId = child
                        .firstIdentifierOf(
                                org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.eec.profiles.Profile.class);
                final CheckedFuture<Optional<org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.eec.profiles.Profile>, ReadFailedException> eecProfileFuture = read
                        .read(store, eecProfileId);
                return eecProfileFuture.checkedGet();
                
            case LegatoConstants.CMP_PROFILES:
                final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.color.mapping.profiles.Profile> cmpProfileId = child
                        .firstIdentifierOf(
                                org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.color.mapping.profiles.Profile.class);
                final CheckedFuture<Optional<org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.color.mapping.profiles.Profile>, ReadFailedException> cmpProfileFuture = read
                        .read(store, cmpProfileId);
                return cmpProfileFuture.checkedGet();

            default:
                LOG.info("IN DEFAULT CASE :  NO MATCH");
            }
        } catch (final ReadFailedException e) {
            LOG.error("Unable to read node ", e);
        }
        return Optional.absent();
    }

    @SuppressWarnings("deprecation")
    public static void deleteFromOperationalDB(InstanceIdentifier<?> nodeIdentifier, DataBroker dataBroker) {
        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        tx.delete(LogicalDatastoreType.OPERATIONAL, nodeIdentifier);

        try {
            tx.submit().checkedGet();
        } catch (org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException e) {
            LOG.error("Error in deleteFromOperationalDB(). Err: ", e);
        }
    }

    public static <T extends DataObject> void addToOperationalDB(T typeOfProfile, InstanceIdentifier<T> profilesTx,
            DataBroker dataBroker) {
        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        tx.merge(LogicalDatastoreType.OPERATIONAL, profilesTx, typeOfProfile);
        try {
            tx.submit().checkedGet();
        } catch (org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException e) {
            LOG.error("Error in adding data to OperationalDB(). Err: ", e);
        }

    }

}
