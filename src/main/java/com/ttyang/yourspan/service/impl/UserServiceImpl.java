package com.ttyang.yourspan.service.impl;

import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ttyang.yourspan.mapper.UserMapper;
import com.ttyang.yourspan.pojo.ForgetForm;
import com.ttyang.yourspan.pojo.LoginForm;
import com.ttyang.yourspan.pojo.RegisterForm;
import com.ttyang.yourspan.pojo.User;
import com.ttyang.yourspan.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author ttyang
 * @version 1.0
 * @since 2022-11-17
 */
@Service("userServiceImpl")
@Transactional(rollbackFor = Exception.class)
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    @Override
    public User login(LoginForm loginForm) {
        return baseMapper.selectOne(new QueryWrapper<User>().eq("uid", loginForm.getUserId()).eq("password", DigestUtil.md5Hex(loginForm.getPassword())));
    }

    @Override
    public User getUserByUid(Integer uid) {
        return baseMapper.selectOne(new QueryWrapper<User>().eq("uid", uid));
    }

    @Override
    public boolean checkEmail(String email) {
        return baseMapper.selectOne(new QueryWrapper<User>().eq("email", email)) == null;
    }

    @Override
    public User register(RegisterForm registerForm) {
        // 向表中新增用户
        save(new User(null, DigestUtil.md5Hex(registerForm.getPassword()), registerForm.getEmail(), null, null, null, null));
        // 通过queryWrapper从数据库中获取该注册邮箱的User对象并返回
        return baseMapper.selectOne(new QueryWrapper<User>().eq("email", registerForm.getEmail()));
    }

    @Override
    public User forget(ForgetForm forgetForm) {
        // 负责邮箱验证码通过后重置对应用户密码的服务
        User user = new User();
        user.setPassword(DigestUtil.md5Hex(forgetForm.getPassword()));
        update(user, new UpdateWrapper<User>().eq("email", forgetForm.getEmail()));
        return baseMapper.selectOne(new QueryWrapper<User>().eq("email", forgetForm.getEmail()));
    }
}
