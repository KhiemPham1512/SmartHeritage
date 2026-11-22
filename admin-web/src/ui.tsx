// FILE: admin-web/src/ui.tsx — thành phần giao diện dùng chung giữa các màn W2..W8
import type { CSSProperties } from "react";

export const BRAND_RED = "#80140e";

// Tô màu icon SVG trong public/ (bộ icon Figma docs/local/logos) bằng CSS mask
export function MaskIcon({ src, color = BRAND_RED, size = 19 }: { src: string; color?: string; size?: number }) {
  const style: CSSProperties = {
    width: size,
    height: size,
    flexShrink: 0,
    display: "inline-block",
    backgroundColor: color,
    WebkitMaskImage: `url('${src}')`,
    maskImage: `url('${src}')`,
    WebkitMaskSize: "contain",
    maskSize: "contain",
    WebkitMaskRepeat: "no-repeat",
    maskRepeat: "no-repeat",
    WebkitMaskPosition: "center",
    maskPosition: "center",
  };
  return <span aria-hidden style={style} />;
}

// Phân trang kiểu Figma (Trước · 1 2 3 … · Sau): hiện trang đầu, cuối và lân cận trang hiện tại
export function Pager({ page, pageCount, onChange }: { page: number; pageCount: number; onChange: (p: number) => void }) {
  const pages = [...new Set([1, page - 1, page, page + 1, pageCount])]
    .filter((p) => p >= 1 && p <= pageCount)
    .sort((a, b) => a - b);
  return (
    <div style={{ display: "flex", alignItems: "center", gap: 6 }}>
      <button type="button" style={pagerBtnStyle} disabled={page <= 1} onClick={() => onChange(page - 1)}>Trước</button>
      {pages.map((p, i) => (
        <span key={p} style={{ display: "inline-flex", gap: 6, alignItems: "center" }}>
          {i > 0 && p - pages[i - 1] > 1 ? <span style={{ color: "#94a3b8" }}>…</span> : null}
          <button
            type="button"
            onClick={() => onChange(p)}
            aria-current={p === page ? "page" : undefined}
            style={{
              ...pageNumStyle,
              color: p === page ? BRAND_RED : "#475569",
              backgroundColor: p === page ? "#fde8e6" : "transparent",
              fontWeight: p === page ? 700 : 500,
            }}
          >
            {p}
          </button>
        </span>
      ))}
      <button type="button" style={pagerBtnStyle} disabled={page >= pageCount} onClick={() => onChange(page + 1)}>Sau</button>
    </div>
  );
}

const pagerBtnStyle: CSSProperties = {
  height: 34, padding: "0 12px", border: "1px solid #cbd5e1", borderRadius: 8,
  backgroundColor: "#fff", fontSize: 13, fontWeight: 600, cursor: "pointer",
};
const pageNumStyle: CSSProperties = { minWidth: 30, height: 30, border: "none", borderRadius: 6, cursor: "pointer", fontSize: 13 };
