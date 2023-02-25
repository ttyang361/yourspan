package com.ttyang.yourspan.controller;

import com.ttyang.yourspan.pojo.Capacity;
import com.ttyang.yourspan.pojo.User;
import com.ttyang.yourspan.service.CapacityService;
import com.ttyang.yourspan.service.UserService;
import com.ttyang.yourspan.util.MyJwtTool;
import com.ttyang.yourspan.util.Result;
import com.ttyang.yourspan.util.ResultEnum;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Api(tags = "管理员控制器")
@RestController
@RequestMapping("/sms/adminController")
@CrossOrigin
public class AdminController {
    @Autowired
    private UserService userService;
    @Autowired
    private CapacityService capacityService;

    @ApiOperation("获取所有用户信息接口")
    @GetMapping("/getAllUserInfo")
    public Result<?> getAllUserInfo(@ApiParam("token") @RequestHeader("token") String token) {
        // 判断token是否过期，这里MyJwtTool.isValidToken(token)后续需要注意修改
        if (MyJwtTool.isNotValidToken(token)) {
            return Result.build(null, ResultEnum.TOKEN_ERROR);
        }
        if (!userService.getUserByUid(MyJwtTool.getUidFromToken(token)).getIsAdmin()) {
            return Result.build(null, ResultEnum.NOT_ADMIN);
        }
        List<User> userList = userService.getAllUserInfo();
        List<Capacity> capacityList = capacityService.getAllCapacity();
        Map<String, List<?>> map = new LinkedHashMap<>();
        map.put("userList", userList);
        map.put("capacityList", capacityList);
        return Result.ok(map);
    }

    @ApiOperation("冻结用户接口")
    @PostMapping("/freezeUser")
    public Result<?> freezeUser(@ApiParam("token") @RequestHeader("token") String token, @ApiParam("用户id") @RequestBody String uid) {
        // 判断token是否过期，这里MyJwtTool.isValidToken(token)后续需要注意修改
        if (MyJwtTool.isNotValidToken(token)) {
            return Result.build(null, ResultEnum.TOKEN_ERROR);
        }
        if (!userService.getUserByUid(MyJwtTool.getUidFromToken(token)).getIsAdmin()) {
            return Result.build(null, ResultEnum.NOT_ADMIN);
        }
        int start = uid.indexOf(":") + 1;
        int end = uid.indexOf("}");
        uid = uid.substring(start, end);
        userService.freezeUser(Integer.valueOf(uid));
        return Result.ok();
    }

    @ApiOperation("解冻用户接口")
    @PostMapping("/unfreezeUser")
    public Result<?> unfreezeUser(@ApiParam("token") @RequestHeader("token") String token, @ApiParam("用户id") @RequestBody String uid) {
        // 判断token是否过期，这里MyJwtTool.isValidToken(token)后续需要注意修改
        if (MyJwtTool.isNotValidToken(token)) {
            return Result.build(null, ResultEnum.TOKEN_ERROR);
        }
        if (!userService.getUserByUid(MyJwtTool.getUidFromToken(token)).getIsAdmin()) {
            return Result.build(null, ResultEnum.NOT_ADMIN);
        }
        int start = uid.indexOf(":") + 1;
        int end = uid.indexOf("}");
        uid = uid.substring(start, end);
        userService.unfreezeUser(Integer.valueOf(uid));
        return Result.ok();
    }

    @ApiOperation("注销用户接口")
    @PostMapping("/deleteUser")
    public Result<?> deleteUser(@ApiParam("token") @RequestHeader("token") String token, @ApiParam("用户id") @RequestBody String uid) {
        // 判断token是否过期，这里MyJwtTool.isValidToken(token)后续需要注意修改
        if (MyJwtTool.isNotValidToken(token)) {
            return Result.build(null, ResultEnum.TOKEN_ERROR);
        }
        if (!userService.getUserByUid(MyJwtTool.getUidFromToken(token)).getIsAdmin()) {
            return Result.build(null, ResultEnum.NOT_ADMIN);
        }
        int start = uid.indexOf(":") + 1;
        int end = uid.indexOf("}");
        uid = uid.substring(start, end);
        userService.deleteUser(Integer.valueOf(uid));
        return Result.ok();
    }

    @ApiOperation("修改用户容量接口")
    @PostMapping("/editUserCapacity/{capacity}")
    public Result<?> editUserCapacity(@ApiParam("token") @RequestHeader("token") String token, @ApiParam("用户id") @RequestBody String uid, @PathVariable("capacity") Integer capacity) {
        // 判断token是否过期，这里MyJwtTool.isValidToken(token)后续需要注意修改
        if (MyJwtTool.isNotValidToken(token)) {
            return Result.build(null, ResultEnum.TOKEN_ERROR);
        }
        if (!userService.getUserByUid(MyJwtTool.getUidFromToken(token)).getIsAdmin()) {
            return Result.build(null, ResultEnum.NOT_ADMIN);
        }
        int start = uid.indexOf(":") + 1;
        int end = uid.indexOf("}");
        uid = uid.substring(start, end);
        capacityService.editUserCapacity(Integer.valueOf(uid), capacity);
        return Result.ok();
    }

    @ApiOperation("设置用户为管理员接口")
    @PostMapping("/setAsAdmin")
    public Result<?> setAsAdmin(@ApiParam("token") @RequestHeader("token") String token, @ApiParam("用户id") @RequestBody String uid) {
        // 判断token是否过期，这里MyJwtTool.isValidToken(token)后续需要注意修改
        if (MyJwtTool.isNotValidToken(token)) {
            return Result.build(null, ResultEnum.TOKEN_ERROR);
        }
        if (!userService.getUserByUid(MyJwtTool.getUidFromToken(token)).getIsAdmin()) {
            return Result.build(null, ResultEnum.NOT_ADMIN);
        }
        int start = uid.indexOf(":") + 1;
        int end = uid.indexOf("}");
        uid = uid.substring(start, end);
        userService.setAsAdmin(Integer.valueOf(uid));
        return Result.ok();
    }
}
