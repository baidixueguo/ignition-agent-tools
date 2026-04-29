package com.codex.ignition.agenttools.gateway;

import com.inductiveautomation.ignition.common.licensing.LicenseState;
import com.inductiveautomation.ignition.gateway.dataroutes.RouteGroup;
import com.inductiveautomation.ignition.gateway.model.AbstractGatewayModuleHook;
import com.inductiveautomation.ignition.gateway.model.GatewayContext;
import java.util.Optional;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class AgentToolsGatewayHook extends AbstractGatewayModuleHook {
    private static final Logger LOGGER = LogManager.getLogger(AgentToolsGatewayHook.class);

    private AgentToolsRoutes routes;

    @Override
    public void setup(GatewayContext context) {
        AgentToolsConfig config = AgentToolsConfig.load();
        this.routes = new AgentToolsRoutes(config, new GatewayTagService(context), new AuditLogger());
        LOGGER.info("Ignition Agent Tools setup complete. Configured=" + config.isConfigured() + ", enabled=" + config.isEnabled());
    }

    @Override
    public void startup(LicenseState activationState) {
        LOGGER.info("Ignition Agent Tools startup complete.");
    }

    @Override
    public void shutdown() {
        LOGGER.info("Ignition Agent Tools stopped.");
    }

    @Override
    public void mountRouteHandlers(RouteGroup routes) {
        this.routes.mount(routes);
    }

    @Override
    public Optional<String> getMountPathAlias() {
        return Optional.of(com.codex.ignition.agenttools.common.AgentToolsModule.MOUNT_ALIAS);
    }

    @Override
    public boolean isFreeModule() {
        return true;
    }
}
