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
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.BwpFlowParameterProfiles;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.CosProfiles;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.CosProfilesBuilder;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.cos.profiles.Profile;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.cos.profiles.ProfileKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
//import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.color.mapping.profiles.ProfileKey;;
public class LegatoCosProfileController extends UnimgrDataTreeChangeListener<Profile>{

	public LegatoCosProfileController(DataBroker dataBroker) {
		super(dataBroker);
		registerListener();
		// TODO Auto-generated constructor stub
	}

	private static final Logger LOG = LoggerFactory
			.getLogger(LegatoCosProfileController.class);


	private static final InstanceIdentifier<Profile> PROFILE_IID = InstanceIdentifier
			.builder(MefGlobal.class).child(CosProfiles.class).child(Profile.class).build();
	
	
	private ListenerRegistration<LegatoCosProfileController> dataTreeChangeListenerRegistration;
	
	
	public void registerListener() {
		LOG.info("Initializing LegatoSlsProfileController:init() ");

		          
		dataTreeChangeListenerRegistration = dataBroker.registerDataTreeChangeListener(
						new DataTreeIdentifier<Profile>(
								LogicalDatastoreType.CONFIGURATION, PROFILE_IID),
								this);

	}
	
	
	
	
	@Override
	public void close() throws Exception {
		if (dataTreeChangeListenerRegistration != null) {
		dataTreeChangeListenerRegistration.close();
		}
	}

	
	
	@Override
	public void add(DataTreeModification<Profile> newDataObject) {
		// TODO Auto-generated method stub
		// TODO Auto-generated method stub
				if(newDataObject.getRootNode() != null && newDataObject.getRootPath() != null ){
					LOG.info( "  Node Added  " + newDataObject.getRootNode().getIdentifier());
					addToOperational(newDataObject.getRootNode().getDataAfter());
					
				}
				
				
				
		
	}

	
	public void addToOperational(Profile profiles)
	 {
		 LOG.info("=================Entered add to operational function================");
		List<Profile> cosProfiles = new ArrayList<Profile>() ;
		cosProfiles.add(profiles);
		
		CosProfilesBuilder cosProfilesBuilder =new CosProfilesBuilder();
		cosProfilesBuilder.setProfile(cosProfiles);
		
		CosProfiles cosProfilesBuilded = cosProfilesBuilder.build();
		 WriteTransaction addProfileTx = dataBroker.newWriteOnlyTransaction();
		 InstanceIdentifier<CosProfiles> profilesTx = InstanceIdentifier
					.create(MefGlobal.class).child(CosProfiles.class);
		 
		 addProfileTx.merge (LogicalDatastoreType.OPERATIONAL,profilesTx,cosProfilesBuilded);
		 try {
			 addProfileTx.submit().checkedGet();
			 }catch (org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException e) {
					LOG.error("Error in adding data to OperationalDB(). Err: ", e);
				}
		 
		 LOG.info("=================Data added  to operational datastore================");
		 
	 }
	 
	@Override
	public void remove(DataTreeModification<Profile> removedDataObject) {
		// TODO Auto-generated method stub
		if(removedDataObject.getRootNode() != null && removedDataObject.getRootPath() != null ){
			LOG.info( "  Node removed  " + removedDataObject.getRootNode().getIdentifier());
			
			deleteNode(removedDataObject.getRootNode().getDataBefore());
			
		}
		
	}
	
	private void deleteNode(Profile profileObj) {
		LOG.info(" inside deleteNode()");
		try {
			assert profileObj != null;
			removeFromOperational(InstanceIdentifier.create(MefGlobal.class).child(CosProfiles.class)
					.child(Profile.class, new ProfileKey(profileObj.getId())));
		} catch (Exception ex) {
			LOG.error("error: ", ex);
		}
		LOG.info(" ********* END deleteNode() ***************** ");
	}

	public void removeFromOperational(InstanceIdentifier<Profile> instanceIdentifier)
	{
		LOG.info("=================Entered remove from operational function================");
		
		/*InstanceIdentifier<Profile> profileIdentifier = InstanceIdentifier
				.create(MefGlobal.class).child(CosProfiles.class)
				.child(Profile.class,new ProfileKey(instanceIdentifier.getId()));
		*/
		WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
		tx.delete(LogicalDatastoreType.OPERATIONAL, instanceIdentifier);
		
		try {
			tx.submit().checkedGet();
		} catch (org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException e) {
			LOG.error("Error in deleteing data from OperationalDB(). Err: ", e);
		}

		LOG.info("=================Data removed from operational datastore================");
	}
	@Override
	public void update(DataTreeModification<Profile> modifiedDataObject) {
		// TODO Auto-generated method stub
		if(modifiedDataObject.getRootNode() != null && modifiedDataObject.getRootPath() != null ){
			LOG.info( "  Node modified  " + modifiedDataObject.getRootNode().getIdentifier());
			updateNode(modifiedDataObject.getRootNode().getDataAfter());
		}
		
	}

	@SuppressWarnings("unchecked")
	private void updateNode(Profile profile) {
		LOG.info(" inside updateNode()");
		try {
			assert profile != null;
			/*
			 * Optional<Profile> OptionalProfile = readProfile(dataBroker,
			 * LogicalDatastoreType.CONFIGURATION,
			 * InstanceIdentifier.create(MefGlobal.class).child(SlsProfiles.class).child(
			 * Profile.class, new ProfileKey(profile.getId())));
			 */ Optional<Profile> OptionalProfile = (Optional<Profile>) LegatoUtils.readProfile(
					LegatoConstants.COS_PROFILES, dataBroker, LogicalDatastoreType.CONFIGURATION,
					InstanceIdentifier.create(MefGlobal.class).child(CosProfiles.class).child(Profile.class,
							new ProfileKey(profile.getId())));
			if (OptionalProfile.isPresent()) {
				removeFromOperational(InstanceIdentifier.create(MefGlobal.class).child(CosProfiles.class)
						.child(Profile.class, new ProfileKey(profile.getId())));
				updateOperational(OptionalProfile.get());
			}
		} catch (Exception ex) {
			LOG.error("error: ", ex);
		}
		LOG.info(" ********* END updateNode() ***************** ");
	}	


	private void updateOperational(Profile profile) {
		// TODO Auto-generated method stub
		LOG.info("=================Entered update operational function================");
		List<Profile> cosProfiles = new ArrayList<Profile>() ;
		cosProfiles.add(profile);
		
		CosProfilesBuilder cosProfilesBuilder =new CosProfilesBuilder();
		cosProfilesBuilder.setProfile(cosProfiles);
		
		CosProfiles cosProfilesBuilded = cosProfilesBuilder.build();
		 WriteTransaction addProfileTx = dataBroker.newWriteOnlyTransaction();
		 InstanceIdentifier<CosProfiles> profilesTx = InstanceIdentifier
					.create(MefGlobal.class).child(CosProfiles.class);
		 
		 addProfileTx.merge (LogicalDatastoreType.OPERATIONAL,profilesTx,cosProfilesBuilded);
		 try {
		 addProfileTx.submit().checkedGet();
		 }catch (org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException e) {
				LOG.error("Error in updating data from OperationalDB(). Err: ", e);
			}
		 LOG.info("=================Data updated in operational datastore================");
	}
	
	//=====================================BWP Profiles==================================================================
	
	/*private static final InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.bwp.flow.parameter.profiles.Profile> BWP_PROFILE_IID = InstanceIdentifier
			.builder(MefGlobal.class).child(BwpFlowParameterProfiles.class).child(org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.bwp.flow.parameter.profiles.Profile.class).build();
	*/
	
	

}













