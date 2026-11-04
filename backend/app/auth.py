# FILE: backend/app/auth.py
import os
import time
import json
import hmac
import base64
import hashlib
from fastapi import Depends, HTTPException
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from sqlalchemy import text
from sqlalchemy.orm import Session
from app.database import get_db

SECRET_KEY = os.getenv("JWT_SECRET", "smart-heritage-secret-key-2026")
TOKEN_EXPIRE_SECONDS = 60 * 60 * 24 * 7  # 7 ngày

# Kích hoạt nút ổ khóa Authorize chuẩn của Swagger UI
security_scheme = HTTPBearer(auto_error=False)


def hash_password(password: str) -> str:
    return hashlib.sha256(password.encode("utf-8")).hexdigest()


def verify_password(plain_password: str, stored_hash: str) -> bool:
    if plain_password == stored_hash:
        return True
    return hash_password(plain_password) == stored_hash


def _b64url_encode(data: bytes) -> str:
    return base64.urlsafe_b64encode(data).rstrip(b"=").decode("utf-8")


def _b64url_decode(data: str) -> bytes:
    padding = "=" * (-len(data) % 4)
    return base64.urlsafe_b64decode(data + padding)


def create_access_token(user_id: int, email: str, role: str) -> str:
    header = {"alg": "HS256", "typ": "JWT"}
    payload = {
        "sub": str(user_id),
        "email": email,
        "role": role,
        "exp": int(time.time()) + TOKEN_EXPIRE_SECONDS
    }
    segments = [
        _b64url_encode(json.dumps(header, separators=(",", ":")).encode("utf-8")),
        _b64url_encode(json.dumps(payload, separators=(",", ":")).encode("utf-8"))
    ]
    signing_input = ".".join(segments).encode("utf-8")
    signature = hmac.new(SECRET_KEY.encode("utf-8"), signing_input, hashlib.sha256).digest()
    segments.append(_b64url_encode(signature))
    return ".".join(segments)


def decode_access_token(token: str) -> dict:
    try:
        header_b64, payload_b64, sig_b64 = token.split(".")
        signing_input = f"{header_b64}.{payload_b64}".encode("utf-8")
        expected_sig = hmac.new(SECRET_KEY.encode("utf-8"), signing_input, hashlib.sha256).digest()
        if not hmac.compare_digest(_b64url_encode(expected_sig), sig_b64):
            raise ValueError("Chữ ký Token không hợp lệ")
        payload = json.loads(_b64url_decode(payload_b64).decode("utf-8"))
        if payload.get("exp", 0) < int(time.time()):
            raise ValueError("Token đã hết hạn")
        return payload
    except Exception as e:
        raise HTTPException(status_code=401, detail=f"Token không hợp lệ hoặc đã hết hạn: {str(e)}")


def get_current_user(
    credentials: HTTPAuthorizationCredentials = Depends(security_scheme),
    db: Session = Depends(get_db)
) -> dict:
    if not credentials or not credentials.credentials:
        raise HTTPException(status_code=401, detail="Vui lòng đăng nhập (Thiếu Bearer Token)")

    token = credentials.credentials.strip()
    # Tự động xử lý dù người dùng có gõ thêm chữ 'Bearer ' hay không
    if token.lower().startswith("bearer "):
        token = token[7:].strip()

    payload = decode_access_token(token)
    row = db.execute(
        text("""
            SELECT user_id, email, full_name, avatar_url, role, preferred_lang, auto_beacon_notify, is_active, created_at
            FROM dbo.Users WHERE user_id = :uid
        """),
        {"uid": int(payload["sub"])}
    ).mappings().first()
    if not row or not row["is_active"]:
        raise HTTPException(status_code=403, detail="Tài khoản không tồn tại hoặc đã bị khóa")
    return dict(row)