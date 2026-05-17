# Ignition Agent Tools

Ignition Agent Tools is an Ignition 8.3 gateway module that exposes a small HTTP API for common tag automation workflows. It is designed for external scripts, agents, and operational tooling that need a controlled way to browse, read, write, create, or delete tags without building a full Ignition module of their own.

## Features

- Authenticated REST endpoints mounted under `/system/data/agent-tools/v1`
- Tag browse, read, write, upsert, and delete operations
- Request/response DTOs shared between modules
- Gateway-side audit logging and request IDs
- A zero-dependency Python client and CLI

## API Surface

- `GET /system/data/agent-tools/v1/health`
- `POST /system/data/agent-tools/v1/tags/browse`
- `POST /system/data/agent-tools/v1/tags/read`
- `POST /system/data/agent-tools/v1/tags/write`
- `POST /system/data/agent-tools/v1/tags/upsert`
- `POST /system/data/agent-tools/v1/tags/delete`

See [docs/api.md](docs/api.md) for request examples.

## Repository Layout

- `common`
  Shared request/response models, constants, and error types.
- `gateway`
  Gateway hook, route registration, authentication, audit logging, and tag service integration.
- `python-client`
  A small Python package and CLI for interacting with the gateway API.

## Requirements

- Java 17
- Ignition 8.3 SDK dependencies
- Gradle wrapper included in this repository
- Python 3.10+ for the optional client package

## Configuration

The module currently has no Gateway UI settings page. Configuration is loaded at startup from JVM properties or environment variables.

Supported settings:

- JVM property `agenttools.enabled`
- JVM property `agenttools.apiKey`
- JVM property `agenttools.apiKeyHash`
- JVM property `agenttools.maxBatchSize`
- JVM property `agenttools.maxDeleteCount`
- Environment variable `IGNITION_AGENT_TOOLS_ENABLED`
- Environment variable `IGNITION_AGENT_TOOLS_API_KEY`
- Environment variable `IGNITION_AGENT_TOOLS_API_KEY_HASH`
- Environment variable `IGNITION_AGENT_TOOLS_MAX_BATCH_SIZE`
- Environment variable `IGNITION_AGENT_TOOLS_MAX_DELETE_COUNT`

For production use, prefer `apiKeyHash` over a raw API key so the gateway is not configured with a long-lived cleartext secret.

## Build

```powershell
./gradlew.bat clean build
```

This produces the module build output with signing disabled in local builds.

## Python Client

Install from the local package directory:

```powershell
cd python-client
python -m pip install .
```

Example:

```python
from ignition_agent_tools import IgnitionAgentToolsClient

client = IgnitionAgentToolsClient(
    base_url="http://localhost:8088/system/data/agent-tools/v1",
    api_key="your-secret",
)

print(client.health())
```

CLI:

```powershell
ignition-agent-tools --base-url http://localhost:8088/system/data/agent-tools/v1 --api-key your-secret health
```

## Security Notes

- Every endpoint requires `Authorization: Bearer <api-key>`.
- The module returns a request ID for traceability and emits audit logs per operation.
- This repository does not include runtime credentials or gateway-specific deployment secrets.

## Current Limitations

- Only existing tag providers are supported
- Tag groups must already exist before being referenced
- No Designer UI is included
- No UDT definition management is included
- `overrides` are currently handled as root-level property replacement rather than deep member patching

## License

Apache-2.0
