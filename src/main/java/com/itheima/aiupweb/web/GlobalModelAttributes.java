package com.itheima.aiupweb.web;

import com.itheima.aiupweb.common.BusinessException;
import com.itheima.aiupweb.common.NotFoundException;
import com.itheima.aiupweb.forum.Category;
import com.itheima.aiupweb.forum.CategoryService;
import com.itheima.aiupweb.user.CurrentUserService;
import com.itheima.aiupweb.user.CurrentUserView;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;

@ControllerAdvice
public class GlobalModelAttributes {

    private final CurrentUserService currentUserService;
    private final CategoryService categoryService;

    public GlobalModelAttributes(CurrentUserService currentUserService, CategoryService categoryService) {
        this.currentUserService = currentUserService;
        this.categoryService = categoryService;
    }

    @ModelAttribute("currentUser")
    public CurrentUserView currentUser() {
        return currentUserService.currentUser().map(CurrentUserView::new).orElse(null);
    }

    @ModelAttribute("isAdmin")
    public boolean isAdmin() {
        return currentUserService.currentUser().map(user -> user.isAdmin()).orElse(false);
    }

    @ModelAttribute("navCategories")
    public java.util.List<Category> navCategories() {
        return categoryService.listAll();
    }

    @ModelAttribute("currentPath")
    public String currentPath(HttpServletRequest request) {
        return request.getRequestURI();
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound() {
        return "error/404";
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleAccessDenied() {
        return "error/403";
    }

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleBusiness(BusinessException exception, org.springframework.ui.Model model) {
        model.addAttribute("errorMessage", exception.getMessage());
        return "error/400";
    }
}
