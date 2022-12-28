package com.ttyang.yourspan.pojo;

import lombok.Data;

/**
 * @author ttyang
 * @version 1.0
 * @since 2022-11-16
 */
@Data
public class LoginForm {
    /**
     * 登录表单项——用户id
     */
    private String userId;
    /**
     * 登录表单项——密码
     */
    private String password;
    /**
     * 登录表单项——验证码
     */
    private String verifyCode;
}
