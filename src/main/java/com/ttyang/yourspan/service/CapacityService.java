package com.ttyang.yourspan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ttyang.yourspan.pojo.Capacity;

public interface CapacityService extends IService<Capacity> {
    boolean updateCapacity(Integer uid, long fileSize);

    void updateCapacity1(Integer uidFromToken, long fileSize);
}
