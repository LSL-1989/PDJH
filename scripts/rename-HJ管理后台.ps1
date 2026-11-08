# 将 HJ后台 重命名为 HJ管理后台，并将内部工程 Gocent.PDJH 重命名为 Gocent.Manager
# 需先停止管理后台 npm run dev
$root = Split-Path -Parent $PSScriptRoot
$src = Join-Path $root 'HJ后台'
$dst = Join-Path $root 'HJ管理后台'
if (-not (Test-Path -LiteralPath $src)) {
  if (Test-Path -LiteralPath $dst) { Write-Host '已存在 HJ管理后台，跳过目录改名。' }
  else { Write-Error "源目录不存在: $src"; exit 1 }
} elseif (Test-Path -LiteralPath $dst) {
  Write-Error "目标已存在: $dst"; exit 1
} else {
  Rename-Item -LiteralPath $src -NewName 'HJ管理后台'
  Write-Host '完成: HJ后台 -> HJ管理后台'
}

$pdjh = Join-Path $root 'HJ管理后台\Gocent.PDJH'
$mgr = Join-Path $root 'HJ管理后台\Gocent.Manager'
if (Test-Path -LiteralPath $pdjh) {
  if (Test-Path -LiteralPath $mgr) { Write-Error "目标已存在: $mgr"; exit 1 }
  Rename-Item -LiteralPath $pdjh -NewName 'Gocent.Manager'
  Write-Host '完成: Gocent.PDJH -> Gocent.Manager'
} elseif (Test-Path -LiteralPath $mgr) {
  Write-Host '已存在 Gocent.Manager，跳过内部工程改名。'
} else {
  Write-Error "未找到 HJ管理后台/Gocent.PDJH 或 Gocent.Manager"
  exit 1
}
