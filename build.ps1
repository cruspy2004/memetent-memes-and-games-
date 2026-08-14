<#
.SYNOPSIS
  Builds Project Memetent and packages it for distribution.

.DESCRIPTION
  Five stages:

    1. compile   sources -> build/
    2. jar       build/  -> dist/memetent.jar
    3. assets    downscale the meme GIFs and stage ONLY the files the games load
    4. image     jpackage --type app-image -> package/Project Memetent/  (portable)
    5. installer jpackage --type exe       -> package/*.exe              (needs WiX v3)

  Stage 3 matters more than it sounds. The repo carries ~18MB of unreferenced audio
  (hamasteroirignl.mp3, chippioriginal.mp3), an unused PSD and a 1.2MB unused PNG. Staging
  from an explicit manifest keeps all of that out of the build, and the GIFs are downscaled
  from 640px to the 420px the meme rail actually renders at.

.PARAMETER Installer
  Also build a Windows .exe installer. Requires WiX Toolset v3.14 on PATH
  (https://github.com/wixtoolset/wix3/releases). Without it, the portable app-image from
  stage 4 is still produced and is fully runnable.

.PARAMETER SkipAssetOptimization
  Stage the original full-size GIFs instead of downscaling. Faster builds while iterating.

.EXAMPLE
  ./build.ps1
  ./build.ps1 -Installer
#>
param(
  [switch]$Installer,
  [switch]$SkipAssetOptimization
)

$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

$AppName    = 'Project Memetent'
$AppVersion = '1.0.0'
$MainClass  = 'Launcher'
$Vendor     = 'Memetent'

# jpackage and jar ship with the JDK but are frequently not on PATH; resolve via JAVA_HOME.
function Get-JdkTool([string]$name) {
  $onPath = Get-Command $name -ErrorAction SilentlyContinue
  if ($onPath) { return $onPath.Source }
  if ($env:JAVA_HOME) {
    $candidate = Join-Path $env:JAVA_HOME "bin\$name.exe"
    if (Test-Path $candidate) { return $candidate }
  }
  throw "Could not find $name. Install JDK 21+ and set JAVA_HOME, or put $name on PATH."
}

$javac    = Get-JdkTool 'javac'
$java     = Get-JdkTool 'java'
$jar      = Get-JdkTool 'jar'
$jpackage = Get-JdkTool 'jpackage'

$classpath = 'lib/jlayer-1.0.1.jar'

Write-Host ''
Write-Host '=== 1/5  compile ===' -ForegroundColor Cyan
if (Test-Path build) { Remove-Item build -Recurse -Force }
New-Item -ItemType Directory build | Out-Null

$sources = @(
  (Get-ChildItem -Path . -Filter *.java -File | ForEach-Object { $_.Name })
  (Get-ChildItem -Path components -Filter *.java -File | ForEach-Object { "components/$($_.Name)" })
  (Get-ChildItem -Path utility   -Filter *.java -File | ForEach-Object { "utility/$($_.Name)" })
  (Get-ChildItem -Path shim/jaco/mp3/player -Filter *.java -File | ForEach-Object { "shim/jaco/mp3/player/$($_.Name)" })
  (Get-ChildItem -Path tools -Filter *.java -File | ForEach-Object { "tools/$($_.Name)" })
) | ForEach-Object { $_ }

& $javac -cp $classpath -d build $sources
if ($LASTEXITCODE -ne 0) { throw 'compile failed' }
Write-Host '  ok' -ForegroundColor Green

Write-Host ''
Write-Host '=== 2/5  verify layout ===' -ForegroundColor Cyan
# The -D argument must be quoted: PowerShell otherwise splits it at the first dot.
& $java -cp "build;$classpath" '-Djava.awt.headless=true' LayoutSmokeTest
if ($LASTEXITCODE -ne 0) { throw 'layout smoke test failed -- refusing to package' }

Write-Host ''
Write-Host '=== 3/5  stage assets ===' -ForegroundColor Cyan
$staging = 'dist/app'
if (Test-Path dist) { Remove-Item dist -Recurse -Force }
New-Item -ItemType Directory $staging | Out-Null
New-Item -ItemType Directory "$staging/images" | Out-Null

# Mirrors Launcher.REQUIRED. Anything not listed here does not ship.
$assets = @(
  'background.png', 'Brickbreacker_picture.png', 'Dinogame_pic.png',
  'hamaster.mp3', 'chippi.mp3', 'gigachad.mp3',
  'images/Ground.png', 'images/Sun.png',
  'images/Dino-stand.png', 'images/Dino-left-up.png',
  'images/Dino-right-up.png', 'images/Dino-big-eyes.png',
  'images/Cactus-1.png', 'images/Cactus-2.png', 'images/Cactus-5.png'
)
$gifs = @('hamaster.gif', 'chippi.gif', 'gigachad2.gif')

foreach ($a in $assets) { Copy-Item $a (Join-Path $staging $a) -Force }

if ($SkipAssetOptimization) {
  Write-Host '  copying GIFs at full size (-SkipAssetOptimization)'
  foreach ($g in $gifs) { Copy-Item $g (Join-Path $staging $g) -Force }
} else {
  & $java '-Xmx2g' -cp "build;$classpath" '-Djava.awt.headless=true' OptimizeAssets $staging
  if ($LASTEXITCODE -ne 0) { throw 'asset optimization failed' }
}

$stagedMb = [math]::Round(((Get-ChildItem $staging -Recurse -File | Measure-Object Length -Sum).Sum / 1MB), 1)
Write-Host "  staged $stagedMb MB" -ForegroundColor Green

Write-Host ''
Write-Host '=== 4/5  jar + app image ===' -ForegroundColor Cyan
& $jar --create --file dist/memetent.jar --main-class $MainClass -C build .
if ($LASTEXITCODE -ne 0) { throw 'jar failed' }

# The jar sits alongside the assets so Assets.resolve() finds them by walking up from the
# code source location. jlayer goes in too, since the app needs it at runtime.
Copy-Item dist/memetent.jar "$staging/memetent.jar" -Force
Copy-Item $classpath "$staging/jlayer-1.0.1.jar" -Force

if (Test-Path package) { Remove-Item package -Recurse -Force }
New-Item -ItemType Directory package | Out-Null

$jpackageArgs = @(
  '--name', $AppName,
  '--app-version', $AppVersion,
  '--vendor', $Vendor,
  '--input', $staging,
  '--main-jar', 'memetent.jar',
  '--main-class', $MainClass,
  '--dest', 'package',
  '--java-options', '-Xmx1g',
  '--java-options', '-Dsun.java2d.uiScale.enabled=true',
  # Without this jpackage bundles the whole JDK (~147MB). These are the modules a Swing app
  # on JLayer actually needs, and jlink trims the runtime to roughly a third of that.
  #   java.desktop  - Swing, AWT, ImageIO, javax.sound (JLayer's audio output)
  #   java.logging  - required by java.desktop
  #   java.prefs    - touched by some look-and-feel code paths
  #   jdk.unsupported - sun.misc.Unsafe, pulled in by older libraries like JLayer
  '--add-modules', 'java.base,java.desktop,java.logging,java.prefs,jdk.unsupported'
)
if (Test-Path 'app-icon.ico') { $jpackageArgs += @('--icon', 'app-icon.ico') }

& $jpackage @jpackageArgs '--type' 'app-image'
if ($LASTEXITCODE -ne 0) { throw 'jpackage app-image failed' }

$imageMb = [math]::Round(((Get-ChildItem "package/$AppName" -Recurse -File | Measure-Object Length -Sum).Sum / 1MB), 1)
Write-Host "  portable app image: package/$AppName  ($imageMb MB)" -ForegroundColor Green

# Prove the packaged layout resolves its media. Run from a different working directory on
# purpose: that is what the audience's machine looks like, and the original code -- which
# loaded every asset off a bare relative path -- would fail exactly here.
Write-Host '  verifying packaged assets resolve...'
$appDir = (Resolve-Path "package/$AppName/app").Path
Push-Location ([System.IO.Path]::GetTempPath())
try {
  & $java -cp "$appDir\memetent.jar;$appDir\jlayer-1.0.1.jar" '-Djava.awt.headless=true' Launcher --verify-assets | Out-Null
  if ($LASTEXITCODE -ne 0) { throw 'packaged app cannot find its media files' }
} finally {
  Pop-Location
}
Write-Host '  ok' -ForegroundColor Green

Write-Host ''
Write-Host '=== 5/5  installer ===' -ForegroundColor Cyan
if (-not $Installer) {
  Write-Host '  skipped (pass -Installer to build one)'
} elseif (-not (Get-Command 'candle.exe' -ErrorAction SilentlyContinue)) {
  Write-Warning '  WiX Toolset v3 not found on PATH, so no .exe was built.'
  Write-Warning '  Install from https://github.com/wixtoolset/wix3/releases, then rerun with -Installer.'
  Write-Warning "  The portable image in package/$AppName works and can be zipped as-is."
} else {
  & $jpackage @jpackageArgs '--type' 'exe' '--win-dir-chooser' '--win-shortcut' '--win-menu'
  if ($LASTEXITCODE -ne 0) { throw 'jpackage exe failed' }
  Write-Host '  installer written to package/' -ForegroundColor Green
}

Write-Host ''
Write-Host 'Done.' -ForegroundColor Green
Write-Host "  Run locally :  ./run.ps1"
Write-Host "  Portable app:  package/$AppName/$AppName.exe"
Write-Host "  To hand out :  zip the package/$AppName folder"
Write-Host ''
