package com.ttyang.yourspan.util;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author ttyang
 * @version 1.0
 * @since 2022-11-16
 */
@AllArgsConstructor
@Getter
public enum ResultEnum {
    /**
     * SUCCESS          客户端发送的请求得到服务端的许可
     * FAIL             客户端发送的请求得到服务端的否定
     * CODE_ERROR       客户端发送给服务端的登录验证码或者注册邮箱验证码错误
     * LOGIN_ERROR      客户端输入的用户名或密码错误
     * TOKEN_ERROR      客户端对应的token过期
     * TIME_OUT         客户端的请求处理超时
     * EMAIL_ERROR      用于注册的邮箱已被注册
     * EMAIL_NOT_VALID  用于找回密码的邮箱不合法(未注册)
     */
    SUCCESS(200,"成功"),
    FAIL(201,"失败"),
    CODE_ERROR(203,"验证码错误"),
    LOGIN_ERROR(204,"用户名或密码错误"),
    TOKEN_ERROR(205,"token失效"),
    TIME_OUT(206,"会话已过期，请重新登陆"),
    EMAIL_NOT_VALID(207,"该邮箱未注册"),
    EMAIL_ERROR(207,"该邮箱已被注册");
    private final Integer code;
    private final String message;
}
