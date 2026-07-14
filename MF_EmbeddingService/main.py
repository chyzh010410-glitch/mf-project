import os
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field
from sentence_transformers import SentenceTransformer

MODEL_NAME = os.getenv("MF_EMBEDDING_MODEL", "BAAI/bge-m3")
model = SentenceTransformer(MODEL_NAME)
app = FastAPI(title="MF Embedding Service")

class EmbedRequest(BaseModel):
    texts: list[str] = Field(min_length=1, max_length=128)

@app.get("/health")
def health():
    return {"status": "ok", "model": MODEL_NAME}

@app.post("/embed")
def embed(request: EmbedRequest):
    if any(not item.strip() for item in request.texts):
        raise HTTPException(400, "texts must not contain blank values")
    vectors = model.encode(request.texts, normalize_embeddings=True).tolist()
    return {"model": MODEL_NAME, "embeddings": vectors}
