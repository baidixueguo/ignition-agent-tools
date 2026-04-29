plugins {
    id("io.ia.sdk.modl") version "0.1.1"
}

val sdk_version by extra("8.3.0")

allprojects {
    version = "0.1.0"
}

ignitionModule {
    name.set("Ignition Agent Tools")
    fileName.set("Ignition-Agent-Tools")
    id.set("com.codex.ignition.agenttools.AgentTools")
    moduleVersion.set("${project.version}")
    moduleDescription.set("Gateway-side REST automation module for Ignition tag operations.")
    requiredIgnitionVersion.set("8.3.0")
    projectScopes.putAll(
        mapOf(
            ":common" to "G",
            ":gateway" to "G",
        )
    )
    moduleDependencies.set(emptyMap())
    hooks.putAll(
        mapOf(
            "com.codex.ignition.agenttools.gateway.AgentToolsGatewayHook" to "G",
        )
    )
    documentationFiles.from(project.file("docs"))
    documentationIndex.set("api.md")
    skipModlSigning.set(true)
}
