package com.ttyang.yourspan.controller;

import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ttyang.yourspan.pojo.User;
import com.ttyang.yourspan.service.UserService;
import com.ttyang.yourspan.util.MyJwtTool;
import com.ttyang.yourspan.util.Result;
import com.ttyang.yourspan.util.ResultEnum;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

/**
 * @author ttyang
 * @version 1.0
 * @since 2022-11-17
 */
@Api(tags = "用户控制器")
@RestController
@RequestMapping("/sms/userController")
@CrossOrigin
public class UserController {
    @Autowired
    private UserService userService;

    @ApiOperation("修改密码接口")
    @PostMapping("/resetPwd/{oldPwd}/{newPwd}")
    public Result<?> resetPwd(@ApiParam("token") @RequestHeader String token, @ApiParam("旧密码") @PathVariable("oldPwd") String oldPwd, @ApiParam("新密码") @PathVariable("newPwd") String newPwd) {
        // 若token已过期
        if (MyJwtTool.isValidToken(token)) {
            return Result.build(null, ResultEnum.TOKEN_ERROR);
        }
        oldPwd = DigestUtil.md5Hex(oldPwd);
        newPwd = DigestUtil.md5Hex(newPwd);
        Integer uid = MyJwtTool.getUidFromToken(token);
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        assert uid != null;
        queryWrapper.eq("uid", uid);
        queryWrapper.eq("password", oldPwd);
        User user = userService.getOne(queryWrapper);
        if (user != null) {
            user.setPassword(newPwd);
            userService.saveOrUpdate(user);
        } else {
            return Result.fail().message("原密码有误，请重试");
        }
        return Result.ok();
    }

    @ApiOperation("修改个人信息接口")
    @PostMapping("/saveOrUpdate")
    public Result<?> saveOrUpdate(@ApiParam("JSON格式的User对象") @RequestBody User user) {
        userService.saveOrUpdate(user);
        return Result.ok();
    }

    @ApiOperation("上传头像接口")
    @PostMapping("/avatarUpload")
    public Result<?> avatarUpload(@ApiParam("头像文件") @RequestPart("multipartFile") MultipartFile multipartFile) {
        String headPath = "E:/MyIdeaProjects/yourspan/target/classes/public/upload/";
        String fileName = IdUtil.simpleUUID()
                .toLowerCase()
                .concat(Objects.requireNonNull(multipartFile.getOriginalFilename()).substring(multipartFile.getOriginalFilename().lastIndexOf(".")));
        String avatarPath = headPath.concat(fileName);
        try {
            multipartFile.transferTo(new File(avatarPath));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Result.ok("upload/".concat(fileName));
    }
}
