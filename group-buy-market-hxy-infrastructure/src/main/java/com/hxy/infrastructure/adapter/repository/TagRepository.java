package com.hxy.infrastructure.adapter.repository;

import com.hxy.domain.tag.adapter.repository.ITagRepository;
import com.hxy.domain.tag.model.entity.CrowdTagsJobEntity;
import com.hxy.infrastructure.dao.ICrowdTagsDao;
import com.hxy.infrastructure.dao.ICrowdTagsDetailDao;
import com.hxy.infrastructure.dao.ICrowdTagsJobDao;
import com.hxy.infrastructure.dao.po.CrowdTags;
import com.hxy.infrastructure.dao.po.CrowdTagsDetail;
import com.hxy.infrastructure.dao.po.CrowdTagsJob;
import com.hxy.infrastructure.redis.IRedisService;
import org.redisson.api.RBitSet;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;


@Repository
public class TagRepository implements ITagRepository {

    @Resource
    private ICrowdTagsDao crowdTagsDao;
    @Resource
    private ICrowdTagsDetailDao crowdTagsDetailDao;
    @Resource
    private ICrowdTagsJobDao crowdTagsJobDao;
    @Resource
    private IRedisService redisService;


    @Override
    public CrowdTagsJobEntity queryCrowdTagsJobEntity(String tagId, String batchId) {
        CrowdTagsJob crowdTagsJobReq = new CrowdTagsJob();
        crowdTagsJobReq.setTagId(tagId);
        crowdTagsJobReq.setBatchId(batchId);


        CrowdTagsJob crowdTagsJobRes = crowdTagsJobDao.queryCrowdTagsJob(crowdTagsJobReq);

        return  CrowdTagsJobEntity.builder()
                .tagType(crowdTagsJobRes.getTagType())
                .tagRule(crowdTagsJobRes.getTagRule())
                .statStartTime(crowdTagsJobRes.getStatStartTime())
                .statEndTime(crowdTagsJobRes.getStatEndTime())
                .build();
    }

    @Override
    public void addCrowdTagsUserId(String tagId, String userId) {

        CrowdTagsDetail crowdTagsDetailReq = new CrowdTagsDetail();
        crowdTagsDetailReq.setTagId(tagId);
        crowdTagsDetailReq.setUserId(userId);

        crowdTagsDetailDao.addCrowdTagsUserId(crowdTagsDetailReq);

        RBitSet bitSet = redisService.getBitSet(tagId);
        bitSet.set(redisService.getIndexFromUserId(userId));
    }

    @Override
    public void updateCrowdTagsStatistics(String tagId, int size) {

        CrowdTags crowdTagsReq = new CrowdTags();
        crowdTagsReq.setTagId(tagId);
        crowdTagsReq.setStatistics(size);

        crowdTagsDao.updateCrowdTagsStatistics(crowdTagsReq);
    }
}
