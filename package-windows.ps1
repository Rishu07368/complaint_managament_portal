$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$distRoot = Join-Path $projectRoot "dist"
$inputDir = Join-Path $distRoot "package-input"
$outputDir = Join-Path $distRoot "package-output"
$previousAppDirs = @(
    (Join-Path $distRoot "ComplaintPortal"),
    (Join-Path $distRoot "ComplaintPortal-Fixed"),
    (Join-Path $distRoot "ComplaintPortal-Fixed2"),
    (Join-Path $distRoot "ComplaintPortal-Fixed3")
)
$appDir = Join-Path $distRoot "ComplaintPortal-Final"
$builtAppDir = Join-Path $outputDir "ComplaintPortal"

Push-Location $projectRoot
try {
    mvn -q clean package
    if ($LASTEXITCODE -ne 0) { throw "Maven package failed." }

    if (Test-Path $inputDir) { Remove-Item -LiteralPath $inputDir -Recurse -Force }
    if (Test-Path $outputDir) { Remove-Item -LiteralPath $outputDir -Recurse -Force }
    New-Item -ItemType Directory -Path $inputDir | Out-Null
    Copy-Item -LiteralPath (Join-Path $projectRoot "target\complaint-portal-1.0.0.jar") `
        -Destination (Join-Path $inputDir "ComplaintPortal.jar")
    Copy-Item -LiteralPath (Join-Path $projectRoot "application.properties.example") `
        -Destination (Join-Path $inputDir "application.properties.example")

    if (Test-Path $appDir) { Remove-Item -LiteralPath $appDir -Recurse -Force }
    jpackage `
        --type app-image `
        --name ComplaintPortal `
        --app-version 1.0.0 `
        --vendor "Complaint Portal" `
        --input $inputDir `
        --main-jar ComplaintPortal.jar `
        --main-class com.complaints.app.Main `
        --dest $outputDir
    if ($LASTEXITCODE -ne 0) { throw "jpackage failed." }
    Copy-Item -LiteralPath (Join-Path $projectRoot "application.properties.example") `
        -Destination (Join-Path $builtAppDir "application.properties.example")
    $existingConfig = Join-Path $appDir "application.properties"
    if (-not (Test-Path $existingConfig)) {
        foreach ($previousAppDir in $previousAppDirs) {
            $candidate = Join-Path $previousAppDir "application.properties"
            if (Test-Path $candidate) {
                $existingConfig = $candidate
                break
            }
        }
    }
    if (Test-Path $existingConfig) {
        Copy-Item -LiteralPath $existingConfig -Destination (Join-Path $builtAppDir "application.properties")
    }

    if (Test-Path $appDir) { Remove-Item -LiteralPath $appDir -Recurse -Force }
    Move-Item -LiteralPath $builtAppDir -Destination $appDir
}
finally {
    if (Test-Path $inputDir) { Remove-Item -LiteralPath $inputDir -Recurse -Force }
    if (Test-Path $outputDir) { Remove-Item -LiteralPath $outputDir -Recurse -Force }
    Pop-Location
}

Write-Host "Windows application created: $appDir\ComplaintPortal.exe"
