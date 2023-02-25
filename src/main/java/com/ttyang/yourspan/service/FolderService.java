package com.ttyang.yourspan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ttyang.yourspan.pojo.Folder;

import java.sql.Date;
import java.util.List;

public interface FolderService extends IService<Folder> {
    List<Folder> getAllFoldersByUid(Integer uid);

    boolean createNewFolder(String folderName, String parentFolderId, Integer uid, Date createTime, Date modifiedTime);

    boolean modifyFolderName(String currentFolderId, String newFolderName);

    Folder getFolderByUidAndParentFolderId(Integer uid, int parentFolderId);

    void deleteFolderByFlid(Integer flid);
}
