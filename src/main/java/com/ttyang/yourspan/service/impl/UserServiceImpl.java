package com.ttyang.yourspan.service.impl;

import cn.hutool.core.lang.UUID;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.tobato.fastdfs.service.FastFileStorageClient;
import com.ttyang.yourspan.mapper.UserMapper;
import com.ttyang.yourspan.pojo.*;
import com.ttyang.yourspan.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * @author ttyang
 * @version 1.0
 * @since 2022-11-17
 */
@Service("userServiceImpl")
@Transactional(rollbackFor = Exception.class)
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    @Autowired
    private FileService fileService;
    @Autowired
    private FolderService folderService;
    @Autowired
    private RatingsService ratingsService;
    @Autowired
    private CapacityService capacityService;
    @Autowired
    private FastFileStorageClient fastFileStorageClient;

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
        save(new User(null, DigestUtil.md5Hex(registerForm.getPassword()), registerForm.getEmail(), "用户" + UUID.fastUUID(), null, null, null, null, null));
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

    @Override
    public List<User> getAllUserInfo() {
        // 获取所有用户信息，除去uid=1000的记录行
        return baseMapper.selectList(new QueryWrapper<User>().ne("uid", 1000));
    }

    @Override
    public void freezeUser(Integer uid) {
        // 冻结用户
        User user = new User();
        user.setIsFrozen(true);
        update(user, new UpdateWrapper<User>().eq("uid", uid));
    }

    @Override
    public void unfreezeUser(Integer valueOf) {
        // 解冻用户
        User user = new User();
        user.setIsFrozen(false);
        update(user, new UpdateWrapper<User>().eq("uid", valueOf));
    }

    @Override
    public void deleteUser(Integer uid) {
        // 从file表中取出所有uid匹配f_owner_id
        List<File> files = fileService.getAllFilesByUid1(uid);
        for (File file : files) {
            // 删除file表内对应记录行，并删除fastdfs中的文件
            String fullPath = fileService.deleteFileByFid(file.getFid().toString());
            if (fullPath != null) {
                fastFileStorageClient.deleteFile(fullPath);
            } else {
                throw new RuntimeException("注销用户失败：删除文件异常！");
            }
            // 删除ratings表内fid匹配的记录行
            ratingsService.deleteRatingsByFid(file.getFid().toString());
        }
        // 删除folder表内所有uid匹配fl_owner_id的记录行
        List<Folder> folders = folderService.getAllFoldersByUid(uid);
        for (Folder folder : folders) {
            folderService.deleteFolderByFlid(folder.getFlid());
        }
        // 删除capacity表内uid匹配的记录行
        capacityService.remove(new QueryWrapper<Capacity>().eq("uid", uid));
        // 删除ratings表内uid匹配的记录行
        ratingsService.deleteRatingsByUid(uid);
        // 删除user表内uid匹配的记录行
        removeById(uid);
    }

    @Override
    public void setAsAdmin(Integer valueOf) {
        // 设置用户为管理员
        User user = new User();
        user.setIsAdmin(true);
        update(user, new UpdateWrapper<User>().eq("uid", valueOf));
    }
}
