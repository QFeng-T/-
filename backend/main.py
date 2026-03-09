from fastapi import FastAPI, UploadFile, File, HTTPException
from fastapi.middleware.cors import CORSMiddleware
import uvicorn
import os
from dotenv import load_dotenv
from app.api.routes import api_router
from app.services.model_service import ModelService

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

@app.get("/health")
def health_check():
    model_status = "ok" if model_service.model is not None else "error"
    upload_dir = os.getenv("UPLOAD_DIR", "backend/uploads")
    storage_status = "ok" if os.path.exists(upload_dir) else "error"
    
    db_status = "ok"
    try:
        from app.services.data_service import DataService
        ds = DataService()
        if ds.connection is None:
            db_status = "error"
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
