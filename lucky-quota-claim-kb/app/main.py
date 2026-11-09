from fastapi import FastAPI

from app.api.routes import router

app = FastAPI(title="lucky-quota-claim-kb", docs_url="/docs")
app.include_router(router)
