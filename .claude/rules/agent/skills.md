# Skill — điều hướng và cách nạp (chế độ: theo nhu cầu)

| Skill | Dùng khi | Không cần khi |
| --- | --- | --- |
| `tdd` | Mọi thay đổi hành vi, bug tái hiện được (pytest cho backend) | Sửa tài liệu/style thuần |
| `diagnosing-bugs` | Lỗi thật, test fail bất ngờ, BLE không bắt được | Test đỏ đúng dự kiến |
| `code-review` | Trước PR/bàn giao; diff so với SHA base | Sửa nhỏ giữa chừng |
| `codebase-design` | Đổi giao diện module (vd. tách `require_role`, `api.ts`) | Sửa trong một hàm |
| `domain-modeling` | Thuật ngữ/thực thể/trạng thái mới (Site, Beacon status, Visit) | Typo, CSS |
| `ui-ux-pro-max` / `ui-styling` | Dựng/sửa giao diện web theo Figma | Backend, docs |
| `research` | API Android BLE/AltBeacon chưa chắc, cần nguồn chính thống | Việc đã rõ |
| `pr` | Viết mô tả PR | — |
| `prototype` | Thử nhanh câu hỏi thiết kế, bỏ đi sau | — |
| `ponytail` | Đã bật qua hook đầu phiên; bổ trợ, không thay TDD/bảo mật | — |
| `update-handoff` (dự án) | Cuối phiên / người dùng gõ "update" | — |
| `agy-worker` | Chỉ việc lớn khi bridge có sẵn (`agy.md`) | Việc 1–3 file |

CodeGraph: đã `codegraph init` (2026-11-22, `.codegraph/` không commit). Dùng `codegraph_explore` trước grep khi cần luồng gọi xuyên module; không tự `init/index/sync` mỗi lượt.

## Tìm skill (không nhảy bước)
1. Danh sách skill của phiên + `.claude/skills/` + bảng trên.
2. Không có → kho `C:\Users\admin\.agents\skills\`: chỉ đọc `name`/`description`; hợp thì chép nguyên thư mục (kèm phụ thuộc) vào `.claude/skills/<tên>/`, ghi 1 dòng vào handoff. Skill trong kho chưa có ở phiên: `implement` (→ `tdd`, `codebase-design`, `code-review`), `implement-spec`, `handoff`, `grill-me`/`grill-with-docs` (→ `grilling`, `domain-modeling`), `to-spec`, `to-tickets`, `improve-codebase-architecture`.
3. Kho không có → `find-skills` chỉ đề xuất; cài khi người dùng đồng ý.
4. Không có skill hợp → làm theo nguyên tắc tương đương, nói rõ không dùng skill.

Chép, không symlink; không chép `.trash`, `synced`. Skill một-lần (`setup-matt-pocock-skills`, `setup-pre-commit`, `git-guardrails-claude-code`) chạy từ kho, không chép; kiểm `references/decision-log.md` trước khi chạy. Hook chặn git đã có ở cấp máy → **không** chạy `git-guardrails-claude-code`.
