# MF_Project 项目进度与联调使用说明

更新时间：2026-07-06  
适用对象：项目负责人、后续联调负责人、部署/集成负责人

## 1. 项目总览

`F:\20260518-xiangmu\MF_Project` 当前是一个多项目组合仓库，核心目标是围绕 MF 品牌形成电商业务、数据中台、官网展示、AI/宠物辅助能力等多个子系统。

当前主要目录如下：

| 目录 | 作用 | 当前状态 |
| --- | --- | --- |
| `MF_EP` | 电商平台主业务系统，包含后端、管理端、客户端、商家端 | 已具备本地运行与前端构建能力，正在做视觉统一和品牌接入 |
| `MF_DataCenter` | 数据中台，用于读取 MF_EP 业务库并形成指标、质量、治理视图 | 已具备本地运行脚本、健康检查脚本、生产打包脚本 |
| `MF_Website` | 品牌官网/展示站 | 已有设计方针和接入 MF_Pet 的说明文档 |
| `MF_Pet` | MF 宠物/陪伴类前端或互动模块 | 已有开发任务文档和复刻执行说明 |
| `MF_Logo` | 官方 logo 源文件目录 | `logo.png` 已作为官方 logo 源文件使用 |
| `Folder_description` | 面向负责人/协作者的说明文档目录 | 本文档和 logo 规范等都放在这里 |

## 2. 当前完成进度

### 2.1 MF_EP 电商平台

项目路径：

```text
F:\20260518-xiangmu\MF_Project\MF_EP\projects\mf
```

MF_EP 当前包含：

| 模块 | 路径 | 说明 |
| --- | --- | --- |
| 后端 | `mf-fertilizer` | Spring Boot 多模块后端，主服务端口为 `8080` |
| 管理端 | `mf-frontend` | 平台后台管理端，Vite + Vue + Element Plus |
| 客户端 | `mf-frontend-client` | C 端用户商城/内容/订单等页面 |
| 商家端 | `mf-frontend-merchant` | 商家登录、商品、订单、店铺资料等管理页面 |
| AI 客服服务 | `MF_AgentService` | 独立 Agent 服务；MF_EP AI 客服通过 HTTP API 接入 |

已完成事项：

1. 后端本地运行依赖已确认：MySQL 使用 `fertilizer` 库，Redis 使用 `127.0.0.1:6379`，密码 `123456`。
2. 管理端 `mf-frontend` 已优先对齐 `MF_DataCenter/datacenter-web` 的中台视觉风格。
3. 客户端 `mf-frontend-client` 和商家端 `mf-frontend-merchant` 已接入官方 logo。
4. 客户端和商家端登录页已调整为与管理端一致的左右分栏登录布局。
5. 客户端 AI 客服入口 `/ai` 已改为调用 `MF_AgentService` 的 `/api/agent/chat`，不再调用 MF_EP 内部 `/client/ai/chat`。
6. 旧 `MF_EP/projects/mf/mf-ai` 目录已删除，不再作为启动、联调、验收或部署范围。
7. 客户端、商家端构建已通过。
8. 官方 logo 使用规范已写入：

```text
F:\20260518-xiangmu\MF_Project\Folder_description\MF_Logo_官方Logo使用规范.md
```

已验证构建：

```powershell
cd F:\20260518-xiangmu\MF_Project\MF_EP\projects\mf\mf-frontend-client
npm run build

cd F:\20260518-xiangmu\MF_Project\MF_EP\projects\mf\mf-frontend-merchant
npm run build
```

构建结果：通过。  
备注：Vite 输出中存在 VueUse PURE 注释和大 chunk 警告，属于非阻断警告。

AI 客服新边界：

```text
MF_EP 客户端 /ai
  -> /agent-api/api/agent/chat
  -> MF_AgentService: http://localhost:8092
```

AgentService 详细接口见：

```text
F:\20260518-xiangmu\MF_Project\Folder_description\MF_AgentService_MF_EP_AI客服对接接口文档.md
```

### 2.2 MF_DataCenter 数据中台

项目路径：

```text
F:\20260518-xiangmu\MF_Project\MF_DataCenter
```

当前定位：

MF_DataCenter 是连接 MF_EP 业务库的“数据中台”。它不直接替代 MF_EP 的业务后台，而是从 MF_EP 的 `fertilizer` 数据库读取业务数据，再写入/维护自己的 `mf_datacenter` 数据库，用于指标、数据质量、来源契约、治理状态等中台能力。

已完成事项：

1. 已有本地启动脚本：

```powershell
cd F:\20260518-xiangmu\MF_Project\MF_DataCenter
.\scripts\start-local.ps1
```

2. 已有健康检查脚本：

```powershell
.\scripts\check-health.ps1
```

3. 已有生产打包脚本：

```powershell
.\scripts\package-prod.ps1
```

4. 当前 DataCenter 关注的数据库关系：

| 数据库 | 所属系统 | 用法 |
| --- | --- | --- |
| `fertilizer` | MF_EP | 业务源库，DataCenter 只读 |
| `mf_datacenter` | MF_DataCenter | 中台自有库，保存指标、快照、治理、质量结果 |

本地访问地址：

| 服务 | 地址 |
| --- | --- |
| DataCenter 后端健康检查 | `http://127.0.0.1:8091/api/system/status` |
| DataCenter 前端 | `http://127.0.0.1:5176` |

### 2.3 品牌与视觉统一

官方 logo 源文件：

```text
F:\20260518-xiangmu\MF_Project\MF_Logo\logo.png
```

已接入位置：

```text
MF_EP\projects\mf\mf-frontend-client\src\assets\brand\logo.png
MF_EP\projects\mf\mf-frontend-merchant\src\assets\brand\logo.png
```

建议后续统一策略：

1. 所有前端系统统一从各自 `src/assets/brand/logo.png` 引入 logo。
2. 不要在业务页面随意拉伸、变色、裁切 logo。
3. 管理端、商家端、数据中台优先保持“中台后台”风格：克制、专业、浅绿色背景、白色卡片、细边框、8px 圆角。
4. 客户端可保留一定商城亲和力，但登录页、导航、按钮、卡片底层风格应与 MF_EP 管理端保持一致。

## 3. 如何本地运行 MF_EP

### 3.1 前置依赖

建议负责人先确认本机具备：

1. JDK，可运行 Spring Boot。
2. Maven。
3. Node.js 和 npm。
4. MySQL，账号建议为 `root / 123456`。
5. Redis，端口 `6379`，密码 `123456`。

MF_EP 后端配置文件：

```text
F:\20260518-xiangmu\MF_Project\MF_EP\projects\mf\mf-fertilizer\fertilizer-api\src\main\resources\application.yml
```

关键配置：

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/fertilizer
    username: root
    password: 123456
  data:
    redis:
      host: localhost
      port: 6379
      password: 123456
```

### 3.2 启动 Redis

如果本机 Redis 安装在 `E:\Program Files\Redis`，可以使用：

```powershell
& "E:\Program Files\Redis\redis-server.exe" --port 6379 --requirepass 123456
```

验证：

```powershell
& "E:\Program Files\Redis\redis-cli.exe" -a 123456 ping
```

返回 `PONG` 即可。

### 3.3 构建后端

```powershell
cd F:\20260518-xiangmu\MF_Project\MF_EP\projects\mf\mf-fertilizer
mvn clean package -DskipTests
```

### 3.4 启动后端

优先尝试：

```powershell
cd F:\20260518-xiangmu\MF_Project\MF_EP\projects\mf\mf-fertilizer
mvn -pl fertilizer-api -am spring-boot:run
```

如遇到本地 classpath 问题，可改用手动 classpath 启动方式。基本思路是：

```powershell
cd F:\20260518-xiangmu\MF_Project\MF_EP\projects\mf\mf-fertilizer
mvn -pl fertilizer-api dependency:build-classpath -Dmdep.outputFile=target/runtime-classpath.txt -Dmdep.includeScope=runtime
```

然后用 `fertilizer-api/target/classes`、`fertilizer-core/target/classes`、`fertilizer-common/target/classes` 加上 `target/runtime-classpath.txt` 中的依赖，启动：

```text
com.mf.fertilizer.FertilizerApplication
```

后端启动成功后，MF_EP API 端口为：

```text
http://127.0.0.1:8080
```

### 3.5 启动管理端

```powershell
cd F:\20260518-xiangmu\MF_Project\MF_EP\projects\mf\mf-frontend
npm install
npm run dev
```

通常访问：

```text
http://localhost:5173
```

默认管理员账号此前已验证：

```text
账号：admin
密码：admin123
```

如果登录失败，优先检查：

1. 后端 `8080` 是否启动。
2. Redis 是否启动并使用密码 `123456`。
3. MySQL 中 `fertilizer` 库是否存在并完成初始化。

### 3.6 启动客户端

```powershell
cd F:\20260518-xiangmu\MF_Project\MF_EP\projects\mf\mf-frontend-client
npm install
npm run dev
```

如 `5173` 已被管理端占用，Vite 会自动选择下一个可用端口，终端会显示实际地址。

### 3.7 启动商家端

```powershell
cd F:\20260518-xiangmu\MF_Project\MF_EP\projects\mf\mf-frontend-merchant
npm install
npm run dev
```

商家端用于商家登录、商品管理、订单管理、店铺资料等。

## 4. 如何本地运行 MF_DataCenter

### 4.1 启动

```powershell
cd F:\20260518-xiangmu\MF_Project\MF_DataCenter
.\scripts\start-local.ps1
```

启动后访问：

```text
http://127.0.0.1:5176
```

后端健康检查：

```text
http://127.0.0.1:8091/api/system/status
```

### 4.2 健康检查

```powershell
cd F:\20260518-xiangmu\MF_Project\MF_DataCenter
.\scripts\check-health.ps1
```

负责人重点看：

1. API 是否正常。
2. Web 是否可访问。
3. `fertilizer` 源库契约是否通过。
4. `mf_datacenter` 自有数据是否正常。
5. 指标、数据质量、治理状态是否可刷新。

### 4.3 停止

```powershell
cd F:\20260518-xiangmu\MF_Project\MF_DataCenter
.\scripts\stop-local.ps1
```

## 5. MF_EP 与 MF_DataCenter 的连接方式

推荐负责人按下面方式理解两个系统的关系：

```text
MF_EP
  负责真实电商业务：
  用户、商品、商家、订单、支付模拟、内容、后台配置等
  数据写入 fertilizer 数据库

MF_DataCenter
  负责数据治理和运营分析：
  从 fertilizer 只读抽取数据
  将指标、质量、治理结果写入 mf_datacenter 数据库
```

联调顺序建议：

1. 先启动 MySQL，确认存在 `fertilizer` 和 `mf_datacenter` 两个库。
2. 启动 Redis。
3. 启动 MF_EP 后端 `8080`。
4. 启动 MF_EP 管理端，确认 `admin / admin123` 可登录。
5. 启动 MF_EP 客户端和商家端，确认页面可打开。
6. 启动 MF_DataCenter，运行健康检查。
7. 在 DataCenter 中确认源库契约、指标快照、数据质量页面可用。

关键原则：

1. MF_EP 负责写业务数据。
2. MF_DataCenter 只读 MF_EP 的 `fertilizer` 库。
3. MF_DataCenter 只写自己的 `mf_datacenter` 库。
4. 不要让 DataCenter 直接修改电商业务表。

## 6. 当前视觉统一进度

已完成：

1. 管理端 `mf-frontend` 已调整为接近 `MF_DataCenter/datacenter-web` 的中台视觉。
2. 客户端 `mf-frontend-client` 已同步主题变量、卡片、按钮、输入框、表格基础风格。
3. 商家端 `mf-frontend-merchant` 已同步主题变量、卡片、按钮、输入框、表格基础风格。
4. 客户端登录页已改为管理端同类左右布局，并保留登录/注册 Tab。
5. 商家端登录页已改为管理端同类左右布局。
6. 商家端入驻页已加入官方 logo 和统一卡片风格。
7. 客户端导航和商家端侧边栏已加入官方 logo。

仍建议负责人确认：

1. 官方 logo 在登录页左侧视觉区的尺寸是否满意。
2. 客户端是否需要更明显的商城氛围，还是继续完全靠近后台系统。
3. 商家端是否需要继续细化商品/订单/资料页的表格密度和筛选区样式。
4. 管理端、商家端、客户端是否要统一端口分配，避免都默认抢 `5173`。

## 7. 当前已知问题和注意事项

1. 部分历史 Vue 文件或文档在 PowerShell 终端中会显示乱码。这通常是终端编码显示问题，不代表页面一定损坏。修改中文文案前要谨慎。
2. MF_EP 后端本地运行依赖 Redis。Redis 未启动时，登录可能失败。
3. 如果 `http://localhost:5173` 看不了，不一定是账号问题，可能是前端服务没启动、端口被占用、后端没启动或代理接口失败。
4. `admin / admin123` 已在后端和 Redis 正常时验证过可用。
5. 商家端目录中存在 `index.html`、`vite.config.js`，它们对商家端 Vite 构建/运行是必要入口，不建议删除。
6. 构建后会产生 `dist` 和 Vite 缓存文件，提交代码前建议只提交源码和必要资产，不提交构建噪音。

## 8. 负责人接手建议

建议按以下顺序推进项目整体连接：

1. 先固定本地/测试环境端口表：

| 系统 | 建议端口 |
| --- | --- |
| MF_EP 后端 | `8080` |
| MF_EP 管理端 | `5173` 或固定为专用端口 |
| MF_EP 客户端 | 建议单独固定端口 |
| MF_EP 商家端 | 建议单独固定端口 |
| MF_DataCenter 后端 | `8091` |
| MF_DataCenter 前端 | `5176` |

2. 再固定数据库边界：

| 系统 | 数据库 |
| --- | --- |
| MF_EP | `fertilizer` |
| MF_DataCenter | `mf_datacenter`，同时只读 `fertilizer` |

3. 然后统一启动脚本：

建议后续在项目根目录或运维目录提供一个总控脚本，分阶段启动：

```text
1. 检查 MySQL
2. 检查 Redis
3. 启动 MF_EP 后端
4. 启动 MF_EP 各前端
5. 启动 MF_DataCenter
6. 运行 DataCenter 健康检查
```

4. 最后做验收清单：

| 验收项 | 判断标准 |
| --- | --- |
| 管理端登录 | `admin / admin123` 可登录 |
| 客户端打开 | 首页、登录页、商品页可访问 |
| 商家端打开 | 登录页、商家首页可访问 |
| 后端 API | `8080` 正常响应 |
| DataCenter | `5176` 可访问，`8091` 健康检查正常 |
| 数据连接 | DataCenter 能读取 `fertilizer`，写入 `mf_datacenter` |
| 品牌统一 | 登录页、导航、后台 layout 使用官方 logo |

## 9. 推荐先看的文档

负责人可以按顺序阅读：

```text
F:\20260518-xiangmu\MF_Project\Folder_description\MF_Logo_官方Logo使用规范.md
F:\20260518-xiangmu\MF_Project\Folder_description\MF_DataCenter_企业级架构文档.md
F:\20260518-xiangmu\MF_Project\Folder_description\MF_DataCenter_AI_Agent_开发文档.md
F:\20260518-xiangmu\MF_Project\MF_EP\projects\mf\项目总结.md
F:\20260518-xiangmu\MF_Project\MF_EP\projects\mf\manual-regression-checklist.md
```

## 10. 一句话交接结论

当前项目已经具备“MF_EP 电商业务系统 + MF_DataCenter 数据中台”的本地联调基础：MF_EP 负责电商业务和 `fertilizer` 数据库，MF_DataCenter 负责读取业务库并形成中台治理/指标能力。下一步负责人应重点把端口、数据库、启动脚本和健康检查串成统一流程，并继续把三个 MF_EP 前端的视觉细节收敛到同一套品牌规范下。
