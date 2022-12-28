package com.ttyang.yourspan.pojo;

import lombok.Data;

/**
 * @author ttyang
 * @version 1.0
 * @since 2022-11-17
 */
@Data
public class ForgetForm {
    /**
     * 忘记密码表单项——邮箱
     */
    private String email;
    /**
     * 忘记密码表单项——密码
     */
    private String password;
}
