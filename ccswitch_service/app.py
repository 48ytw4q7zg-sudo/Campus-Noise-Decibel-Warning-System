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
import statistics
import datetime

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

# 区域自适应配置（内存缓存 + area_config.json 持久化）
_area_adaptive_config_file = os.path.join(os.path.dirname(__file__), "area_config.json")
_area_adaptive_config = {
    "图书馆": {"windowSize": 15, "kValue": 2.0},
    "食堂": {"windowSize": 10, "kValue": 3.0},
    "操场": {"windowSize": 10, "kValue": 3.0},
    "宿舍": {"windowSize": 15, "kValue": 2.0},
}

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



def _load_area_config():
    """从 area_config.json 加载区域自适应配置到内存缓存。

    文件不存在时使用默认值并自动创建持久化文件。
    """
    global _area_adaptive_config, _area_adaptive_config_file
    if os.path.isfile(_area_adaptive_config_file):
        try:
            with open(_area_adaptive_config_file, 'r', encoding='utf-8') as f:
                loaded = json.load(f)
            if isinstance(loaded, dict) and loaded:
                _area_adaptive_config = loaded
                logger.info(f"已加载区域自适应配置: {len(loaded)} 个区域")
                return
        except (json.JSONDecodeError, OSError) as e:
            logger.warning(f"读取 area_config.json 失败，使用默认配置: {e}")
    # 文件不存在 → 持久化默认配置
    _save_area_config()


def _save_area_config():
    """将内存中的区域自适应配置持久化到 area_config.json。"""
    global _area_adaptive_config, _area_adaptive_config_file
    try:
        with open(_area_adaptive_config_file, 'w', encoding='utf-8') as f:
            json.dump(_area_adaptive_config, f, ensure_ascii=False, indent=2)
        logger.info(f"区域自适应配置已保存到 {_area_adaptive_config_file}")
    except OSError as e:
        logger.error(f"保存区域自适应配置失败: {e}")


def get_time_segment(time_point=None):
    """根据时间返回时段标签。

    映射规则（与 ThresholdServiceImpl.getCurrentTimeSegment 对齐）：
      07:30-08:00 → 早读
      08:00-12:00 → 上课
      12:00-14:00 → 午休
      14:00-18:00 → 上课
      18:00-22:00 → 活动/晚自修
      22:00-07:30 → 夜间静校

    time_point: datetime 对象，None 则用当前时间。
    """
    if time_point is None:
        time_point = datetime.datetime.now()
    t = time_point.hour * 60 + time_point.minute
    if 450 <= t < 480:
        return "早读"          # 07:30-08:00
    elif 480 <= t < 720:
        return "上课"          # 08:00-12:00
    elif 720 <= t < 840:
        return "午休"          # 12:00-14:00
    elif 840 <= t < 1080:
        return "上课"          # 14:00-18:00
    elif 1080 <= t < 1320:
        return "活动/晚自修"   # 18:00-22:00
    else:
        return "夜间静校"      # 22:00-07:30


def compute_adaptive(decibel_history, k_value=2.0):
    """统计自适应阈值计算：μ ± k×σ。

    Args:
        decibel_history: 近期分贝值列表（滑动窗口数据）
        k_value: 灵敏度系数（默认 2.0，图书馆/宿舍=2.0，食堂/操场=3.0）

    Returns:
        dict: {"mean", "stdDev", "upperLimit", "lowerLimit"} 或 None（数据不足）
    """
    if len(decibel_history) < 3:
        return None  # insufficient data
    mu = statistics.mean(decibel_history)
    sigma = statistics.stdev(decibel_history)
    upper = mu + k_value * sigma
    lower = max(0, mu - k_value * sigma)
    return {
        "mean": round(mu, 1),
        "stdDev": round(sigma, 1),
        "upperLimit": round(upper, 1),
        "lowerLimit": round(lower, 1),
    }


def _lookup_rule(location, time_segment):
    """从内存缓存的阈值规则中查找匹配 (location + time_segment) 的规则。

    返回匹配的规则 dict，未找到返回 None。
    """
    if _threshold_rules is None:
        return None
    rules = _threshold_rules.get("rules", [])
    for rule in rules:
        if rule.get("location") == location and rule.get("timeSegment") == time_segment:
            return rule
    # 模糊匹配：timeSegment 含 "/" 则拆分匹配任一部分
    for rule in rules:
        rl = rule.get("location")
        ts = rule.get("timeSegment", "")
        if rl == location:
            segments = ts.split("/")
            if time_segment in segments:
                return rule
            # 反向：当前时段含 "/" 时也尝试匹配规则中的单一标签
            current_segments = time_segment.split("/")
            for s in current_segments:
                if s in segments:
                    return rule
    return None


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


@app.route('/api/config/reload', methods=['GET', 'POST'])
def config_reload():
    """配置重载端点

    从 ccswitch settings.json 重新加载 API 配置，
    如果 ccswitch 不可用则回退到 .env。
    同时重新检查阈值规则，返回净化后的模型名。
    """
    success = reload_config()
    # 同时重新加载阈值规则
    _load_threshold_rules()
    model = Config.ANTHROPIC_MODEL  # 已净化
    if success:
        logger.info(f"配置已从 ccswitch 重新加载: model={model}, base_url={Config.ANTHROPIC_BASE_URL}")
        event_data = json.dumps({
            'event': 'config_reloaded',
            'timestamp': time.time(),
            'config_source': Config.CONFIG_SOURCE,
            'model': model,
            'base_url': Config.ANTHROPIC_BASE_URL,
            'is_proxy': Config.CCSWITCH_IS_PROXY,
        })
        _broadcast_sse(event_data)
        return jsonify({
            'success': True,
            'message': '配置已从 ccswitch 重新加载',
            'config_source': Config.CONFIG_SOURCE,
            'model': model,
            'base_url': Config.ANTHROPIC_BASE_URL,
            'raw_model': Config.CCSWITCH_RAW_MODEL,
            'rules_reloaded': _threshold_rules is not None,
        })
    else:
        logger.info("ccswitch 不可用，已回退到 .env 配置")
        return jsonify({
            'success': True,
            'message': 'ccswitch 不可用，已回退到 .env 配置',
            'config_source': Config.CONFIG_SOURCE,
            'model': model,
            'rules_reloaded': _threshold_rules is not None,
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


# ======================== 阈值计算引擎 API ========================


@app.route('/api/threshold/compute', methods=['POST'])
def threshold_compute():
    """单条阈值实时计算端点

    Input: { "location": "图书馆", "decibel": 52.3,
             "decibelHistory": [45.1, 48.2, 50.0, ...] }
    Process:
      1. 查当前时段的业务规则阈值（从 threshold_rules.json 内存缓存）
      2. 用 decibelHistory 计算统计自适应阈值（μ±k×σ）
      3. 应用混合模型判断 3 个触发条件
    Return: { ruleBasedThreshold, adaptiveUpper, adaptiveLower,
              isAbnormal, hybridMode, triggerReason }
    """
    global _area_adaptive_config
    body = request.get_json(silent=True)
    if not body:
        return jsonify({'success': False, 'message': '请求体不能为空'}), 400

    location = body.get('location', '').strip()
    decibel = body.get('decibel')
    decibel_history = body.get('decibelHistory', [])

    if not location:
        return jsonify({'success': False, 'message': 'location 不能为空'}), 400
    if decibel is None:
        return jsonify({'success': False, 'message': 'decibel 不能为空'}), 400

    # 1. 查找业务规则阈值
    current_segment = get_time_segment()
    rule = _lookup_rule(location, current_segment)
    if rule:
        rule_based_threshold = int(rule.get('thresholdValue', 55))
    else:
        rule_based_threshold = 55  # 兜底默认值

    # 2. 统计自适应阈值
    area_cfg = _area_adaptive_config.get(location,
        {"windowSize": 10, "kValue": 3.0})
    k_value = area_cfg.get("kValue", 3.0)
    adaptive_result = compute_adaptive(
        [float(d) for d in decibel_history], k_value
    )

    adaptive_upper = None
    adaptive_lower = None
    if adaptive_result:
        adaptive_upper = adaptive_result["upperLimit"]
        adaptive_lower = adaptive_result["lowerLimit"]

    # 3. 混合模型判断
    is_special = current_segment in ("午休", "夜间静校")
    decibel_jump = False
    is_abnormal = False
    hybrid_mode = "STAT_ADAPTIVE"
    trigger_reason = ""

    # 检查分贝骤升 ≥15dB
    if len(decibel_history) >= 2:
        recent_avg = sum(float(d) for d in decibel_history[-3:]) / min(len(decibel_history), 3)
        if float(decibel) - recent_avg >= 15:
            decibel_jump = True

    # 检查连续窗口异常率（从 decibelHistory 近似）
    abnormal_rate_triggered = False
    if len(decibel_history) >= 6:
        window_size = max(3, area_cfg.get("windowSize", 10) // 2)
        windows = []
        for w_start in range(0, len(decibel_history) - window_size + 1,
                             window_size):
            window = decibel_history[w_start:w_start + window_size]
            mu_w = statistics.mean([float(d) for d in window])
            sigma_w = statistics.stdev([float(d) for d in window]) if len(window) >= 2 else 0
            upper_w = mu_w + k_value * sigma_w
            abnormal_count = sum(1 for d in window if float(d) > upper_w)
            rate = abnormal_count / len(window)
            windows.append(rate > 0.10)
        # 检查最近 3 个窗口
        recent_windows = windows[-3:] if len(windows) >= 3 else windows
        if len(recent_windows) >= 3 and all(recent_windows):
            abnormal_rate_triggered = True

    # 决定混合模式
    if abnormal_rate_triggered or is_special or decibel_jump:
        hybrid_mode = "RULE_BASED"
        threshold_for_judge = rule_based_threshold
        reasons = []
        if abnormal_rate_triggered:
            reasons.append("连续3个窗口异常率>10%")
        if is_special:
            reasons.append(f"处于特殊时段（{current_segment}）")
        if decibel_jump:
            reasons.append("检测到分贝骤升>=15dB")
        trigger_reason = "；".join(reasons)
    else:
        # 正常模式：用自适应上限
        if adaptive_upper is not None:
            threshold_for_judge = adaptive_upper
        else:
            threshold_for_judge = rule_based_threshold

    is_abnormal = float(decibel) > threshold_for_judge

    return jsonify({
        'success': True,
        'ruleBasedThreshold': rule_based_threshold,
        'adaptiveUpper': adaptive_upper,
        'adaptiveLower': adaptive_lower,
        'isAbnormal': is_abnormal,
        'hybridMode': hybrid_mode,
        'triggerReason': trigger_reason,
        'currentTimeSegment': current_segment,
    })


@app.route('/api/threshold/batch-compute', methods=['POST'])
def threshold_batch_compute():
    """批量阈值计算端点

    Input: { "records": [{"location":"图书馆","decibel":52.3,
             "timePoint":"2026-06-11T14:30:00"}, ...] }
    Process: 按 location 分组 → 按 timePoint 排序 →
             对每条记录用其之前的记录构建滑动窗口 → 计算阈值
    Return: { "results": [{"id":..., "isAbnormal":true,
             "thresholdValue":40, "judgedByModel":"HYBRID"}, ...] }
    """
    global _area_adaptive_config
    body = request.get_json(silent=True)
    if not body:
        return jsonify({'success': False, 'message': '请求体不能为空'}), 400

    records = body.get('records', [])
    if not records:
        return jsonify({'success': False, 'message': 'records 不能为空'}), 400

    # 按 location 分组
    groups = {}
    for idx, rec in enumerate(records):
        loc = rec.get('location', '').strip()
        if not loc:
            continue
        groups.setdefault(loc, []).append((idx, rec))

    results = [None] * len(records)

    for location, group_records in groups.items():
        # 按 timePoint 排序
        group_records.sort(key=lambda x: x[1].get('timePoint', ''))

        # 获取该区域的配置
        area_cfg = _area_adaptive_config.get(location,
            {"windowSize": 10, "kValue": 3.0})
        window_size = area_cfg.get("windowSize", 10)
        k_value = area_cfg.get("kValue", 3.0)

        # 滑动窗口：对每条记录，取其之前（含自身）最多 window_size 条
        history = []  # 按时间排序的分贝值列表
        for orig_idx, rec in group_records:
            decibel = rec.get('decibel')
            time_point_str = rec.get('timePoint')

            if decibel is None:
                results[orig_idx] = {
                    'id': rec.get('id'),
                    'isAbnormal': False,
                    'thresholdValue': 0,
                    'judgedByModel': 'NONE',
                    'error': 'decibel is null',
                }
                continue

            # 解析时间点用于获取时段
            try:
                tp = datetime.datetime.fromisoformat(time_point_str) if time_point_str else None
            except (ValueError, TypeError):
                tp = None
            segment = get_time_segment(tp)

            # 查找业务规则阈值
            rule = _lookup_rule(location, segment)
            rule_based_threshold = int(rule.get('thresholdValue', 55)) if rule else 55

            # 统计自适应 (用当前滑动窗口)
            adaptive = compute_adaptive(history, k_value)
            adaptive_upper = adaptive["upperLimit"] if adaptive else None

            # 特殊时段检查
            is_special = segment in ("午休", "夜间静校")

            # 分贝骤升检查
            decibel_jump = False
            if len(history) >= 2:
                recent_avg = sum(history[-3:]) / min(len(history), 3)
                if float(decibel) - recent_avg >= 15:
                    decibel_jump = True

            # 决定判断模型
            if is_special or decibel_jump:
                judged_by = "RULE_BASED"
                threshold_val = rule_based_threshold
            elif adaptive_upper is not None:
                judged_by = "STAT_ADAPTIVE"
                threshold_val = adaptive_upper
            else:
                judged_by = "RULE_BASED"
                threshold_val = rule_based_threshold

            is_abnormal = float(decibel) > threshold_val

            results[orig_idx] = {
                'id': rec.get('id'),
                'isAbnormal': is_abnormal,
                'thresholdValue': threshold_val,
                'judgedByModel': judged_by,
            }

            # 将当前分贝值加入滑窗（保持窗口大小）
            history.append(float(decibel))
            if len(history) > window_size:
                history.pop(0)

    return jsonify({
        'success': True,
        'results': results,
    })


@app.route('/api/threshold/area-config', methods=['GET', 'PUT'])
def area_config():
    """区域自适应参数配置端点

    GET:  返回当前内存中的区域自适应参数。
    PUT:  更新内存配置并持久化到 area_config.json。
    """
    global _area_adaptive_config

    if request.method == 'GET':
        return jsonify({
            'success': True,
            'data': _area_adaptive_config,
        })

    if request.method == 'PUT':
        body = request.get_json(silent=True)
        if not body:
            return jsonify({
                'success': False,
                'message': '请求体不能为空',
            }), 400

        # 验证并更新
        valid_locations = {"图书馆", "食堂", "操场", "宿舍"}
        for loc, cfg in body.items():
            if loc not in valid_locations:
                return jsonify({
                    'success': False,
                    'message': f'功能区名称无效: {loc}，必须为：图书馆/食堂/操场/宿舍',
                }), 400
            ws = cfg.get('windowSize')
            kv = cfg.get('kValue')
            if not isinstance(ws, (int, float)) or ws < 5 or ws > 100:
                return jsonify({
                    'success': False,
                    'message': f'{loc} 的 windowSize 必须在 5-100 之间',
                }), 400
            if not isinstance(kv, (int, float)) or kv < 1.0 or kv > 5.0:
                return jsonify({
                    'success': False,
                    'message': f'{loc} 的 kValue 必须在 1.0-5.0 之间',
                }), 400

        # 更新内存
        for loc, cfg in body.items():
            _area_adaptive_config[loc] = {
                'windowSize': int(cfg.get('windowSize')),
                'kValue': float(cfg.get('kValue')),
            }

        # 持久化
        _save_area_config()

        return jsonify({
            'success': True,
            'message': '区域配置已更新',
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
            '/api/config/reload': '配置重载 (GET/POST)',
            '/api/threshold-rules': '获取阈值规则',
            '/api/threshold-rules/reload': '重新加载阈值规则 (POST)',
            '/api/threshold/compute': '单条阈值实时计算 (POST)',
            '/api/threshold/batch-compute': '批量阈值计算 (POST)',
            '/api/threshold/area-config': '区域自适应参数配置 (GET/PUT)',
            '/api/sse': 'SSE 实时推送 (GET)',
        },
    })


if __name__ == '__main__':
    logger.info(f"启动 ccswitch 配置服务: {Config.HOST}:{Config.PORT}")
    app.run(host=Config.HOST, port=Config.PORT, debug=Config.DEBUG)
