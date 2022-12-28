package com.ttyang.yourspan.controller;

import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ttyang.yourspan.pojo.Folder;
import com.ttyang.yourspan.pojo.User;
import com.ttyang.yourspan.service.FileService;
import com.ttyang.yourspan.service.FolderService;
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
import java.util.*;

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
    @Autowired
    private FileService fileService;
    @Autowired
    private FolderService folderService;

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
        String fileName = IdUtil.simpleUUID().toLowerCase().concat(Objects.requireNonNull(multipartFile.getOriginalFilename()).substring(multipartFile.getOriginalFilename().lastIndexOf(".")));
        String avatarPath = headPath.concat(fileName);
        try {
            multipartFile.transferTo(new File(avatarPath));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Result.ok("upload/".concat(fileName));
    }

    @ApiOperation("获取用户所有文件信息接口")
    @GetMapping("/getFilesByToken")
    public Result<?> getFilesByToken(@ApiParam("token") @RequestHeader("token") String token) {
        // 判断token是否过期，这里MyJwtTool.isValidToken(token)后续需要注意修改
        if (MyJwtTool.isValidToken(token)) {
            return Result.build(null, ResultEnum.TOKEN_ERROR);
        }
        // 从token中获取到用户的uid
        Integer uid = MyJwtTool.getUidFromToken(token);
        // 分别取出该用户拥有的所有文件夹信息和文件信息
        List<com.ttyang.yourspan.pojo.File> fileList = fileService.getAllFilesByUid(uid);
        List<Folder> folderList = folderService.getAllFoldersByUid(uid);
        // 对接收结果进行整合
        // 将File对象放入对应的Folder对象的files里
        for (Folder folder : folderList) {
            // 初始化每一个Folder对象的files集合和childrenFolders集合
            folder.setFiles(new ArrayList<>());
            folder.setChildrenFolders(new ArrayList<>());
            for (com.ttyang.yourspan.pojo.File file : fileList) {
                // 如果该File对象的fVirtualFolder与Folder对象的flid相等，则将该File对象放入Folder对象的files集合内
                if (file.getFVirtualFolder().equals(folder.getFlid())) {
                    folder.getFiles().add(file);
                }
            }
        }
        // 将所有Folder对象按照父子级关系进行整合
        Folder result = new Folder().mergeFoldersFromList(folderList);
        return Result.ok(result);
    }

    /**
     * 从前台拿到需要上传的文件和文件信息表单，将其通过fastdfs工具类上传至fastdfs服务器，并将文件名、所属者id、服务器返回的文件卷名、文件路径等文件信息保存到数据库的file表
     *
     * @return 文件上传成功或失败的响应信息
     */
    @ApiOperation("上传文件接口")
    @PostMapping("/uploadFile")
    public Result<?> uploadFile() {

        return Result.ok();
    }

    /**
     * 从前台拿到需要下载的文件的卷名、路径等文件信息，根据卷名及路径通过fastdfs工具类从fastdfs服务器下载该文件并通过HttpServletResponse返回给客户端
     *
     * @return 文件下载成功或失败的响应信息
     */
    @ApiOperation("下载文件接口")
    @GetMapping("/downloadFile")
    public Result<?> downloadFile() {
        return Result.ok();
    }

    /**
     * 从前台拿到需要修改的文件的fid和修改后的文件名，利用FileService访问数据库查找到该文件信息并按照修改后的文件名进行更新操作
     *
     * @return 文件名修改成功或失败的响应信息
     */
    @ApiOperation("更改文件名接口")
    @PostMapping("/changeFileName")
    public Result<?> changeFileName() {
        return Result.ok();
    }

    /**
     * 从前台拿到需要删除的文件的卷名、路径等文件信息，根据卷名及路径通过fastdfs工具类从fastdfs服务器删除该文件，并利用FileService访问数据库通过fid查找到该文件信息并进行删除操作
     *
     * @return 文件删除成功或失败的响应信息
     */
    @ApiOperation("删除文件接口")
    @PostMapping("/deleteFile")
    public Result<?> deleteFile() {
        return Result.ok();
    }

    /**
     * 暂不实现，前台发送该文件的fid，利用FileService访问数据库通过fid查找到该文件信息，根据文件信息去生成对应的链接，进一步选择保存该文件到自己的网盘或者直接下载该文件，另外可以拓展出分享链接附带访问密码的功能
     *
     * @return 生成的分享文件的链接
     */
    @ApiOperation("分享文件接口")
    @GetMapping("/getShareLink")
    public Result<?> getShareLink() {
        return Result.ok();
    }

    /**
     * 暂不实现，前台发送该文件的fid和变更后的状态，利用FileService访问数据库通过fid查找到该文件信息并更新文件的权限状态
     *
     * @return 设置权限成功或失败的响应信息
     */
    @ApiOperation("设置文件权限接口")
    @PostMapping("/setFileAuthority")
    public Result<?> setFileAuthority() {
        return Result.ok();
    }

    /**
     * 从前台拿到该文件的fid和目标文件夹的flid，利用FileService访问数据库通过fid查找到该文件信息并更新文件的fVirtualFolder为flid
     *
     * @return 移动文件成功或失败的响应信息
     */
    @ApiOperation("移动文件接口")
    @PostMapping("/moveFile")
    public Result<?> moveFile() {
        return Result.ok();
    }

    /**
     * 从前台拿到该文件夹父级文件夹的flid、文件夹名、所属者id信息，利用FolderService访问数据库插入该条文件夹记录行
     *
     * @return 创建文件夹成功或失败的响应信息
     */
    @ApiOperation("创建文件夹接口")
    @PostMapping("/createNewFolder")
    public Result<?> createNewFolder() {
        return Result.ok();
    }

    /**
     * 从前台拿到该文件夹的flid，利用FolderService访问数据库删除该条文件夹记录行，取出file表内fVirtualFolder等于flid的文件数据，通过fastdfs工具类删除fastdfs服务器内所有对应的文件，最后删除file表内所有对应的文件信息
     *
     * @return 文件夹删除成功或失败的响应信息
     */
    @ApiOperation("删除文件夹接口")
    @PostMapping("/deleteFolder")
    public Result<?> deleteFolder() {
        return Result.ok();
    }

    /**
     * 从前台拿到该文件夹的flid和更改后的文件夹名称，利用FolderService访问数据库更新该条文件夹记录行
     *
     * @return 文件夹名称修改成功或失败的响应信息
     */
    @ApiOperation("更改文件夹名接口")
    @PostMapping("/changeFolderName")
    public Result<?> changeFolderName() {
        return Result.ok();
    }
}
