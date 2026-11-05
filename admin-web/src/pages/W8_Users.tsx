// FILE: admin-web/src/pages/W8_Users.tsx
// NHIỆM VỤ CỦA TV5: Lập trình màn hình W8 - Quản lý Ngôn ngữ & Tài khoản hệ thống
// API sử dụng: GET /api/users, POST /api/users, PUT /api/users/{id}

export default function W8_Users() {
  return (
    <div style={{ background: "#fff", padding: 24, borderRadius: 10, border: "1px solid #e7e5e4" }}>
      <h3 style={{ marginTop: 0 }}>W8. Quản lý Ngôn ngữ & Tài khoản (Phần việc của TV5)</h3>
      <p>TV5 triển khai danh sách người dùng, phân quyền (ADMIN / CONTENT_MANAGER / VISITOR) và cấu hình ngôn ngữ tại file này.</p>
    </div>
  );
}