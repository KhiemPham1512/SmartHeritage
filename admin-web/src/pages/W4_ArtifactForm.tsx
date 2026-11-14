// FILE: admin-web/src/pages/W4_ArtifactForm.tsx
// NHIỆM VỤ CỦA TV3 (MINH TÂM): Lập trình màn hình W4 - Thêm/Sửa hiện vật đa ngôn ngữ & Upload Ảnh/MP3
// API sử dụng: POST /api/upload/media, POST /api/artifacts, PUT /api/artifacts/{id}

export default function W4_ArtifactForm() {
  return (
    <div style={{ background: "#fff", padding: 24, borderRadius: 10, border: "1px solid #e7e5e4" }}>
      <h3 style={{ marginTop: 0 }}>W4. Thêm / Sửa Hiện vật & Đa ngôn ngữ (Phần việc của TV3 - Minh Tâm)</h3>
      <p>TV3 triển khai form nhập thông tin hiện vật (Tab Tiếng Việt / Tiếng Anh) và tải file Ảnh + Audio MP3 tại file này.</p>
    </div>
  );
}