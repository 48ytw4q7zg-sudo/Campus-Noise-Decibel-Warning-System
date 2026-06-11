# 08e · 特殊场景 Cookbook

> 出现需求才查 · feature-coder 11 类特殊场景识别配套 · 35 题数据驱动

---

### G 类:V4-D04 新增 Phase 4 特殊场景 FAQ(2026-05-10 升级 · 配合 feature-coder 11 类特殊场景识别)

#### Q40(Phase 4 特殊场景 · 2026-05-10): ECharts 图表模式库(35 题 100% 覆盖 · 5 类图表代码段)

**触发场景**:PRD §3 含「统计/看板/图表/可视化/大屏/仪表盘」关键词 · feature-coder 自动识别 → 后端 +`/api/<x>/stats` 聚合接口 + 前端 ECharts 配置。35 题中 35 题都涉及图表场景(物业缴费统计/学生成绩分布/订单销售看板/健身打卡热力图等)。

**后端聚合接口模式**:

```java
// controller/PaymentController.java
@GetMapping("/stats")
public Result<PaymentStatsResponse> getStats(@RequestParam(required = false) String dateRange) {
    return Result.success(paymentService.getStats(dateRange));
}

// service/impl/PaymentServiceImpl.java
@Override
public PaymentStatsResponse getStats(String dateRange) {
    // 1. 总金额(标量)
    BigDecimal totalAmount = baseMapper.selectObjs(
        new LambdaQueryWrapper<Payment>().select(Payment::getAmount)
    ).stream().map(o -> (BigDecimal) o).reduce(BigDecimal.ZERO, BigDecimal::add);

    // 2. 按月分组聚合(时序图表数据源)
    List<Map<String, Object>> monthlyData = baseMapper.selectMaps(
        new QueryWrapper<Payment>()
            .select("DATE_FORMAT(create_time, '%Y-%m') as month, SUM(amount) as total")
            .groupBy("month")
            .orderByAsc("month")
    );

    return new PaymentStatsResponse(totalAmount, monthlyData);
}
```

**5 类图表前端配置代码段**(放 `views/<X>StatsPage.vue` script setup):

| 图表类型 | 适用场景 | 关键 option 配置 |
|---|---|---|
| **柱状图** | 月度销售对比 / 学生分数段统计 | `series: [{ type: 'bar', data: [...] }]` |
| **饼图** | 订单状态占比 / 用户角色分布 | `series: [{ type: 'pie', radius: '70%', data: [{name, value}] }]` |
| **折线图** | 时序趋势(每日缴费/打卡数) | `series: [{ type: 'line', smooth: true, data: [...] }]` |
| **雷达图** | 多维评分(教师评估/产品对比) | `radar: { indicator: [{name, max}] }, series: [{ type: 'radar', data: [{value, name}] }]` |
| **漏斗图** | 转化漏斗(注册→登录→下单→支付) | `series: [{ type: 'funnel', data: [{name, value}] }]` |

**统一前端代码模式**(以柱状图为例 · 其他类型仅 series.type 不同):

```vue
<template>
  <div ref="chartRef" style="width: 100%; height: 400px"></div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import * as echarts from 'echarts';
import { getPaymentStats } from '@/api/payment';

const chartRef = ref(null);
let chartInstance = null;

const renderChart = async () => {
  const stats = await getPaymentStats();
  chartInstance = echarts.init(chartRef.value);
  chartInstance.setOption({
    title: { text: '月度缴费统计' },
    tooltip: {},
    xAxis: { data: stats.monthlyData.map(d => d.month) },
    yAxis: {},
    series: [{
      name: '缴费金额',
      type: 'bar',  // 改 'pie'/'line'/'radar'/'funnel' 即切图表
      data: stats.monthlyData.map(d => d.total),
    }],
  });
};

onMounted(renderChart);
onUnmounted(() => chartInstance?.dispose());  // ⚠️ 必须 dispose · 否则内存泄漏
</script>
```

**踩坑点**:
- ⚠️ **必须 `onUnmounted` 调 `chartInstance.dispose()`** —— echarts 实例不释放会持续占用内存(尤其单页应用切换路由后)
- ⚠️ **避免在 `onMounted` 之前访问 `chartRef.value`** —— DOM 还没挂载,会拿到 null
- ⚠️ **窗口 resize 处理**:加 `window.addEventListener('resize', () => chartInstance.resize())` + `onUnmounted` 移除

**pom.xml 后端不需要加依赖**(echarts 是前端库),前端 `package.json` 加 `"echarts": "^5.4.3"`(由 init-skeleton 预置 · 学生不需手动加)。

---

#### Q41(Phase 4 特殊场景 · 2026-05-10): 定时任务 @Scheduled 最佳实践(分页/幂等/重试 3 模式)

**触发场景**:PRD §3 含「定时/自动/每天/每周/凌晨/隔 X 分钟」关键词 · feature-coder 自动识别 → +`scheduler/<X>Scheduler.java` + 启动类 `@EnableScheduling`。35 题中 51.4%(18/35)题涉及定时任务(物业每月生成账单/电商每天清理过期订单/健身每周统计打卡等)。

**模式 1:简单定时(无分页/无幂等)** —— 适合数据量 < 1000 行:

```java
@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentScheduler {
    private final PaymentService paymentService;

    /** 每天凌晨 1 点扫描超时未支付订单 · 标记为已取消 */
    @Scheduled(cron = "0 0 1 * * ?")  // 秒 分 时 日 月 周
    public void cancelExpiredOrders() {
        log.info("[Scheduler] 开始扫描超时未支付订单");
        int count = paymentService.cancelExpiredOrders();
        log.info("[Scheduler] 完成 · 取消 {} 笔超时订单", count);
    }
}
```

⚠️ **启动类必加** `@EnableScheduling`:

```java
@SpringBootApplication
@EnableScheduling  // 启用定时任务自动扫描
public class PropertyApplication {
    public static void main(String[] args) {
        SpringApplication.run(PropertyApplication.class, args);
    }
}
```

**模式 2:分页定时(数据量大)** —— 适合数据量 > 1000 行 · 防内存溢出:

```java
@Scheduled(cron = "0 0 2 * * ?")
public void cleanupExpiredData() {
    int pageNum = 1, pageSize = 500;
    while (true) {
        Page<Order> page = orderService.page(
            new Page<>(pageNum, pageSize),
            new LambdaQueryWrapper<Order>().eq(Order::getStatus, "EXPIRED")
        );
        if (page.getRecords().isEmpty()) break;
        page.getRecords().forEach(orderService::archiveOrder);
        pageNum++;
        if (pageNum > page.getPages()) break;
    }
}
```

**模式 3:幂等 + 重试定时(关键业务)** —— 防重复执行 + 失败自动重试:

```java
@Component
@Slf4j
@RequiredArgsConstructor
public class BillScheduler {
    private final BillService billService;
    private static final String LOCK_KEY = "scheduler:generate-bill:lock";

    /** 每月 1 日凌晨生成账单(关键业务 · 必须幂等 + 重试) */
    @Scheduled(cron = "0 0 0 1 * ?")
    public void generateMonthlyBills() {
        // 幂等检查:如本月账单已生成则跳过
        if (billService.isCurrentMonthGenerated()) {
            log.info("[Scheduler] 本月账单已生成 · 跳过");
            return;
        }

        // 重试 3 次
        for (int i = 1; i <= 3; i++) {
            try {
                billService.generateMonthlyBills();
                log.info("[Scheduler] 账单生成成功 · 第 {} 次尝试", i);
                return;
            } catch (Exception e) {
                log.error("[Scheduler] 第 {} 次尝试失败 · 异常 {}", i, e.getMessage());
                if (i == 3) throw new BusinessException(5001, "账单生成失败 · 已重试 3 次");
                try { Thread.sleep(1000 * i); } catch (InterruptedException ex) {}
            }
        }
    }
}
```

**踩坑点**:
- ⚠️ **cron 表达式 6 字段**:`秒 分 时 日 月 周`(Java Spring 是 6 字段,跟 Linux crontab 5 字段不一样)
- ⚠️ **多实例部署需要分布式锁**:本课程教学场景单实例足够,生产环境用 Redisson `RLock` 防多实例重复执行
- ⚠️ **不在事务方法上加 `@Scheduled`**:`@Transactional` 跟 `@Scheduled` 同时用会让事务范围错位 · 建议 Scheduler 方法只调 Service · 事务在 Service 层加

---

#### Q42(Phase 4 特殊场景 · 2026-05-10): 乐观锁与并发防护(MySQL version + Mapper UpdateById + 重试)

**触发场景**:PRD §3 含「乐观锁/防超卖/抢单/抢购/秒杀」关键词 · feature-coder 自动识别 → entity `@Version` + UpdateById 自动版本判断 + 重试逻辑。组合场景题(电商秒杀/抢单 / 健身抢课 / 选课系统等)必用。

**MySQL 表结构(由 db-designer 生成)**:

```sql
CREATE TABLE goods (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    stock INT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,  -- 乐观锁版本号
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**Entity 加 @Version**(由 entity-coder/feature-coder 生成):

```java
@Data
@TableName("goods")
public class Goods {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private Integer stock;

    @Version  // ⭐ MyBatis-Plus 自动维护 · UpdateById 时 SQL 自动加 WHERE version = ?
    private Integer version;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
```

**ServiceImpl 抢购扣库存逻辑**(by feature-coder 生成):

```java
@Service
@Slf4j
@RequiredArgsConstructor
public class GoodsServiceImpl extends ServiceImpl<GoodsMapper, Goods> implements GoodsService {

    @Override
    @Transactional
    public void deductStock(Long goodsId, Integer quantity) {
        for (int i = 1; i <= 3; i++) {
            Goods goods = getById(goodsId);
            if (goods == null) throw new BusinessException(2001, "商品不存在");
            if (goods.getStock() < quantity) throw new BusinessException(2002, "库存不足");

            goods.setStock(goods.getStock() - quantity);
            // ⭐ updateById 自动加 WHERE id = ? AND version = ?
            // 若版本号已被其他事务改 · 返回 0(影响行数)· 即冲突
            boolean success = updateById(goods);
            if (success) {
                log.info("[Stock] 扣库存成功 · 第 {} 次尝试 · 商品 {} · 数量 {}", i, goodsId, quantity);
                return;
            }
            log.warn("[Stock] 第 {} 次扣库存冲突 · 重试", i);
            try { Thread.sleep(50 * i); } catch (InterruptedException e) {}
        }
        throw new BusinessException(2003, "并发冲突 · 请重试");
    }
}
```

**MyBatis-Plus 自动生成的 SQL**(乐观锁透明拦截):

```sql
-- updateById(goods) 实际执行 SQL(MP 拦截器自动加 version 判断 + 加 1)
UPDATE goods
SET name = ?, stock = ?, version = version + 1, update_time = NOW()
WHERE id = ? AND version = ?  -- ⭐ 自动加这个 version 判断
```

**踩坑点**:
- ⚠️ **`@Version` 字段必须 Integer 或 Long**(不能 int/long 基本类型 · MP 要求包装类)
- ⚠️ **必须用 `updateById(entity)` 而不是 `update(updateWrapper)`**:`@Version` 只在 updateById 自动生效;手写 wrapper 需自己加 `eq(Goods::getVersion, oldVersion)`
- ⚠️ **重试间隔指数退避**:首次 50ms / 次次 100ms / 末次 150ms — 避免雪崩重试
- ⚠️ **重试上限 3 次**:超过仍冲突说明并发太高 · 抛 BusinessException 让前端重试 · 不要无限重试
- ⚠️ **跟悲观锁(SELECT ... FOR UPDATE)区别**:乐观锁性能高(无锁 · 读多写少场景);悲观锁阻塞强(查询时锁行 · 写多场景)· 教学统一用乐观锁

---

#### Q43(Phase 4 特殊场景 · 2026-05-10): 文件上传分级方案(单文件/多文件/分片)

**触发场景**:PRD §3 含「上传/图片/附件/头像/导入文件」关键词 · feature-coder 自动识别 → 后端 +`@PostMapping("/upload")` + `MultipartFile` + 本地 uploads/ + 静态资源映射 + 前端 `<el-upload>`。35 题 31.4%(11/35)题涉及上传(物业投诉附件 / 课程作业附件 / 用户头像 / 商品图片等)。

**3 级方案选择(按文件大小 + 数量)**:

| 级别 | 适用 | 方案 |
|---|---|---|
| **L1 单文件** | < 10MB · 单张图片/PDF | `@RequestParam("file") MultipartFile file` + `file.transferTo(localPath)` |
| **L2 多文件** | < 50MB 总 · 多张图片(投诉附件 3-5 张) | `@RequestParam("files") MultipartFile[] files` + 循环 `transferTo` |
| **L3 分片** | > 50MB(视频/大文件) | 本课程教学不强制 · 用 vue-uploader 库或自实现分片 |

**L1 单文件方案(教学主流)**:

application.yml 配置(由 init-skeleton 预置或学生加):

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 10MB       # 单文件
      max-request-size: 50MB    # 总请求大小
```

后端 Controller(by feature-coder 生成):

```java
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Slf4j
public class FileController {
    private static final String UPLOAD_DIR = "./uploads";  // 本地目录
    private static final List<String> ALLOWED_TYPES = List.of(".jpg", ".png", ".pdf", ".docx");
    private static final long MAX_SIZE = 10 * 1024 * 1024;  // 10MB

    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) throw new BusinessException(4001, "文件不能为空");
        if (file.getSize() > MAX_SIZE) throw new BusinessException(4002, "文件超过 10MB");

        String originalName = file.getOriginalFilename();
        String ext = originalName.substring(originalName.lastIndexOf('.'));
        if (!ALLOWED_TYPES.contains(ext.toLowerCase())) {
            throw new BusinessException(4003, "不支持的文件类型");
        }

        // 路径:./uploads/2026/05/10/<UUID>.ext(防重名 + 按日期分目录)
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String fileName = UUID.randomUUID() + ext;
        Path dirPath = Paths.get(UPLOAD_DIR, today);
        Files.createDirectories(dirPath);
        Path filePath = dirPath.resolve(fileName);
        file.transferTo(filePath.toFile());

        // 返回 URL · 由前端拼接 baseURL 访问
        String url = "/uploads/" + today + "/" + fileName;
        log.info("[Upload] 文件上传成功 · {} · 大小 {} bytes", url, file.getSize());
        return Result.success(url);
    }
}
```

**静态资源映射**(WebMvcConfig.java · 由 init-skeleton 预置 · feature-coder 触发上传场景时追加):

```java
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 浏览器访问 /uploads/2026/05/10/xxx.jpg → 实际读 ./uploads/2026/05/10/xxx.jpg
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:./uploads/");
    }
}
```

**前端 `<el-upload>` 组件**(by vue-page-coder/feature-coder 生成):

```vue
<template>
  <el-upload
    action="/api/files/upload"
    :on-success="handleSuccess"
    :before-upload="beforeUpload"
    accept=".jpg,.png,.pdf,.docx"
    :show-file-list="true"
  >
    <el-button type="primary">点击上传</el-button>
    <template #tip>
      <div class="el-upload__tip">支持 jpg/png/pdf/docx · 单文件 ≤ 10MB</div>
    </template>
  </el-upload>
</template>

<script setup>
import { ElMessage } from 'element-plus';

const beforeUpload = (file) => {
  const isAllowed = ['.jpg', '.png', '.pdf', '.docx'].some(ext => file.name.toLowerCase().endsWith(ext));
  if (!isAllowed) ElMessage.error('文件类型不支持');
  if (file.size > 10 * 1024 * 1024) ElMessage.error('文件超过 10MB');
  return isAllowed && file.size <= 10 * 1024 * 1024;
};

const handleSuccess = (response) => {
  if (response.code === 200) {
    ElMessage.success('上传成功');
    // 把 response.data(URL)回填到 form 里
    formData.attachmentUrl = response.data;
  }
};
</script>
```

**踩坑点**:
- ⚠️ **路径穿越攻击防护**:**禁止**直接用 `file.getOriginalFilename()` 作为文件名 — 攻击者可传 `../../etc/passwd` · 必须用 UUID 重命名
- ⚠️ **类型校验**:**不能**只检查后缀(攻击者可改 .exe → .jpg)· 严肃场景需要查 magic number(本课程教学简化:`@RequestParam` + 后缀白名单 + Apache Tika 识别 MIME · 可选)
- ⚠️ **本地目录权限**:`./uploads/` 必须是 SpringBoot 进程可写 · 生产环境通常用 `/var/www/uploads/`
- ⚠️ **生产环境用云存储**:OSS/S3/七牛 · 教学统一本地 · 根 `CLAUDE.md` §二(后端规范)已禁止云存储
- ⚠️ **ECS 重启数据丢失**:上传到 ./uploads/ 在 ECS 上要挂载持久化卷 · 教学环境本地开发不影响

---

#### Q44(Phase 4 特殊场景 · 2026-05-10): 简易推荐撮合算法(Jaccard/贪心/规则 3 模式)

**触发场景**:PRD §3 含「推荐/匹配/撮合/智能筛选」关键词 · feature-coder 自动识别 → ServiceImpl 加算法方法。组合场景题(交友匹配 / 课程推荐 / 商品推荐 / 拼车撮合等)必用。

**3 模式选择**:

| 模式 | 适用 | 算法思路 |
|---|---|---|
| **Jaccard 相似度** | 标签匹配(用户兴趣 vs 商品标签) | `相似度 = |交集| / |并集|` · 排序选 Top-N |
| **贪心选择** | 按权重排序 + 容量限制(撮合拼车 / 课程时间冲突) | 按权重降序 · 逐个加入直到容量满 |
| **规则匹配** | 多条件 if-else 链(性别 + 年龄 + 兴趣) | `score += weight if 条件成立`,score 排序 |

**模式 1:Jaccard 相似度推荐**(by feature-coder · ServiceImpl 内):

```java
public List<Goods> recommendByTags(Long userId, int topN) {
    // 1. 取用户兴趣标签
    Set<String> userTags = userTagMapper.selectTagsByUserId(userId);
    if (userTags.isEmpty()) return Collections.emptyList();

    // 2. 取所有商品 + 算 Jaccard 相似度
    List<Goods> allGoods = list();
    return allGoods.stream()
        .map(g -> Map.entry(g, jaccardSimilarity(userTags, parseTags(g.getTags()))))
        .filter(e -> e.getValue() > 0)
        .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
        .limit(topN)
        .map(Map.Entry::getKey)
        .collect(Collectors.toList());
}

private double jaccardSimilarity(Set<String> a, Set<String> b) {
    Set<String> intersection = new HashSet<>(a);
    intersection.retainAll(b);
    Set<String> union = new HashSet<>(a);
    union.addAll(b);
    return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
}

private Set<String> parseTags(String tagsCsv) {
    return tagsCsv == null ? Collections.emptySet() : Arrays.stream(tagsCsv.split(","))
        .map(String::trim).collect(Collectors.toSet());
}
```

**模式 2:贪心选择(拼车撮合)**:

```java
public List<RidePool> matchRides(Long userId, LocalDate date) {
    // 1. 取所有当日待撮合订单 · 按时间窗口排序
    List<Ride> candidates = rideMapper.selectAvailable(date)
        .stream()
        .sorted(Comparator.comparing(Ride::getDepartTime))
        .collect(Collectors.toList());

    // 2. 贪心:按时间顺序逐个加入 · 容量满则建新池
    List<RidePool> pools = new ArrayList<>();
    RidePool currentPool = null;
    for (Ride r : candidates) {
        if (currentPool == null || currentPool.isFull() || !currentPool.timeWindowMatch(r)) {
            currentPool = new RidePool();
            pools.add(currentPool);
        }
        currentPool.add(r);
    }
    return pools;
}
```

**模式 3:规则匹配(交友/约课)**:

```java
public List<User> matchPartners(Long userId) {
    User me = getById(userId);
    return list().stream()
        .filter(u -> !u.getId().equals(userId))
        .map(u -> Map.entry(u, scoreMatch(me, u)))
        .filter(e -> e.getValue() > 50)  // 阈值 · 低于不推荐
        .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
        .limit(20)
        .map(Map.Entry::getKey)
        .collect(Collectors.toList());
}

private int scoreMatch(User me, User other) {
    int score = 0;
    // 规则 1:性别匹配 +30
    if (Objects.equals(me.getPreferGender(), other.getGender())) score += 30;
    // 规则 2:年龄差 < 5 +20
    if (Math.abs(me.getAge() - other.getAge()) < 5) score += 20;
    // 规则 3:共同兴趣 +10*N
    Set<String> commonTags = new HashSet<>(parseTags(me.getTags()));
    commonTags.retainAll(parseTags(other.getTags()));
    score += commonTags.size() * 10;
    // 规则 4:同城 +20
    if (Objects.equals(me.getCity(), other.getCity())) score += 20;
    return score;
}
```

**踩坑点**:
- ⚠️ **算法不依赖外部库** —— 教学场景用 Java 标准库(Stream + Set + HashMap)即可 · 不引入 mahout/spark
- ⚠️ **算法注释清楚思路** —— 答辩时学生能讲清「为啥用 Jaccard 不用 Cosine 余弦」(标签场景 Jaccard 简单 · 实数向量场景才用余弦)
- ⚠️ **批量匹配性能优化** —— 全量用户 N 跟全量商品 M 算 Jaccard 是 O(N*M) · 数据量大需要先按标签倒排索引 + Lucene/ES(本课程教学场景 N+M < 1000 时直接遍历即可)
- ⚠️ **冷启动问题** —— 新用户无标签时退化为热门推荐(按销量/评分排)· 学生项目 P0 简化为「无标签返回空列表」即可

---

