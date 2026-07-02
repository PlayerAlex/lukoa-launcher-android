param(
    [Parameter(Mandatory = $true)]
    [string]$TargetVersion,

    [string]$FromVersion = "",
    [string]$SourceFile = "",
    [string]$OutputFile = "",
    [string[]]$CurrentHighlights = @(),
    [ValidateSet("Long", "Short")]
    [string]$Format = "Long"
)

$ErrorActionPreference = "Stop"

function Parse-VersionValue {
    param([string]$Value)

    $normalized = $Value.Trim()
    if ($normalized.StartsWith("v", [System.StringComparison]::OrdinalIgnoreCase)) {
        $normalized = $normalized.Substring(1)
    }
    return [version]::Parse($normalized)
}

function Get-DefaultSourceFile {
    param([string]$ProjectRoot)

    $candidate = Join-Path $ProjectRoot "..\..\outputs\制作进度.md"
    if (Test-Path -LiteralPath $candidate) {
        return (Resolve-Path -LiteralPath $candidate).Path
    }

    throw "找不到制作进度文档，请用 -SourceFile 指定来源。"
}

function Get-PreviousTagVersion {
    param(
        [version]$Target,
        [string]$ProjectRoot
    )

    $tags = @()
    try {
        $tags = (& git -C $ProjectRoot tag --list "v*")
    } catch {
        return $null
    }

    $candidates = foreach ($tag in $tags) {
        $text = $tag.Trim()
        if ([string]::IsNullOrWhiteSpace($text)) { continue }
        try {
            $value = Parse-VersionValue $text
        } catch {
            continue
        }
        if ($value -lt $Target) {
            [pscustomobject]@{
                Tag = $text
                Version = $value
            }
        }
    }

    return $candidates |
        Sort-Object Version -Descending |
        Select-Object -First 1
}

function Parse-ProgressSections {
    param([string]$Content)

    $pattern = '(?ms)^## 版本 (?<version>\d+\.\d+\.\d+)\s*\r?\n(?<body>.*?)(?=^## 版本 \d+\.\d+\.\d+\s*$|\z)'
    $matches = [regex]::Matches($Content, $pattern)

    $sections = foreach ($match in $matches) {
        $versionText = $match.Groups["version"].Value.Trim()
        $versionValue = Parse-VersionValue $versionText
        [pscustomobject]@{
            VersionText = $versionText
            Version = $versionValue
            Train = "{0}.{1}" -f $versionValue.Major, $versionValue.Minor
            Body = $match.Groups["body"].Value.Trim()
        }
    }

    return $sections | Sort-Object Version
}

function Get-SectionDescriptor {
    param([string[]]$Lines)

    $cleanLines = foreach ($line in $Lines) {
        $trimmed = $line.Trim()
        if ([string]::IsNullOrWhiteSpace($trimmed)) { continue }
        if ($trimmed -eq "构建验证：") { break }
        if ($trimmed -like '```*') { continue }
        $trimmed
    }

    $preferred = $cleanLines | Where-Object {
        $_ -notmatch '^这是' -and
        $_ -notmatch '^(?:\d+\.|-)\s+'
    } | Select-Object -First 1

    if ($preferred) {
        return $preferred.TrimEnd("：")
    }

    $fallback = $cleanLines | Select-Object -First 1
    if ($fallback) {
        return $fallback.TrimEnd("：")
    }

    return ""
}

function Get-SectionBullets {
    param([string[]]$Lines)

    $bullets = New-Object System.Collections.Generic.List[string]
    $inCodeBlock = $false

    foreach ($line in $Lines) {
        $trimmed = $line.Trim()
        if ($trimmed -like '```*') {
            $inCodeBlock = -not $inCodeBlock
            continue
        }
        if ($inCodeBlock) { continue }
        if ($trimmed -eq "构建验证：") { break }
        if ($trimmed -match '^\d+\.\s+(.+)$') {
            $text = $matches[1].Trim().TrimEnd("：")
            if ($text -and -not $bullets.Contains($text)) {
                $bullets.Add($text)
            }
        }
    }

    if ($bullets.Count -eq 0) {
        foreach ($line in $Lines) {
            $trimmed = $line.Trim()
            if ($trimmed -match '^-+\s+(.+)$') {
                $text = $matches[1].Trim().TrimEnd("：")
                if ($text -and -not $bullets.Contains($text)) {
                    $bullets.Add($text)
                }
            }
        }
    }

    return @($bullets | Select-Object -First 4)
}

function Build-TrainSection {
    param(
        [string]$Train,
        [object]$RepresentativeSection
    )

    $lines = $RepresentativeSection.Body -split "`r?`n"
    $descriptor = Get-SectionDescriptor $lines
    $bullets = Get-SectionBullets $lines

    $builder = New-Object System.Text.StringBuilder
    [void]$builder.AppendLine(("### {0}.x" -f $Train))
    [void]$builder.AppendLine()
    if ($descriptor) {
        [void]$builder.AppendLine($descriptor)
        [void]$builder.AppendLine()
    }
    foreach ($bullet in $bullets) {
        [void]$builder.AppendLine(("- {0}" -f $bullet))
    }
    [void]$builder.AppendLine()
    return $builder.ToString()
}

function Build-ShortTrainLine {
    param(
        [string]$Train,
        [object]$RepresentativeSection
    )

    $lines = $RepresentativeSection.Body -split "`r?`n"
    $bullets = Get-SectionBullets $lines
    $summaryParts = @($bullets | Select-Object -First 3)

    if ($summaryParts.Count -eq 0) {
        $descriptor = Get-SectionDescriptor $lines
        if ($descriptor) {
            $summaryParts = @($descriptor)
        }
    }

    $cleanParts = foreach ($part in $summaryParts) {
        $part.Trim().TrimEnd([char[]]"。：")
    }

    $summary = if ($cleanParts.Count -gt 0) {
        ($cleanParts -join "、")
    } else {
        "这一段做了大量调整和修复"
    }

    return ("- {0}.x：{1}" -f $Train, $summary)
}

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$target = Parse-VersionValue $TargetVersion

if ([string]::IsNullOrWhiteSpace($SourceFile)) {
    $SourceFile = Get-DefaultSourceFile $projectRoot
} else {
    $SourceFile = (Resolve-Path -LiteralPath $SourceFile -ErrorAction Stop).Path
}

$resolvedFromVersion = $FromVersion
if ([string]::IsNullOrWhiteSpace($resolvedFromVersion)) {
    $previousTag = Get-PreviousTagVersion -Target $target -ProjectRoot $projectRoot
    if ($previousTag) {
        $resolvedFromVersion = $previousTag.Version.ToString()
    }
}

$content = Get-Content -LiteralPath $SourceFile -Raw -Encoding UTF8
$sections = Parse-ProgressSections $content

if (-not $sections -or $sections.Count -eq 0) {
    throw "制作进度文档里没有识别到版本段落。"
}

$from = if ([string]::IsNullOrWhiteSpace($resolvedFromVersion)) {
    $sections[0].Version
} else {
    Parse-VersionValue $resolvedFromVersion
}

$visibleSections = @($sections | Where-Object {
    $_.Version -ge $from -and $_.Version -le $target
})

if ($visibleSections.Count -eq 0) {
    throw "在制作进度文档里没有找到从 $resolvedFromVersion 到 $TargetVersion 的版本段落。"
}

$visibleTrains = $visibleSections |
    Select-Object -ExpandProperty Train -Unique

$builder = New-Object System.Text.StringBuilder

if ($Format -eq "Short") {
    [void]$builder.AppendLine(("## v{0} 更新速览" -f $TargetVersion))
    [void]$builder.AppendLine()
    [void]$builder.AppendLine(("从 v{0} 到 v{1}，主要变化如下：" -f $from, $TargetVersion))
    [void]$builder.AppendLine()

    foreach ($train in $visibleTrains) {
        $trainSections = @($sections | Where-Object { $_.Train -eq $train })
        $representative = $trainSections | Where-Object { $_.Version.Build -eq 0 } | Select-Object -First 1
        if (-not $representative) {
            $representative = $trainSections | Select-Object -First 1
        }
        [void]$builder.AppendLine((Build-ShortTrainLine -Train $train -RepresentativeSection $representative))
    }

    if ($CurrentHighlights.Count -gt 0) {
        [void]$builder.AppendLine()
        [void]$builder.AppendLine("当前版本补充：")
        foreach ($highlight in $CurrentHighlights) {
            $text = $highlight.Trim()
            if ($text) {
                [void]$builder.AppendLine(("- {0}" -f $text))
            }
        }
    }

    [void]$builder.AppendLine()
    [void]$builder.AppendLine("升级提醒：")
    [void]$builder.AppendLine("- 建议先做一次手动备份。")
    [void]$builder.AppendLine("- 如果你还停在很早的旧版，必要时可以先手动安装一次最新版 APK。")
} else {
    [void]$builder.AppendLine(("## 从 v{0} 到 v{1} 的升级摘要" -f $from, $TargetVersion))
    [void]$builder.AppendLine()
    [void]$builder.AppendLine("这份公告由制作进度文档自动整理，适合给跨多个小版本升级的用户看。")
    [void]$builder.AppendLine()

    foreach ($train in $visibleTrains) {
        $trainSections = @($sections | Where-Object { $_.Train -eq $train })
        $representative = $trainSections | Where-Object { $_.Version.Build -eq 0 } | Select-Object -First 1
        if (-not $representative) {
            $representative = $trainSections | Select-Object -First 1
        }
        $sectionMarkdown = Build-TrainSection -Train $train -RepresentativeSection $representative
        [void]$builder.Append($sectionMarkdown)
    }

    $exactTargetSection = $sections | Where-Object { $_.Version -eq $target } | Select-Object -First 1
    if ($exactTargetSection -and $exactTargetSection.Version.Build -ne 0) {
        $lines = $exactTargetSection.Body -split "`r?`n"
        $descriptor = Get-SectionDescriptor $lines
        $bullets = Get-SectionBullets $lines
        [void]$builder.AppendLine(("### 当前版本 v{0}" -f $TargetVersion))
        [void]$builder.AppendLine()
        if ($descriptor) {
            [void]$builder.AppendLine($descriptor)
            [void]$builder.AppendLine()
        }
        foreach ($bullet in ($bullets | Select-Object -First 4)) {
            [void]$builder.AppendLine(("- {0}" -f $bullet))
        }
        [void]$builder.AppendLine()
    } elseif ($CurrentHighlights.Count -gt 0) {
        [void]$builder.AppendLine(("### 当前版本 v{0}" -f $TargetVersion))
        [void]$builder.AppendLine()
        foreach ($highlight in $CurrentHighlights) {
            $text = $highlight.Trim()
            if ($text) {
                [void]$builder.AppendLine(("- {0}" -f $text))
            }
        }
        [void]$builder.AppendLine()
    }

    [void]$builder.AppendLine("### 升级提醒")
    [void]$builder.AppendLine()
    [void]$builder.AppendLine("- 大跨版本升级前，建议先做一次手动备份。")
    [void]$builder.AppendLine("- 如果你改过酒馆目录名，新版会比旧版更容易识别和接住这种情况。")
    [void]$builder.AppendLine("- 如果你还停在很早的旧版，必要时可以先手动安装一次最新版 APK，再继续用启动器自动更新。")
}

$markdown = $builder.ToString().TrimEnd()

if (-not [string]::IsNullOrWhiteSpace($OutputFile)) {
    Set-Content -LiteralPath $OutputFile -Value $markdown -Encoding UTF8
}

$markdown




