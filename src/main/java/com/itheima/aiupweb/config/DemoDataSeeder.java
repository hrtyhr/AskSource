package com.itheima.aiupweb.config;

import com.itheima.aiupweb.forum.Category;
import com.itheima.aiupweb.forum.CategoryRepository;
import com.itheima.aiupweb.forum.Post;
import com.itheima.aiupweb.forum.PostForm;
import com.itheima.aiupweb.forum.PostRepository;
import com.itheima.aiupweb.forum.PostService;
import com.itheima.aiupweb.forum.ReplyForm;
import com.itheima.aiupweb.user.Role;
import com.itheima.aiupweb.user.User;
import com.itheima.aiupweb.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
@Order(10)
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final UserService userService;
    private final CategoryRepository categoryRepository;
    private final PostRepository postRepository;
    private final PostService postService;
    private final String adminPassword;
    private final String demoPassword;

    public DemoDataSeeder(UserService userService, CategoryRepository categoryRepository,
                          PostRepository postRepository, PostService postService,
                          @Value("${app.demo.admin-password}") String adminPassword,
                          @Value("${app.demo.password}") String demoPassword) {
        this.userService = userService;
        this.categoryRepository = categoryRepository;
        this.postRepository = postRepository;
        this.postService = postService;
        this.adminPassword = adminPassword;
        this.demoPassword = demoPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (postRepository.count() > 0) {
            return;
        }

        User admin = userService.createSeedUser("admin", "admin@aiupweb.local", "管理员", adminPassword, Role.ADMIN);
        User demo = userService.createSeedUser("demo", "demo@aiupweb.local", "演示用户", demoPassword, Role.USER);
        User alice = userService.createSeedUser("alice", "alice@aiupweb.local", "Alice", demoPassword, Role.USER);
        User bob = userService.createSeedUser("bob", "bob@aiupweb.local", "Bob", demoPassword, Role.USER);
        User carol = userService.createSeedUser("carol", "carol@aiupweb.local", "Carol", demoPassword, Role.USER);

        Category java = category("Java 后端", "java", "JVM、并发与后端工程实践", 1);
        Category spring = category("Spring 生态", "spring", "Spring Boot、Security 与数据访问", 2);
        Category ai = category("AI 与大模型", "ai-llm", "RAG、Agent 与模型应用开发", 3);
        Category ops = category("工具与运维", "devops", "Docker、数据库与可观测性", 4);

        List<Post> posts = new ArrayList<>();

        posts.add(postService.create(demo.getId(), form(spring, "Spring Boot 4 项目中如何组织模块化单体？",
                """
                最近在重构一个社区项目，业务包含用户、论坛和 AI 助手三个部分。团队希望保持单体部署，但又不想让包结构变成一锅粥。

                我的做法是按业务特性分包，每个模块只暴露 Service 接口，跨模块通过领域事件通信。例如发帖成功后发布 `PostContentChangedEvent`，AI 模块在事务提交后异步建立向量索引。

                ```java
                eventPublisher.publishEvent(new PostContentChangedEvent(post.getId()));
                ```

                大家在实际项目里会怎么划分模块边界？是否值得为每个模块建立独立的 facade 接口？
                """, "Spring Boot, 架构设计, 领域事件")));

        posts.add(postService.create(alice.getId(), form(ai, "RAG 检索效果不稳定，切片策略应该怎么调？",
                """
                我在做技术社区问答，帖子正文大约 800 到 3000 字，回复长短不一。目前按 800 字切片、120 字重叠，召回结果有时会漏掉关键段落。

                尝试过两种方案：

                1. 纯固定长度切片，简单但容易切断代码块。
                2. 按段落聚合，保留语义完整性，但切片长度不均匀。

                现在的折中方案是优先在段落边界切分，找不到段落再按句子切分。相似度阈值先设为 0.25，topK 取 8，去重后保留 6 条来源。

                有没有更系统的评估方法？除了人工看结果，是否值得准备一套小规模评测集？
                """, "RAG, 向量检索, 切片")));

        posts.add(postService.create(bob.getId(), form(ai, "百炼 text-embedding-v4 维度选择和索引迁移经验",
                """
                百炼的 `text-embedding-v4` 支持不同输出维度。我们项目先用了 1024 维，Redis 索引通过 `FT.CREATE` 建立 HNSW 结构。

                这里有一个容易踩坑的点：Spring AI 的 `EmbeddingModel.dimensions()` 默认可能会调用远程接口。启动时如果 Redis 需要初始化 schema，就会在无 API Key 的环境下直接失败。

                我的处理方式是包一层固定维度的 `EmbeddingModel`，让索引初始化只依赖本地配置：

                ```java
                public int dimensions() {
                    return configuredDimensions;
                }
                ```

                如果后续更换 embedding 模型，索引维度变化必须重建索引，否则查询会直接报维度不匹配。
                """, "Spring AI, 向量检索, Redis")));

        posts.add(postService.create(carol.getId(), form(java, "虚拟线程适合替换哪些类型的线程池？",
                """
                JDK 21 的虚拟线程已经比较成熟。我们把文件导出任务从固定线程池换成了虚拟线程执行器，阻塞 IO 场景下吞吐有提升。

                但目前不会盲目替换所有线程池：

                - CPU 密集型任务仍使用固定大小平台线程池。
                - 有严格并发上限的外部 API 调用仍需要信号量或限流。
                - 某些依赖 `ThreadLocal` 的旧组件需要先验证兼容性。

                ```java
                try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                    executor.submit(() -> exportReport());
                }
                ```

                大家在生产环境遇到过哪些虚拟线程相关的坑？
                """, "Java, 并发, 虚拟线程")));

        posts.add(postService.create(demo.getId(), form(spring, "Spring Security 表单登录与 SSE 的 CSRF 配置",
                """
                项目使用服务端渲染 + 原生 JavaScript。流式问答通过 `fetch` 接收 SSE，结果发现普通的表单都有 CSRF token，但 JavaScript 请求默认没有带。

                我的实现是从页面 meta 标签读取 token：

                ```html
                <meta name="_csrf" th:content="${_csrf.token}">
                <meta name="_csrf_header" th:content="${_csrf.headerName}">
                ```

                发起请求时设置 `X-CSRF-TOKEN` 请求头。注意 CSRF token 不要放在 URL 查询参数中，否则容易被日志或 Referer 泄露。

                另外 SSE 是 POST 请求，和 EventSource 的 GET 用法不同，这里需要手动解析流式响应。
                """, "Spring Security, CSRF, SSE")));

        posts.add(postService.create(alice.getId(), form(ops, "MySQL 社区帖子表的分页查询优化记录",
                """
                帖子列表页支持分类、标签和关键词筛选，数据量到百万级后 `LIKE '%关键词%'` 明显变慢。

                第一阶段做了这些改动：

                - 为 `(status, created_at)` 建联合索引，覆盖公开列表的默认排序。
                - 为 `(category_id, status)` 建联合索引，减少分类筛选扫描。
                - 标签筛选改为 `EXISTS` 子查询，避免多表 JOIN 后的重复行。

                关键词搜索目前仍是 `LIKE`，因为中文分词和全文索引需要额外配置。下一步计划接入 OpenSearch 或 MySQL ngram 全文索引，并保留向量检索作为语义补充。

                如果只需要在 MVP 中支持中文关键词，大家会优先选择 ngram 还是外部搜索引擎？
                """, "MySQL, 索引, 性能优化")));

        posts.add(postService.create(bob.getId(), form(ops, "Docker Compose 本地开发环境健康检查实践",
                """
                社区项目依赖 MySQL 8.4 和 Redis Stack。为了让 `docker compose up -d` 后能直接启动应用，我们给两个服务都加了 healthcheck。

                MySQL 使用 `mysqladmin ping`，Redis 使用 `redis-cli ping`。应用侧仍然需要连接重试，因为 healthcheck 通过不代表数据库已经完成初始化。

                ```yaml
                healthcheck:
                  test: ["CMD-SHELL", "mysqladmin ping -h 127.0.0.1 -uroot -p$$MYSQL_ROOT_PASSWORD --silent"]
                  interval: 5s
                  retries: 30
                ```

                在 CI 中建议使用 Testcontainers 而不是复用本地 Compose，避免测试数据相互污染。
                """, "Docker, MySQL, Redis")));

        posts.add(postService.create(carol.getId(), form(java, "接口耗时突然升高，如何定位是 GC 还是连接池？",
                """
                一次线上排查中，接口 P99 从 200ms 涨到 2s。第一反应是 GC，但开启 GC 日志后发现停顿并不明显。

                后续按这条路径定位：

                1. 查看线程 dump，发现大量线程阻塞在获取数据库连接。
                2. 检查连接池指标，活跃连接数接近上限。
                3. 找到慢 SQL：一个未加索引的模糊查询。
                4. 优化索引并限制并发后恢复。

                经验是不要只看 JVM 指标，连接池、线程池和数据库慢查询要一起看。GC 只是嫌疑之一，不是默认答案。
                """, "Java, JVM, 性能排查")));

        reply(posts.get(0), alice, "模块化单体 + 领域事件是很稳妥的起点。只有在模块需要独立扩缩容或者团队边界非常清晰时，拆分服务才划算。facade 接口建议只暴露真正跨模块调用的能力，不要为了形式完整给每个模块都建。");
        reply(posts.get(0), bob, "我们会把每个模块的 entity 和 repository 设为包内可见，跨模块只能注入 Service。这样编译期就能发现越界依赖，代价是需要多写一些转换代码。");
        reply(posts.get(1), demo, "建议准备 30 到 50 个真实问题，标注期望命中的帖子和关键段落。每次调整切片、topK 或阈值都跑一遍，至少能避免“感觉变好了”的主观判断。");
        reply(posts.get(1), carol, "代码块和表格不要从中间切断。我们的做法是先按 Markdown 结构分块，再对超长块做滑动窗口，并在每一块前面补上帖子标题作为上下文。");
        reply(posts.get(2), alice, "维度变化必须重建索引。如果 Redis 里还是旧维度，查询可能直接报错。可以在索引名称里带上 embedding 模型版本，比如 `aiupweb-v4-1024`，切换时零风险。");
        reply(posts.get(3), bob, "我们只在阻塞 IO 密集且并发量大的地方用虚拟线程。有外部限流要求的地方还是显式用信号量控制，不能把并发交给虚拟线程数量。");
        reply(posts.get(4), carol, "补充一点：如果 SSE 请求超时或被取消，要确保释放限流占用的并发名额，否则用户后续请求会一直被拒绝。可以在 `onCompletion`、`onTimeout` 和错误回调里统一清理。");
        reply(posts.get(5), demo, "MVP 阶段可以先上 MySQL ngram，省一套基础设施。等需要复杂相关度排序、同义词和高亮时再迁移搜索引擎，迁移成本主要在建索引和查询语法。");
        reply(posts.get(6), admin, "Compose 适合本地开发，但应用启动前的重试逻辑不能省。健康检查只代表进程可用，Flyway 迁移完成前仍可能连接失败。");
        reply(posts.get(7), demo, "连接池指标很关键，尤其是等待队列长度。只看平均耗时容易被掩盖，P99 和获取连接等待时间往往能更快暴露问题。");

        log.info("已初始化 {} 个演示用户、{} 个分类和 {} 篇帖子", 5, 4, posts.size());
    }

    private Category category(String name, String slug, String description, int sortOrder) {
        return categoryRepository.findBySlug(slug).orElseGet(() -> {
            Category category = new Category();
            category.setName(name);
            category.setSlug(slug);
            category.setDescription(description);
            category.setSortOrder(sortOrder);
            return categoryRepository.save(category);
        });
    }

    private PostForm form(Category category, String title, String content, String tags) {
        PostForm form = new PostForm();
        form.setCategoryId(category.getId());
        form.setTitle(title);
        form.setContent(content.strip());
        form.setTags(tags);
        return form;
    }

    private void reply(Post post, User author, String content) {
        ReplyForm form = new ReplyForm();
        form.setContent(content);
        postService.addReply(post.getId(), author.getId(), form);
    }
}
