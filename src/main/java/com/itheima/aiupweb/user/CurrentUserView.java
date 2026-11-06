package com.itheima.aiupweb.user;

public class CurrentUserView {

    private final String username;
    private final String displayName;
    private final boolean admin;

    public CurrentUserView(User user) {
        this.username = user.getUsername();
        this.displayName = user.getDisplayName();
        this.admin = user.isAdmin();
    }

    public String getUsername() {
        return username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isAdmin() {
        return admin;
    }
}
