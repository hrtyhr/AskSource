package com.itheima.aiupweb.web;

import com.itheima.aiupweb.forum.Category;
import com.itheima.aiupweb.forum.CategoryRepository;
import com.itheima.aiupweb.forum.Post;
import com.itheima.aiupweb.forum.PostRepository;
import com.itheima.aiupweb.forum.PostStatus;
import com.itheima.aiupweb.user.Role;
import com.itheima.aiupweb.user.User;
import com.itheima.aiupweb.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WebSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Category category;

    @BeforeEach
    void setUp() {
        ensureUser("demo", "演示用户", Role.USER);
        ensureUser("alice", "Alice", Role.USER);
        ensureUser("admin", "管理员", Role.ADMIN);
        category = categoryRepository.findBySlug("testing").orElseGet(() -> {
            Category created = new Category();
            created.setName("测试分类");
            created.setSlug("testing");
            created.setDescription("自动化测试使用");
            created.setSortOrder(99);
            return categoryRepository.save(created);
        });
    }

    @Test
    void publicPagesRender() throws Exception {
        mockMvc.perform(get("/")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("AskSource")));
        mockMvc.perform(get("/posts")).andExpect(status().isOk());
        mockMvc.perform(get("/login")).andExpect(status().isOk());
        mockMvc.perform(get("/register")).andExpect(status().isOk());
    }

    @Test
    void loginWithSeededUserSucceeds() throws Exception {
        mockMvc.perform(formLogin("/login").user("demo").password("Demo@123456"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void aiPageRequiresLogin() throws Exception {
        mockMvc.perform(get("/ai"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void aiPageShowsUnavailableWithoutKey() throws Exception {
        mockMvc.perform(get("/ai").with(user("demo").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("DASHSCOPE_API_KEY")));
    }

    @Test
    void semanticSearchFallsBackToKeywordWithoutKey() throws Exception {
        createPost("Redis vector search fallback", "Redis vector index content for fallback test");
        mockMvc.perform(get("/ai/search").param("q", "Redis"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("关键词降级模式")));
    }

    @Test
    void adminPagesRequireAdminRole() throws Exception {
        mockMvc.perform(get("/admin").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/admin").with(user("demo").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void creatingPostRequiresCsrfAndAuthenticatedUser() throws Exception {
        mockMvc.perform(post("/posts").with(user("demo").roles("USER"))
                        .param("categoryId", category.getId().toString())
                        .param("title", "CSRF protected post title")
                        .param("content", "This post must not be created without a CSRF token.")
                        .param("tags", "security"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/posts").with(user("demo").roles("USER")).with(csrf())
                        .param("categoryId", category.getId().toString())
                        .param("title", "Valid post title from test")
                        .param("content", "This post is created by an integration test with CSRF enabled.")
                        .param("tags", "security, test"))
                .andExpect(status().is3xxRedirection());

        assertThat(postRepository.findAll())
                .anyMatch(post -> post.getTitle().equals("Valid post title from test"));
    }

    @Test
    void nonOwnerCannotEditPost() throws Exception {
        Post post = createPost("Ownership test post", "Ownership test content for edit permissions.");
        mockMvc.perform(get("/posts/{id}/edit", post.getId()).with(user("alice").roles("USER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/posts/{id}/edit", post.getId()).with(user("demo").roles("USER")))
                .andExpect(status().isOk());
    }

    @Test
    void registrationValidationRejectsShortUsername() throws Exception {
        mockMvc.perform(post("/register").with(csrf())
                        .param("username", "ab")
                        .param("email", "not-an-email")
                        .param("displayName", "A")
                        .param("password", "short")
                        .param("confirmPassword", "different"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"));
    }

    @Test
    void summaryWithoutKeyRedirectsWithFriendlyMessage() throws Exception {
        Post post = createPost("Summary fallback post", "Content used to validate summary fallback behavior.");
        mockMvc.perform(post("/posts/{id}/summary", post.getId())
                        .with(user("demo").roles("USER")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/posts/" + post.getId()));
    }

    private void ensureUser(String username, String displayName, Role role) {
        User user = userRepository.findByUsernameIgnoreCase(username).orElseGet(User::new);
        user.setUsername(username);
        user.setDisplayName(displayName);
        user.setEmail(username + "@test.local");
        user.setPasswordHash(passwordEncoder.encode("Demo@123456"));
        user.setRole(role);
        user.setEnabled(true);
        userRepository.save(user);
    }

    private Post createPost(String title, String content) {
        User author = userRepository.findByUsernameIgnoreCase("demo").orElseThrow();
        Post post = new Post();
        post.setAuthor(author);
        post.setCategory(category);
        post.setTitle(title);
        post.setContent(content);
        post.setStatus(PostStatus.PUBLISHED);
        return postRepository.save(post);
    }
}
