package com.lucky.quota.user.kb.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lucky.quota.user.kb.model.KbDocument;

import java.util.List;

public interface KbDocumentMapper extends BaseMapper<KbDocument> {

    int insert(KbDocument document);

    int updateStatus(KbDocument document);

    KbDocument selectById(Long id);

    List<KbDocument> selectAllDocuments();

    int deleteById(Long id);

    /** 待离线索引：排队中或处理中超时（默认 30 分钟） */
    List<Long> selectPendingForOfflineIndex(int limit);

    /** 抢占离线索引任务，返回影响行数 */
    int claimForOfflineIndex(Long id);
}
