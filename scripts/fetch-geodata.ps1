param(
	[string]$Source = $env:L2J_GEODATA_SOURCE,
	[switch]$Force
)

$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem

$root = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot ".."))
$geodataDir = Join-Path $root "deploy\game\data\geodata"
$pathnodeDir = Join-Path $root "deploy\game\data\pathnode"
$tempDir = Join-Path ([System.IO.Path]::GetTempPath()) ("l2j-geodata-" + [System.Guid]::NewGuid().ToString("N"))

$sourcePages = @(
	"https://www.lineage2.org.uk/download/geodata-interlude",
	"https://www.lineage2.org.uk/download/pathnode-interlude",
	"https://sourceforge.net/projects/l2j-geodata/files/"
)

function Get-InstalledFileCount {
	param([string]$Directory, [string]$Filter)

	if (-not (Test-Path -LiteralPath $Directory)) {
		return 0
	}
	return (Get-ChildItem -LiteralPath $Directory -Filter $Filter -File | Measure-Object).Count
}

function Read-DataFileEntries {
	param([string]$Path)

	if (Test-Path -LiteralPath $Path -PathType Container) {
		return Get-ChildItem -LiteralPath $Path -Recurse -File | Where-Object { $_.Extension -in ".l2j", ".pn" }
	}

	$resolved = (Resolve-Path -LiteralPath $Path).Path
	$archive = [System.IO.Compression.ZipFile]::OpenRead($resolved)
	try {
		$entries = @()
		foreach ($entry in $archive.Entries) {
			if ($entry.FullName.EndsWith("/") -or $entry.FullName.EndsWith("\")) {
				continue
			}
			$extension = [System.IO.Path]::GetExtension($entry.FullName).ToLowerInvariant()
			if ($extension -notin ".l2j", ".pn") {
				continue
			}
			$target = Join-Path $tempDir $entry.FullName
			$parent = [System.IO.Path]::GetDirectoryName($target)
			if (-not (Test-Path -LiteralPath $parent)) {
				New-Item -ItemType Directory -Path $parent | Out-Null
			}
			[System.IO.Compression.ZipFileExtensions]::ExtractToFile($entry, $target, $true)
			$entries += Get-Item -LiteralPath $target
		}
		return $entries
	}
	finally {
		$archive.Dispose()
	}
}

if (-not $Source) {
	if ((Get-InstalledFileCount $geodataDir "*.l2j") -gt 0) {
		Write-Host "Geodata is already installed in deploy/game/data/geodata."
		Write-Host "Run with -Force to overwrite, or pass -Source to install a different pack."
		exit 0
	}
	Write-Host "No geodata source given. This repository deliberately does not distribute geodata files."
	Write-Host ""
	Write-Host "Download an Interlude geodata zip once from any community archive, for example:"
	foreach ($page in $sourcePages) {
		Write-Host "  $page"
	}
	Write-Host ""
	Write-Host "Then install it by running:"
	Write-Host "  scripts\fetch-geodata.ps1 -Source C:\path\to\geodata.zip"
	Write-Host "A directory containing the unpacked files works as well."
	exit 2
}

try
{
	if ($Source -match "^https?://") {
		New-Item -ItemType Directory -Force -Path $tempDir | Out-Null
		try {
			$downloadPath = Join-Path $tempDir "geodata-pack.zip"
			Invoke-WebRequest -Uri $Source -UserAgent "Mozilla/5.0 (Windows NT 10.0; Win64; x64)" -OutFile $downloadPath
			$Source = $downloadPath
		}
		catch {
			Write-Warning "Download failed: $($_.Exception.Message)"
			Write-Host "Download the pack manually with a browser, then rerun with -Source pointing at the local file."
			exit 1
		}
	}

	if (-not (Test-Path -LiteralPath $Source)) {
		Write-Warning "Source not found: $Source"
		exit 1
	}

	$entries = @(Read-DataFileEntries -Path $Source)
	if ($entries.Count -eq 0) {
		Write-Warning "No .l2j or .pn files found in $Source."
		exit 1
	}

	New-Item -ItemType Directory -Force -Path $geodataDir, $pathnodeDir | Out-Null

	$geodata = @($entries | Where-Object { $_.Extension -eq ".l2j" })
	$pathnodes = @($entries | Where-Object { $_.Extension -eq ".pn" })

	$copiedGeodata = 0
	foreach ($file in $geodata) {
		$target = Join-Path $geodataDir $file.Name
		if ((Test-Path -LiteralPath $target) -and -not $Force) {
			continue
		}
		Copy-Item -LiteralPath $file.FullName -Destination $target -Force
		$copiedGeodata++
	}

	$copiedPathnodes = 0
	foreach ($file in $pathnodes) {
		$target = Join-Path $pathnodeDir $file.Name
		if ((Test-Path -LiteralPath $target) -and -not $Force) {
			continue
		}
		Copy-Item -LiteralPath $file.FullName -Destination $target -Force
		$copiedPathnodes++
	}

	$totalGeodata = Get-InstalledFileCount $geodataDir "*.l2j"
	$totalPathnodes = Get-InstalledFileCount $pathnodeDir "*.pn"

	Write-Host "Installed $copiedGeodata new .l2j files into deploy/game/data/geodata ($totalGeodata total)"
	Write-Host "Installed $copiedPathnodes new .pn files into deploy/game/data/pathnode ($totalPathnodes total)"

	if ($totalGeodata -lt 50 -and $geodata.Count -gt 0) {
		Write-Warning "Fewer than 50 geodata region files installed. Check that this pack is a complete Interlude geodata archive."
	}
}
finally {
	if (Test-Path -LiteralPath $tempDir) {
		Remove-Item -LiteralPath $tempDir -Recurse -Force
	}
}

Write-Host ""
Write-Host "Next steps:"
Write-Host "  1. Set GeoData = 1 (line of sight and playable movement) or GeoData = 2 (full pathfinding)"
Write-Host "     in deploy/game/config/options.properties."
Write-Host "  2. Restart the game server, for example: docker compose up -d l2j-game"
