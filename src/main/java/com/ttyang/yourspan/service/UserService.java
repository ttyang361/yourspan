package com.ttyang.yourspan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ttyang.yourspan.pojo.ForgetForm;
import com.ttyang.yourspan.pojo.LoginForm;
import com.ttyang.yourspan.pojo.RegisterForm;
import com.ttyang.yourspan.pojo.User;

import java.util.List;

/**
 * @author ttyang
 * @version 1.0
 * @since 2022-11-17
 */
public interface UserService extends IService<User> {
    /**
     * 用户登录服务
     *
     * @param loginForm 登录表单
     * @return 用户实体类
     */
    User login(LoginForm loginForm);

    /**
     * 通过uid获取对应的User实体类服务
     *
     * @param uid 用户uid
     * @return 该uid对应用户实体类
     */

    User getUserByUid(Integer uid);

    /**
     * 检查邮箱是否合法(在user表中的email列中是否存在)服务
     *
     * @param email 邮箱
     * @return 如果不存在返回true，否则false
     */

    boolean checkEmail(String email);

    /**
     * 用户注册服务
     *
     * @param registerForm 注册表单
     * @return 返回注册成功用户实体类
     */

    User register(RegisterForm registerForm);

    /**
     * 忘记密码服务
     *
     * @param forgetForm 忘记密码表单
     * @return 返回重置密码成功用户实体类
     */

    User forget(ForgetForm forgetForm);

    List<User> getAllUserInfo();

    void freezeUser(Integer uid);

    void unfreezeUser(Integer valueOf);

    void deleteUser(Integer valueOf);

    void setAsAdmin(Integer valueOf);
}
