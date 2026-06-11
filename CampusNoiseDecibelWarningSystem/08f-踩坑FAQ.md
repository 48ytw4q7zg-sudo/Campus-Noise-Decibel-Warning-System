# 08f · 踩坑 FAQ

> 报错才查 · 按 报错关键词 找。先用 Ctrl+F 搜你看到的报错信息,90% 能找到。
> 环境 / Claude Code CLI / AI 配置类报错请去 **08a §12**。

---

### B 类:Maven 编译问题

#### Q5: `mvn compile` 报 `Could not transfer artifact ... from/to central`

**原因**:Maven 中央仓库连不上(被墙)。

**解决**:检查 08a §3 步骤中阿里云镜像是否配好。`settings.xml` 应有 `<mirror>...</mirror>` 段,`<url>` 是 `https://maven.aliyun.com/repository/public`。

---

#### Q6: `mvn compile` 报 `Source option 8 is no longer supported. Use 17 or later.`

**原因**:pom.xml 配的 Java 版本是 8,但教师模板/G-01a 应该是 21。**说明你下错模板**或**改坏了 pom.xml**。

**解决**:打开 `backend/pom.xml`,找到:

```xml
<properties>
    <java.version>21</java.version>
</properties>
```

确认是 21。如果不是改成 21 重新 `mvn clean compile`。

---

#### Q7: `mvn compile` 报 `package org.springframework... does not exist`

**原因**:依赖没下载完。

**解决**:

```bash
mvn dependency:resolve
```

强制重新拉依赖,等待完成后重试 `mvn compile`。

---

#### Q8: `mvn spring-boot:run` 报 `Web server failed to start. Port 8080 was already in use.`

**原因**:8080 端口被别的程序占用了(比如之前跑了 SpringBoot 没关)。

**解决**:

```bash
netstat -ano | findstr :8080
```

记下 PID:

```bash
taskkill /F /PID <PID>
```

或修改 `application.yml`:

```yaml
server:
  port: 8081
```

---

#### Q9: `mvn spring-boot:run` 报 `Failed to configure a DataSource`

**原因**:MySQL 连接配置错(密码、URL、数据库不存在)。

**解决**:
1. 确认 MySQL 服务运行
2. 确认 application.yml 密码正确
3. 确认数据库已创建(`SHOW DATABASES;`)

---

### C 类:前端 npm 问题

#### Q10: `pnpm install` 卡在 `sill idealTree buildDeps`

**原因**:网络问题或镜像没配。

**解决**:
1. 确认 08a §5 配了淘宝镜像:`npm config get registry` 显示 `https://registry.npmmirror.com/`
2. 删除 `node_modules` 和 `pnpm-lock.yaml`,重新 `pnpm install`
3. 还不行 → 换手机热点试(学校网偶尔抽风)

---

#### Q11: `pnpm install` 报 `node-gyp` 编译错误

**原因**:某个依赖需要原生编译,Windows 缺 C++ 工具链。

**解决**:Vue 3 + Vite 项目通常不应该有这个错。如果遇到,把报错关键词发 QQ 群,教师提供针对性方案(改用预编译版本依赖)。

---

#### Q12: `pnpm dev` 报 `EADDRINUSE: address already in use :::5173`

**原因**:5173 端口被占用。

**解决**:在 `frontend/vite.config.js` 加:

```js
export default defineConfig({
  server: {
    port: 5174
  }
})
```

---

#### Q13: 浏览器打开 localhost:5173 显示 "无法访问此网站"

**原因**:可能 `pnpm dev` 实际没启动成功。

**解决**:
1. 看 cmd 是否显示 `Local: http://localhost:5173/`
2. 看是否有 ERR! 报错被淹没
3. 关掉浏览器代理(装了 Clash/V2Ray 的同学,让 localhost 走直连)

---

#### Q14: `pnpm dev` 启动报 `ENOENT: no such file or directory, open '...\\src\\views\\LoginPage.vue'`(或 HomePage.vue)🆕 2026-05-11

**完整报错样例**:

```
(!) Failed to run dependency scan. Skipping dependency pre-bundling.
[plugin vite:dep-scan:load:html]
Error: ENOENT: no such file or directory, open 'E:\...\frontend\src\views\LoginPage.vue'
```

**原因**:
- `frontend/src/router/index.js`(由 `/init-skeleton` 生成)**静态 import** 引用了 `LoginPage.vue` + `HomePage.vue` 两个路由组件
- Vite 8 + Rolldown 扫描器在 `pnpm dev` 启动时扫这些 import 路径,**若 `views/` 下文件不存在则直接报 ENOENT**
- 触发场景:`/init-skeleton` 未生成占位 .vue · 或学生手工删了占位文件

**解决方案 A · 重跑 `/init-skeleton`**(推荐 · 一次到位):
- 删掉项目目录 → 回 08b §2 重跑 `/init-skeleton`(会自动生成 `LoginPage.vue` + `HomePage.vue` 占位)
- ⚠️ 重跑会丢已写的代码 · 若已经进 Phase 1+ 不要用本方案,用方案 B

**解决方案 B · 手工兜底创建 2 个占位 .vue**(已进 Phase 1+ 的学生):

在 `frontend/src/views/` 下创建两个文件:

**`frontend/src/views/LoginPage.vue`**:
```vue
<!-- views/LoginPage.vue · 手工兜底占位 · Phase 5 /login-coder 会覆盖 -->
<template>
  <div class="placeholder">
    <h2>登录页占位</h2>
    <p>本页 init-skeleton 旧版未生成 · 兜底创建 · Phase 5 调用 <code>/login-coder</code> 会用真实登录页覆盖。</p>
  </div>
</template>
<script setup></script>
<style scoped>
.placeholder { padding: 40px; text-align: center; color: #666; }
</style>
```

**`frontend/src/views/HomePage.vue`**:
```vue
<!-- views/HomePage.vue · 手工兜底占位 · Phase 5 /vue-page-coder 页面=Home 会覆盖 -->
<template>
  <div class="placeholder">
    <h2>首页占位</h2>
    <p>本页 init-skeleton 旧版未生成 · 兜底创建 · Phase 5 调用 <code>/vue-page-coder 页面=Home</code> 会用真实首页覆盖。</p>
  </div>
</template>
<script setup></script>
<style scoped>
.placeholder { padding: 40px; text-align: center; color: #666; }
</style>
```

创建后重新 `pnpm dev`,应能正常启动 → 浏览器 5173 看到 LoginPage 占位页。

**关联文档**:
- 根因和模板权威源:`项目模板/.claude/commands/init-skeleton.md` ###「views/ 两个占位 .vue 文件」段
- 命名规约:`frontend/src/views/` 下所有 .vue 都用「大驼峰 + Page 后缀」(对齐 frontend.md §一 + login-coder.md + vue-page-coder.md 命名映射规则)

---

### E 类:Git / Gitee 问题

#### Q20: `git push` 失败 `error: failed to push some refs to ...`

**原因**:Gitee 远程仓库与本地不一致(通常是 Gitee 上有 README 但本地没有)。

**解决**:§1.3 强调"创建仓库时不要勾选初始化"。如果你已经勾了:

```bash
git pull origin main --allow-unrelated-histories
# 提示合并冲突,编辑冲突文件后再 commit
git push -u origin main
```

---

#### Q21: `git push` 弹窗输入密码后还是失败 `Authentication failed`

**原因**:Gitee 密码错了,或开了二次验证。

**解决**:
1. Gitee → 个人设置 → 私人令牌 → 生成新令牌(access_token),用令牌代替密码登录
2. 或改用 SSH 方式(详见 Q21b)

---

#### Q21b: 如何配置 Gitee SSH key(推荐免密 push)

**步骤**:

1. 生成 SSH key:

```bash
ssh-keygen -t ed25519 -C "你的邮箱"
```

一路回车(不设 passphrase)。

2. 复制公钥:

```bash
# Windows
type %USERPROFILE%\.ssh\id_ed25519.pub
# Mac
cat ~/.ssh/id_ed25519.pub
```

3. Gitee → 个人设置 → **SSH 公钥** → 添加 → 粘贴 → 标题随便填 → 确定

4. 验证:

```bash
ssh -T git@gitee.com
```

首次会问是否信任,输 `yes`。看到 `Hi xxx! You've successfully authenticated` 即成功。

5. 修改 remote URL 为 SSH:

```bash
git remote set-url origin git@gitee.com:<高校版组织名>/<学号>-<题号简写>.git
```

之后 `git push` 不再要密码。

---

#### Q22: 不小心把 `node_modules` 提交到 Gitee 了(仓库特别大)

**原因**:`.gitignore` 没生效。

**解决**:

```bash
git rm -r --cached frontend/node_modules
git rm -r --cached backend/target
git commit -m "chore: 移除不应提交的目录"
git push
```

并确认 `.gitignore` 里有:

```
node_modules/
target/
.idea/
*.iml
```

---

#### Q23(V2 新): 推到高校版班级项目失败 `repository not found`

**原因**:仓库 URL 写错(写成了个人 namespace 而非高校版班级项目 namespace)。

**解决**:
1. 打开 Gitee 仓库页面,复制顶部的"克隆/下载"中的 HTTPS URL
2. 检查格式是否为 `https://gitee.com/<高校版组织名>/<仓库名>.git`(不是 `gitee.com/<你的用户名>/...`)
3. 修正本地 remote:

```bash
git remote set-url origin https://gitee.com/<高校版组织名>/<仓库名>.git
git push -u origin main
```

---

### F 类:V2 新增 Phase / commit / 对话记录 问题

#### Q24: G-01a 跑完 5 项硬门槛某项不通过

**解决**:不要硬钢全部重跑。把不通过的那项告诉 AI 让它**重生成对应部分**:

```
请重新生成 [项目名] 的 [缺失部分],并保证 [硬门槛 X] 通过。当前问题:[具体说明]
```

例如硬门槛 #2 不通过(版本未锁定):

```
请重新生成 backend/pom.xml 和 frontend/package.json,所有依赖都用具体版本号,不能有 LATEST/^/~。
当前问题:package.json 的 vue 写成 "^3.4.0",应该改为具体的 "3.4.21"。
```

3 次仍失败 → §2.3 兜底方案(教师 zip)。

---

#### Q25: 我的 commit 数距 30 还有差距,怎么补?

⚠️ **不要为了凑数刷无意义 commit**(改一个空格就 commit),老师会检查 commit 内容。

**先自检**:对照 08d §1.1 节奏表确认你的当前位置:

| 检查项 | 判定 |
|---|---|
| **P0 功能数有多少?** | 4-6 / 7-8 / 9+(从 PRD §3 数 P0-* 编号)|
| **每功能用几次 commit?** | 3(推荐:实现 a + 审核 b + 修复 c) / 2(降级:实现 + 合并审修)|
| **Phase 0/1/2/3/6/7/8 commit 都按 08d §1.1 固定段做了吗?** | 应有 16-19 commit · 缺哪一段去补 |

**对照表后再决定补法**:

**情况 A · 你的 P0 ≤ 3 功能 → 标定卡不足,根因不是 commit 数**:
- 回 Phase 0 跑 `/scoping-reviewer 应用修复`,把标定卡 §三 P0 实体属性补到 ≥ 4 个功能
- 重跑相应 Phase 把新 P0 功能实现 + commit(每个 +3 commit)

**情况 B · 你用了"2 commit/功能"降级节奏 + P0 ≤ 6 → 边缘不达标**:
- **升级到 3 commit/功能**:把已合并的 audit+fix commit 之后的功能,改回拆开 commit(`docs(p4-X): R-05+R-06 报告` + `fix(p4-X): apply fixes` 分开)
- 或**做 1-2 个 P1 功能**:每个加 3 commit,自然破 30

**情况 C · 你按 08d §1.1 推荐节奏做了但还是不到 30 → 检查是否漏 commit**:

| 容易漏的 commit | 命令 |
|---|---|
| Phase 1 末「`/rules-updater` 同步 → `/git-committer` 提交」(很多人忘了状态同步)| `chore(rules): Phase 1 末状态同步` |
| Phase 2/3/4/5 末状态同步 | `chore(rules): Phase X 末状态同步` |
| D-01/D-02 排查产生的修复(联调时报错的修复)| `fix(p4-X-debug): <bug 简述>` |
| 三段式提示词演化记录(05 验收 ≥3 个 · 每次演化可独立 commit)| `docs(全 Phase): 三段式提示词演化 - <场景>` |
| README / DEPLOY 文档完善(Phase 8 跑完 readme-writer / deploy-writer 后的修订)| `docs(p8): 完善 README / 完善 DEPLOY` |

**情况 D · 你已经接近 30 但有几次大 commit → 拆细**:
- 把"一个 Phase 1 次大 commit"拆成"多个小 commit"(典型场景:Phase 1 把 `docs(p1)` 大 commit 拆为 `docs(p1): SRS + R-01` + `docs(p1): tech-design + R-02` + `docs(p1): page-prototype + R-02b` 三次)
- 把每次审核后的修复独立 commit(`fix(p4-auth): apply R-05 review issues`)
- 文档完善单独 commit(`docs: improve README quick-start section`)

**底线**:按08d §1.1 的"3 commit/功能 + P0 ≥ 6"组合走,自然 36-41 次,稳过 ≥30 门槛。**不需要刻意补**。

---

#### Q26: 对话记录怎么算"提示词演化 v1-v2-v3"?

**判定标准**:
- v1:你最初的提示词,AI 输出有明显问题
- v2:你**主动**改进提示词(加约束/加上下文/加示例),AI 输出明显变好
- v3:再次改进,接近完美

**例子**:见 §10.4 末尾的"提示词演化:用户注册接口"。

⚠️ **不算演化的情况**:
- 多轮闲聊式追问("再写一段更复杂的"——这是续写,不是演化)
- 同一提示词换不同模型(这是 A/B 测试,不是演化)
- AI 错了你直接告诉它"你错了,改一下"(应该重新写一个完整提示词)

---

#### Q27: AI 编造了一个不存在的 API,怎么发现?

**症状**:
- 代码导入了奇怪的包(`com.foo.bar.WhatEver`)
- 用了 SpringBoot 不存在的 API(如 `@AutoWiredEverything`)
- MyBatis-Plus 用了文档里没的方法

**预防**:
1. CLAUDE.md §一·四(对 AI 的硬约束)中明确写"你不知道的事情请说不知道,不要编造 API"
2. 收到代码后,**关键 import 在官方文档查一下**(SpringBoot/MP/Vue 的官网搜索)
3. mvn compile 不通过/idea 飘红即是信号

**应对**:用 D-01(后端报错)排查模板把报错给 AI,通常 AI 会承认编造并改用真实 API。

---

#### Q28: Phase X 卡住了,模型一直生成不出能用的代码

**思路**:
1. **换模型**:V4 Flash 卡 → 切 `/model opus`(V4 Pro) 试试
2. **换提示词模板**:用 R-XX 审核类追问"这段代码哪里有问题",或用 D-XX 排查类
3. **缩小任务范围**:别一次让 AI 写完整模块,拆成"先写 Service interface" → "再写 ServiceImpl 的某 1 个方法" → "再写 Controller"
4. **手写片段**:AI 卡住时,自己手写关键片段,然后让 AI 补全周边

**终极方案**:QQ 群求助 / 教师邮箱;**不要 5 小时还在和 AI 较劲一段代码**。

#### Q29(V4-D04 新): `course-project-template.zip` 下载不到 / 不会找

**症状**:08b §1.4 要求下载教师 zip,但找不到。

**排查 + 解决**:
1. 看 ✍ **教师指定渠道**(00 总纲 §五 第 7 项填的链接):**QQ 群文件区**(置顶位置)/ **学习通**(课程页 → 资料区)/ **网盘**(教师可能给百度网盘或腾讯文档链接)
2. 都没看到 → 私聊**班长**先问(可能在班级群文件里转发过)
3. 班长也不知道 → **私聊老师 / QQ 群直接问** "请问 course-project-template.zip 在哪下载"

⚠️ **没有这个 zip 不能继续**——它含 `.claude/commands/` 32 个命令 + 根 `CLAUDE.md`(项目宪法) + `.claude/project-status.md`(项目状态),后续所有 `/init-skeleton` `/srs-writer` 等命令都依赖它。

#### Q30(V4-D04 新): zip 解压后 Claude Code 输入 `/` 没有命令补全

**症状**:08b §1.4.4 的验证步骤——在 Claude Code 会话中输入 `/` 但**没有命令下拉**或**只有内置命令**(没有 `/init-skeleton` 等)。

**排查路径**(按顺序试):

1. **查目录是否存在**:在终端 `ls .claude/commands/`(Windows cmd 用 `dir .claude\commands\`)
   - 没有 → 解压时层级错了。回 08b §1.4.2 重新解压(注意**不要把 zip 整体放进去,而是把 zip 内容解压到工作目录**)
   - 有但只有几个 .md → 可能 zip 不完整,重新下载

2. **查文件数**:`.claude/commands/` 下应有**32 个 .md** + 1 个 README.md
   - 不到 32 个 → zip 残缺,重新下载并解压

3. **重启 Claude Code**:退出 `claude`(Ctrl+D 或输 `/exit`)→ 在项目根目录重新运行 `claude`(让 Claude Code 重新扫描 `.claude/commands/`)

4. **检查工作目录**:终端 `pwd` 应显示 `D:\code\<学号>-<题号>\`(精确到这一层 · 项目根)
   - 不对 → `cd /d D:\code\<学号>-<题号>\` 再运行 `claude`(Claude Code 启动时识别的就是**当前目录**作为项目根 · 父目录或子目录都会找不到 `.claude/commands/`)

5. **检查 cc-switch 路由是否就绪**:任务栏看 cc-switch 图标 · 设置 → 路由 → 确认 Claude 路由 ✅ 开启 + 本地代理 `http://127.0.0.1:15721` 运行中(详见 08a §11.4)

6. 还不行 → QQ 群求助,贴出:① `ls .claude/commands/` 的截图(显示 32 个 .md)② Claude Code 输入 `/` 后的截图(显示无补全)③ `claude --version` 的输出 ④ cc-switch 设置 → 路由的截图

#### Q31(V4-D04 新): 解压 zip 后中文文件名乱码 / 路径出现"问号"

**症状**:解压 `course-project-template.zip` 后,某些文件名变成 `???.md` 或乱码字符(如 `é»˜è®¤`),无法打开。

**原因**:Windows 默认解压程序对 UTF-8 文件名的兼容性差(老版 Windows 11 / 7-Zip 旧版本)。

**解决方案**:

1. **优先用 7-Zip 25.x 最新版**(`7zip.org`):右键 zip → "7-Zip" → "解压到 `<学号>-<题号>\`"
2. **WinRAR 6.x+**:同样能正常处理 UTF-8(老版本不行)
3. **Bandizip**:免费且 UTF-8 支持好
4. **避免用**:Windows 资源管理器内置解压(老 Windows 上中文乱码)

如果已经解压乱码:删除目录,换 7-Zip 重新解压。

#### Q32(V4-D04 新): 教师 totorocoder 没被加进我的仓库 / 加错了

**症状**:08b §1.3 步骤 3 应把教师 `totorocoder` 加为开发者,但加错或没加。

**修复步骤**:

1. 进入你的仓库页面(`https://gitee.com/<高校版组织名>/<学号>-<题号>`)
2. 左侧/顶部 → **「管理」** 或 **「成员」** 页签 → **「添加成员」**
3. 搜索 `totorocoder` → 选中 → **权限选「开发者」** → 确认添加
4. 验证:成员列表应能看到 `totorocoder`,标注权限"开发者"

如果搜不到 `totorocoder`:核对教师 Gitee 账号(00 总纲 §五 第 6 项)是否真的是这个;不是的话用教师正确的账号。

---

#### Q33(Phase 7 新): R-07 为什么要拆成 Backend + Frontend 两段跑?

**为啥不能一次跑全部**:R-07 审**跨模块/跨层/端到端横切问题**,扫描深度 + 报告跨度都比 R-05/R-06 大。一次跑全栈会造成两个问题:① AI 上下文超载 → 漏审 issue ② 单份 review.md 横跨前后端 → refactor-helper 应用修复时定位混乱。

**正确做法**:`/code-reviewer-full 范围=Backend 模块=<X>` 跑后端 → `/code-reviewer-full 范围=Frontend 业务流=<Y>` 跑前端。**两份独立 review.md**:`Phase7-R07-Backend-review-<日期>.md` + `Phase7-R07-Frontend-review-<日期>.md`。大项目可再拆 Util 段(`范围=Util`)。**禁止 AI 自决挑模块**——必须显式传 `范围=` + 子参数。详见 `code-reviewer-full.md`(权威源)。

---

#### Q34(Phase 7 新): refactor-helper 跟 perf-optimizer 哪个先跑? 同一个 issue 都能改吗?

**横向协同分工**:**refactor-helper 改结构**(抽方法/拆 Service/抽常量/卫语句)· **perf-optimizer 改性能**(加索引/改分页/路由懒加载/加缓存)· **同 issue 不同时调**——一个 issue 落到结构问题就 refactor-helper · 落到性能问题就 perf-optimizer。

**特殊情况**:若一个 issue 既是结构问题又是性能问题(如"Service 方法 80 行 + 含 N+1 查询"),**先 refactor 改结构**(让方法清晰可读)**再 perf 改性能**(N+1 优化)。两次小步迭代 + 两次 commit。详见 `refactor-helper.md` / `perf-optimizer.md`(权威源)。

---

#### Q35(Phase 7 新): 为什么 R-08 安全审核不审 CSRF?

**根本原因**:本项目采用 **JWT Bearer Token 模式**(token 放 `Authorization: Bearer <jwt>` 请求头 · 不在 Cookie),浏览器**不会自动携带** token 跨站发请求 → 跨站请求伪造(CSRF)在协议层就被天然规避。

**对比**:传统 Session+Cookie 模式才需要 CSRF Token / SameSite Cookie 等防护;JWT Bearer 模式删 CSRF 不是简化也不是疏漏 · 是工程现实。

**保留教学说明**:04 §二 2.7 + 06 R-08 模板均保留"为什么不审 CSRF"教学段(让学生理解协议差异),但 R-08 实操**不**对 CSRF 出 issue。详见 `security-reviewer.md` ⚠️ 不审 CSRF 段(权威源)。

---

#### Q36(Phase 8 新): deploy-writer 跟 readme-writer 顺序为啥不能反?

**硬约束**:**deploy-writer 必须先于 readme-writer**。原因:readme-writer 的 §6 快速开始 + §7 文档索引会**引用** `docs/DEPLOY.md` 的章节 / 命令。先跑 readme-writer → README 引用还不存在的 DEPLOY.md → 学生看到 README 里指向"docs/DEPLOY.md §3"但文件还是空白占位。

**记忆口诀**:**部署在前 · 索引在后**(部署文档先成型,README 才能在文档索引节里引用它)。详见 `deploy-writer.md` / `readme-writer.md` 角色定位(权威源)。

---

#### Q37(Phase 8 新): readme-writer 10 节 / deploy-writer 8 节是怎么算的?

**双胞胎模式约定**:init-skeleton(Phase 0)生成的占位章节 + Phase 8 完善器追加的章节 = 完整版。

**README.md(10 节)**:
- 占位 8 节(init-skeleton.md L277-345 一致):① 项目简介 ② 技术栈 ③ 项目结构 ④ 数据库设计 ⑤ API 接口 ⑥ 快速开始 ⑦ 文档索引 ⑧ 验收清单
- Phase 8 追加 2 节(readme-writer):⑨ AI 协作 ⑩ 联系方式
- 学生 ✍ 占位字段:P0/P1/P2 完成度 / 学号 / 班级 / 邮箱(§十 含隐私权衡说明)
- §九 AI 协作演化记录数 ≥ 3 个 → 引用 `ai-records/` 演化档(对齐 init-skeleton .gitkeep + prompt-evolver §10.3)

**DEPLOY.md(8 节)**:
- 占位 5 节(init-skeleton.md L217-227 一致):① 部署架构 ② 环境要求 ③ 部署步骤 ④ 启动验证 ⑤ 故障排查
- Phase 8 追加 3 节(deploy-writer):⑥ 默认账号密码 ⑦ 安全检查清单(对齐 R-08 维度 5/7)⑧ 云服务器进阶

**双胞胎硬约束**:占位章节顺序 + 节名 100% 对齐 init-skeleton.md(**不重命名 / 不重排 / 不删减**)。详见 `readme-writer.md` / `deploy-writer.md`(权威源)。

---

#### Q38(Phase 0 新 · 2026-05-10): R-00 标定卡审核改了 P0/P1/P2 划分,答辩时怎么解释?

**前提**:Phase 0 末跑 `/scoping-reviewer` 审 + 修标定卡后,`docs/对话记录/Phase0-R00-scoping-review-<日期>.md` 末尾"修改决策记录"段已含每条调整的"原标定 / 调整后 / 理由"。

**答辩素材直接念**:
> "我用 AI 在项目源头跑了一次 R-00 标定卡审核,发现教师标定卡有 X 个 issue:① 模块 A 缺 BBB 功能(领域必备 · 漏了无法跑通主流程)② P1 的 CCC 功能其实是 P0 主业务的依赖项(应该升级到 P0)③ ......。我**主动修正了标定 → 调整后 PRD 全量设计跟实际业务对齐**。这是企业开发的需求二次评审环节(不是擅自改教师标定 · 是质量把关)。"

**评委反馈预期**:
- ✅ **加分点**:学生展示了"主动审视上游设计 + 用 AI 工具落地修正 + 答辩时用工程话术解释"——这正是 V4-2 评分的 AI 协作能力(20 分)和理解度(40 分)的交集得分项
- ❌ **不会扣分**:03-选题库-学生标定卡-母版.md 顶部 § 七 P0 调整审计要求 已明示"本地副本可调整 P1/P2 数量 · 答辩说清楚理由即可"

**答辩问答示例**:
- 问:"你为什么把 X 从 P1 升到 P0 了?教师不是说 X 是 P1 吗?"
- 答:"我用 R-00 审核发现 X 其实是 P0 业务 Y 的前置依赖 —— 没有 X 就跑不通 Y 的主流程。所以我把它升级到 P0。Phase0-R00-scoping-review.md 报告的修改决策记录 1 有完整理由记录,答辩前可以一起翻阅。"

**忌讳点**:**不要**说"我觉得 X 应该是 P0"(主观判断)· **要**说"R-00 5 维度审核(功能完整性/合理性/角色/闭环/划分)发现 X 是依赖项"(工程理由 · 引用审核维度)。

#### Q39(Phase 1 新 · 2026-05-10): 为什么设计阶段(PRD/数据库/API)要 P0+P1+P2 全量设计? 不是先做完 P0 再说吗?

**短答**:**设计阶段全量 + 实现阶段选做** —— 跟企业真实开发一致。

**为啥不是"先设计 P0 · 后续要加 P1 再补设计"**(原 V1 模式 · 已废止):
- 学生 P0 完成后想加 P1 的某个功能,需要回头改 4 份设计文档 + 重审:
  - 改 PRD §3 把 P1 从 3 字段升到 7 字段 → R-01 重审
  - 改 TECH_DESIGN §3 路由加 P1 路由 → R-02 重审(若 P1 路由对应 §6 页面有改动 → R-02b 也要重审 · 2026-05-12 新增)
  - 改 DATABASE_DESIGN §2 表清单加 P1 表 → R-03 重审
  - 改 API_DESIGN §2 接口表加 P1 接口 → R-04 重审
  - 改完才能进 Phase 4-5 加新代码 + R-05/R-06 审新代码
  - **总计:4 个 reviewer 重跑 + 4 个应用修复 ≈ 2-3h** —— 重审痛点

**全量设计模式的工程价值**:
1. **设计成本低**(文字 + 表格 · 30-60 min)vs **实现成本高**(代码 + 测试 + 审核 · 30h+) —— 设计一次到位最划算
2. **企业实践**:瀑布也好敏捷也好,设计文档(SRS/数据库/API)从来都是**完整需求映射** · sprint backlog/ 实现迭代才按优先级分阶段
3. **答辩讲故事**:学生说"我设计了 X+Y+Z 全量需求 · 因 3 周时间限制只实现了 P0 + 1 个 P1 · 剩余按 PRD 已设计可未来扩展"——这是工程师范的回答 · 比"我只做了 P0 没考虑后续"专业得多
4. **R-00 + R-01-R-04 不重审**:Phase 0 R-00 已审标定卡 · Phase 1-3 R-01-R-04 已审全量设计 · 学生 Phase 4-5 加代码不必回头重审设计

**实现阶段优先级实战**:
- 第一轮 Phase 4-5 实现所有 P0(对应 PRD §3 「实现优先级 = P0」的功能 / DATABASE §2 P0 表 / API §2 P0 接口 / TECH §3 P0 路由)→ P0 端到端跑通 → 60 分基础已稳
- 第二轮(若有时间)实现 P1(70-80 分)
- 第三轮(锦上添花)实现 P2(85+ 加分)

**忌讳点**:不要 P0 没跑通就分散精力做 P1 —— 严格按"P0 全部跑通 → 再加 P1"顺序。

---

