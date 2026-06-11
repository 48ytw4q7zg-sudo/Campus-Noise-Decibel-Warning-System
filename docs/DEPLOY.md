# 校园噪音分贝预警员系统 — 部署文档

> 适用版本：v1.0.0 | 最后更新：2026-06-11
> 目标环境：Windows / Linux 均可部署

---

## 1. 部署架构

```
┌─────────────────────────────────────────────────────────┐
│  前端 (Vite 构建产出 dist/)  —  Port 80 (nginx)          │
│  Vue 3.5 + Element Plus + ECharts + Axios               │
│  反向代理 /api/* → localhost:8080                        │
└─────────────┬───────────────────────────────────────────┘
              │ HTTP REST /api/*   JWT Bearer Token
┌─────────────▼───────────────────────────────────────────┐
│  后端 (SpringBoot 3.5.14)  —  Port 8080                 │
│  11 Controller → 18 Service → 6 Mapper                  │
│  JWT 校验 (LoginInterceptor) + 全局异常处理              │
└─────────────┬───────────────────────────────────────────┘
              │ JDBC (3306)
┌─────────────▼───────────────────────────────────────────┐
│  MySQL 8.4 LTS  —  Port 3306  —  数据库名 noise_db       │
│  user / noise_record / threshold_rule / alert_log        │
│  area_config / report                                    │
└─────────────────────────────────────────────────────────┘

┌ - - - - - - - - - - - - - - - - - - - - - - - - - - - - ┐
│  ccswitch 配置服务 (Flask)  —  Port 5000 (P2 可选)       │
│  从 ~/.claude/settings.json 读取 AI 模型配置              │
│  提供 /api/health 健康检查 + /api/config/reload 热更新    │
└ - - - - - - - - - - - - - - - - - - - - - - - - - - - - ┘
```

4 个服务：**MySQL（数据层）→ SpringBoot（业务层）→ 前端（表现层）**，ccswitch 为独立 Flask 微服务（可选，不启动不影响主系统运行）。

---

## 2. 环境要求

| 组件 | 版本要求 | 验证命令 |
|------|---------|---------|
| JDK | 21+ | `java -version` |
| Node.js | 24 LTS+ | `node -v` |
| MySQL | 8.4 LTS | `mysql --version` |
| Maven | 3.9+ | `mvn -v` |
| pnpm | 10.33+ | `pnpm -v` |
| Python | 3.9+ (ccswitch 需要) | `python --version` |

---

## 3. 部署步骤

### 3.1 数据库初始化

```bash
# 1. 启动 MySQL 服务（Windows: services.msc，Linux: systemctl start mysql）

# 2. 执行初始化脚本（创建数据库 + 建表 + 预置数据）
mysql -u root -p < sql/01-init.sql

# 3. 验证
mysql -u root -p noise_db -e "SELECT id, username, role FROM user;"
```

脚本 `sql/01-init.sql` 会：
- 创建 database `noise_db`（utf8mb4）
- 创建 6 张业务表（user、noise_record、threshold_rule、alert_log、area_config、report）
- 预置管理员账号 + 4 个功能区 + 16 条阈值规则

### 3.2 后端部署

```bash
# 1. 检查数据库连接配置
#    编辑 backend/src/main/resources/application.yml
#    确认 spring.datasource.url / username / password 与本地 MySQL 一致

# 2. 编译打包
cd backend
mvn clean package -DskipTests
# 产物：backend/target/noise-1.0.0.jar

# 3. 启动
java -jar target/noise-1.0.0.jar
# 默认监听 http://localhost:8080
```

> 自定义端口：`java -jar target/noise-1.0.0.jar --server.port=9090`

### 3.3 前端部署

**开发模式（调试用）：**

```bash
cd frontend
pnpm install
pnpm dev
# 默认 http://localhost:5173
# Vite proxy 自动将 /api/* 转发到 localhost:8080
```

**生产构建：**

```bash
cd frontend
pnpm install
pnpm build
# 产物：frontend/dist/

# 方式 A：直接预览
pnpm preview

# 方式 B：Nginx 部署（推荐生产环境）
# 将 dist/ 复制到 nginx html 目录
# 配置反向代理见第 8 节
```

### 3.4 ccswitch 配置服务（可选）

```bash
cd ccswitch_service
pip install -r requirements.txt
python app.py
# 默认 http://localhost:5000
# 健康检查：curl http://localhost:5000/api/health
```

不启动 ccswitch 不影响主系统运行，仅 AI 辅助分类和配置热更新功能不可用。

---

## 4. 启动验证

按顺序启动后，逐层验证：

```bash
# 1. MySQL（Port 3306）
mysql -u root -p noise_db -e "SELECT COUNT(*) AS table_count FROM information_schema.tables WHERE table_schema='noise_db';"

# 2. 后端（Port 8080）
curl http://localhost:8080/api/login -X POST \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
# 预期返回：{"code":200,"message":"登录成功","data":{"token":"eyJ..."}}

# 3. 前端（Port 5173 dev / Port 80 prod）
curl http://localhost:5173
# 预期返回 HTML 页面

# 4. ccswitch（Port 5000，可选）
curl http://localhost:5000/api/health
# 预期返回：{"status":"ok","service":"noise_ccswitch_service"}
```

---

## 5. 故障排查

| 问题 | 可能原因 | 解决方法 |
|------|---------|---------|
| 后端启动报 `Access denied for user` | MySQL 用户名/密码不匹配 | 检查 `application.yml` 中 `datasource.username` 和 `password` |
| `Unknown database 'noise_db'` | 未执行 SQL 初始化 | 运行 `mysql -u root -p < sql/01-init.sql` |
| `Communications link failure` | MySQL 服务未启动 | `systemctl start mysql` 或 `net start MySQL` |
| 后端启动报 `Table 'noise_db.user' doesn't exist` | SQL 脚本执行不全 | 重新执行 `sql/01-init.sql` |
| 前端 `pnpm: command not found` | 未安装 pnpm | `npm install -g pnpm@10.33.4` |
| 前端请求后端 401 | token 过期或未传 | 检查 localStorage 中 token 是否存在，重新登录 |
| 前端跨域 (CORS) | vite proxy 未生效或 nginx 未配 | dev 模式检查 `vite.config.js` proxy；prod 模式检查 nginx 配置 |
| ccswitch 启动报 No module named 'flask' | 未安装依赖 | `pip install -r requirements.txt` |
| 后端连接 ccswitch 失败 | ccswitch 未启动或端口占用 | 确认 `http://localhost:5000` 可访问；或修改 `application.yml` 中 `ccswitch.base-url` |

---

## 6. 默认账号

| 角色 | 用户名 | 密码 | 说明 |
|------|--------|------|------|
| 管理员 | `admin` | `admin123` | SQL 预置，拥有全部功能权限 |
| 普通用户 | `zhangsan` | `123456` | 需通过注册接口自行注册（`POST /api/user/register`） |

> 管理员密码 `admin123` 仅用于开发测试，**上线前务必修改**。修改方式：使用 BCrypt 生成新哈希值，执行 `UPDATE user SET password='<new_bcrypt_hash>' WHERE username='admin';`

---

## 7. 安全检查清单

上线前逐项确认：

- [ ] **修改数据库密码**：`application.yml` 中 `spring.datasource.password` 改为强密码
- [ ] **修改 JWT secret**：`application.yml` 中 `jwt.secret` 改为随机长字符串（>=32 字符）
- [ ] **关闭调试日志**：`application.yml` 中 `logging.level.com.example.noise` 改为 `info`
- [ ] **关闭 MyBatis SQL 日志**：注释或删除 `mybatis-plus.configuration.log-impl` 配置
- [ ] **关闭 ccswitch DEBUG 模式**：`ccswitch_service/config.py` 中 `DEBUG` 环境变量设为 `False`
- [ ] **修改管理员默认密码**：`admin123` 必须更换为强密码
- [ ] **确认 `.env` 文件已加入 `.gitignore`**：防止密钥泄露到版本控制
- [ ] **数据库仅监听 127.0.0.1**：`my.ini` 或 `my.cnf` 中 `bind-address = 127.0.0.1`
- [ ] **MySQL 创建专用用户**：不用 root 账号跑应用，创建受限用户 `noise_app` 并授权 `noise_db`
- [ ] **配置 HTTPS**：生产环境使用 nginx 反向代理 + Let's Encrypt 证书，禁止明文传输 JWT token
- [ ] **文件上传限制**：确认 `application.yml` 中 `spring.servlet.multipart.max-file-size` 为 10MB，避免被撑爆磁盘

---

## 8. 云服务器进阶（可选）

### 8.1 一键 Docker Compose

> 将下方内容保存为项目根目录 `docker-compose.yml`，与 `sql/`、`backend/`、`frontend/` 同级。

```yaml
version: '3.8'
services:
  mysql:
    image: mysql:8.4
    container_name: noise-mysql
    restart: always
    ports:
      - "3306:3306"
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: noise_db
    volumes:
      - ./sql/01-init.sql:/docker-entrypoint-initdb.d/01-init.sql
      - mysql_data:/var/lib/mysql

  backend:
    build: ./backend
    container_name: noise-backend
    restart: always
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/noise_db?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&useSSL=false
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: root
    depends_on:
      - mysql

  frontend:
    build: ./frontend
    container_name: noise-frontend
    restart: always
    ports:
      - "80:80"
    depends_on:
      - backend

volumes:
  mysql_data:
```

```bash
# 启动全部服务
docker-compose up -d

# 查看日志
docker-compose logs -f

# 停止
docker-compose down
```

### 8.2 Nginx 反向代理配置

```nginx
server {
    listen 80;
    server_name your-domain.com;

    # 前端静态资源
    root /usr/share/nginx/html;
    index index.html;

    # SPA fallback
    location / {
        try_files $uri $uri/ /index.html;
    }

    # 后端 API 反向代理
    location /api/ {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # 上传文件大小限制
        client_max_body_size 10m;
    }

    # 静态资源缓存
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg)$ {
        expires 30d;
        add_header Cache-Control "public, immutable";
    }
}
```

---

> **部署完成后**，浏览器访问 `http://your-domain.com`，使用管理员账号 `admin/admin123` 登录验证。
>
> 如有问题，先查第 5 节故障排查表，仍未解决则查看后端日志 `logs/` 目录或 `docker-compose logs`。
