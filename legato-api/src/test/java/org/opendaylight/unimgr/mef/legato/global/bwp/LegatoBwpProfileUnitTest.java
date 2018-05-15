/*
 * Copyright (c) 2018 Xoriant Corporation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.unimgr.mef.legato.global.bwp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.unimgr.mef.legato.LegatoBwpProfileController;
import org.opendaylight.unimgr.mef.legato.util.LegatoConstants;
import org.opendaylight.unimgr.mef.legato.util.LegatoUtils;
import org.opendaylight.unimgr.mef.legato.utils.Constants;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.MefGlobal;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.BwpFlowParameterProfiles;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.bwp.flow.parameter.profiles.Profile;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.global.rev171215.mef.global.bwp.flow.parameter.profiles.ProfileKey;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.types.rev171215.Identifier1024;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;


@SuppressWarnings("deprecation")
@RunWith(PowerMockRunner.class)
@PrepareForTest({LogicalDatastoreType.class})
public class LegatoBwpProfileUnitTest {

    @Mock
    private DataBroker dataBroker;
    @Mock
    private WriteTransaction transaction;
    @SuppressWarnings("rawtypes")
    @Mock
    private CheckedFuture checkedFuture;
    
    private LegatoBwpProfileController legatoBwpProfileController;

    @Before
    public void setUp() throws Exception {
        legatoBwpProfileController = mock(LegatoBwpProfileController.class, Mockito.CALLS_REAL_METHODS);
    }


    @SuppressWarnings("unchecked")
    @Test
    public void testBwpAddToOperationalDB() {
        BwpFlowParameterProfiles profile = mock(BwpFlowParameterProfiles.class);
        InstanceIdentifier<BwpFlowParameterProfiles> profilesTx =
                InstanceIdentifier.create(MefGlobal.class).child(BwpFlowParameterProfiles.class);

        when(dataBroker.newWriteOnlyTransaction()).thenReturn(transaction);
        doNothing().when(transaction).merge(any(LogicalDatastoreType.class),
                any(InstanceIdentifier.class), any(BwpFlowParameterProfiles.class));
        when(transaction.submit()).thenReturn(checkedFuture);
        LegatoUtils.addToOperationalDB(profile, profilesTx, dataBroker);
        verify(transaction).merge(any(LogicalDatastoreType.class), any(InstanceIdentifier.class),
                any(BwpFlowParameterProfiles.class));
        verify(transaction).submit();
    }


    @SuppressWarnings("unchecked")
    @Test
    public void testBwpUpdateFromOperationalDB() throws ReadFailedException {

        final InstanceIdentifier<Profile> PROFILE_ID =
                InstanceIdentifier.create(MefGlobal.class).child(BwpFlowParameterProfiles.class)
                        .child(Profile.class, new ProfileKey(new Identifier1024(Constants.ONE)));

        ReadOnlyTransaction readTransaction = mock(ReadOnlyTransaction.class);
        when(dataBroker.newReadOnlyTransaction()).thenReturn(readTransaction);
        CheckedFuture<Optional<Profile>, ReadFailedException> nodeFuture =
                mock(CheckedFuture.class);
        Optional<Profile> optProfile = mock(Optional.class);
        when(readTransaction.read(any(LogicalDatastoreType.class), any(InstanceIdentifier.class)))
                .thenReturn(nodeFuture);
        when(nodeFuture.checkedGet()).thenReturn(optProfile);
        Optional<Profile> expectedOpt =
                (Optional<Profile>) LegatoUtils.readProfile(LegatoConstants.BWP_PROFILES,
                        dataBroker, LogicalDatastoreType.CONFIGURATION, PROFILE_ID);
        verify(readTransaction).read(any(LogicalDatastoreType.class),
                any(InstanceIdentifier.class));
        assertNotNull(expectedOpt);
        assertEquals(expectedOpt, optProfile);

        when(dataBroker.newWriteOnlyTransaction()).thenReturn(transaction);
        doNothing().when(transaction).delete(any(LogicalDatastoreType.class),
                any(InstanceIdentifier.class));
        when(transaction.submit()).thenReturn(checkedFuture);
        assertEquals(true, LegatoUtils.deleteFromOperationalDB(PROFILE_ID, dataBroker));
        verify(transaction).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(transaction).submit();

        BwpFlowParameterProfiles bwpProfile = mock(BwpFlowParameterProfiles.class);

        InstanceIdentifier<BwpFlowParameterProfiles> profilesTx =
                InstanceIdentifier.create(MefGlobal.class).child(BwpFlowParameterProfiles.class);
        WriteTransaction transaction2 = Mockito.mock(WriteTransaction.class);
        when(dataBroker.newWriteOnlyTransaction()).thenReturn(transaction2);
        doNothing().when(transaction2).merge(any(LogicalDatastoreType.class),
                any(InstanceIdentifier.class), any(BwpFlowParameterProfiles.class));
        when(transaction2.submit()).thenReturn(checkedFuture);
        LegatoUtils.addToOperationalDB(bwpProfile, profilesTx, dataBroker);
        verify(transaction2).merge(any(LogicalDatastoreType.class), any(InstanceIdentifier.class),
                any(BwpFlowParameterProfiles.class));
        verify(transaction2).submit();
    }


    @SuppressWarnings("unchecked")
    @Test
    public void testBwpDeleteFromOperationalDB() {
        Profile profile = mock(Profile.class);
        final InstanceIdentifier<Profile> PROFILE_ID =
                InstanceIdentifier.create(MefGlobal.class).child(BwpFlowParameterProfiles.class)
                        .child(Profile.class, new ProfileKey(profile.getId()));

        when(dataBroker.newWriteOnlyTransaction()).thenReturn(transaction);
        doNothing().when(transaction).delete(any(LogicalDatastoreType.class),
                any(InstanceIdentifier.class));
        when(transaction.submit()).thenReturn(checkedFuture);
        assertEquals(true, LegatoUtils.deleteFromOperationalDB(PROFILE_ID, dataBroker));
        verify(transaction).delete(any(LogicalDatastoreType.class), any(InstanceIdentifier.class));
        verify(transaction).submit();

    }


}
