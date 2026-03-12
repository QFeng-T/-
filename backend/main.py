from fastapi import FastAPI, UploadFile, File, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
import uvicorn
import os
from dotenv import load_dotenv
from app.api.routes import api_router
from app.services.model_service import ModelService
from app.db_pool import DatabasePool
from app.services.data_service import DataService

load_dotenv()

app = FastAPI(
    title="FreshID API",
    description="FreshID 后端系统 - V1.4",
    version="1.4.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

model_service = ModelService()

app.include_router(api_router, prefix="/api/v1")

upload_dir = os.getenv("UPLOAD_DIR", "backend/uploads")
os.makedirs(upload_dir, exist_ok=True)
app.mount("/uploads", StaticFiles(directory=upload_dir), name="uploads")

@app.on_event("startup")
async def startup_event():
    await DatabasePool.create_pool()
    print("数据库连接池创建成功")

@app.on_event("shutdown")
async def shutdown_event():
    await DatabasePool.close_pool()

@app.get("/health")
async def health_check():
    model_status = "ok" if model_service.model is not None else "error"
    upload_dir = os.getenv("UPLOAD_DIR", "backend/uploads")
    storage_status = "ok" if os.path.exists(upload_dir) else "error"
    
    db_status = "ok"
    try:
        pool = DatabasePool.get_pool()
        async with pool.acquire() as conn:
            async with conn.cursor() as cursor:
                await cursor.execute("SELECT 1")
    except:
        db_status = "error"
    
    return {
        "status": "ok",
        "message": "服务运行正常",
        "version": "1.4.0",
        "database": db_status,
        "model": model_status,
        "checks": {
            "database": db_status,
            "model": model_status,
            "storage": storage_status
        }
    }

@app.get("/")
def read_root():
    return {"message": "Welcome to FreshID API v1.4"}

if __name__ == "__main__":
    log_level = os.getenv("LOG_LEVEL", "info")
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True, log_level=log_level)
