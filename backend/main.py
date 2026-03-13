from fastapi import FastAPI, UploadFile, File, HTTPException, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.middleware.gzip import GZipMiddleware
from fastapi.staticfiles import StaticFiles
from fastapi.responses import JSONResponse
import uvicorn
import os
import time
from dotenv import load_dotenv
from app.api.routes import api_router
from app.services.model_service import ModelService

load_dotenv()

app = FastAPI(
    title="FreshID API",
    description="FreshID 后端系统 - V1.4",
    version="1.4.0"
)

app.add_middleware(GZipMiddleware, minimum_size=1000)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.middleware("http")
async def add_process_time_header(request: Request, call_next):
    start_time = time.time()
    response = await call_next(request)
    process_time = time.time() - start_time
    response.headers["X-Process-Time"] = str(process_time)
    return response

model_service = ModelService()

app.include_router(api_router, prefix="/api/v1")

upload_dir = os.getenv("UPLOAD_DIR", "uploads")
os.makedirs(upload_dir, exist_ok=True)
app.mount("/uploads", StaticFiles(directory=upload_dir), name="uploads")

@app.get("/health")
async def health_check():
    model_status = "ok" if model_service.model is not None else "error"
    upload_dir = os.getenv("UPLOAD_DIR", "uploads")
    storage_status = "ok" if os.path.exists(upload_dir) else "error"
    
    db_status = "ok"
    try:
        from app.services.data_service import DataService
        data_service = DataService()
        if data_service.connection is None:
            db_status = "warning"
    except Exception as e:
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
    uvicorn.run("main:app", host="0.0.0.0", port=8001, reload=False, log_level=log_level)
