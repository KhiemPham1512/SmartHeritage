// FILE: admin-web/src/pages/W6_FloorMap.tsx
// NHIỆM VỤ CỦA TV4: Lập trình màn hình W6 - Bản đồ vị trí iBeacon & Sơ đồ khu di tích
// API sử dụng: GET /api/sites/map, PUT /api/beacons/{id}/coordinates, PUT /api/sites/1/map

export default function W6_FloorMap() {
  return (
    <div style={{ background: "#fff", padding: 24, borderRadius: 10, border: "1px solid #e7e5e4" }}>
      <h3 style={{ marginTop: 0 }}>W6. Bản đồ Vị trí iBeacon & Khu di tích (Phần việc của TV4)</h3>
      <p>TV4 triển khai sơ đồ mặt bằng tương tác và form chấm tọa độ (X, Y) cho thiết bị iBeacon tại file này.</p>
    </div>
  );
}