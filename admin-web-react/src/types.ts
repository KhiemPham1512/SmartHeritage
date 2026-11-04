export type ArtifactStatus = "PUBLISHED" | "HIDDEN" | "DRAFT";
export interface Artifact {
  artifact_id: number;
  artifact_code: string;
  zone_name: string;
  name_vi: string;
  name_en: string;
  summary_vi?: string | null;
  summary_en?: string | null;
  description_vi?: string | null;
  description_en?: string | null;
  image_url?: string | null;
  audio_vi_url?: string | null;
  audio_en_url?: string | null;
  audio_duration_sec?: number | null;
  translated_langs?: string | null;
  map_x?: number | null;
  map_y?: number | null;
  status: ArtifactStatus;
  beacon_code?: string | null;
  beacon_id?: number | null;
}
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
