Param(
  [Parameter(Position=0)][string]$AgentType
)
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Exec($cmd, $args) {
  $psi = New-Object System.Diagnostics.ProcessStartInfo
  $psi.FileName = $cmd
  $psi.Arguments = $args
  $psi.RedirectStandardOutput = $true
  $psi.RedirectStandardError = $true
  $psi.UseShellExecute = $false
  $p = [System.Diagnostics.Process]::Start($psi)
  $out = $p.StandardOutput.ReadToEnd()
  $err = $p.StandardError.ReadToEnd()
  $p.WaitForExit()
  if ($p.ExitCode -ne 0) { throw "Command failed: $cmd $args`n$err" }
  return $out.Trim()
}

try { $RepoRoot = Exec 'git' 'rev-parse --show-toplevel' } catch { $RepoRoot = (Get-Location).Path }
$CurrentBranch = Exec 'git' 'rev-parse --abbrev-ref HEAD'
$FeatureDir = Join-Path $RepoRoot "specs/$CurrentBranch"
$NewPlan = Join-Path $FeatureDir 'plan.md'
if (!(Test-Path $NewPlan)) { Write-Error "ERROR: No plan.md found at $NewPlan"; exit 1 }

$ClaudeFile  = Join-Path $RepoRoot 'CLAUDE.md'
$GeminiFile  = Join-Path $RepoRoot 'GEMINI.md'
$CopilotFile = Join-Path $RepoRoot '.github/copilot-instructions.md'
$CodexFile   = Join-Path $RepoRoot 'CODEX.md'

$plan = Get-Content -Raw -Path $NewPlan
function Extract($pattern) {
  $m = [Regex]::Match($plan, $pattern, 'Multiline')
  if ($m.Success) { return $m.Groups[1].Value.Trim() } else { return '' }
}

$NewLang = Extract('^\*\*Language/Version\*\*: (.+)$')
if ($NewLang -match 'NEEDS CLARIFICATION') { $NewLang = '' }
$NewFramework = Extract('^\*\*Primary Dependencies\*\*: (.+)$')
if ($NewFramework -match 'NEEDS CLARIFICATION') { $NewFramework = '' }
$NewDb = Extract('^\*\*Storage\*\*: (.+)$')
if ($NewDb -match '^(N/A|NEEDS CLARIFICATION)') { $NewDb = '' }
$NewProjectType = Extract('^\*\*Project Type\*\*: (.+)$')

function Update-AgentFile([string]$TargetFile, [string]$AgentName) {
  Write-Host "Updating $AgentName context file: $TargetFile"
  $templatePath = Join-Path $RepoRoot '.specify/templates/agent-file-template.md'
  if (!(Test-Path $TargetFile)) {
    if (!(Test-Path $templatePath)) { Write-Error "ERROR: Template not found: $templatePath"; return }
    $content = Get-Content -Raw -Path $templatePath
    $content = $content -replace '\[PROJECT NAME\]', [Regex]::Escape((Split-Path -Leaf $RepoRoot))
    $content = $content -replace '\[DATE\]', (Get-Date -Format 'yyyy-MM-dd')
    $techLine = if ($NewLang) { "- $NewLang + $NewFramework ($CurrentBranch)" } else { '' }
    $content = $content -replace '\[EXTRACTED FROM ALL PLAN.MD FILES\]', [Regex]::Escape($techLine)
    if ($NewProjectType -match 'web') {
      $proj = "backend/`nfrontend/`ntests/"
    } else { $proj = "src/`ntests/" }
    $content = $content -replace '\[ACTUAL STRUCTURE FROM PLANS\]', [Regex]::Escape($proj)
    $commands = if ($NewLang -match 'Python') { 'cd src && pytest && ruff check .' }
      elseif ($NewLang -match 'Rust') { 'cargo test && cargo clippy' }
      elseif ($NewLang -match 'JavaScript|TypeScript') { 'npm test && npm run lint' }
      else { "# Add commands for $NewLang" }
    $content = $content -replace '\[ONLY COMMANDS FOR ACTIVE TECHNOLOGIES\]', [Regex]::Escape($commands)
    $content = $content -replace '\[LANGUAGE-SPECIFIC, ONLY FOR LANGUAGES IN USE\]', [Regex]::Escape("$NewLang: Follow standard conventions")
    $recent = if ($NewLang) { "- $CurrentBranch: Added $NewLang + $NewFramework" } else { '' }
    $content = $content -replace '\[LAST 3 FEATURES AND WHAT THEY ADDED\]', [Regex]::Escape($recent)
    Set-Content -Path $TargetFile -Value $content -NoNewline
    Write-Host "✅ $AgentName context file created"
    return
  }

  $content = Get-Content -Raw -Path $TargetFile
  # Preserve manual additions if present
  $manual = ''
  $manualMatch = [Regex]::Match($content, '<!-- MANUAL ADDITIONS START -->[\s\S]*?<!-- MANUAL ADDITIONS END -->')
  if ($manualMatch.Success) { $manual = $manualMatch.Value }

  # Update Active Technologies block
  $blockPattern = '## Active Technologies\s*([\s\S]*?)(\r?\n\r?\n)'
  $m = [Regex]::Match($content, $blockPattern)
  if ($m.Success) {
    $existing = $m.Groups[1].Value.Trim()
    $additions = @()
    if ($NewLang -and ($existing -notmatch [Regex]::Escape($NewLang))) {
      $additions += "- $NewLang + $NewFramework ($CurrentBranch)"
    }
    if ($NewDb -and ($existing -notmatch [Regex]::Escape($NewDb))) {
      $additions += "- $NewDb ($CurrentBranch)"
    }
    if ($additions.Count -gt 0) {
      $newBlock = ($existing + "`n" + ($additions -join "`n")).Trim()
      $content = $content.Remove($m.Groups[1].Index, $m.Groups[1].Length)
      $content = $content.Insert($m.Groups[1].Index, $newBlock)
    }
  }

  # Update Recent Changes block (keep 3 lines)
  $recentPattern = '## Recent Changes\s*([\s\S]*?)(\r?\n\r?\n|$)'
  $m2 = [Regex]::Match($content, $recentPattern)
  if ($m2.Success -and $NewLang) {
    $lines = @()
    $existingLines = $m2.Groups[1].Value.Trim() -split "\r?\n" | Where-Object { $_.Trim() -ne '' }
    $lines += "- $CurrentBranch: Added $NewLang + $NewFramework"
    foreach ($l in $existingLines) { if ($lines.Count -ge 3) { break } $lines += $l }
    $replacement = "## Recent Changes`n" + ($lines -join "`n") + "`n`n"
    $content = [Regex]::Replace($content, $recentPattern, [Regex]::Escape($replacement))
    $content = $content -replace [Regex]::Escape($replacement), $replacement # unescape
  }

  # Update date if present
  $content = [Regex]::Replace($content, 'Last updated: \d{4}-\d{2}-\d{2}', 'Last updated: ' + (Get-Date -Format 'yyyy-MM-dd'))

  # Restore manual additions block (ensure markers exist)
  if ($manual) {
    $content = [Regex]::Replace($content, '<!-- MANUAL ADDITIONS START -->[\s\S]*?<!-- MANUAL ADDITIONS END -->', $manual)
  }

  Set-Content -Path $TargetFile -Value $content -NoNewline
  Write-Host "✅ $AgentName context file updated successfully"
}

switch ($AgentType) {
  'claude'  { Update-AgentFile $ClaudeFile  'Claude Code' }
  'gemini'  { Update-AgentFile $GeminiFile  'Gemini CLI' }
  'copilot' { Update-AgentFile $CopilotFile 'GitHub Copilot' }
  'codex'   { Update-AgentFile $CodexFile   'Codex CLI' }
  ''        {
               if (Test-Path $ClaudeFile)  { Update-AgentFile $ClaudeFile  'Claude Code' }
               if (Test-Path $GeminiFile)  { Update-AgentFile $GeminiFile  'Gemini CLI' }
               if (Test-Path $CopilotFile) { Update-AgentFile $CopilotFile 'GitHub Copilot' }
               if (Test-Path $CodexFile)   { Update-AgentFile $CodexFile   'Codex CLI' }
               if ((-not (Test-Path $ClaudeFile)) -and (-not (Test-Path $GeminiFile)) -and (-not (Test-Path $CopilotFile)) -and (-not (Test-Path $CodexFile))) {
                 Update-AgentFile $ClaudeFile 'Claude Code'
               }
            }
  default   { Write-Error "ERROR: Unknown agent type '$AgentType'`nUsage: ./update-agent-context.ps1 -AgentType [claude|gemini|copilot|codex]"; exit 1 }
}

Write-Host "`nSummary of changes:"
if ($NewLang) { Write-Host "- Added language: $NewLang" }
if ($NewFramework) { Write-Host "- Added framework: $NewFramework" }
if ($NewDb) { Write-Host "- Added database: $NewDb" }
Write-Host "`nUsage: ./update-agent-context.ps1 -AgentType [claude|gemini|copilot|codex]"

