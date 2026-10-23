param()
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$apkPath = Join-Path $projectRoot 'app\build\outputs\apk\debug\app-debug.apk'
if (-not (Test-Path -LiteralPath $apkPath)) { throw 'Chua co APK. Hay chay gradlew.bat :app:assembleDebug truoc.' }
$artifactRoot = Join-Path $projectRoot 'artifacts'
New-Item -ItemType Directory -Path $artifactRoot -Force | Out-Null
$deliveryApk = Join-Path $artifactRoot 'StudentManager-debug.apk'
Copy-Item -LiteralPath $apkPath -Destination $deliveryApk
$sourceZip = Join-Path $artifactRoot 'StudentManager-source.zip'
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
$sourceFiles = @(& git -C $projectRoot ls-files --cached)
if ($LASTEXITCODE -ne 0 -or $sourceFiles.Count -eq 0) { throw 'Can git add cac file nguon truoc khi dong goi.' }
$zipStream = [System.IO.File]::Open($sourceZip, [System.IO.FileMode]::Create)
$zipArchive = [System.IO.Compression.ZipArchive]::new($zipStream, [System.IO.Compression.ZipArchiveMode]::Create)
try {
    foreach ($relativePath in $sourceFiles) {
        $sourcePath = Join-Path $projectRoot $relativePath
        [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile(
            $zipArchive, $sourcePath, "StudentManager/$relativePath",
            [System.IO.Compression.CompressionLevel]::Optimal) | Out-Null
    }
} finally { $zipArchive.Dispose(); $zipStream.Dispose() }
Get-FileHash -LiteralPath $deliveryApk,$sourceZip -Algorithm SHA256 |
    ForEach-Object { "$($_.Hash)  $(Split-Path -Leaf $_.Path)" } |
    Set-Content -LiteralPath (Join-Path $artifactRoot 'SHA256SUMS.txt') -Encoding ascii
Get-Item -LiteralPath $deliveryApk,$sourceZip | Select-Object FullName, Length
