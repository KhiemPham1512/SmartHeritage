// FILE: admin-web/src/types.ts — kiểu dữ liệu dùng chung cho W3/W4 (khớp backend artifact_router.py)
export type ArtifactStatus = "PUBLISHED" | "DRAFT" | "HIDDEN";

export interface ArtifactStats {
  total_artifacts: number;
  published_count: number;
  hidden_or_draft_count: number;
  assigned_beacon_count: number;
}

export interface ArtifactPayload {
  site_id: number;
  artifact_code: string;
  zone_name: string;
  name_vi: string;
  name_en: string;
  summary_vi: string;
  summary_en: string;
  description_vi: string;
  description_en: string;
  image_url: string | null;
  audio_vi_url: string | null;
  audio_en_url: string | null;
  audio_duration_sec: number;
  translated_langs: string;
  map_x: number;
  map_y: number;
  status: ArtifactStatus;
}

export interface Artifact extends Omit<ArtifactPayload, "site_id"> {
  artifact_id: number;
  site_id?: number;
  beacon_id?: number | null;
  beacon_code?: string | null;
}
