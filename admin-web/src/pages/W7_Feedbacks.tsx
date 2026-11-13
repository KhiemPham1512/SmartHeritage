// FILE: admin-web/src/pages/W7_Feedbacks.tsx
import { useCallback, useEffect, useMemo, useState } from "react";
import type { CSSProperties } from "react";
import { apiFetch } from "../api";

interface Feedback {
  feedback_id: number;
  visitor_name: string;
  artifact_id: number;
  artifact_code?: string;
  artifact_name?: string;
  rating: number;
  comment?: string | null;
  is_visible: boolean;
  created_at: string;
}

interface FeedbackStats {
  total_reviews?: number;
  total_feedbacks?: number;
  avg_rating?: number;
  visible_count?: number;
  hidden_count?: number;
}

interface FeedbackResponse {
  stats?: FeedbackStats;
  items?: Feedback[];
}

const EMPTY_STATS: FeedbackStats = {
  total_reviews: 0,
  total_feedbacks: 0,
  avg_rating: 0,
  visible_count: 0,
  hidden_count: 0,
};

const C = {
  red: "#80140E",
  redLight: "#FDF0EF",
  background: "#F1EDE4",
  white: "#FFFFFF",
  text: "#0F172A",
  muted: "#475569",
  border: "#D4CDC3",
  green: "#15803D",
  greenLight: "#EAF7EE",
  orange: "#D58B32",
  gray: "#64748B",
};

export default function W7_Feedbacks() {
  const [feedbacks, setFeedbacks] = useState<Feedback[]>([]);
  const [stats, setStats] = useState<FeedbackStats>(EMPTY_STATS);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [keyword, setKeyword] = useState("");
  const [artifactFilter, setArtifactFilter] = useState("all");
  const [ratingFilter, setRatingFilter] = useState("all");
  const [statusFilter, setStatusFilter] = useState("all");
  const [sortOrder, setSortOrder] = useState("newest");
  const [currentPage, setCurrentPage] = useState(1);
  const [processingId, setProcessingId] = useState<number | null>(null);
  const [notice, setNotice] = useState<{ type: "success" | "error"; text: string } | null>(null);

  const pageSize = 6;

  const loadFeedbacks = useCallback(async () => {
    setLoading(true);
    setError("");
    setNotice(null);

    try {
      const response = await apiFetch<FeedbackResponse>("/api/feedbacks");
      setFeedbacks(Array.isArray(response.items) ? response.items : []);
      setStats(response.stats ?? EMPTY_STATS);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Không thể tải danh sách đánh giá.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void Promise.resolve().then(loadFeedbacks);
  }, [loadFeedbacks]);

  const totalReviewCount = Number(stats.total_reviews ?? stats.total_feedbacks ?? 0);

  const filteredFeedbacks = useMemo(() => {
    let result = [...feedbacks];

    if (keyword.trim()) {
      const search = keyword.toLowerCase().trim();
      result = result.filter((item) =>
        [item.visitor_name, item.comment, item.artifact_name, item.artifact_code]
          .join(" ")
          .toLowerCase()
          .includes(search)
      );
    }

    if (artifactFilter !== "all") {
      result = result.filter((item) => String(item.artifact_id) === artifactFilter);
    }

    if (ratingFilter !== "all") {
      result = result.filter((item) => item.rating === Number(ratingFilter));
    }

    if (statusFilter === "visible") {
      result = result.filter((item) => item.is_visible);
    }

    if (statusFilter === "hidden") {
      result = result.filter((item) => !item.is_visible);
    }

    result.sort((a, b) => {
      const timeA = new Date(a.created_at).getTime();
      const timeB = new Date(b.created_at).getTime();
      return sortOrder === "newest" ? timeB - timeA : timeA - timeB;
    });

    return result;
  }, [feedbacks, keyword, artifactFilter, ratingFilter, statusFilter, sortOrder]);

  const artifacts = useMemo(() => {
    const unique = new Map<string, string>();
    feedbacks.forEach((item) => {
      const id = String(item.artifact_id);
      const name = item.artifact_name || item.artifact_code || `Hiện vật #${id}`;
      unique.set(id, name);
    });
    return Array.from(unique.entries());
  }, [feedbacks]);

  const totalPages = Math.max(1, Math.ceil(filteredFeedbacks.length / pageSize));
  const safePage = Math.min(currentPage, totalPages);
  const paginatedFeedbacks = filteredFeedbacks.slice((safePage - 1) * pageSize, safePage * pageSize);

  const toggleVisibility = async (item: Feedback) => {
    const nextVisible = !item.is_visible;
    const confirmed = window.confirm(
      nextVisible ? "Bạn có muốn hiển thị lại đánh giá này?" : "Bạn có chắc chắn muốn ẩn đánh giá này?"
    );
    if (!confirmed) return;

    setProcessingId(item.feedback_id);
    setError("");

    try {
      await apiFetch(`/api/feedbacks/${item.feedback_id}/visibility`, {
        method: "PUT",
        body: JSON.stringify({ is_visible: nextVisible }),
      });
      await loadFeedbacks();
      setNotice({
        type: "success",
        text: nextVisible ? "Đánh giá đã được hiển thị lại." : "Đánh giá đã được ẩn thành công.",
      });
    } catch (err) {
      setError(err instanceof Error ? err.message : "Không thể cập nhật trạng thái đánh giá.");
    } finally {
      setProcessingId(null);
    }
  };

  const deleteFeedback = async (item: Feedback) => {
    const confirmed = window.confirm(
      `Bạn có chắc chắn muốn xóa đánh giá của ${item.visitor_name}? Hành động này không thể hoàn tác.`
    );
    if (!confirmed) return;

    setProcessingId(item.feedback_id);
    setError("");

    try {
      await apiFetch(`/api/feedbacks/${item.feedback_id}`, {
        method: "DELETE",
      });
      await loadFeedbacks();
      setNotice({ type: "success", text: "Đánh giá đã được xóa thành công." });
    } catch (err) {
      setError(err instanceof Error ? err.message : "Không thể xóa đánh giá.");
      setNotice({ type: "error", text: "Xóa đánh giá thất bại." });
    } finally {
      setProcessingId(null);
    }
  };

  const formatDate = (value: string) => {
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
      return value;
    }
    return date.toLocaleString("vi-VN", {
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  };

  const cardStyle: CSSProperties = {
    background: C.white,
    border: `1.5px solid ${C.border}`,
    borderRadius: 11,
    padding: "16px 20px",
  };

  const selectStyle: CSSProperties = {
    height: 38,
    border: `1.5px solid ${C.border}`,
    borderRadius: 9,
    background: C.white,
    color: C.text,
    padding: "0 12px",
    fontSize: 12,
    outline: "none",
    minWidth: 120,
  };

  const actionButtonStyle: CSSProperties = {
    border: `1.5px solid ${C.border}`,
    borderRadius: 8,
    background: C.white,
    padding: "7px 11px",
    fontSize: 12,
    cursor: "pointer",
    color: C.text,
    fontWeight: 600,
    transition: "all 0.2s ease",
  };

  return (
    <div
      style={{
        width: "100%",
        minHeight: "100%",
        background: C.background,
        padding: "20px",
        boxSizing: "border-box",
        fontFamily: "Arial, sans-serif",
        color: C.text,
      }}
    >
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          gap: 12,
          flexWrap: "wrap",
          marginBottom: 18,
        }}
      >
        <div>
          <div style={{ fontSize: 19, fontWeight: 800, color: C.text }}>Đánh giá & phản hồi</div>
          <div style={{ fontSize: 12, color: C.muted, marginTop: 4 }}>
            Kiểm duyệt bình luận và theo dõi chất lượng trải nghiệm khách tham quan.
          </div>
        </div>

        <button
          type="button"
          onClick={() => void loadFeedbacks()}
          disabled={loading}
          style={{
            ...actionButtonStyle,
            borderColor: C.red,
            color: C.red,
            background: loading ? "#f8fafc" : "#fff",
            opacity: loading ? 0.7 : 1,
          }}
        >
          ↻ Làm mới dữ liệu
        </button>
      </div>

      {error && (
        <div
          role="alert"
          style={{
            background: C.redLight,
            color: C.red,
            border: "1px solid #F2C7C4",
            padding: 12,
            borderRadius: 8,
            marginBottom: 15,
            fontSize: 12,
            fontWeight: 600,
          }}
        >
          ⚠ {error}
        </div>
      )}

      {notice && (
        <div
          style={{
            background: notice.type === "success" ? "#eaf7ee" : "#fff7ed",
            border: `1px solid ${notice.type === "success" ? "#b5d9be" : "#f6d3a6"}`,
            color: notice.type === "success" ? C.green : C.orange,
            padding: 12,
            borderRadius: 8,
            marginBottom: 15,
            fontSize: 12,
            fontWeight: 600,
          }}
        >
          {notice.text}
        </div>
      )}

      <div style={{ display: "grid", gridTemplateColumns: "repeat(4, minmax(0, 1fr))", gap: 12, marginBottom: 18 }}>
        {[
          { label: "Tổng đánh giá", value: totalReviewCount, icon: "▤", color: C.text },
          { label: "Điểm trung bình", value: `${Number(stats.avg_rating ?? 0).toFixed(1)} / 5`, icon: "★", color: C.orange },
          { label: "Đang hiển thị", value: Number(stats.visible_count ?? 0), icon: "◎", color: C.green },
          { label: "Đã ẩn", value: Number(stats.hidden_count ?? 0), icon: "◉", color: C.gray },
        ].map((stat) => (
          <div key={stat.label} style={cardStyle}>
            <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
              <span style={{ fontSize: 22, color: stat.color }}>{stat.icon}</span>
              <div>
                <div style={{ fontSize: 11, color: C.muted, marginBottom: 5 }}>{stat.label}</div>
                <div style={{ fontSize: 21, fontWeight: 800, color: stat.color }}>{stat.value}</div>
              </div>
            </div>
          </div>
        ))}
      </div>

      <div style={{ ...cardStyle, padding: 12, marginBottom: 12 }}>
        <div style={{ display: "flex", alignItems: "center", gap: 10, flexWrap: "wrap" }}>
          <input
            value={keyword}
            onChange={(e) => {
              setKeyword(e.target.value);
              setCurrentPage(1);
            }}
            placeholder="⌕ Tìm khách hàng, hiện vật hoặc bình luận..."
            style={{
              flex: "1 1 250px",
              height: 38,
              border: `1.5px solid ${C.border}`,
              borderRadius: 9,
              padding: "0 12px",
              fontSize: 12,
              minWidth: 200,
              outline: "none",
            }}
          />

          <select value={artifactFilter} onChange={(e) => { setArtifactFilter(e.target.value); setCurrentPage(1); }} style={selectStyle}>
            <option value="all">Tất cả hiện vật</option>
            {artifacts.map(([id, name]) => (
              <option key={id} value={id}>{name}</option>
            ))}
          </select>

          <select value={ratingFilter} onChange={(e) => { setRatingFilter(e.target.value); setCurrentPage(1); }} style={selectStyle}>
            <option value="all">Tất cả số sao</option>
            {[5, 4, 3, 2, 1].map((n) => (
              <option key={n} value={n}>{n} sao</option>
            ))}
          </select>

          <select value={statusFilter} onChange={(e) => { setStatusFilter(e.target.value); setCurrentPage(1); }} style={selectStyle}>
            <option value="all">Tất cả trạng thái</option>
            <option value="visible">Đang hiển thị</option>
            <option value="hidden">Đã ẩn</option>
          </select>

          <select value={sortOrder} onChange={(e) => { setSortOrder(e.target.value); setCurrentPage(1); }} style={selectStyle}>
            <option value="newest">Mới nhất</option>
            <option value="oldest">Cũ nhất</option>
          </select>
        </div>
      </div>

      <div style={{ ...cardStyle, padding: 0, overflow: "hidden" }}>
        <div style={{ padding: "14px 15px", borderBottom: `1px solid ${C.border}`, fontSize: 13, fontWeight: 800 }}>
          Danh sách đánh giá
          <span style={{ color: C.muted, fontWeight: 600, marginLeft: 7 }}>({filteredFeedbacks.length} kết quả)</span>
        </div>

        <div style={{ overflowX: "auto" }}>
          <table style={{ width: "100%", minWidth: 850, borderCollapse: "collapse", fontSize: 11 }}>
            <thead>
              <tr style={{ background: "#F4F6F9" }}>
                {['Tên khách hàng', 'Hiện vật', 'Số sao', 'Nội dung bình luận', 'Thời gian gửi', 'Trạng thái', 'Kiểm duyệt'].map((heading) => (
                  <th key={heading} style={{ padding: "11px 9px", textAlign: "left", color: C.gray, fontWeight: 700, whiteSpace: "nowrap", borderBottom: `1px solid ${C.border}` }}>
                    {heading}
                  </th>
                ))}
              </tr>
            </thead>

            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={7} style={{ padding: 35, textAlign: "center", color: C.muted }}>
                    Đang tải dữ liệu đánh giá...
                  </td>
                </tr>
              ) : paginatedFeedbacks.length === 0 ? (
                <tr>
                  <td colSpan={7} style={{ padding: 35, textAlign: "center", color: C.muted }}>
                    Không tìm thấy đánh giá phù hợp.
                  </td>
                </tr>
              ) : (
                paginatedFeedbacks.map((item) => (
                  <tr key={item.feedback_id} style={{ borderBottom: `1px solid ${C.border}` }}>
                    <td style={{ padding: 11 }}>
                      <div style={{ fontWeight: 700 }}>{item.visitor_name || "Khách tham quan"}</div>
                      <div style={{ color: C.muted, fontSize: 10, marginTop: 4 }}>ID: {item.feedback_id}</div>
                    </td>

                    <td style={{ padding: 11 }}>
                      <div style={{ fontWeight: 600 }}>{item.artifact_name || `Hiện vật #${item.artifact_id}`}</div>
                      <div style={{ color: C.muted, fontSize: 10, marginTop: 4 }}>{item.artifact_code || `ID: ${item.artifact_id}`}</div>
                    </td>

                    <td style={{ padding: 11 }}>
                      <div style={{ color: C.orange, fontSize: 13, whiteSpace: "nowrap" }}>
                        {"★".repeat(Math.max(0, Math.min(5, item.rating)))}
                        <span style={{ color: "#D9DEE6" }}>{"★".repeat(5 - Math.max(0, Math.min(5, item.rating)))}</span>
                      </div>
                      <div style={{ fontSize: 10, color: C.muted, marginTop: 3 }}>{item.rating}/5</div>
                    </td>

                    <td style={{ padding: 11, maxWidth: 260, color: C.text, lineHeight: 1.6 }}>
                      {item.comment || "Không có bình luận"}
                    </td>

                    <td style={{ padding: 11, whiteSpace: "nowrap", color: C.muted }}>
                      {formatDate(item.created_at)}
                    </td>

                    <td style={{ padding: 11 }}>
                      <span
                        style={{
                          display: "inline-block",
                          padding: "5px 9px",
                          borderRadius: 6,
                          background: item.is_visible ? C.greenLight : "#F0F1F3",
                          color: item.is_visible ? C.green : C.gray,
                          fontSize: 10,
                          fontWeight: 700,
                          whiteSpace: "nowrap",
                        }}
                      >
                        {item.is_visible ? "Hiển thị" : "Đã ẩn"}
                      </span>
                    </td>

                    <td style={{ padding: 9 }}>
                      <div style={{ display: "flex", gap: 5, flexWrap: "wrap" }}>
                        <button
                          type="button"
                          disabled={processingId === item.feedback_id}
                          onClick={() => void toggleVisibility(item)}
                          style={{
                            ...actionButtonStyle,
                            padding: "6px 8px",
                            color: item.is_visible ? C.gray : C.green,
                            opacity: processingId === item.feedback_id ? 0.5 : 1,
                          }}
                        >
                          {processingId === item.feedback_id ? "Đang xử lý..." : item.is_visible ? "Ẩn" : "Duyệt"}
                        </button>

                        <button
                          type="button"
                          disabled={processingId === item.feedback_id}
                          onClick={() => void deleteFeedback(item)}
                          style={{
                            ...actionButtonStyle,
                            padding: "6px 8px",
                            color: C.red,
                          }}
                        >
                          Xóa
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        <div style={{ padding: 13, display: "flex", alignItems: "center", justifyContent: "space-between", gap: 10, flexWrap: "wrap", borderTop: `1px solid ${C.border}` }}>
          <span style={{ fontSize: 11, color: C.muted }}>
            Hiển thị {filteredFeedbacks.length === 0 ? 0 : (safePage - 1) * pageSize + 1} - {Math.min(safePage * pageSize, filteredFeedbacks.length)} trên {filteredFeedbacks.length} kết quả
          </span>

          <div style={{ display: "flex", gap: 5 }}>
            <button type="button" disabled={safePage === 1} onClick={() => setCurrentPage((prev) => Math.max(1, prev - 1))} style={{ ...actionButtonStyle, opacity: safePage === 1 ? 0.4 : 1 }}>
              Trước
            </button>
            <span style={{ minWidth: 34, textAlign: "center", padding: "7px 10px", background: C.red, color: C.white, borderRadius: 6, fontSize: 11, fontWeight: 700 }}>
              {safePage}
            </span>
            <button type="button" disabled={safePage === totalPages} onClick={() => setCurrentPage((prev) => Math.min(totalPages, prev + 1))} style={{ ...actionButtonStyle, opacity: safePage === totalPages ? 0.4 : 1 }}>
              Sau
            </button>
          </div>
        </div>
      </div>

      <div style={{ marginTop: 14, fontSize: 11, color: C.muted }}>
        ⓘ Bình luận không phù hợp sẽ bị ẩn khỏi giao diện khách tham quan và cần được kiểm duyệt lại đúng quy trình.
      </div>
    </div>
  );
}
