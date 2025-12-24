#!/bin/sh

# 定义目标文件路径
PWA_PATH="$HOME/.var/app/com.github.chrome.PWADesktopManage/data/chrome_pwa_desktop_manage/flet/app/pwa"

# --- 后台监控逻辑开始 ---
(
    # 循环尝试 30 次，每次间隔 0.5 秒，总共持续 15 秒（覆盖程序启动时间）
    for i in $(seq 1 30); do
        if [ -f "$PWA_PATH" ]; then
            # 一旦发现文件，立即通过宿主机执行加权
            flatpak-spawn --host chmod +x "$PWA_PATH" 2>/dev/null
            # 如果此时 pwa 已经被加上了执行权限，就可以退出监控了
            if [ -x "$PWA_PATH" ]; then
                break
            fi
        fi
        sleep 0.5
    done
) &
# --- 后台监控逻辑结束 ---

# 保持你原有的启动逻辑
export LD_LIBRARY_PATH=/app/lib:$LD_LIBRARY_PATH

# 启动主程序
cd /app
exec /app/chrome_pwa_desktop_manage "$@"

