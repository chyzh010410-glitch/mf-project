"""
苗丰施肥平台 — AI 微服务
端口 5000
提供：智能客服（RAG FAQ+百科）、文章草稿生成
"""
# ⚠️ 必须在所有 HuggingFace 相关 import 之前设置，否则镜像不生效
import os
os.environ["HF_ENDPOINT"] = "https://hf-mirror.com"

import json
import pymysql
from contextlib import contextmanager

from fastapi import FastAPI
from pydantic import BaseModel
from langchain_openai import ChatOpenAI
from langchain_huggingface import HuggingFaceEmbeddings
from langchain_chroma import Chroma
from langchain_text_splitters import RecursiveCharacterTextSplitter
from langchain_core.documents import Document
from langchain_core.prompts import ChatPromptTemplate

from config import (
    MYSQL_CONFIG, OPENAI_API_KEY, OPENAI_BASE_URL,
    LLM_MODEL, EMBEDDING_MODEL, CHROMA_PERSIST_DIR,
)

app = FastAPI(title="苗丰 AI 服务", version="1.0")

# ========== 全局实例（应用启动时初始化，复用所有请求） ==========
# 1. 全局Embeddings实例（只加载一次模型）
global_embeddings = None
# 2. 全局LLM实例
global_llm = None
# 3. 全局向量库实例
global_vector_store = None

# ========== 数据库工具 ==========
@contextmanager
def get_db():
    conn = pymysql.connect(**MYSQL_CONFIG, cursorclass=pymysql.cursors.DictCursor)
    try:
        yield conn
    finally:
        conn.close()

def load_faqs(conn) -> list[dict]:
    with conn.cursor() as cur:
        cur.execute("SELECT id, question, answer, category FROM faq WHERE is_published = 1 AND deleted = 0")
        return cur.fetchall()

def load_encyclopedia(conn) -> list[dict]:
    with conn.cursor() as cur:
        cur.execute(
            "SELECT id, name, scientific_name, description, morphology, distribution, "
            "habitat, care_guide, value_description, tags "
            "FROM encyclopedia_entry WHERE is_published = 1 AND deleted = 0"
        )
        return cur.fetchall()

def load_articles(conn) -> list[dict]:
    with conn.cursor() as cur:
        cur.execute(
            "SELECT id, title, summary, content, tags "
            "FROM encyclopedia_article WHERE is_published = 1 AND deleted = 0"
        )
        return cur.fetchall()

# ========== 向量库构建 ==========
def build_documents() -> list[Document]:
    """从 MySQL 加载 FAQ + 百科词条，转为 LangChain Document 列表"""
    docs = []
    with get_db() as conn:
        # FAQ
        for row in load_faqs(conn):
            text = f"问题: {row['question']}\n回答: {row['answer']}"
            docs.append(Document(
                page_content=text,
                metadata={"source": "faq", "id": row["id"], "category": row["category"] or ""}
            ))
        # 百科词条
        for row in load_encyclopedia(conn):
            parts = [f"名称: {row['name']}"]
            if row.get("scientific_name"): parts.append(f"学名: {row['scientific_name']}")
            if row.get("description"): parts.append(f"简介: {row['description']}")
            if row.get("morphology"): parts.append(f"形态: {row['morphology']}")
            if row.get("distribution"): parts.append(f"分布: {row['distribution']}")
            if row.get("habitat"): parts.append(f"环境: {row['habitat']}")
            if row.get("care_guide"): parts.append(f"养护: {row['care_guide']}")
            if row.get("value_description"): parts.append(f"价值: {row['value_description']}")
            docs.append(Document(
                page_content="\n".join(parts),
                metadata={"source": "encyclopedia", "id": row["id"], "name": row["name"], "tags": row.get("tags", "")}
            ))
        # 文章
        for row in load_articles(conn):
            text = f"标题: {row['title']}\n摘要: {row['summary']}\n内容: {row['content']}"
            docs.append(Document(
                page_content=text,
                metadata={"source": "article", "id": row["id"], "title": row["title"], "tags": row.get("tags", "")}
            ))
    return docs

# ========== 初始化函数（应用启动时执行） ==========
def init_global_instances():
    """初始化全局模型/向量库实例（只执行一次）"""
    global global_embeddings, global_llm, global_vector_store
    
    # 1. 初始化Embeddings（耗时操作，只做一次）
    if global_embeddings is None:
        global_embeddings = HuggingFaceEmbeddings(
            model_name=EMBEDDING_MODEL,
            model_kwargs={"device": "cpu"},
            encode_kwargs={"normalize_embeddings": True},
        )
    
    # 2. 初始化LLM
    if global_llm is None:
        kwargs = {"model": LLM_MODEL, "temperature": 0.7, "api_key": OPENAI_API_KEY}
        if OPENAI_BASE_URL:
            kwargs["base_url"] = OPENAI_BASE_URL
        global_llm = ChatOpenAI(**kwargs)
    
    # 3. 初始化向量库
    if global_vector_store is None:
        global_vector_store = Chroma(
            collection_name="mf_knowledge",
            embedding_function=global_embeddings,
            persist_directory=CHROMA_PERSIST_DIR,
        )

# ========== API 模型 ==========
class ChatRequest(BaseModel):
    question: str

class ArticleRequest(BaseModel):
    topic: str
    category: str = ""

class EncyclopediaRequest(BaseModel):
    name: str

# ========== API ==========
@app.post("/api/chat")
def chat(req: ChatRequest):
    """智能客服：RAG 检索 FAQ + 百科 → LLM 回答"""
    # 复用全局向量库实例
    vector_store = global_vector_store
    results = vector_store.similarity_search_with_score(req.question, k=5)
    relevant_docs = [doc for doc, score in results if score < 1.0]

    if relevant_docs:
        # 有相关知识 → RAG 增强回答
        context = "\n\n---\n\n".join(doc.page_content[:800] for doc in relevant_docs)
        system_prompt = f"""你是一个植物养护和施肥知识专家。根据以下参考知识回答用户问题。
如果参考知识中没有相关信息，请诚实说明。

参考知识：
{context}

请用中文回答，简洁、专业、友好。"""
    else:
        # 闲聊或超出知识库 → 自由对话
        system_prompt = "你叫苗丰小助手，是一个热情友好的农业知识助手，擅长树木养护和施肥技术。请用中文回答，简洁友好。"

    prompt = ChatPromptTemplate.from_messages([
        ("system", system_prompt),
        ("human", "{question}"),
    ])

    try:
        # 复用全局LLM实例
        chain = prompt | global_llm
        answer = chain.invoke({"question": req.question})

        sources = []
        for doc in relevant_docs:
            src = doc.metadata.get("source", "")
            name = doc.metadata.get("name") or doc.metadata.get("title") or ""
            cat = doc.metadata.get("category", "")
            sources.append({"type": src, "name": name, "category": cat})

        return {"answer": answer.content, "sources": sources}
    except Exception as e:
        return {"answer": f"AI 服务出错: {str(e)}", "sources": []}

@app.post("/api/article/draft")
def draft_article(req: ArticleRequest):
    """文章草稿：根据主题检索百科 → LLM 生成科普文章（带摘要）"""
    vector_store = global_vector_store

    results = vector_store.similarity_search_with_score(req.topic, k=4)
    context_docs = [doc for doc, _ in results if doc.metadata.get("source") != "faq"][:3]
    context = "\n\n".join(doc.page_content[:600] for doc in context_docs) if context_docs else "无参考资料"
    category_hint = f"，文章分类为「{req.category}」" if req.category else ""

    prompt = ChatPromptTemplate.from_messages([
        ("system", f"""你是专业农业科普作家。根据参考资料，生成一篇科普文章草稿。
{category_hint}

必须严格按照以下格式输出（不要输出任何其他内容，不要客套话，不要"好的""没问题"）：

【标题】
这里写文章标题（简洁明了，15字以内）

【摘要】
这里写摘要（100字左右，概括文章核心内容）

【正文】
这里写正文（至少三个段落，包含具体养护/施肥建议，语言通俗易懂）

【标签】
3-5个标签，逗号分隔

参考资料：
{context}"""),
        ("human", "请以「{topic}」为主题生成文章"),
    ])

    chain = prompt | global_llm
    result = chain.invoke({"topic": req.topic})
    text = result.content

    # 用标记解析标题/摘要/正文
    def extract(tag, text):
        start = text.find(f"【{tag}】")
        if start == -1:
            return ""
        start += len(f"【{tag}】")
        markers = ["【标题】", "【摘要】", "【正文】", "【标签】"]
        end_tag = next((t for t in markers if text.find(t, start) != -1), None)
        end = text.find(end_tag, start) if end_tag else len(text)
        return text[start:end].strip()

    title = extract("标题", text) or req.topic
    summary = extract("摘要", text)
    content = extract("正文", text) or text
    tags = extract("标签", text)

    # 去掉正文里可能残留的格式杂讯
    title = title.replace("#", "").replace("**", "").strip()
    if len(title) > 50:
        title = title[:50]

    return {
        "title": title,
        "summary": summary,
        "content": content,
        "tags": tags,
    }

@app.post("/api/encyclopedia/draft")
def draft_encyclopedia(req: EncyclopediaRequest):
    """百科词条草稿：输入植物名称 → LLM 生成完善的百科词条"""
    vector_store = global_vector_store
    results = vector_store.similarity_search_with_score(req.name, k=3)
    context = "\n\n".join(doc.page_content[:500] for doc, _ in results) if results else "无参考资料"

    prompt = ChatPromptTemplate.from_messages([
        ("system", f"""你是植物学家，精通树木百科编撰。根据参考资料，为植物生成百科词条。

必须严格按照以下格式输出（不要客套话）：

【学名】
拉丁学名

【别名】
常用别名，逗号分隔

【拼音】
汉语拼音

【科】
所属科

【属】
所属属

【简介】
100字左右的简要介绍

【形态特征】
植株形态、叶片、花果等特征描述

【分布】
地理分布情况

【生长环境】
适宜的气候、土壤、光照、水分等条件

【养护指南】
具体的养护方法、修剪、施肥建议

【价值说明】
经济价值、生态价值、观赏价值等

【标签】
相关标签，逗号分隔

参考资料：
{context}"""),
        ("human", "请为「{name}」编写百科词条"),
    ])

    chain = prompt | global_llm
    result = chain.invoke({"name": req.name})
    text = result.content

    def extract(tag):
        start = text.find(f"【{tag}】")
        if start == -1: return ""
        start += len(f"【{tag}】")
        end_tag = next((t for t in [
            "【学名】","【别名】","【拼音】","【科】","【属】","【简介】",
            "【形态特征】","【分布】","【生长环境】","【养护指南】","【价值说明】","【标签】"
        ] if text.find(t, start) != -1), None)
        end = text.find(end_tag, start) if end_tag else len(text)
        return text[start:end].strip()

    return {
        "name": req.name,
        "scientificName": extract("学名"),
        "alias": extract("别名"),
        "pinyin": extract("拼音"),
        "family": extract("科"),
        "genus": extract("属"),
        "description": extract("简介"),
        "morphology": extract("形态特征"),
        "distribution": extract("分布"),
        "habitat": extract("生长环境"),
        "careGuide": extract("养护指南"),
        "valueDescription": extract("价值说明"),
        "tags": extract("标签"),
    }


@app.post("/api/knowledge/rebuild")
def rebuild_knowledge():
    """✅ 安全重建向量库：无缝切换，无空白期，不影响用户提问"""
    try:
        docs = build_documents()
        if not docs:
            return {"status": "error", "msg": "数据库中无已发布的 FAQ 或百科词条，请先在管理端录入内容"}

        # 1. 文本分割（不变）
        text_splitter = RecursiveCharacterTextSplitter(chunk_size=600, chunk_overlap=80)
        split_docs = text_splitter.split_documents(docs)

        # ================= 核心修改：先创建新向量库 =================
        # 新建临时向量库，把数据全写进去
        new_vector_store = Chroma(
            collection_name="mf_knowledge_new",  # 临时名字
            embedding_function=global_embeddings,
            persist_directory=CHROMA_PERSIST_DIR,
        )
        new_vector_store.add_documents(split_docs)

        # 2. 原子切换：先指针替换 → 再删旧库，中间无空白期
        global global_vector_store
        old_store = global_vector_store
        new_vector_store._collection.name = "mf_knowledge"
        global_vector_store = new_vector_store  # 先切！后续请求立刻用新库
        old_store.delete_collection()           # 再删旧库，安全

        return {"status": "ok", "documents": len(split_docs), "sources": len(docs)}
    except Exception as e:
        return {"status": "error", "msg": str(e)}

@app.get("/api/knowledge/stats")
async def knowledge_stats():
    """查看向量库状态"""
    try:
        vs = global_vector_store
        count = vs._collection.count()
        return {"documents": count, "status": "ready"}
    except Exception:
        return {"documents": 0, "status": "not_initialized"}

# ========== 应用启动时初始化全局实例 ==========
@app.on_event("startup")
def startup_event():
    """FastAPI启动钩子：初始化全局模型/向量库"""
    init_global_instances()

if __name__ == "__main__":
    import uvicorn
    # 启动前手动初始化（直接运行时触发）
    init_global_instances()
    uvicorn.run(app, host="0.0.0.0", port=5000)