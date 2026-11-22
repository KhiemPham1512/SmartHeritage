<#
  Chạy Smart Heritage ở chế độ PRODUCTION trên máy Windows (một tiến trình: backend + web admin).

  Việc script làm:
    1. Build web admin (admin-web\dist) rồi để backend phục vụ ngay ở "/" -> chỉ cần mở một địa chỉ.
    2. Tạo JWT_SECRET ngẫu nhiên (lần đầu) và ghi vào backend\.env. Production từ chối chạy nếu thiếu.
    3. Chạy uvicorn trên 0.0.0.0 để điện thoại cùng Wi-Fi truy cập được.

  Dùng:   powershell -ExecutionPolicy Bypass -File deploy\windows\run-production.ps1
  Tùy chọn:  -Port 8000   -SkipBuild (bỏ qua build web)   -OpenFirewall (mở cổng cho mạng Private; cần chạy bằng Admin)
#>
param(
    [int]$Port = 8000,
    [switch]$SkipBuild,
    [switch]$OpenFirewall
)

$ErrorActionPreference = "Stop"
$root = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$backend = Join-Path $root "backend"
$web = Join-Path $root "admin-web"
$python = Join-Path $backend ".venv\Scripts\python.exe"

if (-not (Test-Path $python)) {
    throw "Chưa có môi trường ảo Python. Chạy trong thư mục backend: py -3.12 -m venv .venv; .\.venv\Scripts\pip install -r requirements.txt"
}
if (-not (Test-Path (Join-Path $backend ".env"))) {
    throw "Chưa có backend\.env (DB_SERVER, DB_NAME...). Sao chép từ backend\.env.example rồi điền tên SQL Server."
}

# 1. Build web (VITE_API_BASE rỗng trong .env.production -> web gọi API cùng gốc)
if (-not $SkipBuild) {
    Push-Location $web
    try {
        if (-not (Test-Path "node_modules")) { npm ci }
        npm run build
        if ($LASTEXITCODE -ne 0) { throw "npm run build thất bại" }
    } finally { Pop-Location }
}
if (-not (Test-Path (Join-Path $web "dist\index.html"))) { throw "Chưa có admin-web\dist. Bỏ -SkipBuild để build." }

# 2. Khóa ký JWT riêng cho máy này (lưu trong backend\.env, đã nằm trong .gitignore)
$envFile = Join-Path $backend ".env"
if (-not (Select-String -Path $envFile -Pattern '^\s*JWT_SECRET=\S{32,}' -Quiet)) {
    $bytes = New-Object byte[] 36
    [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
    $secret = -join ($bytes | ForEach-Object { $_.ToString("x2") })
    [IO.File]::AppendAllText($envFile, "`r`nJWT_SECRET=$secret`r`n")
    Write-Host "Đã tạo JWT_SECRET mới trong backend\.env (phiên đăng nhập cũ sẽ hết hiệu lực)."
}

# 3. Mở cổng cho mạng riêng (tùy chọn)
if ($OpenFirewall) {
    try {
        New-NetFirewallRule -DisplayName "SmartHeritage $Port" -Direction Inbound -Protocol TCP -LocalPort $Port -Action Allow -Profile Private | Out-Null
        Write-Host "Đã mở cổng $Port cho mạng Private."
    } catch {
        Write-Warning "Không mở được tường lửa (cần chạy PowerShell bằng quyền Administrator): $($_.Exception.Message)"
    }
}

$lan = (Get-NetIPAddress -AddressFamily IPv4 | Where-Object { $_.IPAddress -match '^(192\.168|10\.|172\.(1[6-9]|2\d|3[01]))\.' } | Select-Object -First 1).IPAddress
Write-Host ""
Write-Host "Web admin : http://localhost:$Port/        (cùng Wi-Fi: http://${lan}:$Port/)"
Write-Host "API docs  : http://localhost:$Port/docs"
Write-Host "App khách : nhập máy chủ  ${lan}:$Port"
Write-Host "Dừng      : Ctrl+C"
Write-Host ""

$env:APP_ENV = "production"
Set-Location $backend
& $python -m uvicorn app.main:app --host 0.0.0.0 --port $Port
