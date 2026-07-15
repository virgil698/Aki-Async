param(
    [string]$ProjectRoot = (Get-Location).Path,
    [string]$MainPackage = "com.akiasync"
)

$ErrorActionPreference = "Stop"
$root = (Resolve-Path -LiteralPath $ProjectRoot).Path
$mainRoot = Join-Path $root "src/main/java"
$mixinRoot = Join-Path $root "src/mixin/java"
$mainPackagePattern = [regex]::Escape($MainPackage)
$violations = [System.Collections.Generic.List[string]]::new()

function Add-Violation {
    param(
        [string]$Path,
        [int]$LineNumber,
        [string]$Message,
        [string]$Line
    )

    $relative = [System.IO.Path]::GetRelativePath($root, $Path)
    $violations.Add("${relative}:${LineNumber}: ${Message}`n    $($Line.Trim())")
}

if (Test-Path -LiteralPath $mainRoot) {
    Get-ChildItem -LiteralPath $mainRoot -Recurse -Filter "*.java" -File | ForEach-Object {
        $file = $_
        $lines = Get-Content -LiteralPath $file.FullName
        for ($index = 0; $index -lt $lines.Count; $index++) {
            $line = $lines[$index]
            $trimmed = $line.Trim()
            if ($trimmed -match '^(//|/\*|\*)') {
                continue
            }

            if ($line -match '^\s*import\s+(?:static\s+)?(?:net\.minecraft|com\.mojang)(?:\.|;)') {
                Add-Violation $file.FullName ($index + 1) "main code must not import vanilla/Mojang classes" $line
            } elseif ($line -match '\b(?:net\.minecraft|com\.mojang)\.') {
                Add-Violation $file.FullName ($index + 1) "main code must not directly reference vanilla/Mojang classes" $line
            }

            if ($line -match '^\s*import\s+(?:static\s+)?(?:org\.spongepowered\.asm\.mixin|com\.llamalad7\.mixinextras)(?:\.|;)') {
                Add-Violation $file.FullName ($index + 1) "main code must not import Mixin or Mixin Extras" $line
            }

            if ($line -match "^\s*import\s+(?:static\s+)?${mainPackagePattern}\.mixin(?:\.|;)" -and
                $line -notmatch "^\s*import\s+(?:static\s+)?${mainPackagePattern}\.mixin\.(?:Bridge|BridgeManager)(?:\.|;)") {
                Add-Violation $file.FullName ($index + 1) "main code may reference only the template Bridge/BridgeManager API from Mixin output" $line
            }
        }
    }
}

if (Test-Path -LiteralPath $mixinRoot) {
    Get-ChildItem -LiteralPath $mixinRoot -Recurse -Filter "*.java" -File | ForEach-Object {
        $file = $_
        $normalizedPath = $file.FullName.Replace('\', '/')
        $insideMixinsPackage = $normalizedPath -match '/mixin/mixins/'
        $lines = Get-Content -LiteralPath $file.FullName
        $content = $lines -join "`n"

        $containsInjection = $content -match '(?m)^\s*import\s+(?:static\s+)?(?:org\.spongepowered\.asm\.mixin\.Mixin|org\.spongepowered\.asm\.mixin\.gen\.(?:Accessor|Invoker)|org\.spongepowered\.asm\.mixin\.injection\.|com\.llamalad7\.mixinextras\.injector\.)' -or
            $content -match '@(?:Mixin|Accessor|Invoker|Inject|Redirect|Modify\w+|Wrap\w+|Overwrite)\b'

        if ($containsInjection -and -not $insideMixinsPackage) {
            Add-Violation $file.FullName 1 "actual injection/accessor/invoker classes must live under the mixin.mixins package" "package/path boundary"
        }

        for ($index = 0; $index -lt $lines.Count; $index++) {
            $line = $lines[$index]
            if ($line -match "^\s*import\s+(?:static\s+)?${mainPackagePattern}\.(?!mixin(?:\.|;))") {
                Add-Violation $file.FullName ($index + 1) "Mixin-side code must not import main/plugin implementation classes; use Bridge contracts" $line
            }

            $trimmed = $line.Trim()
            if (-not $insideMixinsPackage -and $file.BaseName -match 'Bridge' -and
                $trimmed -notmatch '^(//|/\*|\*)' -and
                $line -match '\b(?:net\.minecraft|com\.mojang)\.') {
                Add-Violation $file.FullName ($index + 1) "Bridge contracts/managers must not expose vanilla/Mojang types to main code" $line
            }
        }
    }
}

if ($violations.Count -gt 0) {
    [Console]::Error.WriteLine("Leaves source-boundary violations:`n" + ($violations -join "`n"))
    exit 1
}

Write-Output "Leaves source boundaries OK."
exit 0
