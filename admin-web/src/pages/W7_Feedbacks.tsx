// FILE: admin-web/src/pages/W7_Feedbacks.tsx
// NHIỆM VỤ CỦA TV5: Lập trình màn hình W7 - Kiểm duyệt đánh giá & phản hồi của khách tham quan
// API sử dụng: GET /api/feedbacks, PUT /api/feedbacks/{id}/visibility, DELETE /api/feedbacks/{id}

export default function W7_Feedbacks() {
  return (
    <div style={{ background: "#fff", padding: 24, borderRadius: 10, border: "1px solid #e7e5e4" }}>
      <h3 style={{ marginTop: 0 }}>W7. Kiểm duyệt Đánh giá & Phản hồi (Phần việc của TV5)</h3>
      <p>TV5 triển khai 4 thẻ thống kê sao đánh giá và bảng duyệt Ẩn/Hiện/Xóa bình luận của du khách tại file này.</p>
    </div>
  );
}