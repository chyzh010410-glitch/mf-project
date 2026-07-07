# mcp-server

`mcp-server` 是 `MF_AgentService` 内部的 MCP 工具服务模块。

当前阶段它不作为独立顶级项目。

## V1 工具分组

```text
product-tools
encyclopedia-tools
order-tools
merchant-tools
datacenter-tools
```

## 设计原则

- 工具接口要小而明确。
- 每个工具只做一件事。
- 工具返回结构化结果。
- 敏感操作必须做身份校验。
- 所有工具调用必须可追踪。

