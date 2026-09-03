param(
    [string]$ProjectRoot = $PSScriptRoot,
    [string]$BaselineRoot = "D:\mymod\NEOFORGE_1_21_1_PORT_OUTPUT\pre34-remediation\workspace\07-hextweaks-neoforge-port"
)

$ErrorActionPreference = "Stop"
$failures = New-Object System.Collections.Generic.List[string]

function RelativeHashMap([string]$Root, [string]$SubPath) {
    $treeRoot = Join-Path $Root $SubPath
    $result = @{}
    Get-ChildItem -LiteralPath $treeRoot -Recurse -File | ForEach-Object {
        $relative = $_.FullName.Substring($treeRoot.Length).TrimStart('\')
        $result[$relative] = (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash
    }
    return $result
}

function CompareExactTree([string]$Name, [string]$SubPath) {
    $baseline = RelativeHashMap $BaselineRoot $SubPath
    $current = RelativeHashMap $ProjectRoot $SubPath
    $missing = @($baseline.Keys | Where-Object { -not $current.ContainsKey($_) })
    $extra = @($current.Keys | Where-Object { -not $baseline.ContainsKey($_) })
    $changed = @($baseline.Keys | Where-Object {
        $current.ContainsKey($_) -and $baseline[$_] -ne $current[$_]
    })
    if ($missing.Count -gt 0 -or $extra.Count -gt 0 -or $changed.Count -gt 0) {
        $failures.Add("$Name parity failed: missing=$($missing.Count) extra=$($extra.Count) changed=$($changed.Count)")
    }
    Write-Output "$Name files=$($current.Count) missing=$($missing.Count) extra=$($extra.Count) changed=$($changed.Count)"
}

function RequireText([string]$Name, [string]$Path, [string[]]$Patterns) {
    $text = Get-Content -LiteralPath (Join-Path $ProjectRoot $Path) -Raw
    foreach ($pattern in $Patterns) {
        if ($text -notmatch $pattern) {
            $failures.Add("$Name is missing required pattern: $pattern")
        }
    }
}

function SameHash([string]$Name, [string]$Path) {
    $oldHash = (Get-FileHash -LiteralPath (Join-Path $BaselineRoot $Path) -Algorithm SHA256).Hash
    $newHash = (Get-FileHash -LiteralPath (Join-Path $ProjectRoot $Path) -Algorithm SHA256).Hash
    if ($oldHash -ne $newHash) {
        $failures.Add("$Name changed unexpectedly: $Path")
    }
}

function NormalizedCode([string]$Path) {
    $text = Get-Content -LiteralPath $Path -Raw
    $text = [regex]::Replace($text, '(?s)/\*.*?\*/', '')
    $text = [regex]::Replace($text, '(?m)//.*$', '')
    return [regex]::Replace($text, '\s+', '')
}

CompareExactTree "RESOURCES" "common\src\main\resources"
CompareExactTree "MINDFLAY_RITUALS" "common\src\main\java\net\walksanator\hextweaks\casting\mindflay"

SameHash "GRAND_SEED_COMPAT" "common\src\main\java\net\walksanator\hextweaks\hexcompat\GrandSpellSeedCompat.kt"
SameHash "GRAND_HANDLER" "common\src\main\java\net\walksanator\hextweaks\casting\handler\GrandSpellHandler.kt"
SameHash "MINDFLAY_REGISTRY" "common\src\main\java\net\walksanator\hextweaks\casting\MindflayRegistry.kt"
SameHash "MOREIOTAS_RITUAL_ACTION" "common\src\main\java\net\walksanator\hextweaks\casting\actions\OpEgyptianPlagues.kt"
SameHash "RITUAL_IOTA" "common\src\main\java\net\walksanator\hextweaks\casting\iota\RitualIota.kt"

$codecPath = "common\src\main\java\net\walksanator\hextweaks\hexcompat\SpellContinuationCodecCompat.kt"
$oldCodec = NormalizedCode (Join-Path $BaselineRoot $codecPath)
$newCodec = NormalizedCode (Join-Path $ProjectRoot $codecPath)
if ($oldCodec -ne $newCodec) {
    $failures.Add("Continuation codec executable code changed; only the target-version comment may differ")
}

$computerSubPath = "common\src\main\java\net\walksanator\hextweaks\computer"
$oldComputer = RelativeHashMap $BaselineRoot $computerSubPath
$newComputer = RelativeHashMap $ProjectRoot $computerSubPath
$computerMissing = @($oldComputer.Keys | Where-Object { -not $newComputer.ContainsKey($_) })
$computerExtra = @($newComputer.Keys | Where-Object { -not $oldComputer.ContainsKey($_) })
$computerChanged = @($oldComputer.Keys | Where-Object {
    $newComputer.ContainsKey($_) -and $oldComputer[$_] -ne $newComputer[$_]
} | Sort-Object)
$expectedComputerChanges = @("IotaSerdeRegistry.kt", "WandPeripheral.kt")
if ($computerMissing.Count -gt 0 -or $computerExtra.Count -gt 0 -or
    (Compare-Object -ReferenceObject $expectedComputerChanges -DifferenceObject $computerChanged)) {
    $failures.Add("ComputerCraft source drift is not limited to the two pre39 ABI call sites")
}
Write-Output "COMPUTERCRAFT files=$($newComputer.Count) changed=$($computerChanged -join ',')"

RequireText "GRAND_SPELL" "common\src\main\java\net\walksanator\hextweaks\casting\PatternRegistry.kt" @(
    'parent\s*:\s*Holder\s*<\s*ActionRegistryEntry\s*>',
    'registerAlternative',
    'parent\.value\(\)\.prototype\.angles',
    'Platform\.isModLoaded\("moreiotas"\)',
    'OpEgyptianPlagues'
)
RequireText "CONTINUATION_CODEC" $codecPath @(
    'SpellContinuation\.CODEC',
    'SpellContinuation\.STREAM_CODEC',
    'decodeLegacy'
)
RequireText "COMPUTERCRAFT_SERDE" "common\src\main\java\net\walksanator\hextweaks\computer\IotaSerdeRegistry.kt" @(
    'SpellContinuationCodecCompat\.encode',
    'SpellContinuationCodecCompat\.decode',
    'MoreIotasIotaTypes',
    'modloc\("ritual"\)'
)
RequireText "SERVER_PROBE" "forge\src\main\java\net\walksanator\hextweaks\forge\HexTweaksProbe.kt" @(
    'checkIotaCodecs',
    'checkWhileContinuation',
    'checkGrandSpellHandler',
    'checkComputerCraft',
    'checkMoreIotasSerde',
    'checkHexalRituals',
    'aggregate=PASS hexcasting=pre-39'
)
RequireText "CLIENT_PROBE" "forge\src\main\java\net\walksanator\hextweaks\forge\HexTweaksClientProbe.java" @(
    'translations=PASS model=PASS patchouli=PASS',
    'hexcasting=pre-39'
)

$fakeApiRoot = Join-Path $ProjectRoot "common\src\main\java\at\petrak\hexcasting"
if (Test-Path -LiteralPath $fakeApiRoot) {
    $failures.Add("Forbidden Hex Casting shim package exists: $fakeApiRoot")
}

$activeBuildText = @(
    (Get-Content -LiteralPath (Join-Path $ProjectRoot "common\build.gradle") -Raw),
    (Get-Content -LiteralPath (Join-Path $ProjectRoot "forge\build.gradle") -Raw)
) -join "`n"
$legacyBuildMatches = [regex]::Matches($activeBuildText, 'pre-34|pre34|pre-2|pre2')
Write-Output "ACTIVE_LEGACY_BUILD_REFS=$($legacyBuildMatches.Count)"
if ($legacyBuildMatches.Count -gt 0) {
    $failures.Add("Active Gradle dependency text references pre2/pre34")
}

$hexalPresent = Test-Path -LiteralPath (Join-Path $ProjectRoot "libs\hexal-0.3.1+1.21.1-neoforge-pre39.jar") -PathType Leaf
$moreIotasPresent = Test-Path -LiteralPath (Join-Path $ProjectRoot "libs\moreiotas-0.1.1+1.21.1-neoforge-pre39.jar") -PathType Leaf
Write-Output "CONTINUATION_CODEC_CODE_PARITY=$($oldCodec -eq $newCodec)"
Write-Output "GRAND_SPELL_LAZY_HOLDER=TRUE"
Write-Output "NO_HEXCASTING_SHIM=$(-not (Test-Path -LiteralPath $fakeApiRoot))"
Write-Output "HEXAL_PRE39_BUILD_INPUT=$($hexalPresent.ToString().ToUpperInvariant())"
Write-Output "MOREIOTAS_PRE39_BUILD_INPUT=$($moreIotasPresent.ToString().ToUpperInvariant())"

if ($failures.Count -gt 0) {
    $failures | ForEach-Object { Write-Output "ERROR: $_" }
    Write-Output "PRE39_FEATURE_PARITY=FAIL failure_count=$($failures.Count)"
    exit 1
}

Write-Output "PRE39_FEATURE_PARITY=PASS"
