package com.example.noise.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.noise.common.BusinessException;
import com.example.noise.common.Result;
import com.example.noise.entity.NoiseRecord;
import com.example.noise.entity.dto.NoiseRecordBatchDTO;
import com.example.noise.entity.dto.NoiseRecordRequest;
import com.example.noise.mapper.NoiseRecordMapper;
import com.example.noise.service.NoiseRecordService;
import com.example.noise.util.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
public class NoiseController {

  private final NoiseRecordService noiseRecordService;
  private final NoiseRecordMapper noiseRecordMapper;

  public NoiseController(NoiseRecordService noiseRecordService,
                         NoiseRecordMapper noiseRecordMapper) {
    this.noiseRecordService = noiseRecordService;
    this.noiseRecordMapper = noiseRecordMapper;
  }

  /** 传感器上报 / 管理员手动录入，双通道识别 */
  @PostMapping("/api/noise/records")
  public Result<Map<String, Long>> createRecord(@Valid @RequestBody NoiseRecordRequest req,
                                                 HttpServletRequest request) {
    // 双通道识别：从 Authorization Header 判断
    String authHeader = request.getHeader("Authorization");
    boolean isSensor = true;

    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      try {
        String token = authHeader.substring(7);
        Claims claims = JwtUtils.parseToken(token);
        String role = (String) claims.get("role");
        if ("管理员".equals(role)) {
          isSensor = false; // 管理员通道
        }
      } catch (Exception e) {
        // token 无效，降级为传感器通道
      }
    }

    Long id = noiseRecordService.createRecord(req, isSensor);
    Map<String, Long> data = new HashMap<>();
    data.put("id", id);
    return Result.success(data, "数据记录成功");
  }

  /** 批量导入噪声数据（仅管理员） */
  @PostMapping("/api/noise/records/batch")
  public Result<Map<String, Integer>> batchCreate(@Valid @RequestBody NoiseRecordBatchDTO batch,
                                                   HttpServletRequest request) {
    checkAdmin(request);
    Map<String, Integer> result = noiseRecordService.batchCreate(batch);
    return Result.success(result, "批量导入完成");
  }

  /** 各功能区最新一条噪声数据（仪表盘用） */
  @GetMapping("/api/noise/records/latest")
  public Result<List<?>> getLatestPerArea() {
    List<?> data = noiseRecordService.getLatestPerArea();
    return Result.success(data);
  }

  /** 噪声记录分页列表，支持多条件筛选 */
  @GetMapping("/api/noise/records")
  public Result<Map<String, Object>> queryPage(@RequestParam(defaultValue = "1") Integer pageNum,
                                                @RequestParam(defaultValue = "20") Integer pageSize,
                                                @RequestParam(required = false) String location,
                                                @RequestParam(required = false) String dateFrom,
                                                @RequestParam(required = false) String dateTo,
                                                @RequestParam(required = false) Double minDb,
                                                @RequestParam(required = false) Double maxDb,
                                                @RequestParam(required = false) Integer isAbnormal,
                                                @RequestParam(defaultValue = "time_point") String sortBy,
                                                @RequestParam(defaultValue = "desc") String sortOrder) {
    IPage<NoiseRecord> page = new Page<>(pageNum, pageSize);
    IPage<NoiseRecord> result = noiseRecordService.queryPage(
        page, location, dateFrom, dateTo, minDb, maxDb, isAbnormal, sortBy, sortOrder);

    Map<String, Object> data = new HashMap<>();
    data.put("records", result.getRecords());
    data.put("total", result.getTotal());
    data.put("pageNum", result.getCurrent());
    data.put("pageSize", result.getSize());
    data.put("pages", result.getPages());
    return Result.success(data);
  }

  /** 噪声记录详情 */
  @GetMapping("/api/noise/records/{id}")
  public Result<NoiseRecord> getDetail(@PathVariable Long id) {
    NoiseRecord record = noiseRecordService.getDetail(id);
    return Result.success(record);
  }

  /** 高级筛选噪声记录（P1-5 扩展：支持 keyword + noiseType） */
  @GetMapping("/api/noise/records/search")
  public Result<Map<String, Object>> searchAdvanced(
      @RequestParam(defaultValue = "1") Integer pageNum,
      @RequestParam(defaultValue = "20") Integer pageSize,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String noiseType,
      @RequestParam(required = false) String location,
      @RequestParam(required = false) String dateFrom,
      @RequestParam(required = false) String dateTo,
      @RequestParam(required = false) Double minDb,
      @RequestParam(required = false) Double maxDb,
      @RequestParam(required = false) Integer isAbnormal,
      @RequestParam(defaultValue = "time_point") String sortBy,
      @RequestParam(defaultValue = "desc") String sortOrder) {
    IPage<NoiseRecord> page = new Page<>(pageNum, pageSize);
    IPage<NoiseRecord> result = noiseRecordService.searchAdvanced(
        page, keyword, noiseType, location, dateFrom, dateTo,
        minDb, maxDb, isAbnormal, sortBy, sortOrder);

    Map<String, Object> data = new HashMap<>();
    data.put("records", result.getRecords());
    data.put("total", result.getTotal());
    data.put("pageNum", result.getCurrent());
    data.put("pageSize", result.getSize());
    data.put("pages", result.getPages());
    return Result.success(data);
  }

  /** CSV 导出噪声记录（管理员专属，上限 10000 条） */
  @GetMapping("/api/noise/records/export")
  public void exportRecords(@RequestParam(required = false) String location,
                              @RequestParam(required = false) String dateFrom,
                              @RequestParam(required = false) String dateTo,
                              @RequestParam(required = false) Double minDb,
                              @RequestParam(required = false) Double maxDb,
                              @RequestParam(required = false) Integer isAbnormal,
                              HttpServletRequest request,
                              HttpServletResponse response) throws IOException {
    checkAdmin(request);

    List<NoiseRecord> records = noiseRecordService.exportRecords(
        location, dateFrom, dateTo, minDb, maxDb, isAbnormal);

    // 设置响应头
    response.setContentType("text/csv;charset=UTF-8");
    String filename = "noise_export_" + java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";
    response.setHeader("Content-Disposition", "attachment; filename=" + filename);

    // 写入 UTF-8 BOM（让 Excel 正确识别中文）
    response.getOutputStream().write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});

    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // 写 CSV 内容
    try (PrintWriter writer = new PrintWriter(response.getOutputStream())) {
      // 表头
      writer.println("id,location,decibel,timePoint,deviceId,isAbnormal,judgedByModel,noiseType,noiseDuration,createTime");

      // 数据行
      for (NoiseRecord r : records) {
        writer.printf("%d,%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
            r.getId(),
            csvEscape(r.getLocation()),
            r.getDecibel() != null ? r.getDecibel().toPlainString() : "",
            r.getTimePoint() != null ? r.getTimePoint().format(dtf) : "",
            csvEscape(r.getDeviceId()),
            r.getIsAbnormal() != null ? String.valueOf(r.getIsAbnormal()) : "",
            csvEscape(r.getJudgedByModel()),
            csvEscape(r.getNoiseType()),
            r.getNoiseDuration() != null ? String.valueOf(r.getNoiseDuration()) : "",
            r.getCreateTime() != null ? r.getCreateTime().format(dtf) : "");
      }
      writer.flush();
    }
  }

  /** 导入 CSV/Excel 噪声数据（管理员） */
  @PostMapping("/api/data/import")
  public Result<Map<String, Object>> importData(@RequestParam("file") MultipartFile file,
                                                 HttpServletRequest request) throws IOException {
    checkAdmin(request);

    // 1. 校验文件类型
    String originalFilename = file.getOriginalFilename();
    if (originalFilename == null || originalFilename.isEmpty()) {
      throw new BusinessException(5002, "文件名为空");
    }
    String lowerName = originalFilename.toLowerCase();
    if (!lowerName.endsWith(".csv") && !lowerName.endsWith(".xlsx") && !lowerName.endsWith(".xls")) {
      throw new BusinessException(5002, "不支持的文件类型，仅支持 .csv / .xlsx / .xls");
    }

    // 2. 校验文件大小（10MB = 10 * 1024 * 1024 bytes）
    if (file.getSize() > 10 * 1024 * 1024) {
      throw new BusinessException(5002, "文件大小超过 10MB 限制");
    }

    // 3. 解析 CSV（纯 Java 解析：BufferedReader + 逗号分割）
    // 对于 .xlsx/.xls，P2 简化处理：提示建议转 CSV
    if (lowerName.endsWith(".xlsx") || lowerName.endsWith(".xls")) {
      throw new BusinessException(5002, "Excel 格式暂不支持，请导出为 CSV 后导入");
    }

    List<Map<String, String>> errors = new ArrayList<>();
    int successCount = 0;
    int failCount = 0;
    int skippedCount = 0;

    // R-08-3: UUID 重命名上传文件，防止路径穿越/文件名注入
    String safeFilename = UUID.randomUUID().toString() + ".csv";

    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

      String headerLine = reader.readLine();
      if (headerLine == null || headerLine.isEmpty()) {
        throw new BusinessException(5002, "CSV 文件为空");
      }

      // 解析表头（允许带 BOM）
      String cleanedHeader = headerLine.replace("﻿", "").trim();
      String[] headers = cleanedHeader.split(",");
      // 查找列索引: location, decibel, timePoint, noiseType, noiseDuration
      int locIdx = -1, dbIdx = -1, timeIdx = -1, ntypeIdx = -1, ndurationIdx = -1;
      for (int i = 0; i < headers.length; i++) {
        String h = headers[i].trim().toLowerCase();
        switch (h) {
          case "location" -> locIdx = i;
          case "decibel" -> dbIdx = i;
          case "timepoint" -> timeIdx = i;
          case "noisetype" -> ntypeIdx = i;
          case "noiseduration" -> ndurationIdx = i;
        }
      }

      if (locIdx == -1 || dbIdx == -1) {
        throw new BusinessException(5002, "CSV 必须包含 location 和 decibel 列");
      }

      String line;
      int rowNum = 1; // 表头算第 1 行，数据从第 2 行开始

      Set<String> validLocations = Set.of("图书馆", "食堂", "操场", "宿舍");

      while ((line = reader.readLine()) != null) {
        rowNum++;
        // 跳过空行和注释行（# 开头）
        String trimmedLine = line.trim();
        if (trimmedLine.isEmpty() || trimmedLine.startsWith("#")) {
          continue;
        }

        try {
          String[] cols = parseCsvLine(line);
          if (cols.length == 0) {
            continue;
          }

          String location = cols.length > locIdx ? cols[locIdx].trim() : "";
          String decibelStr = cols.length > dbIdx ? cols[dbIdx].trim() : "";

          // 校验 location
          if (location.isEmpty()) {
            errors.add(Map.of("row", String.valueOf(rowNum), "field", "location",
                "message", "功能区不能为空"));
            failCount++;
            continue;
          }
          if (!validLocations.contains(location)) {
            errors.add(Map.of("row", String.valueOf(rowNum), "field", "location",
                "message", "功能区不在合法范围（图书馆/食堂/操场/宿舍）"));
            failCount++;
            continue;
          }

          // 校验 decibel
          if (decibelStr.isEmpty()) {
            errors.add(Map.of("row", String.valueOf(rowNum), "field", "decibel",
                "message", "分贝值不能为空"));
            failCount++;
            continue;
          }

          BigDecimal decibel;
          try {
            decibel = new BigDecimal(decibelStr);
          } catch (NumberFormatException e) {
            errors.add(Map.of("row", String.valueOf(rowNum), "field", "decibel",
                "message", "分贝值格式不正确"));
            failCount++;
            continue;
          }

          if (decibel.compareTo(new BigDecimal("20.0")) < 0
              || decibel.compareTo(new BigDecimal("120.0")) > 0) {
            errors.add(Map.of("row", String.valueOf(rowNum), "field", "decibel",
                "message", "分贝值不在合法范围内（20.0-120.0）"));
            failCount++;
            continue;
          }

          // 解析 timePoint（可选，默认为当前时间）
          LocalDateTime timePoint = LocalDateTime.now();
          if (timeIdx != -1 && cols.length > timeIdx) {
            String timeStr = cols[timeIdx].trim();
            if (!timeStr.isEmpty()) {
              try {
                timePoint = LocalDateTime.parse(timeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
              } catch (Exception e) {
                // 尝试 ISO 格式
                try {
                  timePoint = LocalDateTime.parse(timeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                } catch (Exception e2) {
                  errors.add(Map.of("row", String.valueOf(rowNum), "field", "timePoint",
                      "message", "时间格式不正确，应为 yyyy-MM-dd HH:mm:ss"));
                  failCount++;
                  continue;
                }
              }
            }
          }

          // R-08-3: 构建设备 ID，使用 safeFilename 代替原始文件名
          String deviceId = "IMPORT_" + safeFilename.replaceAll("[^a-zA-Z0-9_\\-.]", "_") + "_" + rowNum;

          // 插入记录
          NoiseRecord record = new NoiseRecord();
          record.setLocation(location);
          record.setDecibel(decibel);
          record.setTimePoint(timePoint);
          record.setDeviceId(deviceId);
          record.setIsAbnormal(null); // 未判断，等后续定时判断
          record.setJudgedByModel("RULE_BASED");

          // 可选字段：noiseType
          if (ntypeIdx != -1 && cols.length > ntypeIdx) {
            String noiseType = cols[ntypeIdx].trim();
            if (!noiseType.isEmpty()) {
              record.setNoiseType(noiseType);
            }
          }

          // 可选字段：noiseDuration
          if (ndurationIdx != -1 && cols.length > ndurationIdx) {
            String durStr = cols[ndurationIdx].trim();
            if (!durStr.isEmpty()) {
              try {
                record.setNoiseDuration(Integer.parseInt(durStr));
              } catch (NumberFormatException e) {
                // 忽略非法的 duration，不阻断导入
              }
            }
          }

          // R-08-2: 去重检查 — 同一 (location, timePoint, decibel, deviceId) 已有记录则跳过
          LambdaQueryWrapper<NoiseRecord> dedupWrapper = new LambdaQueryWrapper<>();
          dedupWrapper.eq(NoiseRecord::getLocation, location)
                      .eq(NoiseRecord::getTimePoint, timePoint)
                      .eq(NoiseRecord::getDecibel, decibel)
                      .eq(NoiseRecord::getDeviceId, deviceId);
          Long existingCount = noiseRecordMapper.selectCount(dedupWrapper);
          if (existingCount != null && existingCount > 0) {
            skippedCount++;
            continue;
          }

          noiseRecordMapper.insert(record);
          successCount++;

        } catch (Exception e) {
          errors.add(Map.of("row", String.valueOf(rowNum), "field", "",
              "message", "行解析异常: " + e.getMessage()));
          failCount++;
        }
      }
    }

    Map<String, Object> resultMap = new HashMap<>();
    resultMap.put("successCount", successCount);
    resultMap.put("failCount", failCount);
    resultMap.put("skippedCount", skippedCount);
    resultMap.put("errors", errors);
    return Result.success(resultMap, "导入完成");
  }

  /** 导出完整报表（管理员，CSV 格式） */
  @GetMapping("/api/data/export-report")
  public void exportReport(@RequestParam(required = false) String location,
                            @RequestParam(required = false) String dateFrom,
                            @RequestParam(required = false) String dateTo,
                            @RequestParam(required = false) Double minDb,
                            @RequestParam(required = false) Double maxDb,
                            @RequestParam(required = false) Integer isAbnormal,
                            HttpServletRequest request,
                            HttpServletResponse response) throws IOException {
    checkAdmin(request);

    // 查噪声记录
    List<NoiseRecord> records = noiseRecordService.exportRecords(
        location, dateFrom, dateTo, minDb, maxDb, isAbnormal);

    // 统计汇总：按功能区分组
    Map<String, Long> areaCount = new HashMap<>();
    Map<String, BigDecimal> areaSumDb = new HashMap<>();
    Map<String, Long> areaAbnormal = new HashMap<>();

    long totalAlerts = 0;

    for (NoiseRecord r : records) {
      String loc = r.getLocation();
      areaCount.merge(loc, 1L, Long::sum);
      if (r.getDecibel() != null) {
        areaSumDb.merge(loc, r.getDecibel(), BigDecimal::add);
      }
      if (r.getIsAbnormal() != null && r.getIsAbnormal() == 1) {
        areaAbnormal.merge(loc, 1L, Long::sum);
      }
    }

    // 设置响应头
    response.setContentType("text/csv;charset=UTF-8");
    String filename = "noise_report_"
        + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
        + ".csv";
    response.setHeader("Content-Disposition", "attachment; filename=" + filename);

    // 写入 UTF-8 BOM
    response.getOutputStream().write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});

    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    try (PrintWriter writer = new PrintWriter(response.getOutputStream())) {
      // ======== 第一部分：筛选结果列表 ========
      writer.println("=== 噪声记录数据 ===");
      writer.println("id,location,decibel,timePoint,deviceId,isAbnormal,judgedByModel,noiseType,noiseDuration,createTime");

      for (NoiseRecord r : records) {
        writer.printf("%d,%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
            r.getId(),
            csvEscape(r.getLocation()),
            r.getDecibel() != null ? r.getDecibel().toPlainString() : "",
            r.getTimePoint() != null ? r.getTimePoint().format(dtf) : "",
            csvEscape(r.getDeviceId()),
            r.getIsAbnormal() != null ? String.valueOf(r.getIsAbnormal()) : "",
            csvEscape(r.getJudgedByModel()),
            csvEscape(r.getNoiseType()),
            r.getNoiseDuration() != null ? String.valueOf(r.getNoiseDuration()) : "",
            r.getCreateTime() != null ? r.getCreateTime().format(dtf) : "");
      }

      writer.println();

      // ======== 第二部分：功能区统计汇总 ========
      writer.println("=== 功能区统计汇总 ===");
      writer.println("功能区,记录数,平均分贝,异常记录数");

      for (String area : List.of("图书馆", "食堂", "操场", "宿舍")) {
        long count = areaCount.getOrDefault(area, 0L);
        BigDecimal sum = areaSumDb.getOrDefault(area, BigDecimal.ZERO);
        String avg = count > 0
            ? sum.divide(BigDecimal.valueOf(count), 1, RoundingMode.HALF_UP).toPlainString()
            : "0.0";
        long abnormal = areaAbnormal.getOrDefault(area, 0L);
        writer.printf("%s,%d,%s,%d%n", area, count, avg, abnormal);
      }

      writer.println();

      // ======== 第三部分：告警统计 ========
      writer.println("=== 告警统计 ===");
      writer.println("功能区,告警次数");

      // 统计 alert_log 中各功能区告警数
      // 这里用噪声 mapper 的 selectAlertCountByArea 方法
      List<Map<String, Object>> alertByArea =
          noiseRecordMapper.selectAlertCountByArea(dateFrom, dateTo);

      if (alertByArea != null && !alertByArea.isEmpty()) {
        for (Map<String, Object> al : alertByArea) {
          writer.printf("%s,%s%n", al.get("location"), al.get("alertCount"));
          totalAlerts += ((Number) al.get("alertCount")).longValue();
        }
      } else {
        writer.println("无告警数据");
      }
      writer.println();
      writer.printf("告警总数,%d%n", totalAlerts);

      writer.flush();
    }
  }

  /** 管理员权限校验 */
  private void checkAdmin(HttpServletRequest request) {
    String role = (String) request.getAttribute("role");
    if (!"管理员".equals(role)) {
      throw new com.example.noise.common.BusinessException(403, "越权访问");
    }
  }

  /** CSV 行解析：处理引号内逗号场景（简化版，同 csvEscape 对齐） */
  private String[] parseCsvLine(String line) {
    if (line == null || line.isEmpty()) {
      return new String[0];
    }

    List<String> fields = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    boolean inQuotes = false;

    for (int i = 0; i < line.length(); i++) {
      char c = line.charAt(i);
      if (inQuotes) {
        if (c == '"') {
          // 双引号转义：检查下一个字符是否也是引号
          if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
            current.append('"');
            i++; // 跳过转义引号
          } else {
            inQuotes = false; // 引号结束
          }
        } else {
          current.append(c);
        }
      } else {
        if (c == '"') {
          inQuotes = true;
        } else if (c == ',') {
          fields.add(current.toString());
          current.setLength(0);
        } else {
          current.append(c);
        }
      }
    }
    fields.add(current.toString());

    return fields.toArray(new String[0]);
  }

  /** CSV 字段转义：如果包含逗号、引号或换行，用双引号包裹并转义内部引号 */
  private String csvEscape(String value) {
    if (value == null) {
      return "";
    }
    // CSV 公式注入防护: 以 = @ + - 开头的值加单引号前缀
    if (value.startsWith("=") || value.startsWith("@") || value.startsWith("+") || value.startsWith("-")) {
      value = "'" + value;
    }
    if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
      return "\"" + value.replace("\"", "\"\"") + "\"";
    }
    return value;
  }
}
