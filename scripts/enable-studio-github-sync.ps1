# Enable Android Studio <-> GitHub auto-sync for this repo.
# Run once from PowerShell: .\scripts\enable-studio-github-sync.ps1

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot

Write-Host "Repo: $repoRoot"

# Git hooks: auto-push after commit
$hooksPath = Join-Path $repoRoot ".githooks"
if (-not (Test-Path $hooksPath)) {
    Write-Error "Missing .githooks folder"
}
Write-Host "Git hooks path: $hooksPath"

# GitHub CLI credentials for HTTPS push/pull
if (Get-Command gh -ErrorAction SilentlyContinue) {
    gh auth setup-git 2>$null
    Write-Host "GitHub CLI credential helper configured."
} else {
    Write-Warning "GitHub CLI (gh) not found. Install it for seamless GitHub auth."
}

# Android Studio / IntelliJ: auto-fetch + push settings
$template = Join-Path $repoRoot "studio-config\android-studio-git.xml"
if (-not (Test-Path $template)) {
    Write-Warning "Template not found: $template"
    exit 0
}

$studioRoots = @(
    "$env:APPDATA\Google",
    "$env:LOCALAPPDATA\Google",
    "$env:APPDATA\JetBrains"
)

$installed = @()
foreach ($root in $studioRoots) {
    if (Test-Path $root) {
        $installed += Get-ChildItem $root -Directory -ErrorAction SilentlyContinue |
            Where-Object { $_.Name -match 'AndroidStudio|IntelliJIdea' }
    }
}

if ($installed.Count -eq 0) {
    Write-Warning "Android Studio config folder not found yet."
    Write-Host "After first Android Studio launch, run this script again."
    Write-Host "Or manually copy:"
    Write-Host "  $template"
    Write-Host "to:"
    Write-Host "  %APPDATA%\Google\AndroidStudio*\options\git.xml"
    exit 0
}

foreach ($dir in $installed) {
    $optionsDir = Join-Path $dir.FullName "options"
    if (-not (Test-Path $optionsDir)) {
        New-Item -ItemType Directory -Path $optionsDir -Force | Out-Null
    }
    $dest = Join-Path $optionsDir "git.xml"
    Copy-Item $template $dest -Force
    Write-Host "Updated: $dest"
}

Write-Host ""
Write-Host "Done. In Android Studio:"
Write-Host "  1. File -> Settings -> Version Control -> GitHub -> Add account (if needed)"
Write-Host "  2. Settings -> Version Control -> Git -> enable 'Fetch remote changes automatically'"
Write-Host "  3. Use Commit and Push (Ctrl+Alt+K) or commits will auto-push via hook"
