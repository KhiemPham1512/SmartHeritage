---
paths:
  - "admin-web/**"
---
# Admin Web React + Vite (chỉ nạp khi đụng vùng này)

- Gọi API chỉ qua `src/api.ts` (`apiFetch`, đính kèm Bearer). Gặp 401/403 → đăng xuất về W1.
- Mỗi màn một file `src/pages/Wn_*.tsx`; style inline `CSSProperties` như code hiện có; màu chủ đạo đỏ đô `#80140e`, nền kem `#f1ede4`.
- Số liệu trên màn hình phải đến từ API; không để số cứng của Figma trong bản nộp (chỉ dùng khi API lỗi và phải ghi nhãn "minh họa").
- Icon lấy từ `docs/local/logos/<nhóm>/*.svg`, chép vào `admin-web/public/` giữ nguyên tên; ưu tiên SVG hơn PNG. `lucide-react` chỉ cho icon Figma không cung cấp.
- Đối chiếu Figma bằng ảnh `docs/local/W<n> · *.png` (Figma MCP gói Starter bị giới hạn lượt). Số liệu trong ảnh là minh họa; UUID/minor theo DB, không theo ảnh.
- Trước khi bàn giao: `npm run build` và `npm run lint` phải sạch.
