// FILE: admin-web/src/pages/W4_ArtifactForm.tsx — Thêm / sửa hiện vật đa ngôn ngữ (Figma W4)
import { useEffect, useMemo, useState, type ChangeEvent, type CSSProperties, type ReactNode } from "react";
import { CheckCircle2, Info, LoaderCircle, Play, Upload, UploadCloud, X } from "lucide-react";
import { apiFetch, mediaUrl, uploadMedia } from "../api";
import type { Artifact, ArtifactStatus, BeaconLite, GalleryItem, Language, Site, Translation } from "../types";
import { BRAND_RED } from "../ui";

interface Props {
  artifactId: string | null;
  onCancel: () => void;
  onSaved: () => void;
}

interface LangContent {
  name: string;
  summary: string;
  description: string;
  audio_url: string | null;
}

interface PendingFile {
  file: File;
  preview: string;
}

const BASE_LANGS = ["vi", "en"];
const EMPTY: LangContent = { name: "", summary: "", description: "", audio_url: null };
const LIMIT_MB = { image: 10, audio: 20, video: 100 };
const STATUS_INFO: Record<ArtifactStatus, { label: string; color: string; bg: string }> = {
  PUBLISHED: { label: "Đang hiển thị", color: "#15803d", bg: "#ecfdf3" },
  DRAFT: { label: "Bản nháp", color: "#b45309", bg: "#fff7ed" },
  HIDDEN: { label: "Tạm ẩn", color: "#475569", bg: "#f1f5f9" },
};

const fileName = (url: string | null | undefined) => (url ? decodeURIComponent(url.split("/").pop() ?? url) : "");
const sizeMB = (bytes: number) => (bytes / 1024 / 1024).toLocaleString("vi-VN", { maximumFractionDigits: 1 }) + " MB";
const words = (s: string) => s.trim().split(/\s+/).filter(Boolean).length;
const fmtSec = (s: number) => `${String(Math.floor(s / 60)).padStart(2, "0")}:${String(Math.round(s % 60)).padStart(2, "0")}`;

function parseGallery(json: string | null | undefined): GalleryItem[] {
  try {
    const arr = JSON.parse(json || "[]");
    return Array.isArray(arr) ? arr.filter((x) => typeof x?.url === "string") : [];
  } catch {
    return [];
  }
}

function checkFile(file: File, kind: "image" | "audio" | "video"): string | null {
  const ok = {
    image: /\.(jpe?g|png|webp)$/i,
    audio: /\.mp3$/i,
    video: /\.mp4$/i,
  }[kind].test(file.name);
  if (!ok) return kind === "audio" ? "Âm thanh chỉ nhận định dạng MP3." : "Định dạng không hỗ trợ (JPG, PNG, WebP hoặc MP4).";
  if (file.size > LIMIT_MB[kind] * 1024 * 1024) return `File ${file.name} vượt quá ${LIMIT_MB[kind]} MB.`;
  return null;
}

export default function W4_ArtifactForm({ artifactId, onCancel, onSaved }: Props) {
  const editing = Boolean(artifactId);

  // ---- dữ liệu tham chiếu ----
  const [languages, setLanguages] = useState<Language[]>([]);
  const [sites, setSites] = useState<Site[]>([]);
  const [beacons, setBeacons] = useState<BeaconLite[]>([]);
  const [loading, setLoading] = useState(true);

  // ---- form ----
  const [siteId, setSiteId] = useState(1);
  const [code, setCode] = useState("");
  const [zone, setZone] = useState("");
  const [status, setStatus] = useState<ArtifactStatus>("PUBLISHED");
  const [imageUrl, setImageUrl] = useState<string | null>(null);
  const [imageFile, setImageFile] = useState<PendingFile | null>(null);
  const [gallery, setGallery] = useState<GalleryItem[]>([]);
  const [galleryFiles, setGalleryFiles] = useState<PendingFile[]>([]);
  const [originalBeacon, setOriginalBeacon] = useState<number | null>(null);
  const [beaconId, setBeaconId] = useState<number | null>(null);
  const [duration, setDuration] = useState(0);
  const [content, setContent] = useState<Record<string, LangContent>>({ vi: { ...EMPTY }, en: { ...EMPTY } });
  const [originalExtras, setOriginalExtras] = useState<string[]>([]);
  const [tabs, setTabs] = useState<string[]>(BASE_LANGS);
  const [activeTab, setActiveTab] = useState("vi");
  const [audioFiles, setAudioFiles] = useState<Record<string, File>>({});
  const [title, setTitle] = useState("");

  const [dirty, setDirty] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    let active = true;
    const base = Promise.all([
      apiFetch<Language[]>("/api/languages"),
      apiFetch<Site[]>("/api/sites"),
      apiFetch<BeaconLite[]>("/api/beacons"),
    ]);
    const detail = artifactId
      ? Promise.all([
          apiFetch<Artifact>(`/api/artifacts/${artifactId}?lang=vi`),
          apiFetch<Translation[]>(`/api/artifacts/${artifactId}/translations`),
        ])
      : apiFetch<{ next_code: string }>("/api/artifacts/next-code");

    Promise.all([base, detail])
      .then(([[langs, siteList, beaconList], d]) => {
        if (!active) return;
        setLanguages(langs);
        setSites(siteList);
        setBeacons(beaconList);
        if (Array.isArray(d)) {
          const [a, trans] = d;
          setTitle(`${a.artifact_code} · ${a.name_vi}`);
          setSiteId(a.site_id ?? 1);
          setCode(a.artifact_code);
          setZone(a.zone_name ?? "");
          setStatus(a.status);
          setImageUrl(a.image_url);
          setGallery(parseGallery(a.gallery_json));
          setOriginalBeacon(a.beacon_id ?? null);
          setBeaconId(a.beacon_id ?? null);
          setDuration(a.audio_duration_sec ?? 0);
          const map: Record<string, LangContent> = {
            vi: { name: a.name_vi, summary: a.summary_vi, description: a.description_vi, audio_url: a.audio_vi_url },
            en: { name: a.name_en, summary: a.summary_en, description: a.description_en, audio_url: a.audio_en_url },
          };
          trans.forEach((t) => (map[t.lang_code] = { name: t.name, summary: t.summary, description: t.description, audio_url: t.audio_url }));
          setContent(map);
          setOriginalExtras(trans.map((t) => t.lang_code));
          setTabs([...BASE_LANGS, ...trans.map((t) => t.lang_code)]);
        } else {
          setCode(d.next_code);
        }
      })
      .catch((e: Error) => active && setError(e.message))
      .finally(() => active && setLoading(false));
    return () => {
      active = false;
    };
  }, [artifactId]);

  const langInfo = (codeLang: string) => languages.find((l) => l.lang_code === codeLang);
  const addableLangs = languages.filter((l) => l.is_enabled && !tabs.includes(l.lang_code));
  const touch = () => setDirty(true);

  const setField = (lang: string, key: keyof LangContent, value: string) => {
    setContent((old) => ({ ...old, [lang]: { ...(old[lang] ?? EMPTY), [key]: value } }));
    touch();
  };

  const isComplete = (lang: string) => {
    const c = content[lang];
    return Boolean(c && c.name.trim() && c.summary.trim() && c.description.trim());
  };

  // Chỉ gán beacon chưa gắn hiện vật nào, hoặc beacon đang gắn chính hiện vật này
  const beaconOptions = useMemo(
    () => beacons.filter((b) => b.artifact_id === null || b.beacon_id === originalBeacon),
    [beacons, originalBeacon],
  );

  // ---- chọn file ----
  const pickImage = (e: ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    e.target.value = "";
    if (!file) return;
    const problem = checkFile(file, "image");
    if (problem) return setError(problem);
    setImageFile({ file, preview: URL.createObjectURL(file) });
    setError("");
    touch();
  };

  const pickGallery = (e: ChangeEvent<HTMLInputElement>) => {
    const files = Array.from(e.target.files ?? []);
    e.target.value = "";
    const problems = files.map((f) => checkFile(f, /\.mp4$/i.test(f.name) ? "video" : "image")).filter(Boolean);
    if (problems.length) return setError(problems[0] as string);
    setGalleryFiles((old) => [...old, ...files.map((file) => ({ file, preview: URL.createObjectURL(file) }))]);
    setError("");
    touch();
  };

  const pickAudio = (lang: string) => (e: ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    e.target.value = "";
    if (!file) return;
    const problem = checkFile(file, "audio");
    if (problem) return setError(problem);
    setAudioFiles((old) => ({ ...old, [lang]: file }));
    if (lang === "vi") {
      // Thời lượng thuyết minh lấy theo file tiếng Việt (hiển thị trên app khách)
      const probe = new Audio(URL.createObjectURL(file));
      probe.onloadedmetadata = () => setDuration(Math.round(probe.duration));
    }
    setError("");
    touch();
  };

  const addLanguage = (lang: string) => {
    if (!lang) return;
    setTabs((old) => [...old, lang]);
    setContent((old) => ({ ...old, [lang]: old[lang] ?? { ...EMPTY } }));
    setActiveTab(lang);
    touch();
  };

  const removeLanguage = (lang: string) => {
    const label = langInfo(lang)?.name_native ?? lang;
    if (!window.confirm(`Xóa bản dịch ${label} của hiện vật này?`)) return;
    setTabs((old) => old.filter((l) => l !== lang));
    setActiveTab("vi");
    touch();
  };

  // ---- lưu ----
  const validate = (): Record<string, string> => {
    const errs: Record<string, string> = {};
    if (!/^HV-[A-Za-z0-9-]+$/i.test(code.trim())) errs.code = "Mã phải có dạng HV-xxx.";
    if (!zone.trim()) errs.zone = "Nhập khu vực trưng bày.";
    if (!imageUrl && !imageFile) errs.image = "Cần ảnh đại diện.";
    tabs.forEach((lang) => {
      const c = content[lang] ?? EMPTY;
      if (!c.name.trim()) errs[`${lang}.name`] = "Bắt buộc.";
      if (!c.summary.trim()) errs[`${lang}.summary`] = "Bắt buộc.";
      if (!c.description.trim()) errs[`${lang}.description`] = "Bắt buộc.";
    });
    return errs;
  };

  const save = async (asDraft: boolean) => {
    const errs = validate();
    setFieldErrors(errs);
    if (Object.keys(errs).length) {
      const firstLang = tabs.find((l) => Object.keys(errs).some((k) => k.startsWith(l + ".")));
      if (firstLang) setActiveTab(firstLang);
      setError("Vui lòng điền đủ các trường bắt buộc (dấu *).");
      return;
    }
    setSaving(true);
    setError("");
    try {
      const finalImage = imageFile ? await uploadMedia(imageFile.file) : imageUrl;
      const uploadedGallery: GalleryItem[] = [];
      for (const g of galleryFiles) {
        uploadedGallery.push({ url: await uploadMedia(g.file), type: /\.mp4$/i.test(g.file.name) ? "video" : "image" });
      }
      const audio: Record<string, string | null> = {};
      for (const lang of tabs) {
        audio[lang] = audioFiles[lang] ? await uploadMedia(audioFiles[lang]) : content[lang]?.audio_url ?? null;
      }

      const vi = content.vi;
      const en = content.en;
      const payload = {
        site_id: siteId,
        artifact_code: code.trim().toUpperCase(),
        zone_name: zone.trim(),
        name_vi: vi.name.trim(), summary_vi: vi.summary.trim(), description_vi: vi.description.trim(),
        name_en: en.name.trim(), summary_en: en.summary.trim(), description_en: en.description.trim(),
        image_url: finalImage,
        gallery_json: JSON.stringify([...gallery, ...uploadedGallery]),
        audio_vi_url: audio.vi,
        audio_en_url: audio.en,
        audio_duration_sec: Math.max(1, duration || 1),
        status: asDraft ? "DRAFT" : status,
      };

      let id = artifactId;
      if (editing) {
        const { artifact_code: _c, ...update } = payload; // mã không đổi sau khi tạo
        void _c;
        await apiFetch(`/api/artifacts/${artifactId}`, { method: "PUT", body: JSON.stringify(update) });
      } else {
        const created = await apiFetch<{ artifact_id: number }>("/api/artifacts", { method: "POST", body: JSON.stringify(payload) });
        id = String(created.artifact_id);
      }

      const extras = tabs.filter((l) => !BASE_LANGS.includes(l));
      for (const lang of extras) {
        const c = content[lang];
        await apiFetch(`/api/artifacts/${id}/translations/${lang}`, {
          method: "PUT",
          body: JSON.stringify({ name: c.name.trim(), summary: c.summary.trim(), description: c.description.trim(), audio_url: audio[lang] }),
        });
      }
      for (const lang of originalExtras.filter((l) => !extras.includes(l))) {
        await apiFetch(`/api/artifacts/${id}/translations/${lang}`, { method: "DELETE" });
      }
      if (beaconId !== null && beaconId !== originalBeacon) {
        await apiFetch(`/api/beacons/${beaconId}`, { method: "PUT", body: JSON.stringify({ artifact_id: Number(id) }) });
      }
      onSaved();
    } catch (e) {
      setError(e instanceof Error ? e.message : "Không lưu được hiện vật.");
    } finally {
      setSaving(false);
    }
  };

  const cancel = () => {
    if (dirty && !window.confirm("Bỏ các thay đổi chưa lưu?")) return;
    onCancel();
  };

  if (loading) {
    return (
      <div style={{ ...cardStyle, display: "flex", alignItems: "center", gap: 10, color: "#475569" }}>
        <LoaderCircle size={18} /> Đang tải thông tin hiện vật...
      </div>
    );
  }

  const tab = content[activeTab] ?? EMPTY;
  const tabLabel = activeTab === "vi" ? "Tiếng Việt" : activeTab === "en" ? "English" : langInfo(activeTab)?.name_native ?? activeTab;
  const statusInfo = STATUS_INFO[status];

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 14 }}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", gap: 12 }}>
        <div>
          <h2 style={{ fontSize: 24, fontWeight: 800, color: "#0f172a" }}>{editing ? "Chỉnh sửa hiện vật" : "Thêm hiện vật mới"}</h2>
          <p style={{ fontSize: 13, color: "#475569", marginTop: 2 }}>
            {editing ? title : "Nhập thông tin chung, nội dung từng ngôn ngữ và tệp thuyết minh."}
          </p>
        </div>
        <div style={{ display: "flex", gap: 8 }}>
          <span style={{ ...badgeStyle, color: statusInfo.color, backgroundColor: statusInfo.bg }}>{statusInfo.label}</span>
          {dirty ? <span style={{ ...badgeStyle, color: "#b45309", backgroundColor: "#fff7ed" }}>Có thay đổi chưa lưu</span> : null}
        </div>
      </div>

      {error ? <div role="alert" style={errorBoxStyle}>{error}</div> : null}

      <div style={{ display: "grid", gridTemplateColumns: "0.72fr 1.28fr", gap: 18, alignItems: "start" }}>
        {/* ---------------- CỘT TRÁI: THÔNG TIN CHUNG ---------------- */}
        <div style={cardStyle}>
          <h3 style={cardTitleStyle}>Thông tin chung</h3>
          <p style={cardSubStyle}>Thông tin dùng chung cho mọi ngôn ngữ</p>

          <Field label="Khu di tích / Tầng" required>
            <select value={siteId} onChange={(e) => { setSiteId(Number(e.target.value)); touch(); }} style={inputStyle}>
              {sites.map((s) => (
                <option key={s.site_id} value={s.site_id}>{s.name_vi}{s.floor_name ? ` · ${s.floor_name}` : ""}</option>
              ))}
            </select>
          </Field>
          <Field label="Khu vực trưng bày" required error={fieldErrors.zone}>
            <input value={zone} onChange={(e) => { setZone(e.target.value); touch(); }} placeholder="VD: Phòng Đông Sơn - Tầng 1" style={inputStyle} />
          </Field>
          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12 }}>
            <Field label="Mã hiện vật" required error={fieldErrors.code}>
              <input value={code} disabled={editing} onChange={(e) => { setCode(e.target.value); touch(); }} style={{ ...inputStyle, backgroundColor: editing ? "#f8fafc" : "#fff" }} />
            </Field>
            <Field label="iBeacon đã gắn">
              <select
                value={beaconId ?? ""}
                onChange={(e) => { setBeaconId(e.target.value ? Number(e.target.value) : null); touch(); }}
                style={inputStyle}
                title={originalBeacon !== null ? "Gỡ gán beacon ở màn Thiết bị iBeacon (W5)" : undefined}
              >
                <option value="" disabled={originalBeacon !== null}>— Chưa gán —</option>
                {beaconOptions.map((b) => <option key={b.beacon_id} value={b.beacon_id}>{b.beacon_code}</option>)}
              </select>
            </Field>
          </div>
          <Field label="Trạng thái hiển thị">
            <select value={status} onChange={(e) => { setStatus(e.target.value as ArtifactStatus); touch(); }} style={inputStyle}>
              <option value="PUBLISHED">Đang hiển thị</option>
              <option value="DRAFT">Bản nháp</option>
              <option value="HIDDEN">Tạm ẩn</option>
            </select>
          </Field>

          <div style={dividerStyle} />
          <div style={labelStyle}>Ảnh đại diện <b style={{ color: BRAND_RED }}>*</b></div>
          <div style={{ display: "flex", gap: 14, alignItems: "center" }}>
            {imageFile || imageUrl ? (
              <img src={imageFile?.preview ?? mediaUrl(imageUrl)} alt="Ảnh đại diện" style={{ width: 108, height: 100, objectFit: "cover", borderRadius: 8 }} />
            ) : (
              <div style={{ width: 108, height: 100, borderRadius: 8, backgroundColor: "#f1f5f9" }} />
            )}
            <div>
              <div style={{ fontSize: 13, color: "#0f172a" }}>{imageFile ? imageFile.file.name : fileName(imageUrl) || "Chưa có ảnh"}</div>
              {imageFile ? <div style={{ fontSize: 12, color: "#64748b", marginTop: 2 }}>{sizeMB(imageFile.file.size)}</div> : null}
              <label style={{ ...outlineBtnStyle, marginTop: 8 }}>
                <Upload size={15} /> {imageFile || imageUrl ? "Thay ảnh" : "Chọn ảnh"}
                <input type="file" accept=".jpg,.jpeg,.png,.webp" onChange={pickImage} hidden />
              </label>
            </div>
          </div>
          <div style={hintStyle}>JPG, PNG hoặc WebP · Tối đa 10 MB</div>
          {fieldErrors.image ? <div style={fieldErrorStyle}>{fieldErrors.image}</div> : null}

          <div style={dividerStyle} />
          <div style={{ display: "flex", justifyContent: "space-between" }}>
            <span style={labelStyle}>Bộ ảnh / Video</span>
            <span style={hintStyle}>{gallery.length + galleryFiles.length} tệp</span>
          </div>
          <div style={{ display: "grid", gridTemplateColumns: "repeat(3, 1fr)", gap: 8, marginTop: 8 }}>
            {gallery.map((g, i) => (
              <GalleryTile key={g.url} src={mediaUrl(g.url)} video={g.type === "video"} onRemove={() => { setGallery((old) => old.filter((_, j) => j !== i)); touch(); }} />
            ))}
            {galleryFiles.map((g, i) => (
              <GalleryTile key={g.preview} src={g.preview} video={/\.mp4$/i.test(g.file.name)} onRemove={() => { setGalleryFiles((old) => old.filter((_, j) => j !== i)); touch(); }} />
            ))}
          </div>
          <label style={dropZoneStyle}>
            <span style={{ display: "flex", alignItems: "center", gap: 6, color: BRAND_RED, fontWeight: 600 }}><UploadCloud size={16} /> Tải bộ ảnh / Video</span>
            <span style={hintStyle}>Chọn nhiều tệp · JPG, PNG ≤ 10 MB, MP4 ≤ 100 MB</span>
            <input type="file" multiple accept=".jpg,.jpeg,.png,.webp,.mp4" onChange={pickGallery} hidden />
          </label>
        </div>

        {/* ---------------- CỘT PHẢI: NỘI DUNG ĐA NGÔN NGỮ ---------------- */}
        <div style={cardStyle}>
          <div style={{ display: "flex", justifyContent: "space-between" }}>
            <div>
              <h3 style={cardTitleStyle}>Nội dung đa ngôn ngữ</h3>
              <p style={cardSubStyle}>Mỗi ngôn ngữ có tên, bài viết và file thuyết minh riêng.</p>
            </div>
            <span style={{ ...badgeStyle, color: "#475569", backgroundColor: "#f1f5f9", alignSelf: "flex-start" }}>{tabs.length} ngôn ngữ</span>
          </div>

          <div style={tabBarStyle} role="tablist">
            {tabs.map((lang) => {
              const active = lang === activeTab;
              const label = lang === "vi" ? "Tiếng Việt" : lang === "en" ? "English" : langInfo(lang)?.name_native ?? lang;
              return (
                <button
                  key={lang}
                  type="button"
                  role="tab"
                  aria-selected={active}
                  onClick={() => setActiveTab(lang)}
                  style={{ ...tabStyle, color: active ? BRAND_RED : "#475569", borderBottomColor: active ? BRAND_RED : "transparent", backgroundColor: active ? "#fdf2f1" : "transparent" }}
                >
                  {label}
                  {active ? <b style={{ fontSize: 11.5, marginLeft: 6 }}>{lang.toUpperCase()}</b> : null}
                  {!active && isComplete(lang) ? <CheckCircle2 size={14} color="#16a34a" style={{ marginLeft: 6 }} /> : null}
                </button>
              );
            })}
            <select
              value=""
              onChange={(e) => addLanguage(e.target.value)}
              disabled={addableLangs.length === 0}
              title={addableLangs.length === 0 ? "Bật thêm ngôn ngữ ở màn Ngôn ngữ & tài khoản (W8)" : undefined}
              style={addLangStyle}
              aria-label="Thêm ngôn ngữ"
            >
              <option value="">+ Thêm ngôn ngữ</option>
              {addableLangs.map((l) => <option key={l.lang_code} value={l.lang_code}>{l.name_native} ({l.lang_code.toUpperCase()})</option>)}
            </select>
          </div>

          <Field label={`Tên hiện vật · ${tabLabel}`} required error={fieldErrors[`${activeTab}.name`]}>
            <input value={tab.name} onChange={(e) => setField(activeTab, "name", e.target.value)} style={inputStyle} />
          </Field>
          <Field label={`Mô tả ngắn (hiện trên popup khi tới gần) · ${tabLabel}`} required error={fieldErrors[`${activeTab}.summary`]}>
            <input value={tab.summary} maxLength={500} onChange={(e) => setField(activeTab, "summary", e.target.value)} style={inputStyle} />
          </Field>
          <Field label={`Bài viết lịch sử chi tiết · ${tabLabel}`} required error={fieldErrors[`${activeTab}.description`]}>
            <textarea value={tab.description} rows={9} onChange={(e) => setField(activeTab, "description", e.target.value)} style={{ ...inputStyle, height: "auto", padding: "10px 12px", lineHeight: 1.55, resize: "vertical" }} />
            <div style={{ display: "flex", justifyContent: "space-between", ...hintStyle }}>
              <span>{tabLabel} ({activeTab.toUpperCase()})</span>
              <span>{words(tab.description)} từ</span>
            </div>
          </Field>

          <div style={{ display: "flex", alignItems: "center", gap: 8, marginTop: 6 }}>
            <span style={labelStyle}>Âm thanh thuyết minh</span>
            <span style={{ ...badgeStyle, color: BRAND_RED, backgroundColor: "#fdecea" }}>{tabLabel} · {activeTab.toUpperCase()}</span>
          </div>
          <div style={audioRowStyle}>
            <div style={{ display: "flex", alignItems: "center", gap: 12, minWidth: 0 }}>
              <span style={playCircleStyle}><Play size={15} color={BRAND_RED} /></span>
              <div style={{ minWidth: 0 }}>
                <div style={{ fontSize: 13.5, color: "#0f172a", overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>
                  {audioFiles[activeTab]?.name ?? (fileName(tab.audio_url) || "Chưa có file thuyết minh")}
                </div>
                <div style={hintStyle}>
                  {audioFiles[activeTab] ? `${sizeMB(audioFiles[activeTab].size)} · Chờ tải lên khi lưu` : tab.audio_url ? "Đã tải lên" : activeTab === "vi" || activeTab === "en" ? "" : "Chưa có → app phát audio tiếng Anh"}
                  {activeTab === "vi" && duration ? ` · ${fmtSec(duration)}` : ""}
                </div>
              </div>
            </div>
            <label style={outlineBtnStyle}>
              <Upload size={15} /> Tải âm thanh .mp3
              <input type="file" accept=".mp3" onChange={pickAudio(activeTab)} hidden />
            </label>
          </div>
          {tab.audio_url && !audioFiles[activeTab] ? <audio controls src={mediaUrl(tab.audio_url)} style={{ width: "100%", marginTop: 8, height: 34 }} /> : null}
          <div style={{ display: "flex", justifyContent: "space-between", marginTop: 8 }}>
            <span style={hintStyle}>Định dạng MP3 · Tối đa 20 MB. Chỉ áp dụng cho tab {tabLabel}.</span>
            {!BASE_LANGS.includes(activeTab) ? (
              <button type="button" onClick={() => removeLanguage(activeTab)} style={{ background: "none", border: "none", color: "#b91c1c", fontSize: 12.5, cursor: "pointer" }}>
                Xóa bản dịch này
              </button>
            ) : null}
          </div>
        </div>
      </div>

      <div style={{ ...cardStyle, display: "flex", justifyContent: "space-between", alignItems: "center", padding: "14px 20px" }}>
        <span style={{ display: "flex", alignItems: "center", gap: 8, fontSize: 13, color: "#475569" }}>
          <Info size={16} /> Dấu * là thông tin bắt buộc. Các thay đổi chỉ hiển thị sau khi lưu.
        </span>
        <div style={{ display: "flex", gap: 10 }}>
          <button type="button" style={secondaryBtnStyle} onClick={cancel} disabled={saving}>Hủy</button>
          <button type="button" style={secondaryBtnStyle} onClick={() => void save(true)} disabled={saving}>Lưu bản nháp</button>
          <button type="button" style={primaryBtnStyle} onClick={() => void save(false)} disabled={saving}>
            {saving ? <LoaderCircle size={16} /> : <CheckCircle2 size={16} />} {editing ? "Lưu thay đổi" : "Tạo hiện vật"}
          </button>
        </div>
      </div>
    </div>
  );
}

function Field({ label, required, error, children }: { label: string; required?: boolean; error?: string; children: ReactNode }) {
  return (
    <label style={{ display: "block", marginTop: 14 }}>
      <span style={labelStyle}>{label}{required ? <b style={{ color: BRAND_RED }}> *</b> : null}</span>
      <div style={{ marginTop: 6 }}>{children}</div>
      {error ? <div style={fieldErrorStyle}>{error}</div> : null}
    </label>
  );
}

function GalleryTile({ src, video, onRemove }: { src: string; video: boolean; onRemove: () => void }) {
  return (
    <div style={{ position: "relative", height: 66, borderRadius: 8, overflow: "hidden", backgroundColor: video ? BRAND_RED : "#f1f5f9" }}>
      {video ? (
        <div style={{ height: "100%", display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", color: "#fff", fontSize: 11.5, gap: 4 }}>
          <Play size={18} /> Video
        </div>
      ) : (
        <img src={src} alt="" style={{ width: "100%", height: "100%", objectFit: "cover" }} />
      )}
      <button type="button" onClick={onRemove} aria-label="Xóa tệp" style={tileRemoveStyle}><X size={12} /></button>
    </div>
  );
}

const cardStyle: CSSProperties = { backgroundColor: "#fff", border: "1px solid #e2e8f0", borderRadius: 11, padding: "18px 20px" };
const cardTitleStyle: CSSProperties = { fontSize: 16, fontWeight: 600, color: "#0f172a" };
const cardSubStyle: CSSProperties = { fontSize: 12.5, color: "#475569", marginTop: 2 };
const labelStyle: CSSProperties = { fontSize: 13, fontWeight: 600, color: "#1e293b" };
const hintStyle: CSSProperties = { fontSize: 11.5, color: "#64748b", marginTop: 4 };
const fieldErrorStyle: CSSProperties = { fontSize: 12, color: "#b91c1c", marginTop: 4 };
const inputStyle: CSSProperties = { width: "100%", height: 40, padding: "0 12px", border: "1px solid #cbd5e1", borderRadius: 8, fontSize: 13.5, color: "#0f172a", backgroundColor: "#fff" };
const dividerStyle: CSSProperties = { height: 1, backgroundColor: "#e2e8f0", margin: "16px 0" };
const badgeStyle: CSSProperties = { fontSize: 12, fontWeight: 600, padding: "4px 10px", borderRadius: 6 };
const errorBoxStyle: CSSProperties = { backgroundColor: "#fef2f2", color: "#b91c1c", border: "1px solid #fca5a5", padding: "8px 12px", borderRadius: 8, fontSize: 13, fontWeight: 600 };
const outlineBtnStyle: CSSProperties = { display: "inline-flex", alignItems: "center", gap: 8, height: 36, padding: "0 12px", border: "1px solid #cbd5e1", borderRadius: 8, backgroundColor: "#fff", fontSize: 13, fontWeight: 600, color: "#0f172a", cursor: "pointer", whiteSpace: "nowrap" };
const dropZoneStyle: CSSProperties = { display: "flex", flexDirection: "column", alignItems: "center", gap: 4, marginTop: 10, padding: "14px 10px", border: "1.5px dashed #cbd5e1", borderRadius: 8, cursor: "pointer", fontSize: 13 };
const tileRemoveStyle: CSSProperties = { position: "absolute", top: 4, right: 4, width: 20, height: 20, borderRadius: "50%", border: "none", backgroundColor: "rgba(15,23,42,0.6)", color: "#fff", display: "flex", alignItems: "center", justifyContent: "center", cursor: "pointer" };
const tabBarStyle: CSSProperties = { display: "flex", alignItems: "center", gap: 4, borderBottom: "1px solid #e2e8f0", marginTop: 16 };
const tabStyle: CSSProperties = { display: "inline-flex", alignItems: "center", height: 40, padding: "0 16px", background: "none", border: "none", borderBottom: "2px solid transparent", fontSize: 13.5, cursor: "pointer" };
const addLangStyle: CSSProperties = { height: 32, marginLeft: 8, border: "none", color: BRAND_RED, fontWeight: 600, fontSize: 13.5, backgroundColor: "transparent", cursor: "pointer" };
const audioRowStyle: CSSProperties = { display: "flex", justifyContent: "space-between", alignItems: "center", gap: 12, marginTop: 10, padding: "12px 14px", border: "1px solid #e2e8f0", borderRadius: 8 };
const playCircleStyle: CSSProperties = { width: 34, height: 34, borderRadius: "50%", backgroundColor: "#fdecea", display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 };
const secondaryBtnStyle: CSSProperties = { height: 40, padding: "0 16px", border: "1px solid #cbd5e1", borderRadius: 8, backgroundColor: "#fff", fontSize: 13.5, fontWeight: 600, color: "#0f172a", cursor: "pointer" };
const primaryBtnStyle: CSSProperties = { display: "inline-flex", alignItems: "center", gap: 8, height: 40, padding: "0 18px", border: "none", borderRadius: 8, backgroundColor: BRAND_RED, color: "#fff", fontSize: 13.5, fontWeight: 700, cursor: "pointer" };
