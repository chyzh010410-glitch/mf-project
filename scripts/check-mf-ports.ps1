$ErrorActionPreference = "Stop"
. "$PSScriptRoot\mf-ports.ps1"

$rows = foreach ($name in $MFPorts.Keys) {
  $port = $MFPorts[$name]
  $connections = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue

  if ($connections) {
    foreach ($connection in $connections) {
      $process = Get-Process -Id $connection.OwningProcess -ErrorAction SilentlyContinue
      [pscustomobject]@{
        Name = $name
        Port = $port
        Status = "Listening"
        ProcessId = $connection.OwningProcess
        ProcessName = $process.ProcessName
        Path = $process.Path
        Description = $MFPortDescriptions[$name]
      }
    }
  } else {
    [pscustomobject]@{
      Name = $name
      Port = $port
      Status = "Free"
      ProcessId = ""
      ProcessName = ""
      Path = ""
      Description = $MFPortDescriptions[$name]
    }
  }
}

$rows | Sort-Object Port | Format-Table -AutoSize

