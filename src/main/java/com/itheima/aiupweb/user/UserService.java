package com.itheima.aiupweb.user;

import com.itheima.aiupweb.common.BusinessException;
import com.itheima.aiupweb.common.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(RegisterForm form) {
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            throw new BusinessException("两次输入的密码不一致");
        }
        String username = form.getUsername().trim();
        String email = form.getEmail().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new BusinessException("用户名已被占用");
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException("邮箱已被注册");
        }
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setDisplayName(form.getDisplayName().trim());
        user.setPasswordHash(passwordEncoder.encode(form.getPassword()));
        user.setRole(Role.USER);
        user.setEnabled(true);
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User requireByUsername(String username) {
        return userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new NotFoundException("用户不存在"));
    }

    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return Optional.empty();
        }
        return userRepository.findByUsernameIgnoreCase(username);
    }

    @Transactional(readOnly = true)
    public User requireById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("用户不存在"));
    }

    @Transactional(readOnly = true)
    public Page<User> search(String query, Pageable pageable) {
        if (StringUtils.hasText(query)) {
            return userRepository.search(query.trim(), pageable);
        }
        return userRepository.findAll(pageable);
    }

    @Transactional
    public void toggleEnabled(Long userId, Long operatorId) {
        if (userId.equals(operatorId)) {
            throw new BusinessException("不能禁用自己的账号");
        }
        User user = requireById(userId);
        if (user.isAdmin() && user.isEnabled()
                && userRepository.countByRoleAndEnabledTrue(Role.ADMIN) <= 1) {
            throw new BusinessException("至少需要保留一个可用的管理员");
        }
        user.setEnabled(!user.isEnabled());
    }

    @Transactional
    public User createSeedUser(String username, String email, String displayName, String rawPassword, Role role) {
        return userRepository.findByUsernameIgnoreCase(username).orElseGet(() -> {
            User user = new User();
            user.setUsername(username);
            user.setEmail(email.toLowerCase(Locale.ROOT));
            user.setDisplayName(displayName);
            user.setPasswordHash(passwordEncoder.encode(rawPassword));
            user.setRole(role);
            user.setEnabled(true);
            return userRepository.save(user);
        });
    }

    @Transactional(readOnly = true)
    public long countUsers() {
        return userRepository.count();
    }

    @Transactional(readOnly = true)
    public long countEnabledUsers() {
        return userRepository.countByEnabledTrue();
    }
}
