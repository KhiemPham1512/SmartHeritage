# FILE: backend/app/routers/auth_router.py
from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy import text
from sqlalchemy.orm import Session

from app.database import get_db
from app.schemas import (
    LoginRequest, RegisterRequest, UserProfileUpdate,
    AdminUserCreate, AdminUserUpdate
)
from app.auth import (
    hash_password, verify_password, create_access_token, get_current_user, require_admin
)

router = APIRouter(prefix="/api", tags=["1. Auth & Users (M1 - W1, W8, Mobile 1,2,3,10)"])

VALID_ROLES = ("ADMIN", "CONTENT_MANAGER", "VISITOR")


def check_role(role: str | None) -> None:
    if role is not None and role not in VALID_ROLES:
        raise HTTPException(status_code=400, detail="Vai trò không hợp lệ")


def check_language(db: Session, lang: str | None) -> None:
    # Users.preferred_lang có khóa ngoại tới Languages → báo 400 rõ ràng thay vì lỗi 500
    if lang is not None and not db.execute(text("SELECT 1 FROM dbo.Languages WHERE lang_code = :c"), {"c": lang}).first():
        raise HTTPException(status_code=400, detail="Ngôn ngữ chưa có trong hệ thống")


@router.post("/auth/login")
def login(req: LoginRequest, db: Session = Depends(get_db)):
    user = db.execute(
        text("SELECT * FROM dbo.Users WHERE email = :email"),
        {"email": req.email.strip()}
    ).mappings().first()

    if not user or not verify_password(req.password, user["password_hash"]):
        raise HTTPException(status_code=401, detail="Email hoặc mật khẩu không chính xác")
    if not user["is_active"]:
        raise HTTPException(status_code=403, detail="Tài khoản của bạn đang bị tạm khóa")

    token = create_access_token(user["user_id"], user["email"], user["role"])
    return {
        "access_token": token,
        "token_type": "bearer",
        "user": {
            "user_id": user["user_id"],
            "email": user["email"],
            "full_name": user["full_name"],
            "role": user["role"],
            "preferred_lang": user["preferred_lang"],
            "auto_beacon_notify": bool(user["auto_beacon_notify"])
        }
    }


@router.post("/auth/register")
def register(req: RegisterRequest, db: Session = Depends(get_db)):
    check_language(db, req.preferred_lang)
    exists = db.execute(
        text("SELECT user_id FROM dbo.Users WHERE email = :email"),
        {"email": req.email.strip()}
    ).first()
    if exists:
        raise HTTPException(status_code=400, detail="Email này đã được sử dụng")

    hashed = hash_password(req.password)
    res = db.execute(
        text("""
            INSERT INTO dbo.Users (email, password_hash, full_name, role, preferred_lang, auto_beacon_notify, is_active)
            OUTPUT INSERTED.user_id, INSERTED.email, INSERTED.full_name, INSERTED.role, INSERTED.preferred_lang
            VALUES (:email, :pwd, :name, 'VISITOR', :lang, 1, 1)
        """),
        {"email": req.email.strip(), "pwd": hashed, "name": req.full_name, "lang": req.preferred_lang}
    ).mappings().first()
    db.commit()

    token = create_access_token(res["user_id"], res["email"], res["role"])
    return {"access_token": token, "token_type": "bearer", "user": dict(res)}


@router.post("/auth/guest")
def guest_session(preferred_lang: str = Query("vi", description="Ngôn ngữ khách chọn ở Màn 1")):
    return {
        "mode": "GUEST",
        "preferred_lang": preferred_lang,
        "auto_beacon_notify": True,
        "message": "Khởi tạo phiên tham quan Khách (Guest) thành công"
    }


@router.get("/users/me")
def get_my_profile(current_user: dict = Depends(get_current_user)):
    return current_user


@router.put("/users/me")
def update_my_profile(
    req: UserProfileUpdate,
    current_user: dict = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    check_language(db, req.preferred_lang)
    db.execute(
        text("""
            UPDATE dbo.Users
            SET full_name = COALESCE(:full_name, full_name),
                preferred_lang = COALESCE(:preferred_lang, preferred_lang),
                auto_beacon_notify = COALESCE(:auto_beacon_notify, auto_beacon_notify),
                avatar_url = COALESCE(:avatar_url, avatar_url)
            WHERE user_id = :uid
        """),
        {
            "full_name": req.full_name,
            "preferred_lang": req.preferred_lang,
            "auto_beacon_notify": req.auto_beacon_notify,
            "avatar_url": req.avatar_url,
            "uid": current_user["user_id"]
        }
    )
    db.commit()
    return {"message": "Cập nhật hồ sơ cá nhân thành công"}


@router.get("/users", dependencies=[Depends(require_admin)])
def list_users_for_admin(
    search: str = Query(None, description="Tìm kiếm theo tên hoặc email"),
    db: Session = Depends(get_db)
):
    sql = """
        SELECT user_id, email, full_name, role, preferred_lang, auto_beacon_notify, is_active, created_at
        FROM dbo.Users WHERE 1=1
    """
    params = {}
    if search:
        sql += " AND (full_name LIKE :s OR email LIKE :s)"
        params["s"] = f"%{search}%"
    sql += " ORDER BY user_id ASC"
    rows = db.execute(text(sql), params).mappings().all()
    return [dict(r) for r in rows]


@router.post("/users", dependencies=[Depends(require_admin)])
def admin_create_user(req: AdminUserCreate, db: Session = Depends(get_db)):
    check_role(req.role)
    check_language(db, req.preferred_lang)
    exists = db.execute(text("SELECT user_id FROM dbo.Users WHERE email = :e"), {"e": req.email}).first()
    if exists:
        raise HTTPException(status_code=400, detail="Email đã tồn tại")
    row = db.execute(
        text("""
            INSERT INTO dbo.Users (email, password_hash, full_name, role, preferred_lang, is_active)
            OUTPUT INSERTED.user_id, INSERTED.email, INSERTED.full_name, INSERTED.role, INSERTED.is_active
            VALUES (:email, :pwd, :name, :role, :lang, :active)
        """),
        {
            "email": req.email,
            "pwd": hash_password(req.password),
            "name": req.full_name,
            "role": req.role,
            "lang": req.preferred_lang,
            "active": req.is_active
        }
    ).mappings().first()
    db.commit()
    return dict(row)


@router.put("/users/{user_id}")
def admin_update_user(
    user_id: int,
    req: AdminUserUpdate,
    admin: dict = Depends(require_admin),
    db: Session = Depends(get_db)
):
    check_role(req.role)
    if user_id == admin["user_id"] and (req.is_active is False or (req.role and req.role != "ADMIN")):
        # Tránh Admin tự khóa / tự hạ quyền rồi không còn ai quản trị được hệ thống
        raise HTTPException(status_code=400, detail="Không thể tự khóa hoặc hạ quyền tài khoản của chính bạn")
    check_language(db, req.preferred_lang)
    updated = db.execute(
        text("""
            UPDATE dbo.Users
            SET full_name = COALESCE(:full_name, full_name),
                role = COALESCE(:role, role),
                preferred_lang = COALESCE(:preferred_lang, preferred_lang),
                is_active = COALESCE(:is_active, is_active),
                password_hash = COALESCE(:pwd, password_hash)
            WHERE user_id = :uid
        """),
        {
            "full_name": req.full_name,
            "role": req.role,
            "preferred_lang": req.preferred_lang,
            "is_active": req.is_active,
            "pwd": hash_password(req.password) if req.password else None,
            "uid": user_id
        }
    ).rowcount
    if not updated:
        db.rollback()
        raise HTTPException(status_code=404, detail="Không tìm thấy tài khoản")
    db.commit()
    return {"message": f"Đã cập nhật tài khoản #{user_id}"}