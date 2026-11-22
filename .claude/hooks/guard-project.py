"""PreToolUse hook riêng dự án. Lệnh git nguy hiểm chung (reset --hard, clean, push --force, push main...)
đã do hook toàn máy ~/.claude/hooks/block-dangerous-git.sh chặn; file này chỉ thêm luật của Smart Heritage.
Exit 2 = chặn, stderr là lý do gửi cho Claude."""
import json
import re
import subprocess
import sys

RULES = [
    (r"\bgit\b.*\bpush\b.*\bdevelop\b", "push thẳng develop (dùng PR)"),
    (r"\bsqlcmd\b.*init_schema", "chạy init_schema.sql (xóa và tạo lại toàn bộ DB)"),
]


def current_branch() -> str:
    try:
        return subprocess.run(["git", "rev-parse", "--abbrev-ref", "HEAD"],
                              capture_output=True, text=True, timeout=5).stdout.strip()
    except Exception:
        return ""


def check(cmd: str, branch: str | None = None) -> str | None:
    for pattern, reason in RULES:
        if re.search(pattern, cmd, re.IGNORECASE):
            return reason
    if re.search(r"\bgit\b.*\bpush\b", cmd) and (branch if branch is not None else current_branch()) == "develop":
        return "push khi đang đứng ở develop"
    return None


def main() -> None:
    sys.stdin.reconfigure(encoding="utf-8", errors="replace")  # Windows mặc định cp1252
    sys.stderr.reconfigure(encoding="utf-8")
    try:
        data = json.loads(sys.stdin.read().lstrip("﻿"))
    except Exception:
        sys.exit(0)  # input lạ thì không chặn nhầm
    reason = check((data.get("tool_input") or {}).get("command") or "")
    if reason:
        print(f"Bị chặn bởi hook dự án: {reason}. Hỏi người dùng trước; họ tự chạy nếu đồng ý.", file=sys.stderr)
        sys.exit(2)


if __name__ == "__main__":
    if sys.argv[1:] == ["--selftest"]:
        assert check("git push origin develop", "feature/x")
        assert check("sqlcmd -S . -i database/init_schema.sql", "feature/x")
        assert check("git push", "develop")
        assert not check("git push -u origin feature/web-fix", "feature/web-fix")
        assert not check("git status", "develop")
        assert not check("git log --oneline origin/develop..origin/main", "feature/x")
        print("selftest ok")
    else:
        main()
