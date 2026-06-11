@echo off
chcp 65001 >nul 2>&1
setlocal enabledelayedexpansion
title 校园噪音分贝预警员系统 - 一键启动
color 0E

echo.
echo ============================================
echo  校园噪音分贝预警员系统 - 正在启动...
echo ============================================
echo.

REM ============================================
REM 路径计算（bat 在项目根目录）
REM ============================================
pushd "%~dp0"
set "PROJECT_ROOT=%CD%"
popd
set "BACKEND_DIR=%PROJECT_ROOT%\backend"
set "FRONTEND_DIR=%PROJECT_ROOT%\frontend"
set "CCSWITCH_DIR=%PROJECT_ROOT%\ccswitch_service"
set "SQL_FILE=%PROJECT_ROOT%\sql\01-init.sql"
set "DB_USER=root"
set "DB_PASS=root"
set "DB_NAME=noise_db"
set "BACKEND_PORT=8080"
set "FRONTEND_PORT=5173"
set "CCSWITCH_PORT=5000"

REM ============================================
REM 路径验证
REM ============================================
echo [INFO] 项目根目录: %PROJECT_ROOT%

if not exist "%BACKEND_DIR%" (
    echo [ERROR] 后端目录不存在: %BACKEND_DIR%
    echo 请确认 bat 文件放在项目根目录
    goto :fail
)
if not exist "%FRONTEND_DIR%" (
    echo [ERROR] 前端目录不存在: %FRONTEND_DIR%
    echo 请确认项目目录结构完整（frontend\）
    goto :fail
)
echo [OK] 目录结构验证通过
echo.

if not exist "%PROJECT_ROOT%\logs" mkdir "%PROJECT_ROOT%\logs"

REM ============================================
REM 环境检测
REM ============================================
echo ============================================
echo  环境依赖检测
echo ============================================
echo.

set "MISSING=0"

where java >nul 2>&1
if errorlevel 1 (
    echo [ERROR] 未找到 Java，请安装 JDK 21
    echo 下载: https://adoptium.net/
    set /A MISSING+=1
) else (
    echo [OK] Java
)

set "MVN_CMD=mvn"
where mvn >nul 2>&1
if errorlevel 1 (
    if exist "%BACKEND_DIR%\mvnw.cmd" (
        set "MVN_CMD=%BACKEND_DIR%\mvnw.cmd"
        echo [OK] Maven Wrapper
    ) else (
        echo [ERROR] 未找到 Maven，请安装 3.9+ 版本
        echo 下载: https://maven.apache.org/
        set /A MISSING+=1
    )
) else (
    echo [OK] Maven
)

where node >nul 2>&1
if errorlevel 1 (
    echo [ERROR] 未找到 Node.js，请安装 24 LTS
    echo 下载: https://nodejs.org/
    set /A MISSING+=1
) else (
    echo [OK] Node.js
)

where pnpm >nul 2>&1
if errorlevel 1 (
    echo [INFO] 正在安装 pnpm...
    call npm install -g pnpm@10 >nul 2>&1
    where pnpm >nul 2>&1
    if errorlevel 1 (
        echo [ERROR] pnpm 安装失败，请手动执行: npm install -g pnpm@10
        set /A MISSING+=1
    ) else (
        echo [OK] pnpm（已自动安装）
    )
) else (
    echo [OK] pnpm
)

where mysql >nul 2>&1
if errorlevel 1 (
    echo [ERROR] 未找到 MySQL，请安装 8.4
    echo 下载: https://dev.mysql.com/downloads/
    set /A MISSING+=1
) else (
    echo [OK] MySQL
)

where curl >nul 2>&1
if errorlevel 1 (
    echo [WARN] 未找到 curl，健康检查将用替代方案
) else (
    echo [OK] curl
)

where python >nul 2>&1
if errorlevel 1 (
    echo [WARN] 未找到 Python，ccswitch 配置服务将跳过（P2加分项）
    set "CCSWITCH_SKIP=1"
) else (
    echo [OK] Python
    set "CCSWITCH_SKIP=0"
)

if !MISSING! GTR 0 (
    echo.
    echo 缺少 !MISSING! 个必需依赖，无法继续
    goto :fail
)

echo.
echo 所有必需依赖检查通过
echo.

REM ============================================
REM 数据库初始化
REM ============================================
echo ============================================
echo  数据库初始化
echo ============================================
echo.

mysql -u%DB_USER% -p%DB_PASS% -e "SELECT 1;" >nul 2>&1
if errorlevel 1 (
    echo [ERROR] 数据库连接失败！
    echo.
    echo 可能原因:
    echo   1. MySQL 未安装或未启动（服务名通常为 MySQL80 / MySQL84）
    echo   2. root 密码不是 %DB_PASS%（请修改 bat 文件中的 DB_PASS 变量）
    echo   3. MySQL 未加入 PATH（运行 mysql --version 验证）
    echo.
    echo 后续步骤需要数据库支持，无法继续
    goto :fail
) else (
    echo [OK] 数据库连接成功
    mysql -u%DB_USER% -p%DB_PASS% -e "CREATE DATABASE IF NOT EXISTS %DB_NAME% DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_unicode_ci;" >nul 2>&1
    if exist "%SQL_FILE%" (
        mysql -u%DB_USER% -p%DB_PASS% %DB_NAME% < "%SQL_FILE%" >nul 2>&1
        if errorlevel 1 (
            echo [WARN] SQL 脚本执行有警告，可能表已存在
        ) else (
            echo [OK] 数据表初始化完成（6 张表 + 12 条阈值规则 + 4 个功能区）
        )
    ) else (
        echo [WARN] SQL 文件不存在: %SQL_FILE%
    )
)

echo.

REM ============================================
REM ccswitch 配置服务（P2 可选）
REM ============================================
echo ============================================
echo  ccswitch 配置服务（Port %CCSWITCH_PORT%）
echo ============================================
echo.

if "%CCSWITCH_SKIP%"=="1" (
    echo [SKIP] Python 不可用，跳过 ccswitch 服务
) else (
    cd /d "%CCSWITCH_DIR%"
    if exist requirements.txt (
        pip show flask >nul 2>&1
        if errorlevel 1 (
            echo [INFO] 安装 ccswitch Python 依赖...
            pip install -r requirements.txt -q >nul 2>&1
        )
        echo [OK] Python 依赖就绪
    )

    REM 清理端口占用
    for /f "tokens=5" %%p in ('netstat -ano ^| findstr ":%CCSWITCH_PORT%" ^| findstr "LISTENING"') do (
        echo [INFO] 清理端口 %CCSWITCH_PORT% 上的进程 %%p
        taskkill /F /PID %%p >nul 2>&1
    )
    timeout /t 1 /nobreak >nul

    start "Noise Ccswitch" cmd /k "cd /d %CCSWITCH_DIR% && python app.py"
    echo [OK] ccswitch 配置服务已启动
)
echo.

REM ============================================
REM 后端编译
REM ============================================
echo ============================================
echo  后端编译
echo ============================================
echo.

cd /d "%BACKEND_DIR%"
if exist target\classes (
    echo [OK] 已编译，跳过 Maven compile
) else (
    echo 正在编译 SpringBoot 后端（首次需 2-5 分钟）...
    call %MVN_CMD% clean compile -DskipTests > "%PROJECT_ROOT%\logs\backend-compile.log" 2>&1
    if errorlevel 1 (
        echo [ERROR] 编译失败，日志: logs\backend-compile.log
        type "%PROJECT_ROOT%\logs\backend-compile.log" | findstr "ERROR"
        goto :fail
    )
    echo [OK] 编译完成
)

echo.

REM ============================================
REM 启动后端
REM ============================================
echo ============================================
echo  启动后端服务（端口 %BACKEND_PORT%）
echo ============================================
echo.

cd /d "%BACKEND_DIR%"

REM 清理端口占用
for /f "tokens=5" %%p in ('netstat -ano ^| findstr ":%BACKEND_PORT%" ^| findstr "LISTENING"') do (
    echo [INFO] 清理端口 %BACKEND_PORT% 上的进程 %%p
    taskkill /F /PID %%p >nul 2>&1
)
timeout /t 2 /nobreak >nul

start "Noise Backend" cmd /k "cd /d %BACKEND_DIR% && %MVN_CMD% spring-boot:run"

echo 等待后端启动...
timeout /t 10 /nobreak >nul

set "BACKEND_OK=0"
for /l %%i in (1,1,36) do (
    curl -s http://localhost:%BACKEND_PORT%/api/auth/login -X POST -H "Content-Type: application/json" -d "{\"username\":\"_\",\"password\":\"_\"}" >nul 2>&1
    if not errorlevel 1 (
        set "BACKEND_OK=1"
        goto :backend_on
    )
    timeout /t 5 /nobreak >nul
    echo   等待中... %%i/36
)
echo [ERROR] 后端启动超时，请检查 "Noise Backend" 窗口
goto :fail

:backend_on
echo [OK] 后端已启动（http://localhost:%BACKEND_PORT%）
echo.

REM ============================================
REM 启动前端
REM ============================================
echo ============================================
echo  启动前端服务（端口 %FRONTEND_PORT%）
echo ============================================
echo.

cd /d "%FRONTEND_DIR%"

if not exist node_modules (
    echo 安装前端依赖（首次需 1-3 分钟）...
    call pnpm install > "%PROJECT_ROOT%\logs\frontend-install.log" 2>&1
    if errorlevel 1 (
        echo [ERROR] 依赖安装失败，日志: logs\frontend-install.log
        goto :fail
    )
    echo [OK] 依赖安装完成
) else (
    echo [OK] node_modules 已存在，跳过 pnpm install
)

REM 清理端口占用
for /f "tokens=5" %%p in ('netstat -ano ^| findstr ":%FRONTEND_PORT%" ^| findstr "LISTENING"') do (
    echo [INFO] 清理端口 %FRONTEND_PORT% 上的进程 %%p
    taskkill /F /PID %%p >nul 2>&1
)
timeout /t 2 /nobreak >nul

start "Noise Frontend" cmd /k "cd /d %FRONTEND_DIR% && pnpm dev"

echo 等待前端启动...
timeout /t 8 /nobreak >nul

set "FRONTEND_OK=0"
for /l %%i in (1,1,18) do (
    curl -s http://localhost:%FRONTEND_PORT%/ >nul 2>&1
    if not errorlevel 1 (
        set "FRONTEND_OK=1"
        goto :frontend_on
    )
    timeout /t 5 /nobreak >nul
    echo   等待中... %%i/18
)
echo [ERROR] 前端启动超时，请检查 "Noise Frontend" 窗口
goto :fail

:frontend_on
echo [OK] 前端已启动（http://localhost:%FRONTEND_PORT%）
echo.

REM ============================================
REM 打开浏览器
REM ============================================
timeout /t 2 /nobreak >nul
start "" "http://localhost:%FRONTEND_PORT%"

echo ============================================
echo  校园噪音分贝预警员系统 — 启动完成！
echo ============================================
echo.
echo 后端: http://localhost:%BACKEND_PORT%
echo 前端: http://localhost:%FRONTEND_PORT%
echo.
echo 测试账号:
echo   管理员: admin / admin123
echo.
echo 关闭此窗口不影响已启动的服务
echo.
pause
exit /b 0

:fail
echo.
echo ============================================
echo  启动失败，请检查上方错误信息
echo ============================================
echo.
echo  常见问题:
echo   1. MySQL 未启动 - 启动 MySQL84 服务
echo   2. 端口被占用 - 关闭占用程序后重试
echo   3. 依赖缺失 - 按提示安装 JDK/Node.js/pnpm/Maven
echo   4. 数据库密码错误 - 修改 bat 中 DB_PASS 变量
echo.
pause
exit /b 1