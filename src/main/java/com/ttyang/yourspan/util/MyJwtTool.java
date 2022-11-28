package com.ttyang.yourspan.util;

import cn.hutool.jwt.JWT;

/**
 * @author ttyang
 * @version 1.0
 * @since 2022-11-17
 */
public class MyJwtTool {
    private static final byte[] KEY = "1234567890".getBytes();

    public static String createTokenByUid(Integer uid) {
        return JWT.create()
                .setPayload("uid", uid)
                .setKey(KEY)
                .sign();
    }

    public static Integer getUidFromToken(String token) {
        return (Integer) JWT.of(token).getPayload("uid");
    }

    public static boolean isValidToken(String token) {
        return JWT.of(token).validate(0);
    }
}
