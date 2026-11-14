# FILE: backend/seed_media.py
"""
Script tự động tạo bộ file Ảnh (.jpg/.png) và Audio thuyết minh (.mp3) mẫu
khớp 100% với đường dẫn trong database/init_schema.sql để Mobile & Web test ngay không bị lỗi 404.
"""
import os
import math
import wave
import struct
import zlib

IMAGES_DIR = os.path.join("static", "images")
AUDIO_DIR = os.path.join("static", "audio")

os.makedirs(IMAGES_DIR, exist_ok=True)
os.makedirs(AUDIO_DIR, exist_ok=True)


def create_solid_png(filepath: str, width: int, height: int, rgb: tuple[int, int, int]):
    """Tạo file ảnh hợp lệ chuẩn nhị phân không cần cài thêm thư viện ngoài."""
    r, g, b = rgb
    raw_row = b"\x00" + bytes([r, g, b]) * width
    raw_data = raw_row * height

    def png_chunk(chunk_type: bytes, data: bytes) -> bytes:
        chunk = chunk_type + data
        return struct.pack("!I", len(data)) + chunk + struct.pack("!I", zlib.crc32(chunk) & 0xFFFFFFFF)

    ihdr = struct.pack("!IIBBBBB", width, height, 8, 2, 0, 0, 0)
    idat = zlib.compress(raw_data, 9)
    png_bytes = (
        b"\x89PNG\r\n\x1a\n"
        + png_chunk(b"IHDR", ihdr)
        + png_chunk(b"IDAT", idat)
        + png_chunk(b"IEND", b"")
    )
    with open(filepath, "wb") as f:
        f.write(png_bytes)


def create_sample_audio(filepath: str, freq_hz: float = 440.0, duration_sec: int = 5):
    """Tạo file âm thanh sóng Sin chuẩn (5 giây) để trình phát Web & Android ExoPlayer phát được ngay."""
    sample_rate = 22050
    num_samples = sample_rate * duration_sec
    with wave.open(filepath, "w") as wav_file:
        wav_file.setnchannels(1)
        wav_file.setsampwidth(2)
        wav_file.setframerate(sample_rate)
        frames = bytearray()
        for i in range(num_samples):
            # Tạo giai điệu êm dịu không bị chói tai khi test loa
            envelope = min(1.0, i / 2000.0, (num_samples - i) / 2000.0)
            val = int(12000 * envelope * math.sin(2.0 * math.pi * freq_hz * (i / sample_rate)))
            frames.extend(struct.pack("<h", val))
        wav_file.writeframes(bytes(frames))


def main():
    # 1. Tạo 5 ảnh hiện vật + 1 ảnh sơ đồ mặt bằng khu di tích khớp với init_schema.sql
    image_files = {
        "hv_001.jpg": (600, 400, (180, 130, 70)),     # Trống đồng Ngọc Lũ (Màu đồng cổ)
        "hv_002.jpg": (600, 400, (70, 115, 175)),     # Bình gốm Chu Đậu (Màu lam gốm)
        "hv_003.jpg": (600, 400, (195, 110, 80)),     # Tượng Apsara (Màu đá sa thạch Chăm Pa)
        "hv_004.jpg": (600, 400, (140, 105, 65)),     # Thạp đồng Đào Thịnh
        "hv_005.jpg": (600, 400, (215, 165, 45)),     # Ấn vàng Triều Nguyễn (Màu vàng hoàng gia)
        "floorplan_tang1.png": (900, 600, (235, 240, 245)),  # Sơ đồ mặt bằng Tầng 1
    }
    for filename, (w, h, color) in image_files.items():
        path = os.path.join(IMAGES_DIR, filename)
        create_solid_png(path, w, h, color)
        print(f"[OK] Da tao anh mau: {path}")

    # 2. Tạo 10 file thuyết minh âm thanh (Việt + Anh) khớp với init_schema.sql
    audio_files = [
        ("ngoc-lu-thuyet-minh-vi.mp3", 440.0),
        ("ngoc-lu-thuyet-minh-en.mp3", 493.88),
        ("chu-dau-vi.mp3", 523.25),
        ("chu-dau-en.mp3", 587.33),
        ("apsara-vi.mp3", 659.25),
        ("apsara-en.mp3", 698.46),
        ("dao-thinh-vi.mp3", 392.00),
        ("dao-thinh-en.mp3", 440.00),
        ("an-vang-vi.mp3", 523.25),
        ("an-vang-en.mp3", 659.25),
    ]
    for filename, freq in audio_files:
        path = os.path.join(AUDIO_DIR, filename)
        create_sample_audio(path, freq_hz=freq, duration_sec=5)
        print(f"[OK] Da tao audio mau: {path}")

    print("\n--> HOAN TAT! Toan bo anh va audio mau trong static/ da san sang!")


if __name__ == "__main__":
    main()