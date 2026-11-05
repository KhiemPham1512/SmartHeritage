// FILE: admin-web/src/pages/W5_Beacons.tsx
// NHIỆM VỤ CỦA TV4: Lập trình màn hình W5 - Quản lý thiết bị phát sóng iBeacon
// API sử dụng: GET /api/beacons/stats, GET / POST / PUT / DELETE /api/beacons

export default function W5_Beacons() {
  return (
    <div style={{ background: "#fff", padding: 24, borderRadius: 10, border: "1px solid #e7e5e4" }}>
      <h3 style={{ marginTop: 0 }}>W5. Quản lý Thiết bị iBeacon (Phần việc của TV4)</h3>
      <p>TV4 triển khai 4 thẻ KPI trạng thái pin/tín hiệu và bảng thêm/sửa/gán thiết bị iBeacon (BC-001...) tại file này.</p>
    </div>
  );
}