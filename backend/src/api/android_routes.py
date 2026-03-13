from fastapi import APIRouter, UploadFile, File, HTTPException, Depends, Query
from datetime import datetime, timedelta
from src.services.model_service import ModelService
from src.services.data_service import DataService
from src.services.storage_service import StorageService
from src.services.auth_service import AuthService, get_current_user
from src.services.cache_service import CacheService
from src.models.schemas import (
    PredictionRequest, PredictionResponse, UserCreate, UserResponse,
    BaseResponse, PaginatedResponse, SyncDownloadResponse,
    SyncUploadRequest, TokenRevokeRequest, ErrorCode,
    SendCodeRequest, SendCodeResponse, GuestUserResponse
)

android_router = APIRouter()

async def get_model_service():
    from src.services.model_service import ModelService
    return ModelService()

async def get_data_service():
    from src.services.data_service import DataService
    return DataService()

async def get_storage_service():
    from src.services.storage_service import StorageService
    return StorageService()

async def get_auth_service():
    from src.services.auth_service import AuthService
    return AuthService()

async def get_cache_service():
    return CacheService()

@android_router.post("/upload", response_model=PredictionResponse)
async def upload_image(
    file: UploadFile = File(...),
    model_service: ModelService = Depends(get_model_service),
    storage_service: StorageService = Depends(get_storage_service),
    data_service: DataService = Depends(get_data_service),
    current_user: dict = Depends(get_current_user)
):
    try:
        file_path = await storage_service.save_file(file)
        prediction = await model_service.predict(file_path)
        prediction_id = await data_service.save_prediction(prediction, file_path, user_id=current_user["user_id"])
        
        return PredictionResponse(
            success=True,
            code=None,
            data={
                "prediction_id": prediction_id,
                "prediction": prediction,
                "file_path": file_path
            },
            message="预测成功"
        )
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail={
                "success": False,
                "code": ErrorCode.MODEL_ERROR,
                "message": f"预测失败: {str(e)}"
            }
        )

@android_router.get("/predictions", response_model=PaginatedResponse)
async def get_predictions(
    page: int = Query(1, ge=1, description="页码"),
    limit: int = Query(20, ge=1, le=100, description="每页数量"),
    data_service: DataService = Depends(get_data_service),
    cache_service: CacheService = Depends(get_cache_service),
    current_user: dict = Depends(get_current_user)
):
    try:
        cache_key = f"predictions:{current_user['user_id']}:{page}:{limit}"
        
        cached_result = cache_service.get(cache_key)
        if cached_result:
            return PaginatedResponse(
                success=True,
                code=None,
                message="获取预测结果成功(缓存)",
                data=cached_result["records"],
                pagination=cached_result.get("pagination")
            )
        
        result = await data_service.get_predictions(user_id=current_user["user_id"], page=page, limit=limit)
        
        cache_service.set(cache_key, result, ttl=300)
        
        return PaginatedResponse(
            success=True,
            code=None,
            message="获取预测结果成功",
            data=result["records"],
            pagination=result.get("pagination")
        )
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail={
                "success": False,
                "code": ErrorCode.DATABASE_ERROR,
                "message": f"获取预测结果失败: {str(e)}"
            }
        )

@android_router.post("/users", response_model=UserResponse)
async def create_user(
    user: UserCreate,
    data_service: DataService = Depends(get_data_service)
):
    try:
        user_id = await data_service.create_user(user)
        return UserResponse(
            success=True,
            code=None,
            data={
                "user_id": user_id,
                "username": user.username
            },
            message="用户创建成功"
        )
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail={
                "success": False,
                "code": ErrorCode.DATABASE_ERROR,
                "message": f"创建用户失败: {str(e)}"
            }
        )

@android_router.get("/users/{user_id}")
async def get_user(
    user_id: int,
    data_service: DataService = Depends(get_data_service)
):
    try:
        user = await data_service.get_user(user_id)
        if not user:
            raise HTTPException(
                status_code=404,
                detail={
                    "success": False,
                    "code": ErrorCode.USER_NOT_FOUND,
                    "message": "用户不存在"
                }
            )
        return {
            "success": True,
            "code": None,
            "data": user,
            "message": "获取用户信息成功"
        }
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail={
                "success": False,
                "code": ErrorCode.DATABASE_ERROR,
                "message": f"获取用户信息失败: {str(e)}"
            }
        )

@android_router.get("/sync/download", response_model=SyncDownloadResponse)
async def sync_download(
    since: datetime = Query(None, description="上次同步时间"),
    data_service: DataService = Depends(get_data_service),
    current_user: dict = Depends(get_current_user)
):
    try:
        if current_user["login_type"] == "guest":
            raise HTTPException(
                status_code=403,
                detail={
                    "success": False,
                    "code": ErrorCode.PERMISSION_DENIED,
                    "message": "游客用户无法使用云端同步功能，请先登录"
                }
            )
        
        sync_data = await data_service.sync_download(current_user["user_id"], since)
        return SyncDownloadResponse(
            success=True,
            code=None,
            message="同步下载成功",
            data=None,
            records=sync_data["records"],
            deleted_ids=sync_data["deleted_ids"],
            last_updated=sync_data["last_updated"]
        )
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail={
                "success": False,
                "code": ErrorCode.DATABASE_ERROR,
                "message": f"同步下载失败: {str(e)}"
            }
        )

@android_router.post("/sync/upload")
async def sync_upload(
    request: SyncUploadRequest,
    data_service: DataService = Depends(get_data_service),
    cache_service: CacheService = Depends(get_cache_service),
    current_user: dict = Depends(get_current_user)
):
    try:
        if current_user["login_type"] == "guest":
            raise HTTPException(
                status_code=403,
                detail={
                    "success": False,
                    "code": ErrorCode.PERMISSION_DENIED,
                    "message": "游客用户无法使用云端同步功能，请先登录"
                }
            )
        
        success = await data_service.sync_upload(current_user["user_id"], request.records, request.deleted_ids)
        if not success:
            raise HTTPException(
                status_code=500,
                detail={
                    "success": False,
                    "code": ErrorCode.DATABASE_ERROR,
                    "message": "同步上传失败"
                }
            )
        
        cache_service.delete_pattern(f"predictions:{current_user['user_id']}:*")
        
        return {
            "success": True,
            "code": None,
            "message": "同步上传成功"
        }
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail={
                "success": False,
                "code": ErrorCode.DATABASE_ERROR,
                "message": f"同步上传失败: {str(e)}"
            }
        )

@android_router.post("/auth/logout")
async def logout(
    data_service: DataService = Depends(get_data_service),
    current_user: dict = Depends(get_current_user)
):
    try:
        await data_service.revoke_user_refresh_tokens(current_user["user_id"])
        return {
            "success": True,
            "code": None,
            "message": "登出成功，已撤销所有 Refresh Token"
        }
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail={
                "success": False,
                "code": ErrorCode.DATABASE_ERROR,
                "message": f"登出失败: {str(e)}"
            }
        )

@android_router.delete("/records/{record_id}")
async def delete_record(
    record_id: int,
    data_service: DataService = Depends(get_data_service),
    storage_service: StorageService = Depends(get_storage_service),
    current_user: dict = Depends(get_current_user)
):
    try:
        success = await data_service.soft_delete_record(record_id, current_user["user_id"])
        if not success:
            raise HTTPException(
                status_code=404,
                detail={
                    "success": False,
                    "code": ErrorCode.USER_NOT_FOUND,
                    "message": "记录不存在"
                }
            )
        return {
            "success": True,
            "code": None,
            "message": "记录删除成功"
        }
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail={
                "success": False,
                "code": ErrorCode.DATABASE_ERROR,
                "message": f"删除记录失败: {str(e)}"
            }
        )

@android_router.post("/auth/revoke")
async def revoke_token(
    request: TokenRevokeRequest,
    data_service: DataService = Depends(get_data_service)
):
    try:
        success = await data_service.revoke_refresh_token(request.refresh_token)
        if not success:
            return {
                "success": True,
                "code": None,
                "message": "Token 已撤销或不存在"
            }
        return {
            "success": True,
            "code": None,
            "message": "Token 撤销成功"
        }
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail={
                "success": False,
                "code": ErrorCode.DATABASE_ERROR,
                "message": f"撤销 Token 失败: {str(e)}"
            }
        )

@android_router.post("/users/guest", response_model=GuestUserResponse)
async def create_guest_user(
    auth_service: AuthService = Depends(get_auth_service),
    data_service: DataService = Depends(get_data_service)
):
    try:
        uid = auth_service.generate_uid()
        username = f"游客{uid[-4:]}"
        
        user_data = await data_service.create_guest_user(username, uid)
        
        access_token, expire = auth_service.create_access_token(
            data={"sub": str(user_data["id"]), "uid": uid, "login_type": "guest"}
        )
        expires_in = int((expire - datetime.utcnow()).total_seconds())
        
        return GuestUserResponse(
            success=True,
            code=None,
            data={
                "access_token": access_token,
                "token_type": "bearer",
                "expires_in": expires_in
            },
            message="游客用户创建成功"
        )
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail={
                "success": False,
                "code": ErrorCode.DATABASE_ERROR,
                "message": f"创建游客用户失败: {str(e)}"
            }
        )

@android_router.post("/auth/send-code", response_model=SendCodeResponse)
async def send_verification_code(
    request: SendCodeRequest,
    auth_service: AuthService = Depends(get_auth_service),
    data_service: DataService = Depends(get_data_service)
):
    try:
        code = auth_service.generate_verification_code()
        expires_in = 300
        expires_at = datetime.utcnow() + timedelta(seconds=expires_in)
        
        auth_service.save_verification_code(request.phone_number, code, expires_in)
        
        await data_service.save_verification_code_db(
            request.phone_number, code, "login", expires_at
        )
        
        print(f"发送验证码到 {request.phone_number}: {code}")
        
        return SendCodeResponse(
            success=True,
            code=None,
            data={"expires_in": expires_in},
            message="验证码已发送，5分钟内有效"
        )
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail={
                "success": False,
                "code": ErrorCode.DATABASE_ERROR,
                "message": f"发送验证码失败: {str(e)}"
            }
        )
