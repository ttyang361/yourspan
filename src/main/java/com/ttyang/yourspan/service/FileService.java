package com.ttyang.yourspan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ttyang.yourspan.pojo.File;

import java.sql.Date;
import java.util.List;

public interface FileService extends IService<File> {
    List<File> getAllFilesByUid(Integer uid);

    boolean uploadFileInfo(String prefixName, Integer uid, String group, String path, String folderId, Date createTime, Date modifiedTime, Boolean isPublic, Long fileSize);

    File getFileByFid(String fileId);

    boolean modifyFileName(String fileId, String newFileName);

    String deleteFileByFid(String fileId);

    boolean modifyFolderOfFile(String fileId, String targetFolderId);

    boolean setFileAuthority(String fileId, Boolean authority);

    List<File> getPublicFiles();

    boolean moveToRecycleBin(String fileId);

    boolean restoreFromRecycleBin(String fileId);

    List<File> getRecycleBinFileList(Integer uid);
}
