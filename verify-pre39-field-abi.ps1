param(
    [string]$ProjectRoot = $PSScriptRoot
)

$ErrorActionPreference = "Stop"

$rules = @(
    [pscustomobject]@{ Symbol = "HexItems.SCROLL_LARGE"; Pattern = 'HexItems\.SCROLL_LARGE(?![A-Za-z0-9_$])'; Accessor = "get" },
    [pscustomobject]@{ Symbol = "HexItems.SPELLBOOK"; Pattern = 'HexItems\.SPELLBOOK(?![A-Za-z0-9_$])'; Accessor = "get" },
    [pscustomobject]@{ Symbol = "HexActions.BRAINSWEEP"; Pattern = 'HexActions\.BRAINSWEEP(?![A-Za-z0-9_$])'; Accessor = "value" },
    [pscustomobject]@{ Symbol = "HexActions.EVAL"; Pattern = 'HexActions\.EVAL(?![A-Za-z0-9_$])'; Accessor = "value" },
    [pscustomobject]@{ Symbol = "HexActions.EXPLODE"; Pattern = 'HexActions\.EXPLODE(?![A-Za-z0-9_$])'; Accessor = "value" },
    [pscustomobject]@{ Symbol = 'HexActions.EXPLODE$FIRE'; Pattern = 'HexActions\.(?:`)?EXPLODE\$FIRE(?:`)?(?![A-Za-z0-9_$])'; Accessor = "value" },
    [pscustomobject]@{ Symbol = "HexEvalSounds.MISHAP"; Pattern = 'HexEvalSounds\.MISHAP(?![A-Za-z0-9_$])'; Accessor = "get" },
    [pscustomobject]@{ Symbol = "HexEvalSounds.THOTH"; Pattern = 'HexEvalSounds\.THOTH(?![A-Za-z0-9_$])'; Accessor = "get" },
    [pscustomobject]@{ Symbol = "HexIotaTypes.BOOLEAN"; Pattern = 'HexIotaTypes\.BOOLEAN(?![A-Za-z0-9_$])'; Accessor = "get" },
    [pscustomobject]@{ Symbol = "HexIotaTypes.CONTINUATION"; Pattern = 'HexIotaTypes\.CONTINUATION(?![A-Za-z0-9_$])'; Accessor = "get" },
    [pscustomobject]@{ Symbol = "HexIotaTypes.DOUBLE"; Pattern = 'HexIotaTypes\.DOUBLE(?![A-Za-z0-9_$])'; Accessor = "get" },
    [pscustomobject]@{ Symbol = "HexIotaTypes.ENTITY"; Pattern = 'HexIotaTypes\.ENTITY(?![A-Za-z0-9_$])'; Accessor = "get" },
    [pscustomobject]@{ Symbol = "HexIotaTypes.GARBAGE"; Pattern = 'HexIotaTypes\.GARBAGE(?![A-Za-z0-9_$])'; Accessor = "get" },
    [pscustomobject]@{ Symbol = "HexIotaTypes.LIST"; Pattern = 'HexIotaTypes\.LIST(?![A-Za-z0-9_$])'; Accessor = "get" },
    [pscustomobject]@{ Symbol = "HexIotaTypes.NULL"; Pattern = 'HexIotaTypes\.NULL(?![A-Za-z0-9_$])'; Accessor = "get" },
    [pscustomobject]@{ Symbol = "HexIotaTypes.PATTERN"; Pattern = 'HexIotaTypes\.PATTERN(?![A-Za-z0-9_$])'; Accessor = "get" },
    [pscustomobject]@{ Symbol = "HexIotaTypes.VEC3"; Pattern = 'HexIotaTypes\.VEC3(?![A-Za-z0-9_$])'; Accessor = "get" }
)

$sourceRoots = @(
    (Join-Path $ProjectRoot "common\src"),
    (Join-Path $ProjectRoot "forge\src")
)
$sourceFiles = Get-ChildItem -LiteralPath $sourceRoots -Recurse -File |
    Where-Object { $_.Extension -in ".java", ".kt" }

$failures = New-Object System.Collections.Generic.List[string]
$seenSymbols = New-Object 'System.Collections.Generic.HashSet[string]'
$occurrenceCount = 0
$staleCount = 0

foreach ($file in $sourceFiles) {
    $text = Get-Content -LiteralPath $file.FullName -Raw
    $isLazyGrandRegistry =
        $file.Name -eq "PatternRegistry.kt" -and
        $text -match 'private\s+fun\s+patternGrand\s*\(\s*parent\s*:\s*Holder\s*<\s*ActionRegistryEntry\s*>'
    foreach ($rule in $rules) {
        $regex = [regex]::new($rule.Pattern)
        foreach ($match in $regex.Matches($text)) {
            $null = $seenSymbols.Add($rule.Symbol)
            $occurrenceCount++
            $tail = $text.Substring($match.Index + $match.Length)
            $expected = '^\s*\.' + [regex]::Escape($rule.Accessor) + '\s*\(\s*\)'
            $isDeferredGrandParent =
                $isLazyGrandRegistry -and
                $rule.Symbol -in @(
                    "HexActions.BRAINSWEEP",
                    "HexActions.EXPLODE",
                    'HexActions.EXPLODE$FIRE'
                )
            if ($tail -notmatch $expected -and -not $isDeferredGrandParent) {
                $lineNumber = 1 + ($text.Substring(0, $match.Index) -split "`n").Count - 1
                $relative = $file.FullName.Substring($ProjectRoot.Length).TrimStart('\')
                $failures.Add("$relative`:$lineNumber $($rule.Symbol) must use .$($rule.Accessor)()")
                $staleCount++
            }
        }
    }
}

foreach ($rule in $rules) {
    if (-not $seenSymbols.Contains($rule.Symbol)) {
        $failures.Add("Expected migrated field is not represented in source: $($rule.Symbol)")
    }
}

$properties = Get-Content -LiteralPath (Join-Path $ProjectRoot "gradle.properties") -Raw
if ($properties -notmatch '(?m)^hexcasting_version=0\.12\.0-devel-pre-39\s*$') {
    $failures.Add("gradle.properties is not pinned to Hex Casting pre-39")
}

$modsToml = Get-Content -LiteralPath (Join-Path $ProjectRoot "forge\src\main\resources\META-INF\neoforge.mods.toml") -Raw
if ($modsToml -notmatch 'versionRange\s*=\s*"\[0\.12\.0-devel-pre-39\]"') {
    $failures.Add("neoforge.mods.toml is not pinned to exact Hex Casting pre-39")
}

$activeBuildText = @(
    (Get-Content -LiteralPath (Join-Path $ProjectRoot "common\build.gradle") -Raw),
    (Get-Content -LiteralPath (Join-Path $ProjectRoot "forge\build.gradle") -Raw)
) -join "`n"
if ($activeBuildText -match 'pre-34|pre34') {
    $failures.Add("Active Gradle dependencies still reference a pre34 artifact")
}

$provider = Join-Path $ProjectRoot "libs\hexcasting-neoforge-1.21.1-0.12.0-devel-pre-39.jar"
$expectedProviderHash = "FFA99DDADAF5ACE9616986E236D094E6F3E63A58ED9996BB5BC37A4313EDB99C"
if (-not (Test-Path -LiteralPath $provider -PathType Leaf)) {
    $failures.Add("Hex Casting pre39 provider JAR is missing")
} elseif ((Get-FileHash -LiteralPath $provider -Algorithm SHA256).Hash -ne $expectedProviderHash) {
    $failures.Add("Hex Casting pre39 provider JAR hash mismatch")
}

$hexal = Join-Path $ProjectRoot "libs\hexal-0.3.1+1.21.1-neoforge-pre39.jar"
$moreIotas = Join-Path $ProjectRoot "libs\moreiotas-0.1.1+1.21.1-neoforge-pre39.jar"

Write-Output "EXPECTED_FIELD_DESCRIPTORS=$($rules.Count)"
Write-Output "SEEN_FIELD_DESCRIPTORS=$($seenSymbols.Count)"
Write-Output "SOURCE_FIELD_OCCURRENCES=$occurrenceCount"
Write-Output "STALE_FIELD_OCCURRENCES=$staleCount"
Write-Output "HEXCASTING_PROVIDER_SHA256=$((Get-FileHash -LiteralPath $provider -Algorithm SHA256).Hash)"
Write-Output "HEXAL_PRE39_LOCAL=$((Test-Path -LiteralPath $hexal -PathType Leaf).ToString().ToUpperInvariant())"
Write-Output "MOREIOTAS_PRE39_LOCAL=$((Test-Path -LiteralPath $moreIotas -PathType Leaf).ToString().ToUpperInvariant())"

if ($failures.Count -gt 0) {
    $failures | ForEach-Object { Write-Output "ERROR: $_" }
    Write-Output "PRE39_FIELD_ABI=FAIL failure_count=$($failures.Count)"
    exit 1
}

Write-Output "PRE39_FIELD_ABI=PASS"
