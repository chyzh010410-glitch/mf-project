# 客服部署

可部署模块为 `customer-agent-app`。生产环境继续使用既有环境变量，例如 `SPRING_PROFILES_ACTIVE`、`MF_EP_BASE_URL`、`MF_DATACENTER_BASE_URL`、内部令牌、知识同步参数和模型密钥。

不要将令牌或模型密钥写入 Git、YAML、脚本或文档。

```powershell
mvn -pl customer-agent-app -am package
mvn -pl customer-agent-app -am spring-boot:run
```

服务端口为 `8092`。部署前应停止旧实例，确保新构建版本获得端口；跨服务业务回归由三方统一生产复验执行。
