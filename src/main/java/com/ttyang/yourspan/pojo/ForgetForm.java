package com.ttyang.yourspan.pojo;

import lombok.Data;

/**
 * @author ttyang
 * @version 1.0
 * @since 2022-11-17
 */
@Data
public class ForgetForm {
    private String email;
    private String password;
    private Integer eVerifyCode;
}
