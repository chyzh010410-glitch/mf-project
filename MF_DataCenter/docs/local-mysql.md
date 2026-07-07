# 本地 MySQL 配置

当前本机 MySQL 已验证可连接：

```text
host: 127.0.0.1
port: 3306
username: root
password: 123456
database: mf_datacenter
```

使用 `local` profile 前，需要确保 Windows 服务 `MySQL80` 处于 Running 状态。若服务停止，请在管理员权限终端或 Windows 服务管理器中启动它。

已创建数据中台独立库：

```sql
CREATE DATABASE IF NOT EXISTS mf_datacenter
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

已按 `datacenter-api/src/main/resources/db/schema.sql` 创建 V1 表：

```text
dc_ai_conversation_log
dc_ai_tool_call_log
dc_unresolved_question
dc_sample_candidate
dc_metric_snapshot
```

本地配置文件：

```text
datacenter-api/src/main/resources/application-local.yml
```

该文件包含本机数据库密码，已加入 `.gitignore`，不要提交到远程仓库。

启动本地 profile：

```bash
cd F:\20260518-xiangmu\MF_Project\MF_DataCenter\datacenter-api
mvn -DskipTests package
java -jar target\datacenter-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

## 当前说明

启用 `local` profile 时，AI 咨询、工具调用、未解决问题、样本候选已经写入 MySQL 表。默认 profile 仍保留 JSON fallback，方便无数据库环境下运行测试和演示。
