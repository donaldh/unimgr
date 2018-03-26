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
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.L2cpEecProfiles;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.L2cpEecProfilesBuilder;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.l2cp.eec.profiles.Profile;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.l2cp.eec.profiles.ProfileKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

/**
 * @author ArifHussain
 *
 */
public class LegatoL2cpEecController  extends UnimgrDataTreeChangeListener<Profile> {
	
	private static final Logger LOG = LoggerFactory.getLogger(LegatoL2cpEecController.class);
	
	private static final InstanceIdentifier<Profile> PROFILE_ID = InstanceIdentifier
			.builder(MefGlobal.class).child(L2cpEecProfiles.class).child(Profile.class).build();
	
	
	private static final InstanceIdentifier<L2cpEecProfiles> L2CP_EEC_PROFILES_ID_OPERATIONAL = InstanceIdentifier
			.builder(MefGlobal.class).child(L2cpEecProfiles.class).build();
			
	
	private ListenerRegistration<LegatoL2cpEecController> dataTreeChangeListenerRegistration;
	

	public LegatoL2cpEecController(DataBroker dataBroker) {
		super(dataBroker);
		registerListener();
	}

	public void registerListener() {
		LOG.info("Initializing LegatoL2cpEecController:init() ");

		dataTreeChangeListenerRegistration = dataBroker.registerDataTreeChangeListener(
				new DataTreeIdentifier<Profile>(LogicalDatastoreType.CONFIGURATION, PROFILE_ID), this);

	}
	
	
	@Override
	public void add(DataTreeModification<Profile> newDataObject) {
		if(newDataObject.getRootNode() != null && newDataObject.getRootPath() != null ){
			LOG.info( "ClassName :: LegatoL2cpEecController, Method:: add(), Message:: Node Added  " + newDataObject.getRootNode().getIdentifier());
			
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
			tx.merge(LogicalDatastoreType.OPERATIONAL, L2CP_EEC_PROFILES_ID_OPERATIONAL,
					new L2cpEecProfilesBuilder().setProfile(proList).build());

			tx.submit().checkedGet();
		} catch (org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException e) {
			LOG.error("Error in addNode(). Err: ", e);
		}

		LOG.info(" ********** END addNode() ****************** ");

	}

	
	@SuppressWarnings("unchecked")
	@Override
	public void update(DataTreeModification<Profile> modifiedDataObject) {
		if (modifiedDataObject.getRootNode() != null && modifiedDataObject.getRootPath() != null) {
			LOG.info( "ClassName :: LegatoL2cpEecController, Method:: update(), Message:: Node modified  " + modifiedDataObject.getRootNode().getIdentifier());

			Profile profile = modifiedDataObject.getRootNode().getDataAfter();
			
			try {
				assert profile != null;
				Optional<Profile> OptionalProfile = (Optional<Profile>) LegatoUtils.readProfile(LegatoConstants.l2CP_EEC_PROFILES, dataBroker, LogicalDatastoreType.CONFIGURATION,
						InstanceIdentifier.create(MefGlobal.class).child(L2cpEecProfiles.class).child(Profile.class,
								new ProfileKey(profile.getId())));

				if (OptionalProfile.isPresent()) {

					LegatoUtils.deleteFromOperationalDB(InstanceIdentifier.create(MefGlobal.class).child(L2cpEecProfiles.class)
							.child(Profile.class, new ProfileKey(profile.getId())), dataBroker);

					UpdateProfileOnOperationalDB(OptionalProfile.get());
				}

			} catch (Exception ex) {
				LOG.error("error: ", ex);
			}
		}
	}
	

	@SuppressWarnings("deprecation")
	private void UpdateProfileOnOperationalDB(Profile profileObj) {
		List<Profile> proList = new ArrayList<Profile>();
		proList.add(profileObj);

		WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
		tx.merge(LogicalDatastoreType.OPERATIONAL, L2CP_EEC_PROFILES_ID_OPERATIONAL, new L2cpEecProfilesBuilder().setProfile(proList).build());

		try {
			tx.submit().checkedGet();
		} catch (org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException e) {
			LOG.error("Error in UpdateProfileOnOperationalDB(). Err: ", e);
		}

	}
	
	
	@Override
	public void remove(DataTreeModification<Profile> removedDataObject) {
		if (removedDataObject.getRootNode() != null && removedDataObject.getRootPath() != null) {
			LOG.info( "ClassName :: LegatoL2cpEecController, Method:: remove(), Message:: Node removed  " + removedDataObject.getRootNode().getIdentifier());
			
			Profile profileObj = removedDataObject.getRootNode().getDataBefore();
			try {
				assert profileObj != null;
				LegatoUtils.deleteFromOperationalDB(InstanceIdentifier.create(MefGlobal.class).child(L2cpEecProfiles.class)
						.child(Profile.class, new ProfileKey(profileObj.getId())), dataBroker);

			} catch (Exception ex) {
				LOG.error("error: ", ex);
			}
		}
		
	}
	

	@Override
	public void close() throws Exception {
		if (dataTreeChangeListenerRegistration != null) {
			dataTreeChangeListenerRegistration.close();
		}
	}


}
