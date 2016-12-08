package org.opendaylight.unimgr.mef.notification.impl;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.controller.sal.core.api.AbstractProvider;
import org.opendaylight.controller.sal.core.api.Broker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by root on 08.12.16.
 */
public class Providers {
    private static final Logger LOGGER = LoggerFactory.getLogger(Providers.class);

    public static class BindingAware implements BindingAwareProvider, AutoCloseable {


        @Override
        public void onSessionInitiated(final BindingAwareBroker.ProviderContext session) {
            LOGGER.info("BindingAwareBroker.ProviderContext initialized");
        }

        @Override
        public void close() throws Exception {}
    }

    public static class BindingIndependent extends AbstractProvider implements AutoCloseable {

        @Override
        public void onSessionInitiated(final Broker.ProviderSession session) {
            LOGGER.info("Broker.ProviderSession initialized");
        }

        @Override
        public void close() throws Exception {}
    }

}
