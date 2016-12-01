package org.opendaylight.unimgr.mef.notification.impl;

import org.opendaylight.controller.sal.core.api.AbstractProvider;
import org.opendaylight.controller.sal.core.api.Broker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by root on 23.11.16.
 */
public class ExampleEventSourceBIProvider  extends AbstractProvider implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(ExampleEventSourceBIProvider.class);

    @Override
    public void onSessionInitiated(Broker.ProviderSession session) {
        LOG.info("ExampleEventSourceBProvider Session Initiated");
    }

    @Override
    public void close() throws Exception {
        LOG.info("ExampleEventSourceBProvider Closed");
    }
}
