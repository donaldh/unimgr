/*
 * Copyright (c) 2018 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.legato.global.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.unimgr.mef.legato.util.LegatoConstants;
import org.opendaylight.unimgr.mef.legato.util.LegatoUtils;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.MefGlobal;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.SlsProfiles;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.sls.profiles.Profile;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.sls.profiles.ProfileKey;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.legato.services.rev171215.MefServices;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.legato.services.rev171215.mef.services.CarrierEthernet;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.legato.services.rev171215.mef.services.carrier.ethernet.SubscriberServices;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.legato.services.rev171215.mef.services.carrier.ethernet.subscriber.services.Evc;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.legato.services.rev171215.mef.services.carrier.ethernet.subscriber.services.EvcKey;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.types.rev171215.EvcIdType;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.types.rev171215.Identifier1024;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import ch.qos.logback.core.Appender;

/**
 * @author Arif.Hussain@Xoriant.Com
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({LogicalDatastoreType.class, LegatoUtils.class, InstanceIdentifier.class})
public class LegatoUtilsTest {
  
  @Rule
  public final ExpectedException exception = ExpectedException.none();
  @Mock private DataBroker dataBroker;
  @Mock private WriteTransaction transaction;
  
  
  @SuppressWarnings({ "rawtypes", "deprecation" })
  @Mock private CheckedFuture checkedFuture;
  @SuppressWarnings("rawtypes")
  @Mock private Appender mockAppender;
  private ch.qos.logback.classic.Logger root;
  private static final EvcIdType EVC_NODE_ID = new EvcIdType("EVC1");
  
  
  @SuppressWarnings("unchecked")
  @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(LegatoUtils.class, Mockito.CALLS_REAL_METHODS);
        root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        when(mockAppender.getName()).thenReturn("MOCK");
        root.addAppender(mockAppender);
  
    }

  
    @SuppressWarnings({"unchecked", "deprecation"})
    @Test
    public void testReadEvc() throws ReadFailedException{
  
        InstanceIdentifier<Evc> EVC_IID = InstanceIdentifier.create(MefServices.class)
            .child(CarrierEthernet.class).child(SubscriberServices.class)
            .child(Evc.class, new EvcKey(new EvcIdType(EVC_NODE_ID)));
        
        ReadOnlyTransaction transaction = mock(ReadOnlyTransaction.class);
        when(dataBroker.newReadOnlyTransaction()).thenReturn(transaction);
        CheckedFuture<Optional<Evc>, ReadFailedException> nodeFuture = mock(CheckedFuture.class);
        Optional<Evc> optNode = mock(Optional.class);
        when(transaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(nodeFuture);
        when(nodeFuture.checkedGet()).thenReturn(optNode);
        Optional<Evc> expectedOpt = LegatoUtils.readEvc(dataBroker, LogicalDatastoreType.CONFIGURATION, EVC_IID);
        verify(transaction).read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        assertNotNull(expectedOpt);
        assertEquals(expectedOpt, optNode);
    }
    
    
    @SuppressWarnings({"unchecked", "deprecation"})
    @Test
    public void testReadProfiles() throws ReadFailedException {
       
        InstanceIdentifier<Profile> PROFILE_ID = InstanceIdentifier.create(MefGlobal.class)
            .child(SlsProfiles.class).child(Profile.class, new ProfileKey(new Identifier1024("1")));
    
        ReadOnlyTransaction transaction = mock(ReadOnlyTransaction.class);
        when(dataBroker.newReadOnlyTransaction()).thenReturn(transaction);
        CheckedFuture<Optional<Profile>, ReadFailedException> nodeFuture = mock(CheckedFuture.class);
        Optional<Profile> optNode = mock(Optional.class);
        when(transaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class))).thenReturn(nodeFuture);
        when(nodeFuture.checkedGet()).thenReturn(optNode);
        Optional<Profile> expectedOpt =
            (Optional<Profile>) LegatoUtils.readProfile(LegatoConstants.SLS_PROFILES, dataBroker,LogicalDatastoreType.CONFIGURATION, PROFILE_ID);
        verify(transaction).read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        assertNotNull(expectedOpt);
        assertEquals(expectedOpt, optNode);
      
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testDeleteFromOperationalDB() {
      InstanceIdentifier<Evc> EVC_IID = InstanceIdentifier.create(MefServices.class)
          .child(CarrierEthernet.class).child(SubscriberServices.class)
          .child(Evc.class, new EvcKey(new EvcIdType(EVC_NODE_ID)));
      
      when(dataBroker.newWriteOnlyTransaction()).thenReturn(transaction);
      doNothing().when(transaction).delete(any(LogicalDatastoreType.class),
          any(InstanceIdentifier.class));
      when(transaction.submit()).thenReturn(checkedFuture);
      assertEquals(true, LegatoUtils.deleteFromOperationalDB(EVC_IID, dataBroker));
      verify(transaction).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
      verify(transaction).submit();
    }
    
}
