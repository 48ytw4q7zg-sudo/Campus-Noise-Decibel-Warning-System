package com.example.noise.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.noise.entity.NoiseRecord;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 噪声记录 Mapper — 继承 MyBatis-Plus BaseMapper，自动获得 CRUD 方法
 */
public interface NoiseRecordMapper extends BaseMapper<NoiseRecord> {

  /**
   * 按功能区分组统计：记录总数、平均分贝值、异常数
   * 时间范围可空（为空时不过滤时间）
   */
  @Select("<script>"
      + "SELECT location, COUNT(*) AS totalRecords, AVG(decibel) AS avgDecibel, "
      + "SUM(CASE WHEN is_abnormal = 1 THEN 1 ELSE 0 END) AS abnormalCount "
      + "FROM noise_record WHERE 1=1 "
      + "<if test='dateFrom != null and dateFrom != \"\"'> AND time_point &gt;= #{dateFrom} </if>"
      + "<if test='dateTo != null and dateTo != \"\"'> AND time_point &lt;= #{dateTo} </if>"
      + " GROUP BY location"
      + "</script>")
  List<Map<String, Object>> selectAreaStats(@Param("dateFrom") String dateFrom,
                                            @Param("dateTo") String dateTo);

  /**
   * 按功能区查询各区域的告警次数
   */
  @Select("<script>"
      + "SELECT location, COUNT(*) AS alertCount "
      + "FROM alert_log WHERE 1=1 "
      + "<if test='dateFrom != null and dateFrom != \"\"'> AND create_time &gt;= #{dateFrom} </if>"
      + "<if test='dateTo != null and dateTo != \"\"'> AND create_time &lt;= #{dateTo} </if>"
      + " GROUP BY location"
      + "</script>")
  List<Map<String, Object>> selectAlertCountByArea(@Param("dateFrom") String dateFrom,
                                                    @Param("dateTo") String dateTo);

  /**
   * 全局汇总：总记录数、平均分贝值、总告警数
   */
  @Select("<script>"
      + "SELECT COUNT(*) AS totalRecords, COALESCE(AVG(decibel), 0) AS avgDecibel "
      + "FROM noise_record WHERE 1=1 "
      + "<if test='dateFrom != null and dateFrom != \"\"'> AND time_point &gt;= #{dateFrom} </if>"
      + "<if test='dateTo != null and dateTo != \"\"'> AND time_point &lt;= #{dateTo} </if>"
      + "</script>")
  Map<String, Object> selectGlobalSummary(@Param("dateFrom") String dateFrom,
                                          @Param("dateTo") String dateTo);

  /**
   * 全局告警总数
   */
  @Select("<script>"
      + "SELECT COUNT(*) AS totalAlerts FROM alert_log WHERE 1=1 "
      + "<if test='dateFrom != null and dateFrom != \"\"'> AND create_time &gt;= #{dateFrom} </if>"
      + "<if test='dateTo != null and dateTo != \"\"'> AND create_time &lt;= #{dateTo} </if>"
      + "</script>")
  Long selectGlobalAlertCount(@Param("dateFrom") String dateFrom,
                              @Param("dateTo") String dateTo);

  /**
   * 雷达图数据：按功能区分组统计（含标准差和噪声类型数）
   */
  @Select("<script>"
      + "SELECT location, "
      + "COUNT(*) AS recordCount, "
      + "COALESCE(AVG(decibel), 0) AS avgDecibel, "
      + "COALESCE(STDDEV_SAMP(decibel), 0) AS stdDev, "
      + "SUM(CASE WHEN is_abnormal = 1 THEN 1 ELSE 0 END) AS abnormalCount, "
      + "COUNT(DISTINCT noise_type) AS noiseTypeCount "
      + "FROM noise_record WHERE 1=1 "
      + "<if test='dateFrom != null and dateFrom != \"\"'> AND time_point &gt;= #{dateFrom} </if>"
      + "<if test='dateTo != null and dateTo != \"\"'> AND time_point &lt;= #{dateTo} </if>"
      + " GROUP BY location"
      + "</script>")
  List<Map<String, Object>> selectRadarStats(@Param("dateFrom") String dateFrom,
                                              @Param("dateTo") String dateTo);

  /**
   * 多维分析：查询时间范围内的噪声记录（用于 Java 层按 time_segment/noise_type 聚合）
   */
  @Select("<script>"
      + "SELECT id, location, decibel, is_abnormal, noise_type, time_point "
      + "FROM noise_record WHERE 1=1 "
      + "<if test='dateFrom != null and dateFrom != \"\"'> AND time_point &gt;= #{dateFrom} </if>"
      + "<if test='dateTo != null and dateTo != \"\"'> AND time_point &lt;= #{dateTo} </if>"
      + "</script>")
  List<NoiseRecord> selectForAnalysis(@Param("dateFrom") String dateFrom,
                                       @Param("dateTo") String dateTo);

  /**
   * 查询有告警记录的噪声记录信息（用于 alert_count 按 time_segment/noise_type 分组）
   */
  @Select("<script>"
      + "SELECT nr.location, nr.noise_type, nr.time_point "
      + "FROM noise_record nr INNER JOIN alert_log al ON al.noise_record_id = nr.id "
      + "WHERE 1=1 "
      + "<if test='dateFrom != null and dateFrom != \"\"'> AND nr.time_point &gt;= #{dateFrom} </if>"
      + "<if test='dateTo != null and dateTo != \"\"'> AND nr.time_point &lt;= #{dateTo} </if>"
      + "</script>")
  List<Map<String, Object>> selectAlertedRecordInfo(@Param("dateFrom") String dateFrom,
                                                     @Param("dateTo") String dateTo);

  /**
   * 查询指定功能区最近已判定的噪声记录（用于滑动窗口自适应阈值计算）
   * 按时间点倒序，限制条数
   */
  @Select("SELECT * FROM noise_record WHERE location = #{location} AND is_abnormal IN (0,1) ORDER BY time_point DESC LIMIT #{limit}")
  List<NoiseRecord> selectRecentForAdaptive(@Param("location") String location, @Param("limit") int limit);
}
