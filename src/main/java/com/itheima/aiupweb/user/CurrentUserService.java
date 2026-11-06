package com.itheima.aiupweb.user;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CurrentUserService {

    private final UserService userService;

    public CurrentUserService(UserService userService) {
        this.userService = userService;
    }

    public Optional<User> currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        return userService.findByUsername(authentication.getName());
    }

    public User requireCurrentUser() {
        return currentUser().orElseThrow(() -> new IllegalStateException("当前请求未登录"));
    }

    public Long requireCurrentUserId() {
        return requireCurrentUser().getId();
    }
}
