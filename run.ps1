<#
.SYNOPSIS
  Compiles and runs Project Memetent from source, for development.

.DESCRIPTION
  Use this while working on the games. For a distributable build, use ./build.ps1.

.PARAMETER Screen
  Which window to open. 'menu' (default) is the real entry point; the others jump straight
  to one game, which is handy when iterating on it.

.PARAMETER Test
  Run the headless layout smoke test instead of opening a window.

.EXAMPLE
  ./run.ps1
  ./run.ps1 -Screen brick
  ./run.ps1 -Test
#>
param(
  [ValidateSet('menu', 'picker', 'brick', 'dino')]
  [string]$Screen = 'menu',
  [switch]$Test
)

$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

function Get-JdkTool([string]$name) {
  $onPath = Get-Command $name -ErrorAction SilentlyContinue
  if ($onPath) { return $onPath.Source }
  if ($env:JAVA_HOME) {
    $candidate = Join-Path $env:JAVA_HOME "bin\$name.exe"
    if (Test-Path $candidate) { return $candidate }
  }
  throw "Could not find $name. Install JDK 21+ and set JAVA_HOME, or put $name on PATH."
}

$javac = Get-JdkTool 'javac'
$java  = Get-JdkTool 'java'
$classpath = 'lib/jlayer-1.0.1.jar'

if (-not (Test-Path build)) { New-Item -ItemType Directory build | Out-Null }

$sources = @(
  (Get-ChildItem -Path . -Filter *.java -File | ForEach-Object { $_.Name })
  (Get-ChildItem -Path components -Filter *.java -File | ForEach-Object { "components/$($_.Name)" })
  (Get-ChildItem -Path utility   -Filter *.java -File | ForEach-Object { "utility/$($_.Name)" })
  (Get-ChildItem -Path shim/jaco/mp3/player -Filter *.java -File | ForEach-Object { "shim/jaco/mp3/player/$($_.Name)" })
  (Get-ChildItem -Path tools -Filter *.java -File | ForEach-Object { "tools/$($_.Name)" })
) | ForEach-Object { $_ }

& $javac -cp $classpath -d build $sources
if ($LASTEXITCODE -ne 0) { throw 'compile failed' }

if ($Test) {
  # Quoted: PowerShell otherwise splits the -D argument at the first dot.
  & $java -cp "build;$classpath" '-Djava.awt.headless=true' LayoutSmokeTest
  exit $LASTEXITCODE
}

$entry = switch ($Screen) {
  'menu'   { 'Launcher' }
  'picker' { 'GameStartupPage' }
  'brick'  { 'Main' }
  'dino'   { 'UserInterface' }
}

Write-Host "Launching $entry ..." -ForegroundColor Cyan
& $java -cp "build;$classpath" $entry
