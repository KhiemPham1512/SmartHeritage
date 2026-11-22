---
name: update-handoff
description: Cập nhật handoff 4 tầng của Smart Heritage (.claude/local/). Dùng khi người dùng gõ "update", cuối phiên, sau một việc nhiều bước, hoặc khi context dài.
---
# Cập nhật handoff

File trong `.claude/local/` (không commit):
- `handoff.md` — trạng thái HIỆN TẠI, rủi ro, bước kế. Sửa đè. Trần 60 dòng / 5 KB.
- `handoff-tried.md` — bảng "Đã thử — KHÔNG làm lại": `| ngày | việc | kết luận | bằng chứng |`, ≤250 ký tự/dòng.
- `handoff-reference.md` — thứ ổn định: phiên bản, IP/cổng demo, tài khoản mẫu, đường dẫn. Sửa tại chỗ.
- `handoff-archive.md` — lịch sử đầy đủ; chỉ nối thêm cuối file; chỉ grep, không đọc cả file.

Quy trình:
1. Đọc `handoff.md`, sửa đè: ngày, trạng thái đúng hiện tại (xóa câu lỗi thời), rủi ro, bước kế.
2. Việc xong → xóa khỏi `handoff.md`. Thí nghiệm có kết luận → 1 dòng `handoff-tried.md`. Số liệu/lệnh/log dài → nối nguyên văn vào archive dưới `## YYYY-MM-DD — <chủ đề>`.
3. Mỗi bước kế kèm 1 câu vì sao và tác dụng.
4. Quyết định của người dùng → 1 dòng `.claude/references/decision-log.md`.
5. Cổng kích thước: `(Get-Content .claude/local/handoff.md).Count` và `(Get-Item .claude/local/handoff.md).Length`; vượt trần thì tỉa rồi đo lại.
6. Không dán log dài vào `handoff.md`, không tạo file handoff theo ngày, không ghi mật khẩu/chuỗi kết nối.
