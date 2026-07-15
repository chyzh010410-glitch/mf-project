# MF_AgentService

苗丰智能客服的模块化单体工程。首期只交付客服 Agent；商家和运营助手在拥有独立业务能力、权限矩阵和评测集后再增加启动模块。

## 模块

```text
agent-contract      公共 DTO、工具结果和端口接口
agent-core          客服编排、40 类契约、会话、回答策略和提示词资源
agent-integrations  MF_EP、DataCenter、RAG、LLM 和内部工具适配
customer-agent-app  Spring Boot 启动模块、HTTP/SSE 接口和运行配置
deployment          启动脚本与部署说明
docs                架构、安全边界和客服范围文档
```

依赖方向：`customer-agent-app -> agent-core + agent-integrations -> agent-contract`。核心层只依赖受控端口，不直接调用 HTTP、RAG 或 LLM 实现。

## 构建与启动

```powershell
mvn test
mvn -pl customer-agent-app -am package
mvn -pl customer-agent-app -am spring-boot:run
```

服务继续监听 `8092`，保留 `/api/agent/chat`、`/api/agent/chat/stream`、知识同步管理接口以及现有生产环境变量名。

生产启动和环境变量说明见 [deployment/README.md](deployment/README.md)。
