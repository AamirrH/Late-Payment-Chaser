param(
    [switch]$InstallOnly
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$aiDir = Join-Path $root 'ai-service'
$financeDir = Join-Path $root 'finance-service'
$webDir = Join-Path $root 'web'

function Require-Command([string]$name, [string]$hint) {
    if (-not (Get-Command $name -ErrorAction SilentlyContinue)) {
        throw "$name is required. $hint"
    }
}

Require-Command java 'Install Java 17 or newer and make it available on PATH.'
Require-Command python 'Install Python 3.11 or newer and make it available on PATH.'
Require-Command npm 'Install Node.js 20 or newer and make npm available on PATH.'

$venvPython = Join-Path $aiDir '.venv\Scripts\python.exe'
if (-not (Test-Path $venvPython)) {
    & python -m venv (Join-Path $aiDir '.venv')
}
& $venvPython -m pip install -r (Join-Path $aiDir 'requirements.txt')
if ($LASTEXITCODE -ne 0) { throw 'AI dependency installation failed.' }

if (-not (Test-Path (Join-Path $webDir 'node_modules'))) {
    $npmCache = Join-Path $root '.tools\npm-cache'
    Push-Location $webDir
    try {
        & npm ci --cache $npmCache
        if ($LASTEXITCODE -ne 0) { throw 'Web dependency installation failed.' }
    } finally { Pop-Location }
}

if ($InstallOnly) {
    Write-Host 'Dependencies are installed. Re-run without -InstallOnly to start the local stack.'
    exit 0
}

$ai = Start-Process -FilePath $venvPython -ArgumentList @('-m', 'uvicorn', 'app.main:app', '--host', '127.0.0.1', '--port', '8081') -WorkingDirectory $aiDir -WindowStyle Hidden -PassThru
$finance = Start-Process -FilePath 'cmd.exe' -ArgumentList @('/c', '.\mvnw.cmd spring-boot:run') -WorkingDirectory $financeDir -WindowStyle Hidden -PassThru
$web = Start-Process -FilePath 'npm.cmd' -ArgumentList @('run', 'dev') -WorkingDirectory $webDir -WindowStyle Hidden -PassThru

Write-Host "Local processes started: AI $($ai.Id), finance $($finance.Id), web $($web.Id)."
Write-Host 'Open http://127.0.0.1:5173 after the services finish starting.'
Write-Host 'Stop them with: Stop-Process -Id <pid1>,<pid2>,<pid3>'
