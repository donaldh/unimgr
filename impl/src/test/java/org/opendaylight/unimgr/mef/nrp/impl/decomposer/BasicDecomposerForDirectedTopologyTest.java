/*
 * Copyright (c) 2017 Cisco Systems Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.nrp.impl.decomposer;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.unimgr.mef.nrp.api.FailureResult;
import org.opendaylight.unimgr.mef.nrp.api.Subrequrest;
import org.opendaylight.unimgr.mef.nrp.impl.AbstractTestWithTopo;
import org.opendaylight.unimgr.mef.nrp.impl.NrpInitializer;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapi.common.rev170712.ForwardingDirection;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapi.common.rev170712.OperationalState;
import org.opendaylight.yang.gen.v1.urn.mef.yang.tapi.common.rev170712.PortDirection;
import org.opendaylight.yangtools.yang.common.OperationFailedException;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.Assert.*;

/**
 * @author bartosz.michalik@amartus.com
 */
public class BasicDecomposerForDirectedTopologyTest extends AbstractTestWithTopo {

    private BasicDecomposer decomposer;

    @Before
    public void setUp() throws Exception {
        dataBroker = getDataBroker();
        new NrpInitializer(dataBroker).init();
        decomposer = new BasicDecomposer(dataBroker);

    }

    @Rule
    public ExpectedException expected = ExpectedException.none();

    @Test
    public void twoNodesTestDirection() throws FailureResult, OperationFailedException {
        ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();
        n(tx, true, "n1", Stream.of(pI("n1:1"), pO("n1:2")));
        n(tx, true, "n2", Stream.of(pO("n2:1"), pI("n2:2")));
        n(tx, true, "n3", Stream.of(pI("n3:1")));
        l(tx, "n1", "n1:1", "n2", "n2:1", OperationalState.Enabled, ForwardingDirection.Bidirectional);
        tx.submit().checkedGet();
        //when
        List<Subrequrest> decomposed = decomposer.decompose(Arrays.asList(ep("n1:2", PortDirection.Output), ep("n2:2", PortDirection.Input)), null);
        assertNotNull(decomposed);
        assertEquals(2, decomposed.size());
    }

    @Test
    public void threeNodesTestAll() throws FailureResult, OperationFailedException {
        //having
        threeNodesTopo();
        //when
        List<Subrequrest> decomposed = decomposer.decompose(Arrays.asList(ep("n1:2", PortDirection.Output), ep("n3:3", PortDirection.Input)), null);
        assertNotNull(decomposed);
        assertEquals(3, decomposed.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void threeNodesTestIncompatible() throws FailureResult, OperationFailedException {
        //having
        threeNodesTopo();
        //when
        decomposer.decompose(Arrays.asList(ep("n1:2", PortDirection.Input), ep("n3:3", PortDirection.Output)), null);
        fail();
    }

    @Test
    public void fourNodesTestThreeSelected() throws FailureResult, OperationFailedException {
        //having
        fourNodesTopo();

        //when
        List<Subrequrest> decomposed = decomposer.decompose(Arrays.asList(ep("n2:2"), ep("n3:2")), null);
        assertNotNull(decomposed);
        assertEquals(3, decomposed.size());
    }


    @Test
    public void fourNodesTestNone() throws FailureResult, OperationFailedException {
        //having
        fourNodesTopo();
        //when
        List<Subrequrest> decomposed = decomposer.decompose(Arrays.asList(ep("n2:1", PortDirection.Input), ep("n1:1")), null);
        assertNull(decomposed);

    }

    @Test
    public void fourTestPartialPath() throws FailureResult {
        //having
        fourNodesTopo();

        //when
        List<Subrequrest> decomposed = decomposer.decompose(Arrays.asList(ep("n1:1", PortDirection.Output), ep("n2:1"), ep("n4:1")), null);
        assertNull(decomposed);
    }

    @Test
    public void fourTestSingleSink() throws FailureResult {
        //having
        fourNodesTopo();

        //when
        List<Subrequrest> decomposed = decomposer.decompose(Arrays.asList(ep("n1:1", PortDirection.Input), ep("n2:1", PortDirection.Input), ep("n4:1", PortDirection.Output)),
                null);
        assertNotNull(decomposed);
    }



    @Test
    public void fiveNodesTestAll() throws FailureResult, OperationFailedException {
        //having
        fiveNodesTopo();

        //when
        List<Subrequrest> decomposed = decomposer.decompose(Arrays.asList(ep("n1:2"), ep("n5:3")), null);
        assertNotNull(decomposed);
        assertEquals(5, decomposed.size());
    }

    @Test
    public void fiveNodesTestDirected() throws FailureResult {
        //having
        fiveNodesTopo();
        //when
        List<Subrequrest> decomposed = decomposer.decompose(Arrays.asList(ep("n1:2", PortDirection.Output), ep("n5:3", PortDirection.Input)), null);
        assertNotNull(decomposed);
        assertEquals(3, decomposed.size());
    }


    /*

    n2--(1)-->--(3)--n1
    n3--(1)-->--(2)--n2

     */
    private  void threeNodesTopo() {
        ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();
        n(tx, true, "n1", Stream.of(pI("n1:1"), pO("n1:2"), pI("n1:3")));
        n(tx, true, "n2", Stream.of(pO("n2:1"), pI("n2:2")));
        n(tx, true, "n3", Stream.of(pO("n3:1"), pO("n3:2"), pI("n3:3")));
        l(tx, "n1", "n1:3", "n2", "n2:1", OperationalState.Enabled, ForwardingDirection.Bidirectional);
        l(tx, "n2", "n2:2", "n3", "n3:1", OperationalState.Enabled, ForwardingDirection.Bidirectional);
        try {
            tx.submit().checkedGet();
        } catch (TransactionCommitFailedException e) {
            e.printStackTrace();
        }
    }

    /*

    n1--(5)-->--(5)--n2
    n1--(4)-->--(4)--n4
    n2--(3)-->--(3)--n4
    n3--(3)-->--(4)--n2
    n4--(5)-->--(5)--n3

     */
    private  void fourNodesTopo() {
        ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();
        n(tx, true, "n1", Stream.of(pB("n1:1"), pB("n1:2"), pI("n1:3"), pO("n1:4"), pO("n1:5")));
        n(tx, true, "n2", Stream.of(pB("n2:1"), pB("n2:2"), pO("n2:3"), pI("n2:4"), pI("n2:5")));
        n(tx, true, "n3", Stream.of(pB("n3:1"), pB("n3:2"), pO("n3:3"), pO("n3:4"), pI("n3:5")));
        n(tx, true, "n4", Stream.of(pB("n4:1"), pB("n4:2"), pI("n4:3"), pI("n4:4"), pO("n4:5")));
        l(tx, "n1", "n1:5", "n2", "n2:5", OperationalState.Enabled, ForwardingDirection.Unidirectional);
        l(tx, "n1", "n1:4", "n4", "n4:4", OperationalState.Enabled, ForwardingDirection.Unidirectional);
        l(tx, "n2", "n2:3", "n4", "n4:3", OperationalState.Enabled, ForwardingDirection.Unidirectional);
        l(tx, "n3", "n3:4", "n2", "n2:4", OperationalState.Enabled, ForwardingDirection.Unidirectional);
        l(tx, "n4", "n4:5", "n3", "n3:5", OperationalState.Enabled, ForwardingDirection.Unidirectional);
        try {
            tx.submit().checkedGet();
        } catch (TransactionCommitFailedException e) {
            e.printStackTrace();
        }
    }

    /*

    n1--(4)-->--(4)--n2
    n1--(3)-->--(1)--n4
    n2--(3)-->--(4)--n3
    n3--(1)-->--(1)--n1
    n3--(3)-->--(1)--n5
    n5--(4)-->--(2)--n4

     */
    private  void fiveNodesTopo() {
        ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();
        n(tx, true, "n1", Stream.of(pI("n1:1"), pB("n1:2"), pI("n1:3"), pO("n1:4")));
        n(tx, true, "n2", Stream.of(pI("n2:1"), pB("n2:2"), pO("n2:3"), pI("n2:4")));
        n(tx, true, "n3", Stream.of(pO("n3:1"), pB("n3:2"), pO("n3:3"), pI("n3:4")));
        n(tx, true, "n4", Stream.of(pO("n4:1"), pI("n4:2"), pB("n4:3"), pB("n4:4")));
        n(tx, true, "n5", Stream.of(pI("n5:1"), pB("n5:2"), pB("n5:3"), pO("n5:4")));
        l(tx, "n1", "n1:4", "n2", "n2:4", OperationalState.Enabled, ForwardingDirection.Bidirectional);
        l(tx, "n2", "n2:3", "n3", "n3:4", OperationalState.Enabled, ForwardingDirection.Bidirectional);
        l(tx, "n3", "n3:1", "n1", "n1:1", OperationalState.Enabled, ForwardingDirection.Bidirectional);
        l(tx, "n3", "n3:3", "n5", "n5:1", OperationalState.Enabled, ForwardingDirection.Bidirectional);
        l(tx, "n4", "n4:1", "n1", "n1:3", OperationalState.Enabled, ForwardingDirection.Bidirectional);
        l(tx, "n5", "n5:4", "n4", "n4:2", OperationalState.Enabled, ForwardingDirection.Bidirectional);
        try {
            tx.submit().checkedGet();
        } catch (TransactionCommitFailedException e) {
            e.printStackTrace();
        }
    }


    private AbstractTestWithTopo.Pair pO(String id) {
        return new Pair(id, PortDirection.Output);
    }

    private AbstractTestWithTopo.Pair pI(String id) {
        return new Pair(id, PortDirection.Input);
    }

    private AbstractTestWithTopo.Pair pB(String id) {
        return new Pair(id, PortDirection.Bidirectional);
    }


}