/*
 * Copyright (c) 2018 Xoriant Corporation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.legato;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;

import org.opendaylight.unimgr.api.UnimgrDataTreeChangeListener;
import org.opendaylight.unimgr.mef.legato.dao.EVCDao;
import org.opendaylight.unimgr.mef.legato.util.LegatoUtils;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.legato.services.rev171215.MefServices;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.legato.services.rev171215.mef.services.CarrierEthernet;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.legato.services.rev171215.mef.services.carrier.ethernet.SubscriberServices;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.legato.services.rev171215.mef.services.carrier.ethernet.SubscriberServicesBuilder;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.legato.services.rev171215.mef.services.carrier.ethernet.subscriber.services.Evc;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.legato.services.rev171215.mef.services.carrier.ethernet.subscriber.services.EvcKey;

import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.CreateConnectivityServiceInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.CreateConnectivityServiceOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.DeleteConnectivityServiceInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.DeleteConnectivityServiceInputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.UpdateConnectivityServiceInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.UpdateConnectivityServiceOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev180307.TapiConnectivityService;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.types.rev171215.EvcIdType;

import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

/**
 * @author santanu.de@xoriant.com
 */

public class LegatoServiceController extends UnimgrDataTreeChangeListener<Evc> {

    public LegatoServiceController(DataBroker dataBroker) {
        super(dataBroker);
        registerListener();
    }

    private static final Logger LOG = LoggerFactory
            .getLogger(LegatoServiceController.class);

    private static final InstanceIdentifier<Evc> EVC_IID = InstanceIdentifier
            .builder(MefServices.class).child(CarrierEthernet.class)
            .child(SubscriberServices.class).child(Evc.class).build();

    private static final InstanceIdentifier<SubscriberServices> EVC_IID_OPERATIONAL = InstanceIdentifier
            .builder(MefServices.class).child(CarrierEthernet.class)
            .child(SubscriberServices.class).build();

    private ListenerRegistration<LegatoServiceController> dataTreeChangeListenerRegistration;

    private static final Map<String, String> EVC_UUIDMap = new HashMap<String, String>();

    private TapiConnectivityService prestoConnectivityService;

    public void setPrestoConnectivityService(
            TapiConnectivityService prestoConnectivityService) {
        this.prestoConnectivityService = prestoConnectivityService;
    }

    public void registerListener() {
        LOG.info("Initializing LegatoServiceController:int() ");

        assert prestoConnectivityService != null;

        dataTreeChangeListenerRegistration = dataBroker
                .registerDataTreeChangeListener(new DataTreeIdentifier<Evc>(
                        LogicalDatastoreType.CONFIGURATION, EVC_IID), this);
    }

    public void close() throws Exception {
        if (dataTreeChangeListenerRegistration != null) {
            dataTreeChangeListenerRegistration.close();
        }
    }

    @Override
    public void add(DataTreeModification<Evc> newDataObject) {
        if (newDataObject.getRootNode() != null
                && newDataObject.getRootPath() != null) {
            LOG.info("  Node Added  "
                    + newDataObject.getRootNode().getIdentifier());

            Optional<Evc> OptionalEvc = LegatoUtils.readEvc(dataBroker,
                    LogicalDatastoreType.CONFIGURATION, newDataObject
                            .getRootPath().getRootIdentifier());

            if (OptionalEvc.isPresent()) {
                addNode(OptionalEvc.get());
            }
        }
    }

    @Override
    public void remove(DataTreeModification<Evc> removedDataObject) {
        if (removedDataObject.getRootNode() != null
                && removedDataObject.getRootPath() != null) {
            LOG.info("  Node removed  "
                    + removedDataObject.getRootNode().getIdentifier());

            deleteNode(removedDataObject.getRootNode().getDataBefore());
        }
    }

    @Override
    public void update(DataTreeModification<Evc> modifiedDataObject) {
        if (modifiedDataObject.getRootNode() != null
                && modifiedDataObject.getRootPath() != null) {
            LOG.info("  Node modified  "
                    + modifiedDataObject.getRootNode().getIdentifier());
            Optional<Evc> OptionalEvc = LegatoUtils.readEvc(dataBroker,
                    LogicalDatastoreType.CONFIGURATION, modifiedDataObject
                            .getRootPath().getRootIdentifier());

            if (OptionalEvc.isPresent()) {
                updateNode(OptionalEvc.get());
            }
        }
    }

    private void addNode(Evc evc) {
        LOG.info(" inside addNode()");

        try {
            assert evc != null;
            createConnection(evc);

        } catch (Exception ex) {
            LOG.error("error: ", ex);
        }

        LOG.info(" ********** END addNode() ****************** ");

    }

    private void updateNode(Evc evc) {
        LOG.info(" inside updateNode()");

        try {
            assert evc != null;
            updateConnection(evc);

        } catch (Exception ex) {
            LOG.error("error: ", ex);
        }

        LOG.info(" ********** END updateNode() ****************** ");

    }

    private void deleteNode(Evc evc) {
        LOG.info(" inside deleteNode()");

        try {
            assert evc != null;
            deleteConnection(evc.getEvcId().getValue());
            
        } catch (Exception ex) {
            LOG.error("error: ", ex);
        }

        LOG.info(" ********** END deleteNode() ****************** ");

    }

    private void createConnection(Evc evc) {
        LOG.info("inside createConnection()");

        try {
            EVCDao evcDao =  LegatoUtils.parseNodes(evc);
            assert evcDao != null 
                    && evcDao.getUniList() != null && evcDao.getConnectionType() != null;
            LOG.info(" connection-type :{} ", evcDao.getConnectionType());
            
            callCreateConnectionService(
                        LegatoUtils.buildCreateConnectivityServiceInput(evcDao, evcDao.getConnectionType()),
                        evcDao.getEvcId());
            
        } catch (Exception ex) {

            LOG.error("Error in createConnection(). Err: ", ex);
        }

    }
    
    private void updateConnection(Evc evc) {
        LOG.info("inside updateConnection()");

        try {
            EVCDao evcDao =  LegatoUtils.parseNodes(evc);
            assert evcDao != null
                    && evcDao.getUniList() != null && evcDao.getConnectionType() != null;
            LOG.info(" connection-type :{} ", evcDao.getConnectionType());

                if (EVC_UUIDMap.containsKey(evcDao.getEvcId())) {
                    LOG.info(
                            "Update UUID: {} of EVC Id: {} ",
                            EVC_UUIDMap.get(evcDao.getEvcId()),
                            evcDao.getEvcId());

                    List<String> uniList = evcDao.getUniList();
                    assert uniList != null && uniList.size() > 0;

                    for (String uniStr : uniList) {
                        callUpdateConnectionService(
                                LegatoUtils.buildUpdateConnectivityServiceInput(
                                        evcDao, uniStr, EVC_UUIDMap.get(evcDao.getEvcId()), evcDao.getConnectionType()),
                                                evcDao.getEvcId());
                    }
                } else {
                    LOG.info("UUID does not exists for EVC Id : {}",
                            evcDao.getEvcId());
                }
        } catch (Exception ex) {

            LOG.error("Error in updateConnection(). Err: ", ex);
        }

    }

    private void deleteConnection(String evcId) {
        LOG.info(" inside deleteConnection()");
        try {

            assert EVC_UUIDMap != null;

            if (EVC_UUIDMap.containsKey(evcId)) {
                LOG.info("Deleting UUID: {} of EVC Id: {} ",
                        EVC_UUIDMap.get(evcId), evcId);
                // on successful deletion of service, remove respective element from evc_UUIDMap
                if (callDeleteConnectionService(new DeleteConnectivityServiceInputBuilder()
                        .setServiceIdOrName(EVC_UUIDMap.get(evcId)).build())) {
                    EVC_UUIDMap.remove(evcId);
                }

                // delete EVC node from OPERATIONAL DB              
                LegatoUtils.deleteFromOperationalDB(InstanceIdentifier
                        .create(MefServices.class).child(CarrierEthernet.class)
                        .child(SubscriberServices.class)
                        .child(Evc.class, new EvcKey(new EvcIdType(evcId))), dataBroker);

            } else {
                LOG.info("UUID does not exists for EVC Id : {}", evcId);
            }

        } catch (Exception ex) {
            LOG.error("error: ", ex);
        }

        LOG.info(" ********** END deleteConnection() ****************** ");
    }

    private void callCreateConnectionService(
            CreateConnectivityServiceInput createConnServiceInput, String evcId) {
        try {
            Future<RpcResult<CreateConnectivityServiceOutput>> response = this.prestoConnectivityService
                    .createConnectivityService(createConnServiceInput);

            if (response.get().isSuccessful()) {
                LOG.info("call Success = {}, response = {} ", response.get()
                        .isSuccessful(), response.get().getResult());
                LOG.info("evcId = {}, UUID = {} ", evcId, response.get()
                        .getResult().getService().getUuid().getValue());

                EVC_UUIDMap.put(evcId, response.get().getResult().getService()
                        .getUuid().getValue());

                LOG.info("======== {} ", EVC_UUIDMap.toString());

                Optional<Evc> OptionalEvc = LegatoUtils.readEvc(
                        dataBroker,
                        LogicalDatastoreType.CONFIGURATION,
                        InstanceIdentifier
                                .create(MefServices.class)
                                .child(CarrierEthernet.class)
                                .child(SubscriberServices.class)
                                .child(Evc.class,
                                        new EvcKey(new EvcIdType(evcId))));

                // Add Node in OPERATIONAL DB
                if (OptionalEvc.isPresent()) {
                    UpdateEvcOnOperationalDB(OptionalEvc.get());
                }

            } else
                LOG.info("call Failure = {} >> {} ", response.get()
                        .isSuccessful(), response.get().getErrors());
        } catch (Exception ex) {
            LOG.error("Error in callCreateConnectionService(). Err: ", ex);
        }
    }

    private void callUpdateConnectionService(
            UpdateConnectivityServiceInput updateConnectivityServiceInput,
            String evcId) {
        try {
            Future<RpcResult<UpdateConnectivityServiceOutput>> response = this.prestoConnectivityService
                    .updateConnectivityService(updateConnectivityServiceInput);

            if (response.get().isSuccessful()) {
                LOG.info("call Success = {}, response = {} ", response.get()
                        .isSuccessful(), response.get().getResult());

                Optional<Evc> OptionalEvc = LegatoUtils.readEvc(
                        dataBroker,
                        LogicalDatastoreType.CONFIGURATION,
                        InstanceIdentifier
                                .create(MefServices.class)
                                .child(CarrierEthernet.class)
                                .child(SubscriberServices.class)
                                .child(Evc.class,
                                        new EvcKey(new EvcIdType(evcId))));

                // update EVC Node in OPERATIONAL DB
                if (OptionalEvc.isPresent()) {                  
                    LegatoUtils.deleteFromOperationalDB(InstanceIdentifier
                            .create(MefServices.class)
                            .child(CarrierEthernet.class)
                            .child(SubscriberServices.class)
                            .child(Evc.class, new EvcKey(new EvcIdType(evcId))), dataBroker);

                    UpdateEvcOnOperationalDB(OptionalEvc.get());
                }

            } else
                LOG.info("call Failure = {} >> {} ", response.get()
                        .isSuccessful(), response.get().getErrors());
        } catch (Exception ex) {

            LOG.error("Error in UpdateConnectivityServiceInput(). Err: ", ex);
        }
    }

    private boolean callDeleteConnectionService(
            DeleteConnectivityServiceInput deleteConnectivityServiceInput) {
        try {
            this.prestoConnectivityService
                    .deleteConnectivityService(deleteConnectivityServiceInput);
            return true;

        } catch (Exception ex) {
            LOG.error("Fail to call callDeleteConnectionService(). Err: ", ex);
            return false;
        }
    }

    private void UpdateEvcOnOperationalDB(Evc evc) {
        List<Evc> evcList = new ArrayList<Evc>();
        evcList.add(evc);

        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        tx.merge(LogicalDatastoreType.OPERATIONAL, EVC_IID_OPERATIONAL,
                new SubscriberServicesBuilder().setEvc(evcList).build());

        try {
            tx.submit().checkedGet();
        } catch (org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException e) {
            LOG.error("Error in UpdateEvcOnOperationalDB(). Err: ", e);
        }
    }

}
