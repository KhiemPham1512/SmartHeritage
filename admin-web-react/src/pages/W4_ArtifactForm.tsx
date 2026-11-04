import { type ChangeEvent, type FormEvent, useEffect, useState, type ReactNode } from "react";
import { ArrowLeft, ImagePlus, Music2, Save, UploadCloud, X, LoaderCircle } from "lucide-react";
import { apiFetch, mediaUrl, uploadMedia } from "../api";
import type { Artifact, ArtifactPayload, ArtifactStatus } from "../types";

interface Props { artifactId: string | null; onCancel: () => void; onSaved: () => void; }
type FormState = {
  site_id: number; artifact_code: string; zone_name: string; name_vi: string; name_en: string;
  summary_vi: string; summary_en: string; description_vi: string; description_en: string;
  image_url: string; audio_vi_url: string; audio_en_url: string; audio_duration_sec: number;
  translated_langs: string; map_x: string; map_y: string; status: ArtifactStatus;
};
const initial: FormState = {
  site_id: 1, artifact_code: "", zone_name: "", name_vi: "", name_en: "",
  summary_vi: "", summary_en: "", description_vi: "", description_en: "",
  image_url: "", audio_vi_url: "", audio_en_url: "", audio_duration_sec: 0,
  translated_langs: "VI,EN", map_x: "0", map_y: "0", status: "DRAFT",
};
const requiredFields: (keyof FormState)[] = ["artifact_code", "zone_name", "name_vi", "name_en", "summary_vi", "summary_en", "description_vi", "description_en"];

export default function W4_ArtifactForm({ artifactId, onCancel, onSaved }: Props) {
  const editing = Boolean(artifactId);
  const [form, setForm] = useState<FormState>(initial);
  const [imageFile, setImageFile] = useState<File | null>(null);
  const [audioViFile, setAudioViFile] = useState<File | null>(null);
  const [audioEnFile, setAudioEnFile] = useState<File | null>(null);
  const [imagePreview, setImagePreview] = useState("");
  const [loading, setLoading] = useState(editing);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    if (!artifactId) return;
    let active = true;
    apiFetch<Artifact>(`/artifacts/${artifactId}?lang=vi`)
      .then(data => { if (!active) return; setForm({
        site_id: 1, artifact_code: data.artifact_code || "", zone_name: data.zone_name || "",
        name_vi: data.name_vi || "", name_en: data.name_en || "", summary_vi: data.summary_vi || "",
        summary_en: data.summary_en || "", description_vi: data.description_vi || "",
        description_en: data.description_en || "", image_url: data.image_url || "",
        audio_vi_url: data.audio_vi_url || "", audio_en_url: data.audio_en_url || "",
        audio_duration_sec: data.audio_duration_sec || 0, translated_langs: data.translated_langs || "VI,EN",
        map_x: String(data.map_x ?? 0), map_y: String(data.map_y ?? 0),
        status: data.status || "DRAFT",
      }); })
      .catch(e => { if (active) setError(e instanceof Error ? e.message : "Không tải được thông tin hiện vật."); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [artifactId]);

  const update = (key: keyof FormState, value: string | number) => setForm(old => ({ ...old, [key]: value }));
  const validateFile = (file: File, kind: "image" | "audio") => {
    const allowed = kind === "image" ? ["image/jpeg", "image/png", "image/webp"] : ["audio/mpeg", "audio/mp3"];
    const max = kind === "image" ? 5 * 1024 * 1024 : 20 * 1024 * 1024;
    const extAllowed = kind === "image" ? /\.(jpe?g|png|webp)$/i : /\.mp3$/i;
    if (!allowed.includes(file.type) || !extAllowed.test(file.name)) throw new Error(kind === "image" ? "Ảnh chỉ hỗ trợ JPG, PNG hoặc WEBP." : "Âm thanh chỉ hỗ trợ MP3.");
    if (file.size > max) throw new Error(`${kind === "image" ? "Ảnh" : "Âm thanh"} vượt quá dung lượng ${kind === "image" ? "5" : "20"} MB.`);
  };
  const onFile = (kind: "image" | "vi" | "en") => (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0]; if (!file) return;
    try {
      validateFile(file, kind === "image" ? "image" : "audio");
      if (kind === "image") { setImageFile(file); setImagePreview(URL.createObjectURL(file)); }
      if (kind === "vi") setAudioViFile(file);
      if (kind === "en") setAudioEnFile(file);
      setError("");
    } catch (e) { setError(e instanceof Error ? e.message : "File không hợp lệ."); event.target.value = ""; }
  };

  const submit = async (event: FormEvent) => {
    event.preventDefault(); setError(""); setFieldErrors({});
    const errors: Record<string, string> = {};
    requiredFields.forEach(key => { if (!String(form[key] ?? "").trim()) errors[key] = "Trường này không được để trống."; });
    if (!editing && !/^HV-[A-Za-z0-9-]+$/i.test(form.artifact_code.trim())) errors.artifact_code = "Mã hiện vật phải có định dạng HV-xxx.";
    if (!Number.isFinite(Number(form.map_x)) || !Number.isFinite(Number(form.map_y))) errors.map_x = "Tọa độ phải là số hợp lệ.";
    if (Object.keys(errors).length) { setFieldErrors(errors); setError("Vui lòng kiểm tra các trường bắt buộc."); return; }
    setSaving(true);
    try {
      const imageUrl = imageFile ? await uploadMedia(imageFile) : (form.image_url || null);
      // Backend currently names uploads with second-level timestamps; serialize MP3 uploads
      // to prevent two same-extension files selected together from overwriting each other.
      const audioViUrl = audioViFile ? await uploadMedia(audioViFile) : (form.audio_vi_url || null);
      if (audioViFile && audioEnFile) await new Promise(resolve => window.setTimeout(resolve, 1100));
      const audioEnUrl = audioEnFile ? await uploadMedia(audioEnFile) : (form.audio_en_url || null);
      const payload: ArtifactPayload = {
        site_id: form.site_id, artifact_code: form.artifact_code.trim(), zone_name: form.zone_name.trim(),
        name_vi: form.name_vi.trim(), name_en: form.name_en.trim(), summary_vi: form.summary_vi.trim(),
        summary_en: form.summary_en.trim(), description_vi: form.description_vi.trim(), description_en: form.description_en.trim(),
        image_url: imageUrl, audio_vi_url: audioViUrl, audio_en_url: audioEnUrl,
        audio_duration_sec: Math.max(0, Number(form.audio_duration_sec) || 0),
        translated_langs: form.translated_langs, map_x: Number(form.map_x), map_y: Number(form.map_y), status: form.status,
      };
      if (editing) {
        const { artifact_code: _code, site_id: _site, ...updatePayload } = payload;
        await apiFetch(`/artifacts/${artifactId}`, { method: "PUT", body: updatePayload });
      } else {
        await apiFetch("/artifacts", { method: "POST", body: payload });
      }
      onSaved();
    } catch (e) { setError(e instanceof Error ? e.message : "Không thể lưu hiện vật."); }
    finally { setSaving(false); }
  };

  if (loading) return <section className="content"><div className="panel form-loading"><LoaderCircle className="spin"/> Đang tải thông tin hiện vật...</div></section>;
  return <section className="content">
    <div className="page-intro"><div><button className="back-link" onClick={onCancel}><ArrowLeft size={16}/> Quay lại danh sách</button><div className="eyebrow">CONTENT MANAGEMENT SYSTEM</div><h1>{editing ? "Chỉnh sửa hiện vật" : "Thêm hiện vật mới"}</h1><p>Nhập nội dung song ngữ và tải lên tư liệu đa phương tiện cho hiện vật.</p></div></div>
    {error && <div className="alert error" role="alert">{error}</div>}
    <form className="form-layout" onSubmit={submit} noValidate>
      <div className="panel form-panel">
        <div className="section-title"><span>01</span><div><h2>Thông tin cơ bản</h2><p>Thông tin nhận diện và vị trí trưng bày.</p></div></div>
        <div className="form-grid">
          <Field label="Mã hiện vật" required error={fieldErrors.artifact_code}><input value={form.artifact_code} disabled={editing} placeholder="VD: HV-006" onChange={e => update("artifact_code", e.target.value)}/></Field>
          <Field label="Khu vực trưng bày" required error={fieldErrors.zone_name}><input value={form.zone_name} placeholder="VD: Khu Đông Sơn – Tầng 1" onChange={e => update("zone_name", e.target.value)}/></Field>
          <Field label="Tọa độ X"><input type="number" value={form.map_x} onChange={e => update("map_x", e.target.value)}/></Field>
          <Field label="Tọa độ Y"><input type="number" value={form.map_y} onChange={e => update("map_y", e.target.value)}/></Field>
          <Field label="Trạng thái"><select value={form.status} onChange={e => update("status", e.target.value)}><option value="DRAFT">Bản nháp</option><option value="PUBLISHED">Đang hiển thị</option><option value="HIDDEN">Tạm ẩn</option></select></Field>
          <Field label="Ngôn ngữ hỗ trợ"><select value={form.translated_langs} onChange={e => update("translated_langs", e.target.value)}><option value="VI,EN">Tiếng Việt + English</option><option value="VI">Chỉ tiếng Việt</option><option value="EN">English only</option></select></Field>
        </div>
      </div>

      <div className="panel form-panel">
        <div className="section-title"><span>02</span><div><h2>Nội dung tiếng Việt</h2><p>Nội dung hiển thị và thuyết minh trong ứng dụng.</p></div></div>
        <div className="form-grid single">
          <Field label="Tên hiện vật tiếng Việt" required error={fieldErrors.name_vi}><input value={form.name_vi} onChange={e => update("name_vi", e.target.value)} placeholder="Nhập tên hiện vật"/></Field>
          <Field label="Tóm tắt tiếng Việt" required error={fieldErrors.summary_vi}><textarea rows={3} value={form.summary_vi} onChange={e => update("summary_vi", e.target.value)} placeholder="Tóm tắt ngắn trên Bottom Sheet"/></Field>
          <Field label="Bài thuyết minh chi tiết tiếng Việt" required error={fieldErrors.description_vi}><textarea rows={6} value={form.description_vi} onChange={e => update("description_vi", e.target.value)} placeholder="Nhập nội dung thuyết minh tiếng Việt"/></Field>
        </div>
      </div>

      <div className="panel form-panel">
        <div className="section-title"><span>03</span><div><h2>English content</h2><p>English title, summary and detailed narration.</p></div></div>
        <div className="form-grid single">
          <Field label="Artifact name (English)" required error={fieldErrors.name_en}><input value={form.name_en} onChange={e => update("name_en", e.target.value)} placeholder="Enter artifact name"/></Field>
          <Field label="Short summary (English)" required error={fieldErrors.summary_en}><textarea rows={3} value={form.summary_en} onChange={e => update("summary_en", e.target.value)} placeholder="Short text shown in the app"/></Field>
          <Field label="Detailed narration (English)" required error={fieldErrors.description_en}><textarea rows={6} value={form.description_en} onChange={e => update("description_en", e.target.value)} placeholder="Enter detailed narration in English"/></Field>
        </div>
      </div>

      <div className="panel form-panel">
        <div className="section-title"><span>04</span><div><h2>Tư liệu đa phương tiện</h2><p>Ảnh đại diện và audio guide cho hai ngôn ngữ.</p></div></div>
        <div className="media-grid">
          <div className="media-field"><label><ImagePlus size={18}/> Ảnh hiện vật <small>JPG, PNG, WEBP · tối đa 5 MB</small></label>
            <label className="upload-box">{imagePreview || form.image_url ? <img src={imagePreview || mediaUrl(form.image_url)} alt="Xem trước ảnh hiện vật"/> : <><UploadCloud size={26}/><b>Chọn ảnh tải lên</b><span>Kéo thả hoặc nhấn để chọn file</span></>}<input type="file" accept=".jpg,.jpeg,.png,.webp,image/jpeg,image/png,image/webp" onChange={onFile("image")}/></label>
            {imageFile && <div className="selected-file">{imageFile.name}<button type="button" onClick={() => {setImageFile(null);setImagePreview("");}}><X size={14}/></button></div>}
          </div>
          <AudioUpload label="Audio guide tiếng Việt" file={audioViFile} currentUrl={form.audio_vi_url} onChange={onFile("vi")} onClear={() => setAudioViFile(null)}/>
          <AudioUpload label="Audio guide English" file={audioEnFile} currentUrl={form.audio_en_url} onChange={onFile("en")} onClear={() => setAudioEnFile(null)}/>
          <div className="duration-field"><Field label="Thời lượng audio (giây)"><input type="number" min="0" value={form.audio_duration_sec} onChange={e => update("audio_duration_sec", Number(e.target.value))}/></Field></div>
        </div>
      </div>
      <div className="form-footer"><span><b>*</b> Các trường nội dung có dấu * là bắt buộc.</span><div><button type="button" className="btn btn-outline" onClick={onCancel} disabled={saving}>Hủy</button><button type="submit" className="btn btn-primary" disabled={saving}>{saving ? <><LoaderCircle className="spin" size={17}/> Đang lưu...</> : <><Save size={17}/> {editing ? "Lưu thay đổi" : "Tạo hiện vật"}</>}</button></div></div>
    </form>
  </section>;
}

function Field({ label, required, error, children }: { label: string; required?: boolean; error?: string; children: ReactNode }) {
  return <label className="field"><span>{label}{required && <b className="required"> *</b>}</span>{children}{error && <small className="field-error">{error}</small>}</label>;
}
function AudioUpload({ label, file, currentUrl, onChange, onClear }: { label: string; file: File | null; currentUrl: string; onChange: (e: ChangeEvent<HTMLInputElement>) => void; onClear: () => void }) {
  return <div className="media-field"><label><Music2 size={18}/> {label}<small>MP3 · tối đa 20 MB</small></label><label className="audio-upload"><UploadCloud size={22}/><b>{file ? file.name : currentUrl ? "Đã có audio – chọn file để thay thế" : "Chọn file MP3"}</b><input type="file" accept=".mp3,audio/mpeg" onChange={onChange}/></label>{currentUrl && !file && <audio className="audio-preview" controls src={mediaUrl(currentUrl)} />}{file && <div className="selected-file">{file.name}<button type="button" onClick={onClear}><X size={14}/></button></div>}</div>;
}
