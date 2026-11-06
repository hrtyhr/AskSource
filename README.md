# AskSource

> 让每个技术答案，都有出处。

集成大语言模型的技术交流平台：论坛发布、检索、管理，以及基于社区内容的 RAG 助手（语义搜索、多轮流式问答、帖子 AI 导读）。

## 技术栈

- Spring Boot 4.1.2 / Java 17
- Spring MVC + Thymeleaf + 原生 CSS/JS
- Spring Security（Session、BCrypt、CSRF）
- Spring Data JPA + Flyway
- MySQL 8.4 + Redis Stack（RediSearch / RedisJSON）
- Spring AI 2.0.2（OpenAI 兼容协议）
- 百炼 `qwen-plus` + `text-embedding-v4`（1024 维）

## 环境要求

- JDK 17 或更高版本
- Docker Desktop（含 Compose）
- 可选：阿里云百炼 API Key

## 启动

1. 启动 MySQL 与 Redis Stack：

```bash
docker compose up -d
```

容器就绪后，MySQL 暴露在 `localhost:3307`，Redis 暴露在 `localhost:6380`

2. 启动应用：

无 Key 模式（论坛完整可用，AI 自动降级为关键词检索）：

```bash
./mvnw spring-boot:run
```

启用真实 AI（bash）：

```bash
DASHSCOPE_API_KEY=sk-your-key ./mvnw spring-boot:run
```

PowerShell：

```powershell
$env:DASHSCOPE_API_KEY="sk-your-key"
./mvnw spring-boot:run
```

3. 访问 `http://localhost:8080`。

## 演示账号

| 账号 | 密码 | 角色 |
|---|---|---|
| `admin` | `Admin@123456` | 管理员 |
| `demo` | `Demo@123456` | 普通用户 |
| `alice` / `bob` / `carol` | `Demo@123456` | 普通用户 |

首次启动会自动写入 4 个分类、8 篇帖子及回复。可通过环境变量修改初始密码：

- `ADMIN_PASSWORD`
- `DEMO_PASSWORD`

生产环境请关闭演示数据：`SEED_ENABLED=false`。

## 配置项

| 环境变量 | 默认值 | 说明 |
|---|---|---|
| `MYSQL_URL` | `jdbc:mysql://localhost:3307/aiupweb...` | 数据库连接 |
| `MYSQL_USERNAME` | `aiupweb` | 数据库用户 |
| `MYSQL_PASSWORD` | `aiupweb123` | 数据库密码 |
| `REDIS_HOST` | `localhost` | Redis 地址 |
| `REDIS_PORT` | `6380` | Redis 端口 |
| `DASHSCOPE_API_KEY` | 未配置 | 百炼 API Key |
| `DASHSCOPE_BASE_URL` | 百炼兼容模式地址 | OpenAI 兼容端点 |
| `DASHSCOPE_CHAT_MODEL` | `qwen-plus` | 对话模型 |
| `DASHSCOPE_EMBEDDING_MODEL` | `text-embedding-v4` | 向量模型 |
| `AI_ENABLED` | `true` | AI 总开关 |
| `AI_INDEXING_ENABLED` | `true` | 发帖/回复自动索引 |
| `SEED_ENABLED` | `true` | 演示数据开关 |
| `SERVER_PORT` | `8080` | 应用端口 |

API Key 只通过环境变量注入，不要写入代码或提交到仓库。

## AI 工作方式

1. 发帖或回复提交后，应用发布领域事件；事务提交后异步切片、向量化并写入 Redis。
2. 问答时按语义检索帖子与回复，拼接最近对话历史和来源上下文，通过 SSE 流式返回。
3. 每条回答附带服务端生成的来源链接；模型被要求只依据社区资料回答。
4. 帖子详情页可生成 AI 导读，结果缓存；回复变化时缓存自动失效。
5. 未配置 Key 时：
   - `/ai/search` 自动展示关键词结果并标注降级；
   - AI 问答输入禁用，直接请求 SSE 会返回降级提示；
   - 索引状态记录为 `SKIPPED`，配置 Key 后重启会自动补偿索引。

管理员可在 `/admin` 查看索引状态并触发全量重建。

## 测试

```bash
./mvnw test
```

测试使用 H2 与 mock 配置，不依赖 MySQL、Redis 或真实模型。覆盖切片、Markdown 清洗、越权、CSRF、管理权限、AI 降级与注册校验等。

## 构建

```bash
./mvnw clean package
java -jar target/AskSource-0.0.1-SNAPSHOT.jar
```

## 重置本地数据

```bash
docker compose down -v
docker compose up -d
```

该命令会删除 MySQL 与 Redis 的本地数据卷，仅在确认无需保留数据时使用。


