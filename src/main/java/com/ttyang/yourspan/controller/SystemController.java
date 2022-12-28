package com.ttyang.yourspan.controller;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.extra.mail.MailUtil;
import com.ttyang.yourspan.pojo.*;
import com.ttyang.yourspan.service.MenuService;
import com.ttyang.yourspan.service.UserService;
import com.ttyang.yourspan.util.MyJwtTool;
import com.ttyang.yourspan.util.Result;
import com.ttyang.yourspan.util.ResultEnum;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

/**
 * @author ttyang
 * @version 1.0
 * @since 2022-11-17
 */
@Api(tags = "系统控制器")
@RestController
@RequestMapping("/sms/system")
public class SystemController {

    @Autowired
    private UserService userService;
    @Autowired
    private MenuService menuService;

    @ApiOperation("向服务端发送请求获取验证码图片接口")
    @GetMapping("/getVerifyCodeImage")
    public void getVerifyCodeImage(HttpServletRequest request, HttpServletResponse response) {
        // 获取验证码图片
        LineCaptcha lineCaptcha = CaptchaUtil.createLineCaptcha(90, 35);
        // 获取验证码字符串
        String verifyCode = lineCaptcha.getCode();
        // 将验证码字符串放入session域，为后面验证做准备
        request.getSession().setAttribute("verifyCode", verifyCode);
        System.out.println(verifyCode);
        // 将验证码图片发送给客户端
        try {
            lineCaptcha.write(response.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @ApiOperation("登录验证接口")
    @PostMapping("/login")
    public Result<?> login(@ApiParam("登录页提交信息的form表单") @RequestBody LoginForm loginForm, HttpServletRequest request) {
        // 验证码校验
        // 1.分别从session和表单中取出验证码
        String sessionVerifyCode = (String) request.getSession().getAttribute("verifyCode");
        String loginVerifyCode = loginForm.getVerifyCode();
        // 2.判断验证码是否失效
        if ("".equals(sessionVerifyCode) || null == sessionVerifyCode) {
            return Result.fail().message("验证码失效，请重试！");
        }
        // 3.若验证码不相等
        if (!sessionVerifyCode.equalsIgnoreCase(loginVerifyCode)) {
            return Result.fail().message("验证码不正确，请重新输入！");
        }
        // 4.将验证码移出session域，防止后续不规范操作发生异常
        request.getSession().removeAttribute("verifyCode");
        // 准备一个map存放响应给客户端的数据(data)
        Map<String, String> map = new LinkedHashMap<>();
        try {
            User user = userService.login(loginForm);
            if (null != user) {
                map.put("token", MyJwtTool.createTokenByUid(user.getUid()));
            } else {
                throw new RuntimeException("用户名或密码错误！");
            }
            return Result.ok(map).message("登录成功！");
        } catch (RuntimeException e) {
            e.printStackTrace();
            return Result.fail().message(e.getMessage());
        }
    }

    @ApiOperation("注销登录接口")
    @PostMapping("/logout")
    public Result<?> logout() {
        return Result.ok().message("注销登录成功！");
    }

    @ApiOperation("通过token获取用户信息接口")
    @GetMapping("/getUserInfoByToken")
    public Result<?> getUserInfoByToken(@ApiParam("token") @RequestHeader("token") String token) {
        // 判断token是否过期，这里MyJwtTool.isValidToken(token)后续需要注意修改
        if (MyJwtTool.isValidToken(token)) {
            return Result.build(null, ResultEnum.TOKEN_ERROR);
        }
        // 从token中获取uid
        Integer uid = MyJwtTool.getUidFromToken(token);
        // 通过uid获取User对象并将信息封装入map内作为data返回给客户端
        Map<String, User> map = new LinkedHashMap<>();
        User user = userService.getUserByUid(uid);
        map.put("user", user);
        return Result.ok(map);

    }

    @ApiOperation("注册接口")
    @PostMapping("/register/{verifyCode}")
    public Result<?> register(@ApiParam("注册页提交信息的form表单") @RequestBody RegisterForm registerForm, @PathVariable("verifyCode") String verifyCode, HttpServletRequest request) {
        // 检查该邮箱是否已注册，若已注册则返回”该邮箱已被注册“信息
        if (!userService.checkEmail(registerForm.getEmail())) {
            return Result.build(null, ResultEnum.EMAIL_ERROR);
        }
        // 分别取出路径中的验证码和session中的验证码
        Integer registerVerifyCode = Integer.parseInt(verifyCode);
        Integer sessionVerifyCode = (Integer) request.getSession().getAttribute("eVerifyCode");
        // 若验证码失效
        if (null == sessionVerifyCode || 0 == sessionVerifyCode) {
            return Result.fail().message("验证码已失效，请重试！");
        }
        // 若验证码不相等
        if (!registerVerifyCode.equals(sessionVerifyCode)) {
            return Result.fail().message("验证码错误，请重试！");
        }
        // 将验证码移出session域
        request.removeAttribute("eVerifyCode");
        // 准备一个map存放用于响应客户端的数据
        Map<String, String> map = new LinkedHashMap<>();
        try {
            User user = userService.register(registerForm);
            if (null != user) {
                map.put("token", MyJwtTool.createTokenByUid(user.getUid()));
                return Result.ok(map);
            } else {
                throw new RuntimeException("创建用户时出现异常");
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
            return Result.fail().message(e.getMessage());
        }
    }

    @ApiOperation("忘记密码接口")
    @PostMapping("/forgetPwd/{verifyCode}")
    public Result<?> forgetPwd(@ApiParam("忘记密码页提交信息的form表单") @RequestBody ForgetForm forgetForm, @PathVariable("verifyCode") String verifyCode, HttpServletRequest request) {
        // 检查该邮箱是否已注册，若未注册则返回”该邮箱未注册“信息
        if (userService.checkEmail(forgetForm.getEmail())) {
            return Result.build(null, ResultEnum.EMAIL_NOT_VALID);
        }
        Integer forgetVerifyCode = Integer.parseInt(verifyCode);
        Integer sessionVerifyCode = (Integer) request.getSession().getAttribute("eVerifyCode");
        if (null == sessionVerifyCode || 0 == sessionVerifyCode) {
            return Result.fail().message("验证码已过期，请重试！");
        }
        if (!sessionVerifyCode.equals(forgetVerifyCode)) {
            return Result.fail().message("验证码错误，请重试！");
        }
        request.removeAttribute("eVerifyCode");
        // 准备一个map存放用于响应客户端的数据
        Map<String, String> map = new LinkedHashMap<>();
        try {
            User user = userService.forget(forgetForm);
            if (null != user) {
                map.put("token", MyJwtTool.createTokenByUid(user.getUid()));
                return Result.ok(map);
            } else {
                throw new RuntimeException("重置密码时出现异常");
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
            return Result.fail().message(e.getMessage());
        }
    }

    @ApiOperation("发送验证码接口")
    @PostMapping("/sendVerifyCode/{email}")
    public Result<?> sendVerifyCode(HttpServletRequest request, @PathVariable("email") String email) {
        // 生成5位随机数
        int code = RandomUtil.randomInt(10000, 100000);
        // 向目标邮箱发送验证码
        try {
            MailUtil.send(email, "YoursPan网盘", "这是您的验证码：" + code + "\n为了您的账号安全，如非您本人操作，请不要告诉任何人呦！", false);
        } catch (RuntimeException e) {
            e.printStackTrace();
            return Result.fail().message("请检查邮箱是否正确！");
        }
        // 将验证码存放入session域
        request.getSession().setAttribute("eVerifyCode", code);
        return Result.ok();
    }

    @ApiOperation("获取用户所有目录对象接口")
    @GetMapping("/getAllMenusByToken")
    public Result<?> getAllMenusByToken(@ApiParam("token") @RequestHeader("token") String token) {
        // 判断token是否过期，这里MyJwtTool.isValidToken(token)后续需要注意修改
        if (MyJwtTool.isValidToken(token)) {
            return Result.build(null, ResultEnum.TOKEN_ERROR);
        }
        // 从数据库获取所有目录表数据并以List<Menu>形式返回
        List<Menu> l1 = menuService.getAllMenus();
        List<Menu> l2 = new ArrayList<>();
        // 遍历目录List<Menu> l1，去除id为1的项，并将所有parentId为1的放入另一个List<Menu> l2中
        for (Menu menu : l1) {
            if (menu.getId() != 1) {
                if (menu.getParentId() == 1) {
                    menu.setChildren(new ArrayList<>());
                    l2.add(menu);
                }
            }
        }
        // 再次遍历List，若parentId与l2中的id相等则将l2中对应的项的children设为该项
        for (Menu menu : l1) {
            for (Menu menu1 : l2) {
                if (Objects.equals(menu.getParentId(), menu1.getId())) {
                    menu1.getChildren().add(menu);
                }
            }
        }
        // 将l2放入ok(data)中
        return Result.ok(l2);
    }
}
