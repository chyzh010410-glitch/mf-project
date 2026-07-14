param(
    [string]$Model = "deepseek-v4-flash"
)

$ErrorActionPreference = "Stop"

$secureKey = Read-Host "请输入 DeepSeek API Key（输入不会显示）" -AsSecureString
$bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secureKey)
try {
    $env:DEEPSEEK_API_KEY = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
    $env:MF_AGENT_LLM_ENABLED = "true"
    $env:MF_AGENT_SPRING_AI_CHAT_MODEL = "openai"
    $env:MF_AGENT_OPENAI_BASE_URL = "https://api.deepseek.com"
    $env:MF_AGENT_OPENAI_MODEL = $Model

    Write-Host "正在启动 MF_AgentService（DeepSeek 已启用，Key 不会写入文件或输出）..."
    mvn spring-boot:run
} finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
    Remove-Item Env:DEEPSEEK_API_KEY -ErrorAction SilentlyContinue
}
