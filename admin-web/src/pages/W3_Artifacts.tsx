// FILE: admin-web/src/pages/W3_Artifacts.tsx
// NHIỆM VỤ CỦA TV3 (MINH TÂM): Lập trình màn hình W3 - Quản lý danh sách hiện vật
// API sử dụng: GET /api/artifacts/stats, GET /api/artifacts?lang=vi, DELETE /api/artifacts/{id}

interface W3Props {
  onNavigateW4: () => void;
}

export default function W3_Artifacts({ onNavigateW4 }: W3Props) {
  return (
    <div style={{ background: "#fff", padding: 24, borderRadius: 10, border: "1px solid #e7e5e4" }}>
      <h3 style={{ marginTop: 0 }}>W3. Quản lý Hiện vật (Phần việc của TV3 - Minh Tâm)</h3>
      <p>TV3 triển khai bảng danh sách hiện vật, bộ lọc tìm kiếm và 4 thẻ thống kê hiện vật tại file này.</p>
      <button
        type="button"
        onClick={onNavigateW4}
        style={{ padding: "8px 14px", background: "#d97706", color: "#fff", border: "none", borderRadius: 6, cursor: "pointer" }}
      >
        + Chuyển sang form Thêm Hiện vật (W4)
      </button>
    </div>
  );
}