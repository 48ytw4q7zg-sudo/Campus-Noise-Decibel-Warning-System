# -*- coding: utf-8 -*-
"""
校园噪音分贝预警员系统 — ccswitch 配置服务 v2026.6.11
基于 Flask 的配置管理服务，提供健康检查、配置重载、阈值规则管理。
新增：settings.json 文件监控 + SSE 实时推送配置变更。
作者：QXW
"""
from flask import Flask, request, jsonify, Response
from flask_cors import CORS
import json
import os
import time
import logging
import threading
from pathlib import Path
from queue import Queue

# 配置日志
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger('noise_ccswitch_service')

from config import Config, reload_config

level = getattr(logging, Config.LOG_LEVEL, logging.INFO)
logger.setLevel(level)

logger.info(f"配置来源: {Config.CONFIG_SOURCE}")
logger.info(f"AI 模型: {Config.ANTHROPIC_MODEL}, Base URL: {Config.ANTHROPIC_BASE_URL}")
if Config.CCSWITCH_RAW_MODEL and Config.CCSWITCH_RAW_MODEL != Config.ANTHROPIC_MODEL:
    logger.info(f"模型名已净化: '{Config.CCSWITCH_RAW_MODEL}' -> '{Config.ANTHROPIC_MODEL}'")

app = Flask(__name__)
CORS(app)

_SERVER_VERSION = "2026.6.11"
start_time = time.time()

# 阈值规则内存缓存
_threshold_rules = None
_threshold_rules_loaded_at = 0

# SSE 订阅者管理
_sse_subscribers = []  # list of Queue
_sse_lock = threading.Lock()
_settings_mtime = 0  # settings.json 最后修改时间


def _load_threshold_rules():
    """从 threshold_rules.json 加载阈值规则到内存缓存。

    返回 dict，加载失败返回 None。
    """
    global _threshold_rules, _threshold_rules_loaded_at
    rules_path = Config.THRESHOLD_RULES_FILE
    if not os.path.isfile(rules_path):
        logger.warning(f"阈值规则文件不存在: {rules_path}")
        _threshold_rules = None
        _threshold_rules_loaded_at = 0
        return None
    try:
        with open(rules_path, 'r', encoding='utf-8') as f:
            rules = json.load(f)
        _threshold_rules = rules
        _threshold_rules_loaded_at = time.time()
        logger.info(f"已加载阈值规则: {len(rules.get('rules', []))} 条规则, version={rules.get('version')}")
        return rules
    except (json.JSONDecodeError, OSError) as e:
        logger.error(f"加载阈值规则失败: {e}")
        _threshold_rules = None
        _threshold_rules_loaded_at = 0
        return None


# 启动时首次加载阈值规则
_load_threshold_rules()


def _find_settings_path():
    """查找 settings.json 路径"""
    for name in ("settings.json", "settings.local.json"):
        p = Path.home() / ".claude" / name
        if p.is_file():
            return p
    return None


def _settings_watcher_loop():
    """后台线程：每 5 秒检查 settings.json 修改时间，变更时自动重载并推送 SSE"""
    global _settings_mtime
    while True:
        try:
            time.sleep(5)
            settings_path = _find_settings_path()
            if not settings_path:
                continue
            current_mtime = settings_path.stat().st_mtime
            if _settings_mtime == 0:
                _settings_mtime = current_mtime
                continue
            if current_mtime != _settings_mtime:
                _settings_mtime = current_mtime
                logger.info("检测到 settings.json 变更，自动重载配置...")
                success = reload_config()
                event_data = json.dumps({
                    'event': 'config_changed',
                    'timestamp': time.time(),
                    'config_source': Config.CONFIG_SOURCE,
                    'model': Config.ANTHROPIC_MODEL,
                    'base_url': Config.ANTHROPIC_BASE_URL,
                    'is_proxy': Config.CCSWITCH_IS_PROXY,
                })
                _broadcast_sse(event_data)
                logger.info("SSE 推送已发送: config_changed")
        except Exception as e:
            logger.error(f"settings.json 监控异常: {e}")


def _broadcast_sse(data: str):
    """向所有 SSE 订阅者广播事件"""
    with _sse_lock:
        dead = []
        for q in _sse_subscribers:
            try:
                q.put_nowait(data)
            except Exception:
                dead.append(q)
        for q in dead:
            _sse_subscribers.remove(q)


# 启动后台文件监控线程
_watcher_thread = threading.Thread(target=_settings_watcher_loop, daemon=True)
_watcher_thread.start()


def _get_uptime_seconds() -> float:
    """获取服务运行秒数"""
    return time.time() - start_time


@app.route('/api/health', methods=['GET'])
def health_check():
    """健康检查端点

    返回服务状态、模型配置、运行时长等关键信息。
    """
    uptime_seconds = _get_uptime_seconds()
    result = {
        'status': 'ok',
        'message': '校园噪音分贝预警员系统 — ccswitch 配置服务运行正常',
        'service': 'noise-ccswitch-service',
        'version': _SERVER_VERSION,
        'config_source': Config.CONFIG_SOURCE,
        'model': Config.ANTHROPIC_MODEL,
        'base_url': Config.ANTHROPIC_BASE_URL,
        'uptime_seconds': round(uptime_seconds, 2),
        'threshold_rules_loaded': _threshold_rules is not None,
        'watcher_active': True,
    }
    if _threshold_rules:
        result['threshold_rules'] = {
            'rule_count': len(_threshold_rules.get('rules', [])),
            'version': _threshold_rules.get('version'),
            'updated_at': _threshold_rules.get('updatedAt'),
        }
    if Config.CONFIG_SOURCE == 'ccswitch':
        result['ccswitch'] = {
            'raw_model': Config.CCSWITCH_RAW_MODEL,
            'is_proxy': Config.CCSWITCH_IS_PROXY,
            'model_sanitized': Config.CCSWITCH_RAW_MODEL != Config.ANTHROPIC_MODEL,
        }
        result['config_keys'] = list(Config.EXTRA_ENV.keys()) if Config.EXTRA_ENV else []
    return jsonify(result)


@app.route('/api/config/reload', methods=['POST'])
def config_reload():
    """配置重载端点

    从 ccswitch settings.json 重新加载 API 配置，
    如果 ccswitch 不可用则回退到 .env。
    """
    success = reload_config()
    if success:
        logger.info(f"配置已从 ccswitch 重新加载: model={Config.ANTHROPIC_MODEL}, base_url={Config.ANTHROPIC_BASE_URL}")
        # 推送 SSE 事件
        event_data = json.dumps({
            'event': 'config_reloaded',
            'timestamp': time.time(),
            'config_source': Config.CONFIG_SOURCE,
            'model': Config.ANTHROPIC_MODEL,
            'base_url': Config.ANTHROPIC_BASE_URL,
            'is_proxy': Config.CCSWITCH_IS_PROXY,
        })
        _broadcast_sse(event_data)
        return jsonify({
            'success': True,
            'message': '配置已从 ccswitch 重新加载',
            'config_source': Config.CONFIG_SOURCE,
            'model': Config.ANTHROPIC_MODEL,
            'base_url': Config.ANTHROPIC_BASE_URL,
            'raw_model': Config.CCSWITCH_RAW_MODEL,
        })
    else:
        logger.info("ccswitch 不可用，已回退到 .env 配置")
        return jsonify({
            'success': True,
            'message': 'ccswitch 不可用，已回退到 .env 配置',
            'config_source': Config.CONFIG_SOURCE,
            'model': Config.ANTHROPIC_MODEL,
        })


@app.route('/api/threshold-rules', methods=['GET'])
def get_threshold_rules():
    """获取阈值规则列表

    返回当前加载的阈值规则 JSON。
    """
    if _threshold_rules is None:
        return jsonify({
            'success': False,
            'message': '阈值规则未加载，请检查 threshold_rules.json 文件',
        }), 404
    return jsonify({
        'success': True,
        'data': _threshold_rules,
        'loaded_at': _threshold_rules_loaded_at,
    })


@app.route('/api/threshold-rules/reload', methods=['POST'])
def reload_threshold_rules():
    """重新加载阈值规则

    从 threshold_rules.json 重新读取并更新内存缓存。
    """
    rules = _load_threshold_rules()
    if rules is None:
        return jsonify({
            'success': False,
            'message': '无法加载阈值规则文件',
        }), 500
    return jsonify({
        'success': True,
        'message': f'阈值规则已重新加载 ({len(rules.get("rules", []))} 条)',
        'data': rules,
        'loaded_at': _threshold_rules_loaded_at,
    })


@app.route('/api/sse', methods=['GET'])
def sse_stream():
    """SSE (Server-Sent Events) 端点

    实时推送配置变更事件，前端 EventSource 连接。
    事件类型：config_changed（自动检测到 settings.json 变更）
             config_reloaded（手动 POST /api/config/reload 触发）
    """
    q = Queue(maxsize=50)
    with _sse_lock:
        _sse_subscribers.append(q)

    def generate():
        # 发送初始连接事件
        yield 'data: {"event":"connected","timestamp":%d}\n\n' % time.time()
        try:
            while True:
                try:
                    data = q.get(timeout=30)  # 30s heartbeat
                    yield 'data: %s\n\n' % data
                except Exception:
                    # 超时，发心跳
                    yield ': heartbeat\n\n'
        except GeneratorExit:
            pass
        finally:
            with _sse_lock:
                if q in _sse_subscribers:
                    _sse_subscribers.remove(q)

    return Response(generate(), mimetype='text/event-stream',
                    headers={
                        'Cache-Control': 'no-cache',
                        'X-Accel-Buffering': 'no',
                        'Connection': 'keep-alive',
                    })


@app.route('/', methods=['GET'])
def index():
    """根路径 — 返回服务简要状态"""
    uptime_seconds = _get_uptime_seconds()
    return jsonify({
        'service': '校园噪音分贝预警员系统 — ccswitch 配置服务',
        'version': _SERVER_VERSION,
        'status': 'running',
        'config_source': Config.CONFIG_SOURCE,
        'model': Config.ANTHROPIC_MODEL,
        'uptime_seconds': round(uptime_seconds, 2),
        'watcher_active': True,
        'endpoints': {
            '/api/health': '健康检查',
            '/api/config/reload': '配置重载 (POST)',
            '/api/threshold-rules': '获取阈值规则',
            '/api/threshold-rules/reload': '重新加载阈值规则 (POST)',
            '/api/sse': 'SSE 实时推送 (GET)',
        },
    })


if __name__ == '__main__':
    logger.info(f"启动 ccswitch 配置服务: {Config.HOST}:{Config.PORT}")
    app.run(host=Config.HOST, port=Config.PORT, debug=Config.DEBUG)
