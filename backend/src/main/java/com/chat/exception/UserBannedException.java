package com.chat.exception;

public class UserBannedException extends BusinessException {

    public UserBannedException() {
        super(403, "账号已被封禁");
    }

    public UserBannedException(String reason) {
        super(403, reason == null || reason.isBlank() ? "账号已被封禁" : "账号已被封禁：" + reason);
    }
}
