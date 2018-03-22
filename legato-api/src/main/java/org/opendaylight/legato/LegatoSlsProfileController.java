/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.legato;


import java.util.ArrayList;
import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.legato.util.LegatoConstants;
import org.opendaylight.legato.util.LegatoUtils;
import org.opendaylight.unimgr.api.UnimgrDataTreeChangeListener;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.MefGlobal;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.SlsProfiles;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.SlsProfilesBuilder;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.sls.profiles.Profile;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.sls.profiles.ProfileKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class LegatoSlsProfileController  extends UnimgrDataTreeChangeListener<Profile> {
	
	public LegatoSlsProfileController(final DataBroker dataBroker) {
		super(dataBroker);
		registerListener();
	}

	private static final Logger LOG = LoggerFactory
			.getLogger(LegatoSlsProfileController.class);
	
	
	private static final InstanceIdentifier<Profile> PROFILE_IID = InstanceIdentifier
			.builder(MefGlobal.class).child(SlsProfiles.class).child(Profile.class).build();
	
	
	private static final InstanceIdentifier<SlsProfiles> SLS_PROFILES_IID_OPERATIONAL = InstanceIdentifier
			.builder(MefGlobal.class).child(SlsProfiles.class).build();
			
	
	private ListenerRegistration<LegatoSlsProfileController> dataTreeChangeListenerRegistration;
	
	
	public void registerListener() {
		LOG.info("Initializing LegatoSlsProfileController:init() ");

		dataTreeChangeListenerRegistration = dataBroker.registerDataTreeChangeListener(
				new DataTreeIdentifier<Profile>(LogicalDatastoreType.CONFIGURATION, PROFILE_IID), this);

	}
	

	@Override
	public void close() throws Exception {
		if (dataTreeChangeListenerRegistration != null) {
			dataTreeChangeListenerRegistration.close();
		}
		
	}

	
	
	/* Add node in Operational DB*/
	@Override
	public void add(DataTreeModification<Profile> newDataObject) {
		if(newDataObject.getRootNode() != null && newDataObject.getRootPath() != null ){
			LOG.info( "  Node Added  " + newDataObject.getRootNode().getIdentifier());
			
			addNode(newDataObject.getRootNode().getDataAfter());
		}
		
	}
	
	@SuppressWarnings("deprecation")
	private void addNode(Profile profileObj) {
		LOG.info(" inside addNode()");

		try {
			assert profileObj != null;
			List<Profile> proList = new ArrayList<Profile>();
			proList.add(profileObj);
			
			WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
			tx.merge(LogicalDatastoreType.OPERATIONAL, SLS_PROFILES_IID_OPERATIONAL,
					new SlsProfilesBuilder().setProfile(proList).build());

			tx.submit().checkedGet();
		} catch (org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException e) {
			LOG.error("Error in addNode(). Err: ", e);
		}

		LOG.info(" ********** END addNode() ****************** ");

	}

	
	
	
	/* Update node in Operational DB*/
	@Override
	public void update(DataTreeModification<Profile> modifiedDataObject) {
		if (modifiedDataObject.getRootNode() != null && modifiedDataObject.getRootPath() != null) {
			LOG.info("  Node modified  " + modifiedDataObject.getRootNode().getIdentifier());

			updateNode(modifiedDataObject.getRootNode().getDataAfter());
		}

	}
	
	
	@SuppressWarnings("unchecked")
	private void updateNode(Profile profile) {
		LOG.info(" inside updateNode()");

		try {
			assert profile != null;
			Optional<Profile> OptionalProfile = (Optional<Profile>) LegatoUtils.readProfile(LegatoConstants.SLS_PROFILES, dataBroker, LogicalDatastoreType.CONFIGURATION,
					InstanceIdentifier.create(MefGlobal.class).child(SlsProfiles.class).child(Profile.class,
							new ProfileKey(profile.getId())));

			if (OptionalProfile.isPresent()) {

				deleteProfileFromOperationalDB(InstanceIdentifier.create(MefGlobal.class).child(SlsProfiles.class)
						.child(Profile.class, new ProfileKey(profile.getId())));

				UpdateProfileOnOperationalDB(OptionalProfile.get());
			}

		} catch (Exception ex) {
			LOG.error("error: ", ex);
		}

		LOG.info(" ********** END updateNode() ****************** ");

	}
	
	
	@SuppressWarnings("deprecation")
	private void UpdateProfileOnOperationalDB(Profile profileObj) {
		List<Profile> proList = new ArrayList<Profile>();
		proList.add(profileObj);

		WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
		tx.merge(LogicalDatastoreType.OPERATIONAL, SLS_PROFILES_IID_OPERATIONAL, new SlsProfilesBuilder().setProfile(proList).build());

		try {
			tx.submit().checkedGet();
		} catch (org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException e) {
			LOG.error("Error in UpdateProfileOnOperationalDB(). Err: ", e);
		}

	}
	
	
	
	/* Delete node in Operational DB*/
	@Override
	public void remove(DataTreeModification<Profile> removedDataObject) {
		if (removedDataObject.getRootNode() != null && removedDataObject.getRootPath() != null) {
			LOG.info("  Node removed  " + removedDataObject.getRootNode().getIdentifier());
			
			deleteNode(removedDataObject.getRootNode().getDataBefore());
		}

	}

	
	private void deleteNode(Profile profileObj) {
		LOG.info(" inside deleteNode()");

		try {
			assert profileObj != null;
			deleteProfileFromOperationalDB(InstanceIdentifier.create(MefGlobal.class).child(SlsProfiles.class)
					.child(Profile.class, new ProfileKey(profileObj.getId())));

		} catch (Exception ex) {
			LOG.error("error: ", ex);
		}

		LOG.info(" ********** END deleteNode() ****************** ");

	}

	@SuppressWarnings("deprecation")
	private void deleteProfileFromOperationalDB(InstanceIdentifier<Profile> nodeIdentifier) {
		WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
		tx.delete(LogicalDatastoreType.OPERATIONAL, nodeIdentifier);

		try {
			tx.submit().checkedGet();
		} catch (org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException e) {
			LOG.error("Error in deleteProfileFromOperationalDB(). Err: ", e);
		}
	}
	

}
