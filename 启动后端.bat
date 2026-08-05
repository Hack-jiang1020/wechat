@echo off
title 个人博客微信小程序系统 - 后端启动器
cd /d "%~dp0backend"

where mvn >nul 2>nul
if errorlevel 1 (
  echo [提示] 未检测到 Maven 命令，请先安装 Maven 3.6+ 并加入 PATH。
  echo        若已用 IDEA 或手动执行过 mvn package，可跳过编译直接运行 target\blog-miniapp.jar。
  pause
  exit /b 1
)

if not exist "target\blog-miniapp.jar" (
  echo [1/2] 检测到尚未编译，正在编译打包（首次会自动下载依赖，请耐心等待）...
  call mvn -s "%~dp0maven-settings.xml" clean package -DskipTests
  if errorlevel 1 (
    echo [错误] 编译失败，请查看上方日志。
    pause
    exit /b 1
  )
)

echo [2/2] 正在启动后端服务...
echo 管理后台: http://localhost:8080/admin
echo 默认账号: admin / admin123
echo 按 Ctrl+C 可停止服务。
echo.
java -jar target\blog-miniapp.jar
pause