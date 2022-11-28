package com.ttyang.yourspan.pojo;

import lombok.Data;

/**
 * @author ttyang
 * @version 1.0
 * @since 2022-11-16
 */
@Data
public class LoginForm {
    private String userId;
    private String password;
    private String verifyCode;
}
