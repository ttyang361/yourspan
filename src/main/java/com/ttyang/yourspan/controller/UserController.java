package com.ttyang.yourspan.controller;

import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.tobato.fastdfs.domain.fdfs.StorePath;
import com.github.tobato.fastdfs.service.FastFileStorageClient;
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
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.file.Files;
import java.sql.Date;
import java.time.LocalDate;
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
    @Autowired
    private FastFileStorageClient fastFileStorageClient;

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
     * 从前台拿到需要上传的文件，将其通过fastdfs工具类上传至fastdfs服务器，并将文件名、所属者id、服务器返回的文件卷名、文件路径等文件信息保存到数据库的file表
     *
     * @return 文件上传成功或失败的响应信息
     */
    @ApiOperation("上传文件接口")
    @PostMapping("/uploadFile/{folderId}")
    public Result<?> uploadFile(@ApiParam("token") @RequestHeader("token") String token, @ApiParam("用户上传文件") @RequestPart("multipartFile") MultipartFile multipartFile, @ApiParam("所属文件夹id") @PathVariable("folderId") String folderId) {
        // 判断token是否过期，这里MyJwtTool.isValidToken(token)后续需要注意修改
        if (MyJwtTool.isValidToken(token)) {
            return Result.build(null, ResultEnum.TOKEN_ERROR);
        }
        // 从token中获取到用户的uid
        Integer uid = MyJwtTool.getUidFromToken(token);
        // 取出文件名（这里存在一个问题，如果后缀名包含两个以及更多'.'字符则无法正确获取，例如.tar.gz，后面注意修改）
        String originalFileName = Objects.requireNonNull(multipartFile.getOriginalFilename());
        String prefixName = originalFileName.substring(0, originalFileName.lastIndexOf("."));
        String suffixName = originalFileName.substring(originalFileName.lastIndexOf(".") + 1);
        // 将multipartFile转换为File对象
        try {
            // 创建临时文件
            File file = File.createTempFile(prefixName.concat(IdUtil.simpleUUID().toLowerCase()), suffixName);
            try (InputStream inputStream = multipartFile.getInputStream();
                 OutputStream outputStream = Files.newOutputStream(file.toPath())) {
                IOUtils.copy(inputStream, outputStream);
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException("复制文件流时出现异常");
            }
            // 将文件上传至fastdfs存储器中
            StorePath storePath = fastFileStorageClient.uploadFile(Files.newInputStream(file.toPath()), file.length(), suffixName, null);
            // 将文件信息存入file表内
            boolean result = fileService.uploadFileInfo(originalFileName, uid, storePath.getGroup(), storePath.getPath(), folderId, Date.valueOf(LocalDate.now()), Date.valueOf(LocalDate.now()));
            if (!result) {
                throw new RuntimeException("上传文件信息失败！");
            }
            // 删除临时文件
            if (!file.delete()) {
                throw new RuntimeException("临时文件删除错误！");
            }
        } catch (IOException e) {
            throw new RuntimeException("MultipartFile转换为File出现异常！");
        }
        return Result.ok();
    }

    /**
     * 从前台拿到需要下载的文件的文件ID，从数据库中查找该文件对应的文件信息，再根据卷名及路径通过fastdfs工具类从fastdfs服务器下载该文件并通过HttpServletResponse返回给客户端
     *
     * @return 文件下载成功或失败的响应信息
     */
    @ApiOperation("下载文件接口")
    @GetMapping("/downloadFile/{fileId}")
    public Result<?> downloadFile(@ApiParam("token") @RequestHeader("token") String token, @ApiParam("需要下载的文件ID") @PathVariable("fileId") String fileId, HttpServletResponse response) {
        // 判断token是否过期，这里MyJwtTool.isValidToken(token)后续需要注意修改
        if (MyJwtTool.isValidToken(token)) {
            return Result.build(null, ResultEnum.TOKEN_ERROR);
        }
        com.ttyang.yourspan.pojo.File file = fileService.getFileByFid(fileId);
        response.setHeader("Content-Disposition", "attachment; filename=" + file.getFName());
        fastFileStorageClient.downloadFile(file.getFGroup(), file.getFPath(), ins -> {
            IOUtils.copy(ins, response.getOutputStream());
            return null;
        });
        return Result.ok();
    }

    /**
     * 从前台拿到需要修改的文件的fid和修改后的文件名，利用FileService访问数据库查找到该文件信息并按照修改后的文件名进行更新操作
     *
     * @return 文件名修改成功或失败的响应信息
     */
    @ApiOperation("更改文件名接口")
    @PostMapping("/changeFileName/{fileId}")
    public Result<?> changeFileName(@ApiParam("token") @RequestHeader("token") String token, @ApiParam("文件ID") @PathVariable("fileId") String fileId, @ApiParam("修改后的文件名") @RequestBody String newFileName) {
        // 判断token是否过期，这里MyJwtTool.isValidToken(token)后续需要注意修改
        if (MyJwtTool.isValidToken(token)) {
            return Result.build(null, ResultEnum.TOKEN_ERROR);
        }
        // 处理folderName
        int startIndex = newFileName.indexOf("\"fileName\":\"") + "\"fileName\":\"".length();
        int endIndex = newFileName.indexOf("\"", startIndex);
        newFileName = newFileName.substring(startIndex, endIndex);
        boolean result = fileService.modifyFileName(fileId, newFileName);
        if (!result) {
            throw new RuntimeException("修改文件名异常！");
        }
        return Result.ok();
    }

    /**
     * 从前台拿到需要删除的文件的fid，根据卷名及路径通过fastdfs工具类从fastdfs服务器删除该文件，并利用FileService访问数据库通过fid查找到该文件信息并进行删除操作
     *
     * @return 文件删除成功或失败的响应信息
     */
    @ApiOperation("删除文件接口")
    @PostMapping("/deleteFile/{fileId}")
    public Result<?> deleteFile(@ApiParam("token") @RequestHeader("token") String token, @ApiParam("文件ID") @PathVariable("fileId") String fileId) {
        // 判断token是否过期，这里MyJwtTool.isValidToken(token)后续需要注意修改
        if (MyJwtTool.isValidToken(token)) {
            return Result.build(null, ResultEnum.TOKEN_ERROR);
        }
        String fullPath = fileService.deleteFileByFid(fileId);
        if (fullPath != null) {
            fastFileStorageClient.deleteFile(fullPath);
        } else {
            throw new RuntimeException("删除文件信息异常！");
        }
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
    @PostMapping("/moveFile/{fileId}")
    public Result<?> moveFile(@ApiParam("token") @RequestHeader("token") String token, @ApiParam("文件ID") @PathVariable("fileId") String fileId, @ApiParam("目标文件夹id") @RequestBody String targetFolderId) {
        // 判断token是否过期，这里MyJwtTool.isValidToken(token)后续需要注意修改
        if (MyJwtTool.isValidToken(token)) {
            return Result.build(null, ResultEnum.TOKEN_ERROR);
        }
        // 处理targetFolderId
        int startIndex = targetFolderId.indexOf(":") + 1;
        int endIndex = targetFolderId.indexOf("}");
        targetFolderId = targetFolderId.substring(startIndex, endIndex);
        boolean result = fileService.modifyFolderOfFile(fileId, targetFolderId);
        if (!result) {
            throw new RuntimeException("移动文件异常！");
        }
        return Result.ok();
    }

    /**
     * 从前台拿到该文件夹父级文件夹的flid、文件夹名、所属者id信息，利用FolderService访问数据库插入该条文件夹记录行
     *
     * @return 创建文件夹成功或失败的响应信息
     */
    @ApiOperation("创建文件夹接口")
    @PostMapping("/createNewFolder/{folderId}")
    public Result<?> createNewFolder(@ApiParam("token") @RequestHeader("token") String token, @ApiParam("父级文件夹ID") @PathVariable("folderId") String parentFolderId, @ApiParam("文件夹名称") @RequestBody String folderName) {
        // 判断token是否过期，这里MyJwtTool.isValidToken(token)后续需要注意修改
        if (MyJwtTool.isValidToken(token)) {
            return Result.build(null, ResultEnum.TOKEN_ERROR);
        }
        // 处理folderName
        int startIndex = folderName.indexOf("\"folderName\":\"") + "\"folderName\":\"".length();
        int endIndex = folderName.indexOf("\"", startIndex);
        folderName = folderName.substring(startIndex, endIndex);
        // 从token中获取到用户的uid
        Integer uid = MyJwtTool.getUidFromToken(token);
        boolean result = folderService.createNewFolder(folderName, parentFolderId, uid, Date.valueOf(LocalDate.now()), Date.valueOf(LocalDate.now()));
        if (!result) {
            throw new RuntimeException("创建文件夹异常！");
        }
        return Result.ok();
    }

    /**
     * 从前台拿到该文件夹的flid，利用FolderService访问数据库删除该条文件夹记录行，取出file表内fVirtualFolder等于flid的文件数据，通过fastdfs工具类删除fastdfs服务器内所有对应的文件，最后删除file表内所有对应的文件信息
     *
     * @return 文件夹删除成功或失败的响应信息
     */
    @ApiOperation("删除文件夹接口")
    @PostMapping("/deleteFolder/{folderId}")
    public Result<?> deleteFolder(@ApiParam("token") @RequestHeader("token") String token, @ApiParam("文件夹ID") @PathVariable("folderId") String targetFolderId) {
        // 判断token是否过期，这里MyJwtTool.isValidToken(token)后续需要注意修改
        if (MyJwtTool.isValidToken(token)) {
            return Result.build(null, ResultEnum.TOKEN_ERROR);
        }
        // 拿到该用户的根文件夹
        Folder rootFolder = (Folder) getFilesByToken(token).getData();
        // 广度优先遍历该文件夹找到并取出目标文件夹
        Folder targetFolder = null;
        Queue<Folder> queue = new LinkedList<>();
        queue.offer(rootFolder);
        while (!queue.isEmpty()) {
            Folder currentFolder = queue.poll();
            if (currentFolder.getFlid().equals(Integer.valueOf(targetFolderId))) {
                targetFolder = currentFolder;
                break;
            }
            if (currentFolder.getChildrenFolders() != null) {
                for (Folder folder : currentFolder.getChildrenFolders()) {
                    queue.offer(folder);
                }
            }
        }
        // 再次利用广度优先遍历分别将targetFolder自身及其包含的文件夹及文件的id保存到两个列表中，为后续删除操作做准备
        List<Integer> fileIds = new ArrayList<>();
        List<Integer> folderIds = new ArrayList<>();
        Queue<Folder> folderQueue = new LinkedList<>();
        folderIds.add(Objects.requireNonNull(targetFolder).getFlid());
        folderQueue.offer(targetFolder);
        while (!folderQueue.isEmpty()) {
            Folder currentFolder = folderQueue.poll();
            // 如果当前文件夹的文件列表不为空，则依次将其文件ID添加入fileIds
            if (currentFolder.getFiles() != null) {
                for (com.ttyang.yourspan.pojo.File file : currentFolder.getFiles()) {
                    fileIds.add(file.getFid());
                }
            }
            // 如果当前文件夹的子文件夹列表不为空，则依次将其文件夹ID添加入folderIds，并将子文件夹对象依次放入队列中
            if (currentFolder.getChildrenFolders() != null) {
                for (Folder folder : currentFolder.getChildrenFolders()) {
                    folderIds.add(folder.getFlid());
                    folderQueue.offer(folder);
                }
            }
        }
        // 颠倒folderIds的顺序，使其从子文件夹开始删除
        Collections.reverse(folderIds);
        // 从控制台输出两个列表内容
        System.out.println(fileIds);
        System.out.println(folderIds);
        // 若fileIds列表的长度大于0，则遍历fileIds，调用deleteFile方法从fastdfs中删除所有对应的文件
        if (fileIds.size() > 0) {
            for (Integer fileId : fileIds) {
                deleteFile(token, fileId.toString());
            }
        }
        // 若fileIds列表的长度大于0，则从数据库删除fileIds中对应的文件信息
        if (fileIds.size() > 0) {
            fileService.removeByIds(fileIds);
        }
        // 从数据库删除folderIds中对应的文件夹信息
        folderService.removeByIds(folderIds);
        return Result.ok().message("删除文件夹成功！");
    }

    /**
     * 从前台拿到该文件夹的flid和更改后的文件夹名称，利用FolderService访问数据库更新该条文件夹记录行
     *
     * @return 文件夹名称修改成功或失败的响应信息
     */
    @ApiOperation("更改文件夹名接口")
    @PostMapping("/changeFolderName/{folderId}")
    public Result<?> changeFolderName(@ApiParam("token") @RequestHeader("token") String token, @ApiParam("文件夹ID") @PathVariable("folderId") String currentFolderId, @ApiParam("更改后的文件夹名称") @RequestBody String newFolderName) {
        // 判断token是否过期，这里MyJwtTool.isValidToken(token)后续需要注意修改
        if (MyJwtTool.isValidToken(token)) {
            return Result.build(null, ResultEnum.TOKEN_ERROR);
        }
        // 处理folderName
        int startIndex = newFolderName.indexOf("\"folderName\":\"") + "\"folderName\":\"".length();
        int endIndex = newFolderName.indexOf("\"", startIndex);
        newFolderName = newFolderName.substring(startIndex, endIndex);
        boolean result = folderService.modifyFolderName(currentFolderId, newFolderName);
        if (!result) {
            throw new RuntimeException("修改文件夹名称异常！");
        }
        return Result.ok();
    }
}
