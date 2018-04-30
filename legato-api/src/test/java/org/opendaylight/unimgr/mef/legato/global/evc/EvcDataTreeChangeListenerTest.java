/*
 * Copyright (c) 2018 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.unimgr.mef.legato.global.evc;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.Collection;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification.ModificationType;
import org.opendaylight.unimgr.mef.legato.LegatoServiceController;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.yang.gen.v1.urn.mef.yang.mef.legato.services.rev171215.mef.services.carrier.ethernet.subscriber.services.Evc;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.ChildOf;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.PathArgument;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Arif.Hussain@Xoriant.Com
 *
 */
@RunWith(PowerMockRunner.class)
public class EvcDataTreeChangeListenerTest {

  private LegatoServiceController legatoServiceController;
  
  private static final Logger LOG = LoggerFactory
      .getLogger(EvcDataTreeChangeListenerTest.class);

  @Before
  public void setUp() throws Exception {
    legatoServiceController = mock(LegatoServiceController.class, Mockito.CALLS_REAL_METHODS);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testEvcServiceDataTreeChangeListener() {
    LOG.info("in side testEvcServiceDataTreeChangeListener() ");
    
    Collection<DataTreeModification<Evc>> collection = new ArrayList<DataTreeModification<Evc>>();
    DataTreeModification<Evc> evc = getDataTree(ModificationType.WRITE);
    collection.add(evc);
    evc = getDataTree(ModificationType.DELETE);
    collection.add(evc);
    evc = getDataTree(ModificationType.SUBTREE_MODIFIED);
    collection.add(evc);
    legatoServiceController.onDataTreeChanged(collection);
    verify(legatoServiceController, times(1)).add(any(DataTreeModification.class));
    verify(legatoServiceController, times(1)).remove(any(DataTreeModification.class));
    verify(legatoServiceController, times(1)).update(any(DataTreeModification.class));
  }
  
  
  private DataTreeModification<Evc> getDataTree(final ModificationType modificationType) {
    final DataObjectModification<Evc> evcDataObjModification = new DataObjectModification<Evc>() {
        @Override
        public Collection<DataObjectModification<? extends DataObject>> getModifiedChildren() {
            // TODO Auto-generated method stub
            return null;
        }
        @Override
        public <C extends Identifiable<K> & ChildOf<? super Evc>, K extends Identifier<C>> DataObjectModification<C> getModifiedChildListItem(
                Class<C> arg0, K arg1) {
            // TODO Auto-generated method stub
            return null;
        }
        @Override
        public <C extends ChildOf<? super Evc>> DataObjectModification<C> getModifiedChildContainer(Class<C> arg0) {
            // TODO Auto-generated method stub
            return null;
        }
        @Override
        public DataObjectModification<? extends DataObject> getModifiedChild(PathArgument arg0) {
            // TODO Auto-generated method stub
            return null;
        }
        @Override
        public <C extends Augmentation<Evc> & DataObject> DataObjectModification<C> getModifiedAugmentation(
                Class<C> arg0) {
            // TODO Auto-generated method stub
            return null;
        }
        @Override
        public ModificationType getModificationType() {
            return modificationType;
        }
        @Override
        public PathArgument getIdentifier() {
            // TODO Auto-generated method stub
            return null;
        }
        @Override
        public Class<Evc> getDataType() {
            // TODO Auto-generated method stub
            return null;
        }
        @Override
        public Evc getDataBefore() {
            // TODO Auto-generated method stub
            return null;
        }
        @Override
        public Evc getDataAfter() {
            // TODO Auto-generated method stub
            return null;
        }
    };
    DataTreeModification<Evc> modifiedEvc = new DataTreeModification<Evc>() {
        @Override
        public DataTreeIdentifier<Evc> getRootPath() {
            return null;
        }
        @Override
        public DataObjectModification<Evc> getRootNode() {
            return evcDataObjModification;
        }
    };
    return modifiedEvc;
}

}