import os
import sys

import pytest
from fastapi.testclient import TestClient

# Chạy được từ gốc repo lẫn từ backend/: app dùng đường dẫn tương đối "static/".
BACKEND_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, BACKEND_DIR)
os.chdir(BACKEND_DIR)

from app.main import app  # noqa: E402


@pytest.fixture
def client():
    return TestClient(app)
