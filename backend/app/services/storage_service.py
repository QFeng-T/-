import os
import uuid
from fastapi import UploadFile, HTTPException
import aiofiles
from dotenv import load_dotenv

load_dotenv()

try:
    import magic
    MAGIC_AVAILABLE = True
except ImportError:
    MAGIC_AVAILABLE = False

class StorageService:
    def __init__(self):
        self.base_dir = "backend"
        self.upload_subdir = "uploads"
        self.upload_dir = os.path.join(self.base_dir, self.upload_subdir)
        if not os.path.exists(self.upload_dir):
            os.makedirs(self.upload_dir)
        
        self.base_url = os.getenv("BASE_URL", "http://localhost:8000")
        
        self.allowed_mime_types = [
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/bmp",
            "image/webp"
        ]
        
        self.allowed_extensions = [
            ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp"
        ]
    
    async def validate_file(self, file: UploadFile) -> bool:
        content = await file.read()
        await file.seek(0)
        
        file_extension = os.path.splitext(file.filename)[1].lower()
        if file_extension not in self.allowed_extensions:
            raise HTTPException(status_code=400, detail=f"不支持的文件扩展名: {file_extension}")
        
        if MAGIC_AVAILABLE:
            try:
                mime_type = magic.from_buffer(content, mime=True)
                if mime_type not in self.allowed_mime_types:
                    raise HTTPException(status_code=400, detail=f"不支持的文件类型: {mime_type}")
            except Exception as e:
                print(f"Magic number 校验失败，降级到 MIME 类型校验: {e}")
                if file.content_type not in self.allowed_mime_types:
                    raise HTTPException(status_code=400, detail=f"不支持的文件类型: {file.content_type}")
        else:
            if file.content_type not in self.allowed_mime_types:
                raise HTTPException(status_code=400, detail=f"不支持的文件类型: {file.content_type}")
        
        return True
    
    async def save_file(self, file: UploadFile) -> str:
        await self.validate_file(file)
        
        file_extension = os.path.splitext(file.filename)[1].lower()
        unique_filename = f"{uuid.uuid4()}{file_extension}"
        full_path = os.path.join(self.upload_dir, unique_filename)
        relative_path = os.path.join(self.upload_subdir, unique_filename)
        
        try:
            async with aiofiles.open(full_path, 'wb') as out_file:
                content = await file.read()
                await out_file.write(content)
            return relative_path
        except Exception as e:
            raise Exception(f"文件保存失败: {str(e)}")
    
    def delete_file(self, file_path: str) -> bool:
        try:
            if not os.path.isabs(file_path):
                full_path = os.path.join(self.base_dir, file_path)
            else:
                full_path = file_path
            
            if os.path.exists(full_path):
                os.remove(full_path)
                return True
            return False
        except Exception as e:
            print(f"文件删除失败: {str(e)}")
            return False
    
    def get_file_url(self, file_path: str) -> str:
        if file_path.startswith(('http://', 'https://')):
            return file_path
        return f"{self.base_url}/{file_path}"
