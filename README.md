# Ignition Agent Tools

Ignition Agent Tools 是一个面向 Ignition 8.3 的网关侧模块，提供基于 HTTP 的标签自动化接口，方便外部程序或代理直接执行常见 tag 操作。

当前版本实现了：

- `GET /system/data/agent-tools/v1/health`
- `POST /system/data/agent-tools/v1/tags/browse`
- `POST /system/data/agent-tools/v1/tags/read`
- `POST /system/data/agent-tools/v1/tags/write`
- `POST /system/data/agent-tools/v1/tags/upsert`
- `POST /system/data/agent-tools/v1/tags/delete`

## 目录结构

- `common`
  Ignition 模块共享 DTO、错误码、请求对象。
- `gateway`
  Gateway hook、REST 路由、认证、审计和标签服务适配层。
- `python-client`
  一个零第三方依赖的 Python 客户端和 CLI。

## 配置

当前版本没有 Gateway UI 配置页，配置通过 JVM 参数或环境变量注入，并在模块启动时加载为单例。

支持的配置项：

- JVM 属性 `agenttools.enabled`
- JVM 属性 `agenttools.apiKey`
- JVM 属性 `agenttools.apiKeyHash`
- JVM 属性 `agenttools.maxBatchSize`
- JVM 属性 `agenttools.maxDeleteCount`
- 环境变量 `IGNITION_AGENT_TOOLS_ENABLED`
- 环境变量 `IGNITION_AGENT_TOOLS_API_KEY`
- 环境变量 `IGNITION_AGENT_TOOLS_API_KEY_HASH`
- 环境变量 `IGNITION_AGENT_TOOLS_MAX_BATCH_SIZE`
- 环境变量 `IGNITION_AGENT_TOOLS_MAX_DELETE_COUNT`

建议生产环境直接提供 `apiKeyHash`，避免长期保存明文密钥。当前实现支持通过 `apiKey` 启动自举，模块会在内存中转成 SHA-256 hash 后使用。

## 构建

```powershell
./gradlew.bat clean build
```

## Python 客户端

```python
from ignition_agent_tools import IgnitionAgentToolsClient

client = IgnitionAgentToolsClient(
    base_url="http://localhost:8088/system/data/agent-tools/v1",
    api_key="your-secret",
)

print(client.health())
```

CLI 示例：

```powershell
ignition-agent-tools --base-url http://localhost:8088/system/data/agent-tools/v1 --api-key your-secret health
```

## 当前限制

- 仅支持操作已存在的 Tag Provider
- 仅校验和引用已存在的 Tag Group
- 不包含 Designer UI
- 不包含 UDT Definition 管理
- `overrides` 当前按根级属性覆盖处理，不做复杂成员级补丁
