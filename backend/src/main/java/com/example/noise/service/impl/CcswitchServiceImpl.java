package com.example.noise.service.impl;

import com.example.noise.common.BusinessException;
import com.example.noise.service.CcswitchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ccswitch 配置服务集成实现 — P2
 * 通过 RestTemplate 调用 Flask ccswitch 服务（地址从 application.yml 读取，
 * 默认 http://localhost:5000），所有方法统一 try-catch RestClientException，
 * ccswitch 不可用时抛 BusinessException(7001)。
 */
@Service
public class CcswitchServiceImpl implements CcswitchService {

  private static final Logger log = LoggerFactory.getLogger(CcswitchServiceImpl.class);

  /** ccswitch Flask 服务基地址（application.yml 中配置，默认 http://localhost:5000） */
  @Value("${ccswitch.base-url:http://localhost:5000}")
  private String baseUrl;

  @Override
  public Map<String, Object> getStatus() {
    try {
      RestTemplate rest = new RestTemplate();
      String url = baseUrl + "/api/health";
      ResponseEntity<Map> response = rest.getForEntity(url, Map.class);
      return extractBody(response);
    } catch (RestClientException e) {
      log.warn("ccswitch 服务不可用（GET /api/health）: {}", e.getMessage());
      throw new BusinessException(7001, "ccswitch 服务不可用");
    }
  }

  @Override
  public Map<String, Object> reloadConfig() {
    try {
      RestTemplate rest = new RestTemplate();
      String url = baseUrl + "/api/config/reload";
      // POST 空请求体
      HttpHeaders headers = new HttpHeaders();
      HttpEntity<Void> entity = new HttpEntity<>(headers);
      ResponseEntity<Map> response = rest.exchange(url, HttpMethod.POST, entity, Map.class);
      return extractBody(response);
    } catch (RestClientException e) {
      log.warn("ccswitch 配置重载失败（POST /api/config/reload）: {}", e.getMessage());
      throw new BusinessException(7002, "配置重载失败");
    }
  }

  @Override
  public Map<String, Object> getThresholdRules() {
    try {
      RestTemplate rest = new RestTemplate();
      String url = baseUrl + "/api/threshold-rules";
      ResponseEntity<Map> response = rest.getForEntity(url, Map.class);
      return extractBody(response);
    } catch (RestClientException e) {
      log.warn("获取 ccswitch 阈值规则失败（GET /api/threshold-rules）: {}", e.getMessage());
      throw new BusinessException(7001, "ccswitch 服务不可用");
    }
  }

  @Override
  public Map<String, Object> reloadThresholdRules() {
    try {
      RestTemplate rest = new RestTemplate();
      String url = baseUrl + "/api/threshold-rules/reload";
      HttpHeaders headers = new HttpHeaders();
      HttpEntity<Void> entity = new HttpEntity<>(headers);
      ResponseEntity<Map> response = rest.exchange(url, HttpMethod.POST, entity, Map.class);
      return extractBody(response);
    } catch (RestClientException e) {
      log.warn("ccswitch 阈值规则重载失败（POST /api/threshold-rules/reload）: {}", e.getMessage());
      throw new BusinessException(7001, "ccswitch 服务不可用");
    }
  }

  @Override
  public Map<String, Object> computeThreshold(Map<String, Object> request) {
    try {
      RestTemplate rest = new RestTemplate();
      String url = baseUrl + "/api/threshold/compute";
      HttpHeaders headers = new HttpHeaders();
      HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
      ResponseEntity<Map> response = rest.exchange(url, HttpMethod.POST, entity, Map.class);
      return extractBody(response);
    } catch (RestClientException e) {
      log.warn("ccswitch 阈值计算失败（POST /api/threshold/compute）: {}", e.getMessage());
      throw new BusinessException(7001, "ccswitch 服务不可用");
    }
  }

  @Override
  public Map<String, Object> batchComputeThreshold(Map<String, Object> request) {
    try {
      RestTemplate rest = new RestTemplate();
      String url = baseUrl + "/api/threshold/batch-compute";
      HttpHeaders headers = new HttpHeaders();
      HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
      ResponseEntity<Map> response = rest.exchange(url, HttpMethod.POST, entity, Map.class);
      return extractBody(response);
    } catch (RestClientException e) {
      log.warn("ccswitch 批量阈值计算失败（POST /api/threshold/batch-compute）: {}", e.getMessage());
      throw new BusinessException(7001, "ccswitch 服务不可用");
    }
  }

  @Override
  public Map<String, Object> updateAreaConfig(Map<String, Object> config) {
    try {
      RestTemplate rest = new RestTemplate();
      String url = baseUrl + "/api/threshold/area-config";
      HttpHeaders headers = new HttpHeaders();
      HttpEntity<Map<String, Object>> entity = new HttpEntity<>(config, headers);
      ResponseEntity<Map> response = rest.exchange(url, HttpMethod.PUT, entity, Map.class);
      return extractBody(response);
    } catch (RestClientException e) {
      log.warn("ccswitch 更新区域自适应参数失败（PUT /api/threshold/area-config）: {}", e.getMessage());
      throw new BusinessException(7001, "ccswitch 服务不可用");
    }
  }

  @Override
  public Map<String, Object> getAreaConfig() {
    try {
      RestTemplate rest = new RestTemplate();
      String url = baseUrl + "/api/threshold/area-config";
      ResponseEntity<Map> response = rest.getForEntity(url, Map.class);
      return extractBody(response);
    } catch (RestClientException e) {
      log.warn("查询 ccswitch 区域自适应参数失败（GET /api/threshold/area-config）: {}", e.getMessage());
      throw new BusinessException(7001, "ccswitch 服务不可用");
    }
  }

  /**
   * 从 ResponseEntity 提取 Map 体，null 时返回空 Map。
   */
  @SuppressWarnings("rawtypes")
  private Map<String, Object> extractBody(ResponseEntity<Map> response) {
    if (response == null || response.getBody() == null) {
      return Collections.emptyMap();
    }
    // 保证返回有序 Map 便于调试
    return new LinkedHashMap<>(response.getBody());
  }
}
