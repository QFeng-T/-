from fastapi import APIRouter, HTTPException, Depends, Query
from datetime import datetime
from src.services.data_service import DataService
from src.services.auth_service import AuthService

admin_router = APIRouter(prefix="/admin")

async def get_data_service():
    from src.services.data_service import DataService
    return DataService()

async def get_auth_service():
    from src.services.auth_service import AuthService
    return AuthService()

@admin_router.post("/login")
async def admin_login(
    username: str,
    password: str,
    auth_service: AuthService = Depends(get_auth_service)
):
    try:
        if username == "admin" and password == "admin123":
            access_token, expire = auth_service.create_access_token(
                data={"sub": "admin", "role": "admin"}
            )
            expires_in = int((expire - datetime.utcnow()).total_seconds())
            return {
                "success": True,
                "data": {
                    "access_token": access_token,
                    "token_type": "bearer",
                    "expires_in": expires_in,
                    "user": {"username": "admin", "role": "admin"}
                },
                "message": "登录成功"
            }
        else:
            raise HTTPException(
                status_code=401,
                detail={"success": False, "message": "用户名或密码错误"}
            )
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail={"success": False, "message": f"登录失败: {str(e)}"}
        )

@admin_router.get("/users")
async def get_admin_users(
    page: int = Query(1, ge=1),
    limit: int = Query(20, ge=1, le=100),
    search: str = Query(None),
    data_service: DataService = Depends(get_data_service)
):
    try:
        result = await data_service.get_all_users(page=page, limit=limit, search=search)
        return {
            "success": True,
            "data": result,
            "message": "获取用户列表成功"
        }
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail={"success": False, "message": f"获取用户列表失败: {str(e)}"}
        )

@admin_router.put("/users/{user_id}")
async def update_admin_user(
    user_id: int,
    username: str = None,
    nickname: str = None,
    email: str = None,
    phone_number: str = None,
    data_service: DataService = Depends(get_data_service)
):
    try:
        success = await data_service.update_user(
            user_id, username=username, nickname=nickname,
            email=email, phone_number=phone_number
        )
        if not success:
            raise HTTPException(
                status_code=404,
                detail={"success": False, "message": "用户不存在"}
            )
        return {
            "success": True,
            "message": "用户更新成功"
        }
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail={"success": False, "message": f"更新用户失败: {str(e)}"}
        )

@admin_router.delete("/users/{user_id}")
async def delete_admin_user(
    user_id: int,
    data_service: DataService = Depends(get_data_service)
):
    try:
        success = await data_service.delete_user(user_id)
        if not success:
            raise HTTPException(
                status_code=404,
                detail={"success": False, "message": "用户不存在"}
            )
        return {
            "success": True,
            "message": "用户删除成功"
        }
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail={"success": False, "message": f"删除用户失败: {str(e)}"}
        )

@admin_router.get("/records")
async def get_admin_records(
    page: int = Query(1, ge=1),
    limit: int = Query(20, ge=1, le=100),
    user_id: int = Query(None),
    fruit_name: str = Query(None),
    start_date: datetime = Query(None),
    end_date: datetime = Query(None),
    data_service: DataService = Depends(get_data_service)
):
    try:
        result = await data_service.get_all_records(
            page=page, limit=limit, user_id=user_id,
            fruit_name=fruit_name, start_date=start_date, end_date=end_date
        )
        return {
            "success": True,
            "data": result,
            "message": "获取识别记录成功"
        }
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail={"success": False, "message": f"获取识别记录失败: {str(e)}"}
        )

@admin_router.delete("/records/{record_id}")
async def delete_admin_record(
    record_id: int,
    data_service: DataService = Depends(get_data_service)
):
    try:
        success = await data_service.admin_delete_record(record_id)
        if not success:
            raise HTTPException(
                status_code=404,
                detail={"success": False, "message": "记录不存在"}
            )
        return {
            "success": True,
            "message": "记录删除成功"
        }
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail={"success": False, "message": f"删除记录失败: {str(e)}"}
        )

@admin_router.post("/records/batch-delete")
async def batch_delete_admin_records(
    ids: list[int],
    data_service: DataService = Depends(get_data_service)
):
    try:
        success = await data_service.batch_delete_records(ids)
        return {
            "success": True,
            "message": "批量删除成功"
        }
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail={"success": False, "message": f"批量删除失败: {str(e)}"}
        )

@admin_router.get("/stats/overview")
async def get_stats_overview(
    data_service: DataService = Depends(get_data_service)
):
    try:
        stats = await data_service.get_admin_stats_overview()
        return {
            "success": True,
            "data": stats,
            "message": "获取统计概览成功"
        }
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail={"success": False, "message": f"获取统计概览失败: {str(e)}"}
        )

@admin_router.get("/stats/trend")
async def get_stats_trend(
    days: int = Query(7, ge=1, le=30),
    data_service: DataService = Depends(get_data_service)
):
    try:
        trend = await data_service.get_admin_stats_trend(days=days)
        return {
            "success": True,
            "data": trend,
            "message": "获取趋势数据成功"
        }
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail={"success": False, "message": f"获取趋势数据失败: {str(e)}"}
        )

@admin_router.get("/stats/fruit-distribution")
async def get_fruit_distribution(
    data_service: DataService = Depends(get_data_service)
):
    try:
        distribution = await data_service.get_fruit_distribution()
        return {
            "success": True,
            "data": distribution,
            "message": "获取果蔬分布成功"
        }
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail={"success": False, "message": f"获取果蔬分布失败: {str(e)}"}
        )

@admin_router.get("/stats/login-type")
async def get_login_type_distribution(
    data_service: DataService = Depends(get_data_service)
):
    try:
        distribution = await data_service.get_login_type_distribution()
        return {
            "success": True,
            "data": distribution,
            "message": "获取登录类型分布成功"
        }
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail={"success": False, "message": f"获取登录类型分布失败: {str(e)}"}
        )

@admin_router.get("/models")
async def get_models(
    page: int = Query(1, ge=1),
    limit: int = Query(20, ge=1, le=100),
    data_service: DataService = Depends(get_data_service)
):
    try:
        models = await data_service.get_models(page=page, limit=limit)
        return {
            "success": True,
            "data": models,
            "message": "获取模型列表成功"
        }
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail={"success": False, "message": f"获取模型列表失败: {str(e)}"}
        )

@admin_router.post("/models/{model_id}/activate")
async def activate_model(
    model_id: int,
    data_service: DataService = Depends(get_data_service)
):
    try:
        success = await data_service.activate_model(model_id)
        if not success:
            raise HTTPException(
                status_code=404,
                detail={"success": False, "message": "模型不存在"}
            )
        return {
            "success": True,
            "message": "模型激活成功"
        }
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail={"success": False, "message": f"激活模型失败: {str(e)}"}
        )

@admin_router.delete("/models/{model_id}")
async def delete_model(
    model_id: int,
    data_service: DataService = Depends(get_data_service)
):
    try:
        success = await data_service.delete_model(model_id)
        if not success:
            raise HTTPException(
                status_code=404,
                detail={"success": False, "message": "模型不存在"}
            )
        return {
            "success": True,
            "message": "模型删除成功"
        }
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail={"success": False, "message": f"删除模型失败: {str(e)}"}
        )
