from fastapi import FastAPI

from playzulu.api import router

app = FastAPI(
    title="PlayZulu",
    version="0.1.0",
    description="A scalable FastAPI server for the PlayZulu game series.",
)
app.include_router(router)
