import os
from dotenv import load_dotenv

load_dotenv()

# MySQL 连接（与现有项目共用同一个数据库）
MYSQL_CONFIG = {
    "host": os.getenv("MYSQL_HOST", "localhost"),
    "port": int(os.getenv("MYSQL_PORT", "3306")),
    "user": os.getenv("MYSQL_USER", "root"),
    "password": os.getenv("MYSQL_PASSWORD", "123456"),
    "database": os.getenv("MYSQL_DATABASE", "fertilizer"),
    "charset": "utf8mb4",
}

# HuggingFace 国内镜像（解决模型下载被墙问题）
os.environ["HF_ENDPOINT"] = os.getenv("HF_ENDPOINT", "https://hf-mirror.com")

# LLM 配置（DeepSeek 兼容 OpenAI API 格式）
OPENAI_API_KEY = os.getenv("OPENAI_API_KEY", "你的DeepSeek的API key")
OPENAI_BASE_URL = os.getenv("OPENAI_BASE_URL", "https://api.deepseek.com/v1")
LLM_MODEL = os.getenv("LLM_MODEL", " deepseek-v4-flash")
EMBEDDING_MODEL = os.getenv("EMBEDDING_MODEL", "shibing624/text2vec-base-chinese")  # 中文向量模型

# Chroma 向量库持久化路径
CHROMA_PERSIST_DIR = os.path.join(os.path.dirname(__file__), "data", "chroma_db")
