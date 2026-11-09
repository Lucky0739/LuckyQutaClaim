from pathlib import Path

from pydantic_settings import BaseSettings, SettingsConfigDict

# 固定在 KB 服务根目录读取 .env，避免从仓库根目录启动时读不到密钥
_ENV_FILE = Path(__file__).resolve().parents[2] / ".env"


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=_ENV_FILE, extra="ignore")

    milvus_host: str = "127.0.0.1"
    milvus_port: int = 19530
    # v2：父子块 schema（parent_id/parent_content/heading_path/child_start）
    milvus_collection: str = "kb_chunk_v2"
    # 父块用于提供上下文，子块用于向量检索
    parent_chunk_size: int = 1600
    parent_chunk_overlap: int = 100
    child_chunk_size: int = 320
    child_chunk_overlap: int = 60
    retrieval_top_k: int = 8
    minio_endpoint: str = "127.0.0.1:9000"
    minio_access_key: str = "lucky"
    minio_secret_key: str = "yang2004"
    minio_secure: bool = False
    # 向量模型走 DashScope（阿里云百炼）OpenAI 兼容接口
    embedding_api_key: str = ""
    embedding_base_url: str = "https://dashscope.aliyuncs.com/compatible-mode/v1"
    embedding_model: str = "text-embedding-v4"
    embedding_dim: int = 1536
    # DashScope embedding 单次请求的文本数量上限
    embedding_batch_size: int = 10
    # Java 用户服务（KB Tools 内部接口）
    java_kb_base_url: str = "http://127.0.0.1:8081"
    tool_http_timeout: int = 8


settings = Settings()
