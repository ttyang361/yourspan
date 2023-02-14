package com.ttyang.yourspan.service.impl;

import cn.hutool.core.lang.UUID;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ttyang.yourspan.mapper.UserMapper;
import com.ttyang.yourspan.pojo.*;
import com.ttyang.yourspan.service.CapacityService;
import com.ttyang.yourspan.service.FolderService;
import com.ttyang.yourspan.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDate;

/**
 * @author ttyang
 * @version 1.0
 * @since 2022-11-17
 */
@Service("userServiceImpl")
@Transactional(rollbackFor = Exception.class)
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    @Autowired
    private FolderService folderService;
    @Autowired
    private CapacityService capacityService;

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
        save(new User(null, DigestUtil.md5Hex(registerForm.getPassword()), registerForm.getEmail(), "用户" + UUID.fastUUID(), null, null, null));
        // 通过queryWrapper从数据库中获取该注册邮箱的User对象
        User user = baseMapper.selectOne(new QueryWrapper<User>().eq("email", registerForm.getEmail()));
        // 在folder表中添加一个新文件夹
        if (folderService.createNewFolder("我的资源", "1", user.getUid(), Date.valueOf(LocalDate.now()), Date.valueOf(LocalDate.now()))) {
            Folder folder = folderService.getFolderByUidAndParentFolderId(user.getUid(), 1);
            // 将用户根文件夹id设置为该文件夹id
            user.setRootFdirPath(folder.getFlid());
            updateById(user);
            // 初始化该用户的容量数据
            capacityService.save(new Capacity(user.getUid(), 512.0, 0.0));
            return user;
        }
        return null;
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
