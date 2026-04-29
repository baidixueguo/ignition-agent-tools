# Ignition Agent Tools API

网关模块挂载路径基于 Ignition data routes，因此当前实际访问前缀为：

`/system/data/agent-tools/v1`

所有请求都必须带上：

`Authorization: Bearer <api-key>`

## 统一响应

```json
{
  "success": true,
  "requestId": "db699fd2-8583-411b-9ba4-51a3fd1b2800",
  "data": {},
  "errors": []
}
```

## Health

`GET /health`

## Browse

`POST /tags/browse`

```json
{
  "provider": "default",
  "path": "",
  "recursive": false,
  "maxResults": 100
}
```

## Read

`POST /tags/read`

```json
{
  "provider": "default",
  "paths": [
    "FolderA/Tag1",
    "FolderA/Tag2"
  ]
}
```

## Write

`POST /tags/write`

```json
{
  "provider": "default",
  "writes": [
    {
      "path": "FolderA/Reset",
      "value": true
    }
  ]
}
```

## Upsert

`POST /tags/upsert`

```json
{
  "provider": "default",
  "dryRun": true,
  "continueOnError": false,
  "items": [
    {
      "path": "Line1",
      "kind": "folder"
    },
    {
      "path": "Line1/Temperature",
      "kind": "atomic",
      "valueSource": "memory",
      "dataType": "Float8",
      "value": 23.5,
      "tagGroup": "Default",
      "attributes": {
        "documentation": "Process temperature",
        "historyEnabled": true
      }
    },
    {
      "path": "Line1/Motor1",
      "kind": "udtInstance",
      "typePath": "_types_/Motors/StandardMotor",
      "parameters": {
        "MotorNumber": 1
      }
    }
  ]
}
```

原子标签支持：

- `valueSource=memory`
- `valueSource=opc`
- `valueSource=expression`

当前支持的通用属性：

- `enabled`
- `documentation`
- `readOnly`
- `tooltip`
- `historyEnabled`
- `historyProvider`
- `formatString`
- `engUnit`
- `persistValue`
- `executionMode`
- `executionRate`

## Delete

`POST /tags/delete`

```json
{
  "provider": "default",
  "paths": [
    "Line1/Temperature"
  ],
  "recursive": false
}
```
