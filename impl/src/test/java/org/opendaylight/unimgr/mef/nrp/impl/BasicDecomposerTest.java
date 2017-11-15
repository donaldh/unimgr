/*
 * Copyright (c) 2017 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.nrp.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.unimgr.mef.nrp.api.FailureResult;
import org.opendaylight.unimgr.mef.nrp.api.Subrequrest;
import org.opendaylight.unimgr.mef.nrp.impl.decomposer.BasicDecomposer;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapi.common.rev170712.ForwardingDirection;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapi.common.rev170712.OperationalState;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapi.common.rev170712.PortDirection;
import org.opendaylight.yangtools.yang.common.OperationFailedException;

import javax.sound.sampled.Port;

/**
 * @author bartosz.michalik@amartus.com
 */
public class BasicDecomposerTest extends AbstractTestWithTopo {
    private BasicDecomposer decomposer;

    @Before
    public void setUp() throws Exception {
        dataBroker = getDataBroker();
        new NrpInitializer(dataBroker).init();
        decomposer = new BasicDecomposer(dataBroker);

    }

    @Rule
    public ExpectedException expected = ExpectedException.none();

    @Test()
    public void emptyNodeInventoryTest() throws FailureResult {
        expected.expect(FailureResult.class);
        decomposer.decompose(Arrays.asList(ep("n1:1"), ep("n1:2")), null);
    }

    @Test
    public void singleNodeTest() throws FailureResult, OperationFailedException {
        //having
        ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();
        n(tx, "n1", "n1:1", "n1:2", "n1:3");
        n(tx, "n2", "n2:1", "n2:2", "n2:3");
        tx.submit().checkedGet();
        //when
        List<Subrequrest> decomposed = decomposer.decompose(Arrays.asList(ep("n1:1"), ep("n1:2")), null);

        assertEquals(1, decomposed.size());
    }

    @Test
    public void noPathTest() throws FailureResult, OperationFailedException {
        //having
        ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();
        n(tx, "n1", "n1:1", "n1:2", "n1:3");
        n(tx, "n2", "n2:1", "n2:2", "n2:3");
        tx.submit().checkedGet();
        //when
        List<Subrequrest> decomposed = decomposer.decompose(Arrays.asList(ep("n1:1"), ep("n2:2")), null);
        assertNull(decomposed);
    }

    @Test
    public void twoNodesTest() throws FailureResult, OperationFailedException {
        //having three nodes, but only two nodes connected
        ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();
        n(tx, "n1", "n1:1", "n1:2", "n1:3");
        n(tx, "n2", "n2:1", "n2:2", "n2:3");
        n(tx, "n3", "n3:1", "n3:2", "n3:3");
        l(tx, "n1", "n1:1", "n2", "n2:1", OperationalState.Enabled);
        l(tx, "n2", "n2:3", "n3", "n3:3", OperationalState.Enabled);
        tx.submit().checkedGet();
        //when
        List<Subrequrest> decomposed = decomposer.decompose(Arrays.asList(ep("n1:2"), ep("n2:2")), null);
        assertNotNull(decomposed);
        assertEquals(2, decomposed.size());
    }

    @Test
    public void twoNodesTestDirection() throws FailureResult, OperationFailedException {
        //having three nodes, but only two nodes connected, with directional links and ports
        ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();
        n(tx, true, "n1", Stream.of(new Pair("n1:1", PortDirection.Output)));
        n(tx, true, "n2", Stream.of(new Pair("n2:1", PortDirection.Output), new Pair ("n2:2", PortDirection.Input)));
        n(tx, true, "n3", Stream.of(new Pair("n3:1", PortDirection.Input)));
        l(tx, "n1", "n1:1", "n2", "n2:1", OperationalState.Enabled, ForwardingDirection.Bidirectional);
        tx.submit().checkedGet();
        //when
        List<Subrequrest> decomposed = decomposer.decompose(Arrays.asList(ep("n1:2"), ep("n2:2")), null);
        assertNotNull(decomposed);
        assertEquals(2, decomposed.size());
    }

    @Test
    public void threeNodesTest() throws FailureResult, OperationFailedException {
        //having
        ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();
        n(tx, "n1", "n1:1", "n1:2", "n1:3");
        n(tx, "n2", "n2:1", "n2:2", "n2:3");
        n(tx, "n3", "n3:1", "n3:2", "n3:3");
        l(tx, "n1", "n1:1", "n2", "n2:1", OperationalState.Enabled);
        l(tx, "n2", "n2:3", "n3", "n3:3", OperationalState.Enabled);
        tx.submit().checkedGet();
        //when
        List<Subrequrest> decomposed = decomposer.decompose(Arrays.asList(ep("n1:2"), ep("n3:2")), null);
        assertNotNull(decomposed);
        assertEquals(3, decomposed.size());
    }

    @Test
    public void threeNodesTestDirection() throws FailureResult, OperationFailedException {
        //having three nodes, but only two nodes connected, with directional links and ports
        ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();
        n(tx, true, "n1", Stream.of(new Pair("n1:1", PortDirection.Output), new Pair("n1:2", PortDirection.Input)));
        n(tx, true, "n2", Stream.of(new Pair("n2:1", PortDirection.Output), new Pair ("n2:2", PortDirection.Input)));
        n(tx, true, "n3", Stream.of(new Pair("n3:1", PortDirection.Input), new Pair("3:2", PortDirection.Output)));
        l(tx, "n1", "n1:1", "n2", "n2:1", OperationalState.Enabled, ForwardingDirection.Bidirectional);
        l(tx, "n2", "n2:2", "n3", "n3:1", OperationalState.Enabled, ForwardingDirection.Bidirectional);
        l(tx, "n3", "n3:2", "n1", "n1:2", OperationalState.Enabled, ForwardingDirection.Bidirectional);
        tx.submit().checkedGet();
        //when
        List<Subrequrest> decomposed = decomposer.decompose(Arrays.asList(ep("n1:2"), ep("n3:2")), null);
        assertNotNull(decomposed);
        assertEquals(2, decomposed.size());
    }

    @Test
    public void threeNodesDisabledLinkTest() throws FailureResult, OperationFailedException {
        //having
        ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();
        n(tx, "n1", "n1:1", "n1:2", "n1:3");
        n(tx, "n2", "n2:1", "n2:2", "n2:3");
        n(tx, "n3", "n3:1", "n3:2", "n3:3");
        l(tx, "n1", "n1:1", "n2", "n2:1", OperationalState.Disabled);
        l(tx, "n2", "n2:3", "n3", "n3:3", OperationalState.Enabled);
        tx.submit().checkedGet();
        //when
        List<Subrequrest> decomposed = decomposer.decompose(Arrays.asList(ep("n1:2"), ep("n3:2")), null);
        assertNull(decomposed);
    }


}
