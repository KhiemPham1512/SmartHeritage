# Git — chế độ (A) GitHub nhóm (chốt 2026-11-22)

- Nhánh tích hợp `develop`; `main` chỉ cho bản đã kiểm chứng để nộp/bảo vệ. Mỗi việc một nhánh `feature/<việc>` từ `develop`, PR vào `develop`.
- Không push thẳng `develop`/`main` (hook toàn máy chặn `main`, hook dự án chặn `develop`). Push nhánh feature và mở PR: hỏi trước. Merge PR: giải thích thay đổi, nhánh đích, kiểm chứng, giới hạn, rồi **chờ người dùng đồng ý rõ ràng**. Được mở PR ≠ được merge.
- Ba trạng thái tách bạch: có trên nhánh feature / đã merge `develop` / đã chạy demo thật (điện thoại + web + backend).
- Commit chỉ khi người dùng yêu cầu. Message `<loại>(<phạm vi>): <việc>` (vd. `fix(admin-web): ...`), tiếng Việt hoặc Anh như lịch sử repo. Stage đường dẫn cụ thể, không `git add -A`.
- Không commit `.env`, `*.apk`, `.claude/local/`, `.claude/settings.local.json`, dữ liệu cá nhân.
- Không `reset --hard`, `clean`, `push --force`, `branch -D` khi chưa hỏi (hook chặn).
- Sửa file của thành viên khác (W3–W8, mobile): làm trên nhánh riêng, liệt kê file + lý do trong PR để người dùng báo nhóm.
- Nội dung PR: mục tiêu, thay đổi, kết quả kiểm, review, giới hạn, thay đổi DB/cấu hình. Dùng skill `pr`.
- Lưu ý hiện trạng: PR #13 (mobile M6) đã vào `main` mà chưa vào `develop` → cần PR `main → develop` để đồng bộ (hỏi trước).
