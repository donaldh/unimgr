package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.eventsource.uagent.rev150408;

import org.opendaylight.controller.hweventsource.uagent.NoopDOMConsumer;
import org.opendaylight.controller.hweventsource.uagent.Providers;
import org.opendaylight.controller.hweventsource.uagent.UserAgent;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationService;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class EventsourceUagentModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.eventsource.uagent.rev150408.AbstractEventsourceUagentModule {
    private static final Logger LOG = LoggerFactory.getLogger(EventsourceUagentModule.class);

    public EventsourceUagentModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public EventsourceUagentModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.eventsource.uagent.rev150408.EventsourceUagentModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final BindingAwareBroker.ProviderContext bindingCtx = getBrokerDependency().registerProvider(new Providers.BindingAware());
        final RpcProviderRegistry rpcRegistry = bindingCtx.getSALService(RpcProviderRegistry.class);
        final DOMNotificationService notifyService = getDomBrokerDependency()
                .registerConsumer(new NoopDOMConsumer())
                .getService(DOMNotificationService.class);
        final File outputFile = new File(getOutputFileName());
        UserAgent ua = UserAgent.create(notifyService,rpcRegistry, outputFile);

        if(ua != null){
            LOG.info("HweventsourceUagent has been initialized");
        } else {
            LOG.error("HweventsourceUagent has not been initialized");
        }
        return ua;
    }

}
