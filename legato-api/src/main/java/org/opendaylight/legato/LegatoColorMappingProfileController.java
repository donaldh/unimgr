/*
 * Copyright (c) 2018 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.legato;

import java.util.Collections;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.legato.util.LegatoConstants;
import org.opendaylight.legato.util.LegatoUtils;
import org.opendaylight.unimgr.api.UnimgrDataTreeChangeListener;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.MefGlobal;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.ColorMappingProfiles;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.ColorMappingProfilesBuilder;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.color.mapping.profiles.Profile;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.color.mapping.profiles.ProfileKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

/**
 * @author sanket.shirode@Xoriant.com
 */

public class LegatoColorMappingProfileController extends UnimgrDataTreeChangeListener<Profile> {

    private static final Logger LOG = LoggerFactory.getLogger(LegatoColorMappingProfileController.class);
    private ListenerRegistration<LegatoColorMappingProfileController> dataTreeChangeListenerRegistration;
    private static final InstanceIdentifier<Profile> CMP_PROFILE_IID = InstanceIdentifier.builder(MefGlobal.class)
            .child(ColorMappingProfiles.class).child(Profile.class).build();

    public LegatoColorMappingProfileController(DataBroker dataBroker) {
        super(dataBroker);
        registerListener();
        // TODO Auto-generated constructor stub
    }

    private void registerListener() {
        LOG.info("Initializing LegatoSlsProfileController:init() ");

        dataTreeChangeListenerRegistration = dataBroker.registerDataTreeChangeListener(
                new DataTreeIdentifier<Profile>(LogicalDatastoreType.CONFIGURATION, CMP_PROFILE_IID), this);

    }

    @Override
    public void close() throws Exception {
        if (dataTreeChangeListenerRegistration != null) {
            dataTreeChangeListenerRegistration.close();
        }
    }

    @Override
    public void add(DataTreeModification<Profile> newDataObject) {
        if (newDataObject.getRootNode() != null && newDataObject.getRootPath() != null) {
            LOG.info("  Node Added  " + newDataObject.getRootNode().getIdentifier());
            addToOperationalDB(newDataObject.getRootNode().getDataAfter());

        }
    }
    
    private void addToOperationalDB(Profile profile) {
        try {
            assert profile != null;
        ColorMappingProfiles colorMappingProfiles = new ColorMappingProfilesBuilder().setProfile(Collections.singletonList(profile)).build();   
        InstanceIdentifier<ColorMappingProfiles> profilesTx = InstanceIdentifier.create(MefGlobal.class)
                .child(ColorMappingProfiles.class);
        LegatoUtils.addToOperationalDB(colorMappingProfiles, profilesTx, dataBroker);
        } catch (Exception ex) {
            LOG.error("error: ", ex);
        }

    }


    @Override
    public void remove(DataTreeModification<Profile> removedDataObject) {
        if (removedDataObject.getRootNode() != null && removedDataObject.getRootPath() != null) {
            LOG.info("  Node removed  " + removedDataObject.getRootNode().getIdentifier());
            try {
                assert removedDataObject.getRootNode().getDataBefore() != null;
                LegatoUtils.deleteFromOperationalDB(InstanceIdentifier.create(MefGlobal.class).child(ColorMappingProfiles.class)
                        .child(Profile.class, new ProfileKey(removedDataObject.getRootNode().getDataBefore().getId())),dataBroker);
            } catch (Exception ex) {
                LOG.error("error: ", ex);
            }
        }
    }


    @Override
    public void update(DataTreeModification<Profile> modifiedDataObject) {
        if (modifiedDataObject.getRootNode() != null && modifiedDataObject.getRootPath() != null) {
            LOG.info("  Node modified  " + modifiedDataObject.getRootNode().getIdentifier());
            try {
                assert modifiedDataObject.getRootNode().getDataAfter() != null;
                InstanceIdentifier<Profile> instanceIdentifier = InstanceIdentifier.create(MefGlobal.class)
                        .child(ColorMappingProfiles.class).child(Profile.class, new ProfileKey(modifiedDataObject.getRootNode().getDataAfter().getId()));
                Optional<Profile> OptionalProfile = (Optional<Profile>) LegatoUtils.readProfile(
                        LegatoConstants.CMP_PROFILES, dataBroker, LogicalDatastoreType.CONFIGURATION, instanceIdentifier);
                if (OptionalProfile.isPresent()) {
                    LegatoUtils.deleteFromOperationalDB(instanceIdentifier,dataBroker);
                    addToOperationalDB(OptionalProfile.get());
                }
            } catch (Exception ex) {
                LOG.error("error: ", ex);
            }
        }
    }

}
