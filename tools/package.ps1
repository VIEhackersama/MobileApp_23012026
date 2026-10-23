param()
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$apkPath = Join-Path $projectRoot 'app\build\outputs\apk\debug\app-debug.apk'
if (-not (Test-Path -LiteralPath $apkPath)) { throw 'Chua co APK. Hay chay gradlew.bat :app:assembleDebug truoc.' }
$artifactRoot = Join-Path $projectRoot 'artifacts'
New-Item -ItemType Directory -Path $artifactRoot -Force | Out-Null
$deliveryApk = Join-Path $artifactRoot 'FruitShop-debug.apk'
Copy-Item -LiteralPath $apkPath -Destination $deliveryApk
$sourceZip = Join-Path $artifactRoot 'FruitShop-source.zip'
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
$listedFiles = @(& git -C $projectRoot -c core.quotepath=false ls-files --cached --others --exclude-standard)
if ($LASTEXITCODE -ne 0) { throw 'Khong the doc danh sach file nguon tu Git.' }
# Package the working tree, including new files and excluding removed files, without touching the index.
$sourceFiles = @($listedFiles | Sort-Object -Unique | Where-Object { Test-Path -LiteralPath (Join-Path $projectRoot $_) -PathType Leaf })
if ($sourceFiles.Count -eq 0) { throw 'Khong co file nguon de dong goi.' }
$zipStream = [System.IO.File]::Open($sourceZip, [System.IO.FileMode]::Create)
$zipArchive = [System.IO.Compression.ZipArchive]::new($zipStream, [System.IO.Compression.ZipArchiveMode]::Create)
try {
    foreach ($relativePath in $sourceFiles) {
        $sourcePath = Join-Path $projectRoot $relativePath
        [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile(
            $zipArchive, $sourcePath, "FruitShop/$relativePath",
            [System.IO.Compression.CompressionLevel]::Optimal) | Out-Null
    }
} finally { $zipArchive.Dispose(); $zipStream.Dispose() }
Get-FileHash -LiteralPath $deliveryApk,$sourceZip -Algorithm SHA256 |
    ForEach-Object { "$($_.Hash)  $(Split-Path -Leaf $_.Path)" } |
    Set-Content -LiteralPath (Join-Path $artifactRoot 'SHA256SUMS.txt') -Encoding ascii
Get-Item -LiteralPath $deliveryApk,$sourceZip | Select-Object FullName, Length
