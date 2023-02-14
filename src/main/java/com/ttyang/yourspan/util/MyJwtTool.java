package com.ttyang.yourspan.util;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUtil;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTValidator;

/**
 * @author ttyang
 * @version 1.0
 * @since 2022-11-17
 */
public class MyJwtTool {
    private static final byte[] KEY = "3BcKvp0FhPEzXrIJ".getBytes();

    public static String createTokenByUid(Integer uid) {
        return JWT.create()
                .setPayload("uid", uid)
                .setKey(KEY)
                .setExpiresAt(DateUtil.offset(DateUtil.date(), DateField.HOUR, 1))
                .sign();
    }

    public static String createTokenByFid(Integer fid) {
        return JWT.create()
                .setPayload("fid", fid)
                .setKey(KEY)
                .setExpiresAt(DateUtil.offset(DateUtil.date(), DateField.HOUR, 1))
                .sign();
    }

    public static Integer getUidFromToken(String token) {
        return (Integer) JWT.of(token).getPayload("uid");
    }

    public static Integer getFidFromToken(String token) {
        return (Integer) JWT.of(token).getPayload("fid");
    }

    public static boolean isNotValidToken(String token) {
        try {
            JWTValidator.of(token).validateDate(DateUtil.date());
        } catch (Exception e) {
            return true;
        }
        return false;
    }

}
