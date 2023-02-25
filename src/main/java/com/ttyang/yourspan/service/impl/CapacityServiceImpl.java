package com.ttyang.yourspan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ttyang.yourspan.mapper.CapacityMapper;
import com.ttyang.yourspan.pojo.Capacity;
import com.ttyang.yourspan.service.CapacityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service("capacityServiceImpl")
@Transactional(rollbackFor = Exception.class)
public class CapacityServiceImpl extends ServiceImpl<CapacityMapper, Capacity> implements CapacityService {
    @Override
    public boolean updateCapacity(Integer uid, long fileSize) {
        Capacity capacity = this.getById(uid);
        capacity.setUsed(capacity.getUsed() + fileSize / 1024.0);
        if (capacity.getUsed() > capacity.getTotal()) {
            throw new RuntimeException("容量不足");
        }
        return this.updateById(capacity);
    }

    @Override
    public void updateCapacity1(Integer uidFromToken, long fileSize) {
        Capacity capacity = this.getById(uidFromToken);
        capacity.setUsed(capacity.getUsed() - fileSize / 1024.0);
        this.updateById(capacity);
    }

    @Override
    public List<Capacity> getAllCapacity() {
        return baseMapper.selectList(new QueryWrapper<>());
    }

    @Override
    public void editUserCapacity(Integer valueOf, Integer capacity) {
        Capacity capacity1 = this.getById(valueOf);
        capacity1.setTotal(capacity.doubleValue());
        this.updateById(capacity1);
    }
}
