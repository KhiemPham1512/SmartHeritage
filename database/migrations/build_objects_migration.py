"""Sinh migration idempotent cho index / view / hàm / stored procedure từ PHẦN 3–5 của init_schema.sql.
Chạy lại script này mỗi khi sửa các đối tượng đó trong init_schema.sql để migration không bị lệch:
    python database/migrations/build_objects_migration.py
"""
import os
import re

HERE = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(HERE, "..", "init_schema.sql")
OUT = os.path.join(HERE, "2026-11-22_02_hqtcsdl_objects.sql")


def build(schema: str) -> str:
    start = schema.index("-- PHẦN 3:")
    end = schema.index("-- PHẦN 6:")
    body = schema[schema.rfind("\n", 0, start) + 1: schema.rfind("-- ====", 0, end)]
    body = re.sub(r"\bCREATE (VIEW|PROCEDURE|FUNCTION)\b", r"CREATE OR ALTER \1", body)
    body = re.sub(
        r"^CREATE NONCLUSTERED INDEX (\w+) ON (.+)$",
        lambda m: f"IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = '{m.group(1)}')\n"
                  f"    CREATE NONCLUSTERED INDEX {m.group(1)} ON {m.group(2)}",
        body,
        flags=re.M,
    )
    header = (
        "-- =========================================================================\n"
        "-- MIGRATION 2026-11-22 (02): INDEX, VIEW, HÀM, STORED PROCEDURE (trọng tâm môn HQTCSDL)\n"
        "-- SINH TỰ ĐỘNG từ init_schema.sql PHẦN 3–5 bởi build_objects_migration.py — KHÔNG sửa tay.\n"
        "-- Dành cho DB tạo từ init_schema.sql bản cũ (thiếu SP/View/Index). Chạy lại nhiều lần an toàn.\n"
        "-- Chạy SAU 2026-11-22_01_languages_translations.sql.\n"
        "-- =========================================================================\n"
        "USE SmartHeritageDB;\nGO\n\n"
    )
    return header + body.strip() + "\n"


if __name__ == "__main__":
    with open(SRC, encoding="utf-8") as f:
        out = build(f.read())
    with open(OUT, "w", encoding="utf-8", newline="\r\n") as f:
        f.write(out)
    print("written", OUT)
