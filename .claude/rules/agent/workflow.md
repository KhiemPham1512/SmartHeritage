# Quy trình làm việc

- Đầu phiên chỉ đọc `.claude/local/handoff.md`; grep `handoff-tried.md` trước khi thí nghiệm; `handoff-archive.md` chỉ grep.
- Phân loại yêu cầu trước (backend/DB, web UI, mobile, bug, refactor, review, tài liệu, hỏi đáp), rồi chỉ dùng skill hợp (`rules/agent/skills.md`). Việc nhỏ hiển nhiên không cần vòng thiết kế dài.

## Vòng cho thay đổi có hành vi
1. Nắm ngữ cảnh: handoff + đoạn liên quan của `docs/nghiepvu.md`, code, ảnh Figma. Ghi mục tiêu + tiêu chí nghiệm thu ngắn.
2. Nhánh `feature/<việc>` từ `develop`, ghi SHA base. TDD: một test → đỏ đúng lý do → sửa → xanh → lát tiếp.
3. Triển khai tối thiểu, giữ ranh giới module (router/SQL ở backend, page ở web). Việc lớn gom vào một pipeline agy.
4. Review toàn diff so với SHA base (chuẩn dự án + yêu cầu). Bug → tái hiện, test hồi quy đỏ trước, sửa nguyên nhân gốc.
5. Cập nhật README/tài liệu API liên quan, handoff; PR theo `git.md`.
6. Dọn artifact tạm của việc đã xong.

Kiểm có mức độ: test liên quan trước, rồi build/lint phần đã đổi (không build web khi chỉ sửa backend). Ghi rõ kiểm nào chưa chạy và vì sao. Không cắt validation/bảo mật/test quan trọng để tiết kiệm token.

## Báo cáo sau mỗi vòng
Đã làm gì, giải quyết gì, kiểm bằng cách nào, giới hạn còn lại. Trạng thái tách riêng: trên nhánh / đã merge develop / đã chạy demo trên điện thoại. Bước kế + 1 câu vì sao. Test fail thì dán lỗi thật.

## Dọn dẹp được tự làm
Artifact/thư mục tạm do Claude tạo cho việc đã xong, sau khi đọc và chuyển thông tin cần sang handoff. Đường dẫn tuyệt đối trong workspace, không wildcard rộng, không `git clean`. Không bao giờ tự xóa: mã nguồn, tài liệu, rules/skills, test, `.env`, dữ liệu DB, `docs/local/`, file `.apk`.

## Khi nào dừng hỏi
Merge/push/triển khai; xóa ngoài phạm vi trên; sửa lịch sử git; chạy lại `init_schema.sql` hoặc lệnh phá dữ liệu DB; xung đột kiến trúc lớn; thay đổi hợp đồng API làm vỡ app mobile đã phát hành cho nhóm. Ngoài ra tự chủ làm tới mục tiêu.

## Thứ tự ưu tiên khi mâu thuẫn
Yêu cầu hiện tại + an toàn dữ liệu → CLAUDE.md/rules/decision-log → `docs/nghiepvu.md`, Figma, code thật → skill. README có thể sai: đối chiếu code.
