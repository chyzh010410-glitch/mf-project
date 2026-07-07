# 苗丰 AI 微服务

基于 LangChain + FastAPI 的 RAG 智能客服与内容生成服务，为苗丰施肥管控平台提供 AI 能力。

## 技术架构

```
┌─────────────────────────────────────────────────┐
│                    API 层                         │
│  /api/chat             智能客服问答               │
│  /api/article/draft     文章草稿生成              │
│  /api/encyclopedia/draft 百科词条生成             │
│  /api/knowledge/rebuild  重建向量库              │
│  /api/knowledge/stats    向量库状态              │
├─────────────────────────────────────────────────┤
│                   RAG 管道                        │
│  MySQL FAQ/百科/文章 → LangChain Document         │
│  → HuggingFaceEmbeddings → Chroma 向量库          │
│  → 相似度检索 Top-K → DeepSeek LLM 生成回答       │
├─────────────────────────────────────────────────┤
│                   基础设施                        │
│  HuggingFaceEmbeddings: shibing624/text2vec      │
│                         -base-chinese             │
│  LLM: DeepSeek (兼容 OpenAI API)                  │
│  VectorStore: Chroma + 持久化                     │
│  Server: FastAPI + Uvicorn                        │
└─────────────────────────────────────────────────┘
```

## 快速开始

### 环境要求

- Python 3.11
- MySQL 8.0+（与主项目共用数据库）
- DeepSeek API Key

### 安装

```bash
cd mf-ai
python -m venv venv
venv\Scripts\activate  # Windows
pip install -r requirements.txt
```

### 配置

设置环境变量或创建 `.env`：

```
OPENAI_API_KEY=你的DeepSeek_API_Key
OPENAI_BASE_URL=https://api.deepseek.com/v1
LLM_MODEL=deepseek-v4-pro
```

### 启动

```bash
# 1. 启动服务
python main.py

# 2. 首次构建向量库（从 MySQL 加载百科/FAQ 数据）
curl -X POST http://localhost:5000/api/knowledge/rebuild

# 3. 测试智能客服
curl -X POST http://localhost:5000/api/chat \
  -H "Content-Type: application/json" \
  -d '{"question":"苹果树春天怎么施肥"}'
```

## API 说明

### 智能客服

```
POST /api/chat
Content-Type: application/json
{"question": "苹果树春天怎么施肥"}

Response:
{
  "answer": "苹果树春季施肥建议：...",
  "sources": [
    {"type": "encyclopedia", "name": "红富士苹果", "category": ""},
    {"type": "article", "name": "果树冬季修剪五大要点", "category": ""}
  ]
}
```

检索 FAQ + 百科词条 + 科普文章，用向量相似度找到最相关内容作为上下文，交由 LLM 生成专业回答。有知识库匹配时使用 RAG 增强回答，闲聊时自由对话。

### 文章草稿生成

```
POST /api/article/draft
{"topic": "果树冬季修剪", "category": "养护技巧"}

Response:
{
  "title": "果树冬季修剪：五个关键要点",
  "summary": "冬季修剪是果树管理的重要环节...",
  "content": "一、把握修剪时机\n...",
  "tags": "修剪,冬季管理,果树,养护技巧"
}
```

输入主题和分类，LLM 检索相关知识后按结构化格式生成科普文章草稿。

### 百科词条生成

```
POST /api/encyclopedia/draft
{"name": "巨峰葡萄"}

Response:
{
  "name": "巨峰葡萄",
  "scientificName": "Vitis vinifera 'Kyoho'",
  "family": "葡萄科",
  "genus": "葡萄属",
  "description": "巨峰葡萄是...",
  "morphology": "落叶藤本植物...",
  "distribution": "原产日本...",
  "habitat": "喜光、喜温暖...",
  "careGuide": "定植后需搭架...",
  "valueDescription": "鲜食品种...",
  "tags": "葡萄,果树,庭院"
}
```

输入植物名称，LLM 生成完整的百科词条。

### 重建向量库

```
POST /api/knowledge/rebuild

Response:
{"status": "ok", "documents": 15, "sources": 5}
```

百科/FAQ 数据更新后调用，采用蓝绿切换策略——先构建新库到临时集合，构建完成后瞬间切换指针，再删除旧库，重建期间不影响用户提问。

## 关键设计

### 全局单例

Embeddings 模型（400MB）、LLM 客户端、Chroma 向量库在应用启动时初始化一次，所有请求复用，避免每次加载模型。

### 线程池并发

所有 API 端点使用 `def`（非 async），FastAPI 自动放入线程池执行，多个用户同时提问互不阻塞。

### 向量库蓝绿切换

重建向量库时先创建临时集合 `mf_knowledge_new`，全部数据写入完成后瞬间切换全局指针，再删除旧集合。切换前后无空白期。

### 中文向量模型

使用 `shibing624/text2vec-base-chinese` 替代默认的英文 Embedding 模型，通过 `HF_ENDPOINT` 镜像解决 HuggingFace 下载问题。

## 与 Java 后端集成

Java 端通过 `AiClient`（RestTemplate）调用 Python 服务：

```
C端前端 → Java ClientAiController → AiClient → Python :5000 → DeepSeek
管理端前端 → Java AdminAiController → AiClient → Python :5000 → DeepSeek
```

Python 服务不可用时 Java 端返回 503 降级，不影响主业务。
