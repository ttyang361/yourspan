package com.ttyang.yourspan.controller;

import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.tobato.fastdfs.domain.fdfs.StorePath;
import com.github.tobato.fastdfs.service.FastFileStorageClient;
import com.ttyang.yourspan.pojo.Capacity;
import com.ttyang.yourspan.pojo.Folder;
import com.ttyang.yourspan.pojo.Ratings;
import com.ttyang.yourspan.pojo.User;
import com.ttyang.yourspan.service.*;
import com.ttyang.yourspan.util.MyJwtTool;
import com.ttyang.yourspan.util.Result;
import com.ttyang.yourspan.util.ResultEnum;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.commons.io.IOUtils;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.common.Weighting;
import org.apache.mahout.cf.taste.impl.model.jdbc.MySQLJDBCDataModel;
import org.apache.mahout.cf.taste.impl.neighborhood.NearestNUserNeighborhood;
import org.apache.mahout.cf.taste.impl.recommender.GenericItemBasedRecommender;
import org.apache.mahout.cf.taste.impl.recommender.GenericUserBasedRecommender;
import org.apache.mahout.cf.taste.impl.similarity.PearsonCorrelationSimilarity;
import org.apache.mahout.cf.taste.model.JDBCDataModel;
import org.apache.mahout.cf.taste.neighborhood.UserNeighborhood;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.apache.mahout.cf.taste.recommender.Recommender;
import org.apache.mahout.cf.taste.similarity.ItemSimilarity;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.*;
import java.nio.file.Files;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

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
    @Autowired
    private RatingsService ratingsService;
    @Autowired
    private CapacityService capacityService;
    @Autowired
    private DataSource dataSource;

    @ApiOperation("修改密码接口")
    @PostMapping("/resetPwd/{oldPwd}/{newPwd}")
    public Result<?> resetPwd(@ApiParam("token") @RequestHeader String token, @ApiParam("旧密码") @PathVariable("oldPwd") String oldPwd, @ApiParam("新密码") @PathVariable("newPwd") String newPwd) {
        // 若token已过期
        if (MyJwtTool.isNotValidToken(token)) {
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
        String fileName = IdUtil.simpleUUID().toLowerCase().concat(Objects.requireNonNull(multipartFile.getOriginalFilename()));
        String avatarPath = headPath.concat(fileName);
        try {
            multipartFile.transferTo(new File(avatarPath));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Result.ok("upload/".concat(fileName));
    }

    @ApiOperation(("修改用户头像路径接口"))
    @PostMapping("/updateAvatarPath")
    public Result<?> updateAvatarPath(@ApiParam("token") @RequestHeader("token") String token, @ApiParam("修改后的文件路径") @RequestBody String avatarPath) {
        // 处理avatarPath
        int startIndex = avatarPath.indexOf("\"avatarPath\":\"") + "\"avatarPath\":\"".length();
        int endIndex = avatarPath.indexOf("\"", startIndex);
        avatarPath = avatarPath.substring(startIndex, endIndex);

        Integer uid = MyJwtTool.getUidFromToken(token);
        User user = userService.getById(uid);
        user.setAvatarPath(avatarPath);
        userService.saveOrUpdate(user);
        return Result.ok();
    }

    @ApiOperation("获取用户所有文件信息接口")
    @GetMapping("/getFilesByToken")
    public Result<?> getFilesByToken(@ApiParam("token") @RequestHeader("token") String token) {
        // 判断token是否过期，这里MyJwtTool.isValidToken(token)后续需要注意修改
        if (MyJwtTool.isNotValidToken(token)) {
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
        if (MyJwtTool.isNotValidToken(token)) {
            return Result.build(null, ResultEnum.TOKEN_ERROR);
        }
        // 从token中获取到用户的uid
        Integer uid = MyJwtTool.getUidFromToken(token);
        // 取出文件名（这里存在一个问题，如果后缀名包含两个以及更多'.'字符则无法正确获取，例如.tar.gz，后面注意修改）
        String originalFileName = Objects.requireNonNull(multipartFile.getOriginalFilename());
        String prefixName = originalFileName.substring(0, originalFileName.lastIndexOf("."));
        String suffixName = originalFileName.substring(originalFileName.lastIndexOf(".") + 1);
        // 取出文件大小
        long fileSize = multipartFile.getSize();
        fileSize = fileSize >> 10;
        // 更新capacity表内容
        boolean updateResult = capacityService.updateCapacity(uid, fileSize);
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
            boolean result = fileService.uploadFileInfo(originalFileName, uid, storePath.getGroup(), storePath.getPath(), folderId, Date.valueOf(LocalDate.now()), Date.valueOf(LocalDate.now()), false, fileSize);
            if (!result || !updateResult) {
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
     */
    @ApiOperation("下载文件接口")
    @GetMapping("/downloadFile/{fileId}")
    public void downloadFile(@ApiParam("token") @RequestHeader("token") String token, @ApiParam("需要下载的文件ID") @PathVariable("fileId") String fileId, HttpServletResponse response) {
        // 判断token是否过期，这里MyJwtTool.isValidToken(token)后续需要注意修改
        if (MyJwtTool.isNotValidToken(token)) {
            throw new RuntimeException("token已失效！");
        }
        com.ttyang.yourspan.pojo.File file = fileService.getFileByFid(fileId);
        response.setContentType("application/octet-stream");
        response.addHeader("Content-Disposition", "attachment; filename=" + file.getFName());
        response.setCharacterEncoding("UTF-8");
        byte[] bytes = fastFileStorageClient.downloadFile(file.getFGroup(), file.getFPath(), IOUtils::toByteArray);
        try {
            ServletOutputStream outputStream = response.getOutputStream();
            outputStream.write(bytes);
        } catch (IOException e) {
            throw new RuntimeException("文件下载失败！");
        }
    }

    /**
     * 该接口为下载文件接口的备用接口，不包含token验证
     */
    @ApiOperation("下载文件备用接口")
    @GetMapping("/downloadFileTest/{fileId}")
    public void downloadFileTest(@ApiParam("需要下载的文件ID") @PathVariable("fileId") String fileId, HttpServletResponse response) {
        com.ttyang.yourspan.pojo.File file = fileService.getFileByFid(fileId);
        response.setContentType("application/octet-stream");
        response.addHeader("Content-Disposition", "attachment; filename=" + file.getFName());
        response.setCharacterEncoding("UTF-8");
        byte[] bytes = fastFileStorageClient.downloadFile(file.getFGroup(), file.getFPath(), IOUtils::toByteArray);
        try {
            ServletOutputStream outputStream = response.getOutputStream();
            outputStream.write(bytes);
        } catch (IOException e) {
            throw new RuntimeException("文件下载失败！");
        }
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
        if (MyJwtTool.isNotValidToken(token)) {
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
     * 将文件移动到回收站
     *
     * @return 文件移动成功或失败的响应信息
     */
    @ApiOperation("将文件移动到回收站接口")
    @PostMapping("/moveToRecycleBin/{fileId}")
    public Result<?> moveToRecycleBin(@ApiParam("token") @RequestHeader("token") String token, @ApiParam("文件ID") @PathVariable("fileId") String fileId) {
        // 判断token是否过期，这里MyJwtTool.isValidToken(token)后续需要注意修改
        if (MyJwtTool.isNotValidToken(token)) {
            return Result.build(null, ResultEnum.TOKEN_ERROR);
        }
        boolean result = fileService.moveToRecycleBin(fileId);
        if (!result) {
            throw new RuntimeException("文件移动到回收站异常！");
        }
        return Result.ok();
    }

    /**
     * 将文件从回收站还原
     *
     * @return 文件还原成功或失败的响应信息
     */
    @ApiOperation("将文件从回收站还原接口")
    @PostMapping("/restoreFromRecycleBin/{fileId}")
    public Result<?> restoreFromRecycleBin(@ApiParam("token") @RequestHeader("token") String token, @ApiParam("文件ID") @PathVariable("fileId") String fileId) {
        // 判断token是否过期，这里MyJwtTool.isValidToken(token)后续需要注意修改
        if (MyJwtTool.isNotValidToken(token)) {
            return Result.build(null, ResultEnum.TOKEN_ERROR);
        }
        boolean result = fileService.restoreFromRecycleBin(fileId);
        if (!result) {
            throw new RuntimeException("文件从回收站还原异常！");
        }
        return Result.ok();
    }

    /**
     * 获取回收站文件列表
     *
     * @return 回收站文件列表
     */
    @ApiOperation("获取回收站文件列表接口")
    @GetMapping("/getRecycleBinFileList")
    public Result<?> getRecycleBinFileList(@ApiParam("token") @RequestHeader("token") String token) {
        // 判断token是否过期，这里MyJwtTool.isValidToken(token)后续需要注意修改
        if (MyJwtTool.isNotValidToken(token)) {
            return Result.build(null, ResultEnum.TOKEN_ERROR);
        }
        List<com.ttyang.yourspan.pojo.File> fileList = fileService.getRecycleBinFileList(MyJwtTool.getUidFromToken(token));
        return Result.ok(fileList);
    }

    /**
     * 从前台拿到需要删除的文件的fid，根据卷名及路径通过fastdfs工具类从fastdfs服务器删除该文件，并利用FileService访问数据库通过fid查找到该文件信息并进行删除操作
     *
     * @return 文件删除成功或失败的响应信息
     */
    @ApiOperation("彻底删除文件接口")
    @PostMapping("/deleteFile/{fileId}")
    public Result<?> deleteFile(@ApiParam("token") @RequestHeader("token") String token, @ApiParam("文件ID") @PathVariable("fileId") String fileId) {
        // 判断token是否过期，这里MyJwtTool.isValidToken(token)后续需要注意修改
        if (MyJwtTool.isNotValidToken(token)) {
            return Result.build(null, ResultEnum.TOKEN_ERROR);
        }
        // 取出文件的大小和卷名，并删除表内文件信息
        com.ttyang.yourspan.pojo.File file = fileService.getFileByFid(fileId);
        String fullPath = fileService.deleteFileByFid(fileId);
        // 删除ratings表中相关评分数据
        ratingsService.deleteRatingsByFid(fileId);
        // 更新capacity表中的数据
        long fileSize = file.getFSize();
        capacityService.updateCapacity1(MyJwtTool.getUidFromToken(token), fileSize);
        if (fullPath != null) {
            fastFileStorageClient.deleteFile(fullPath);
        } else {
            throw new RuntimeException("删除文件信息异常！");
        }
        return Result.ok();
    }

    /**
     * 根据用户需要保存的文件的id生成对应的jwt token，该token中包含了文件的id
     *
     * @return 生成的分享文件的token
     */
    @ApiOperation("生成文件token接口")
    @GetMapping("/getShareToken/{fileId}")
    public Result<?> getShareLink(@ApiParam("token") @RequestHeader("token") String token, @ApiParam("文件ID") @PathVariable("fileId") String fileId) {
        // 判断token是否过期，这里MyJwtTool.isValidToken(token)后续需要注意修改
        if (MyJwtTool.isNotValidToken(token)) {
            return Result.build(null, ResultEnum.TOKEN_ERROR);
        }
        return Result.ok(MyJwtTool.createTokenByFid(Integer.valueOf(fileId)));
    }

    /**
     * 通过获取到的token解析出fid并将文件信息返回给前端，选择保存该文件到自己的网盘或者直接下载该文件，另外可以拓展出分享链接附带访问密码的功能
     */
    @ApiOperation("解析文件token接口")
    @GetMapping("/parseShareToken/{fileToken}")
    public Result<?> parseShareToken(@ApiParam("token") @RequestHeader("token") String token, @ApiParam("文件token") @PathVariable("fileToken") String fileToken) {
        // 判断token是否过期，这里MyJwtTool.isValidToken(token)后续需要注意修改
        if (MyJwtTool.isNotValidToken(token)) {
            return Result.build(null, ResultEnum.TOKEN_ERROR);
        }
        Integer fid = MyJwtTool.getFidFromToken(fileToken);
        com.ttyang.yourspan.pojo.File file = fileService.getFileByFid(fid.toString());
        if (file != null) {
            return Result.ok(file);
        } else {
            return Result.build(null, ResultEnum.FILE_NOT_EXIST);
        }
    }

    /**
     * 暂不实现，前台发送该文件的fid和变更后的状态，利用FileService访问数据库通过fid查找到该文件信息并更新文件的权限状态
     *
     * @return 设置权限成功或失败的响应信息
     */
    @ApiOperation("设置文件权限接口")
    @PostMapping("/setFileAuthority/{fileId}")
    public Result<?> setFileAuthority(@ApiParam("token") @RequestHeader("token") String token, @ApiParam("文件ID") @PathVariable("fileId") String fileId, @ApiParam("权限状态") @RequestBody String authority) {
        // 判断token是否过期，这里MyJwtTool.isValidToken(token)后续需要注意修改
        if (MyJwtTool.isNotValidToken(token)) {
            return Result.build(null, ResultEnum.TOKEN_ERROR);
        }
        int startIndex = authority.indexOf(":") + 1;
        int endIndex = authority.indexOf("}");
        authority = authority.substring(startIndex, endIndex);
        boolean result = fileService.setFileAuthority(fileId, authority.equals("true"));
        if (!result) {
            throw new RuntimeException("设置文件权限异常！");
        }
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
        if (MyJwtTool.isNotValidToken(token)) {
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
        if (MyJwtTool.isNotValidToken(token)) {
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
        if (MyJwtTool.isNotValidToken(token)) {
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
        if (MyJwtTool.isNotValidToken(token)) {
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

    /**
     * 获取所有公开文件(去除当前用户公开的文件)
     *
     * @return 所有用户的公开文件列表
     */
    @ApiOperation("获取所有公开文件接口")
    @GetMapping("/getPublicFiles")
    public Result<?> getPublicFiles(@ApiParam("token") @RequestHeader("token") String token) {
        // 判断token是否过期，这里MyJwtTool.isValidToken(token)后续需要注意修改
        if (MyJwtTool.isNotValidToken(token)) {
            return Result.build(null, ResultEnum.TOKEN_ERROR);
        }
        // 获取到当前用户的uid
        Integer uid = MyJwtTool.getUidFromToken(token);
        // 获取所有用户的公开文件
        List<com.ttyang.yourspan.pojo.File> publicFiles = fileService.getPublicFiles();
        // 去除列表中包含当前用户的公开文件
        publicFiles.removeIf(file -> file.getFOwnerId().equals(uid));
        // 获取当前用户的所有文件评分
        List<Ratings> ratings = ratingsService.getRatingsByUid(uid);
        // 将当前用户的所有文件评分存入map中
        Map<Integer, Integer> ratingsMap = new HashMap<>();
        for (Ratings rating : ratings) {
            ratingsMap.put(rating.getFid(), rating.getPreference());
        }
        // 将所有用户的文件评分取出
        List<Ratings> allRatings = ratingsService.list();
        // 将所有用户的文件评分的平均值一并存入map中
        Map<Integer, Double> allAvgRatingsMap = allRatings.stream()
                .collect(Collectors.groupingBy(Ratings::getFid, Collectors.averagingInt(Ratings::getPreference)))
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> (double) Math.round(entry.getValue() * 10) / 10));
        // 遍历publicFiles，将当前用户的评分和该文件的平均评分存入文件对象中
        for (com.ttyang.yourspan.pojo.File publicFile : publicFiles) {
            if (ratingsMap.containsKey(publicFile.getFid())) {
                publicFile.setRate(ratingsMap.get(publicFile.getFid()));
            }
            if (allAvgRatingsMap.containsKey(publicFile.getFid())) {
                publicFile.setAvgRate(allAvgRatingsMap.get(publicFile.getFid()));
            }
        }
        return Result.ok(publicFiles);
    }

    /**
     * 获取对应用户所有的公开文件
     *
     * @return 目标用户的公开文件列表
     */
    @ApiOperation("获取对应用户所有的公开文件接口")
    @GetMapping("/getUserPublicFiles/{uid}")
    public Result<?> getUserPublicFiles(@ApiParam("token") @RequestHeader("token") String token, @ApiParam("目标用户ID") @PathVariable("uid") Integer uid) {
        // 判断token是否过期，这里MyJwtTool.isValidToken(token)后续需要注意修改
        if (MyJwtTool.isNotValidToken(token)) {
            return Result.build(null, ResultEnum.TOKEN_ERROR);
        }
        // 获取所有用户的公开文件
        List<com.ttyang.yourspan.pojo.File> publicFiles = fileService.getPublicFiles();
        // 去除列表中非目标用户的公开文件
        publicFiles.removeIf(file -> !file.getFOwnerId().equals(uid));
        return Result.ok(publicFiles);
    }

    /**
     * 用户添加对应文件的评分
     */
    @ApiOperation("用户添加对应文件的评分接口")
    @PostMapping("/addFileRate/{fileId}")
    public Result<?> addFileRate(@ApiParam("token") @RequestHeader("token") String token, @ApiParam("目标文件ID") @PathVariable("fileId") Integer fileId, @ApiParam("目标文件评分") @RequestBody String rate) {
        // 判断token是否过期，这里MyJwtTool.isValidToken(token)后续需要注意修改
        if (MyJwtTool.isNotValidToken(token)) {
            return Result.build(null, ResultEnum.TOKEN_ERROR);
        }
        // 获取到当前用户的uid
        Integer uid = MyJwtTool.getUidFromToken(token);
        // 处理rate
        int rateInt = Integer.parseInt(rate.substring(rate.indexOf(":") + 1, rate.indexOf("}")));
        // 添加评分
        boolean result = ratingsService.addFileRate(uid, fileId, rateInt);
        if (!result) {
            throw new RuntimeException("添加评分异常！");
        }
        return Result.ok();
    }

    /**
     * 基于用户相似度的推荐方法
     */
    public List<RecommendedItem> recommendByUserCF(Integer uid) {
        // 创建DataModel
        JDBCDataModel dataModel = new MySQLJDBCDataModel(dataSource, "ratings", "uid", "f_id", "preference", null);
        // 使用皮尔逊相关系数计算用户相似度
        UserSimilarity userSimilarity;
        try {
            userSimilarity = new PearsonCorrelationSimilarity(dataModel, Weighting.WEIGHTED);
        } catch (TasteException e) {
            throw new RuntimeException(e);
        }
        // 使用最近邻居算法计算用户邻居
        UserNeighborhood userNeighborhood;
        try {
            userNeighborhood = new NearestNUserNeighborhood(5, userSimilarity, dataModel);
        } catch (TasteException e) {
            throw new RuntimeException(e);
        }
        // 使用GenericUserBasedRecommender进行推荐
        Recommender recommender = new GenericUserBasedRecommender(dataModel, userNeighborhood, userSimilarity);
        // 获取推荐结果
        try {
            return recommender.recommend(uid, 20);
        } catch (TasteException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 基于物品相似度的推荐方法
     */
    public List<RecommendedItem> recommendByItemCF(Integer uid) {
        // 创建DataModel
        JDBCDataModel dataModel = new MySQLJDBCDataModel(dataSource, "ratings", "uid", "f_id", "preference", null);
        // 使用皮尔逊相关系数计算物品相似度
        ItemSimilarity itemSimilarity;
        try {
            itemSimilarity = new PearsonCorrelationSimilarity(dataModel, Weighting.WEIGHTED);
        } catch (TasteException e) {
            throw new RuntimeException(e);
        }
        // 使用GenericItemBasedRecommender进行推荐
        Recommender recommender = new GenericItemBasedRecommender(dataModel, itemSimilarity);
        // 获取推荐结果
        try {
            return recommender.recommend(uid, 20);
        } catch (TasteException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 通过推荐接口获取到的文件ID，获取到文件的详细信息
     */
    @ApiOperation("获取推荐文件的详细信息接口")
    @GetMapping("/getRecommendFileDetail")
    @Cacheable(cacheNames = "recommendFileList")
    public List<com.ttyang.yourspan.pojo.File> getRecommendFileDetail(@ApiParam("token") @RequestHeader("token") String token) {
        // 判断token是否过期，这里MyJwtTool.isValidToken(token)后续需要注意修改
        if (MyJwtTool.isNotValidToken(token)) {
            throw new RuntimeException("token过期！");
        }
        // 获取到当前用户的uid
        Integer uid = MyJwtTool.getUidFromToken(token);
        // 获取到推荐结果
        List<RecommendedItem> recommendedItems = recommendByUserCF(uid);
        // 获取到推荐结果中的文件ID
        List<Integer> fileIds = recommendedItems.stream().map(e -> (int) e.getItemID()).collect(Collectors.toList());
        if (fileIds.size() == 0) {
            return new ArrayList<>();
        }
        return fileService.listByIds(fileIds);
    }

    /**
     * 获取用户剩余的可用空间
     */
    @ApiOperation("获取用户剩余的可用空间接口")
    @GetMapping("/getCapacity")
    public Result<?> getAvailableSpace(@ApiParam("token") @RequestHeader("token") String token) {
        // 判断token是否过期，这里MyJwtTool.isValidToken(token)后续需要注意修改
        if (MyJwtTool.isNotValidToken(token)) {
            return Result.build(null, ResultEnum.TOKEN_ERROR);
        }
        // 获取到当前用户的uid
        Integer uid = MyJwtTool.getUidFromToken(token);
        // 获取用户容量对象
        Capacity capacity = capacityService.getOne(new QueryWrapper<Capacity>().eq("uid", uid));
        Map<String, Capacity> map = new LinkedHashMap<>();
        map.put("capacity", capacity);
        return Result.ok(map);
    }

    /**
     * 自定义测试接口，后续记得删除
     */
    @ApiOperation("测试接口")
    @PostMapping("/test")
    public void test() {
    }
}