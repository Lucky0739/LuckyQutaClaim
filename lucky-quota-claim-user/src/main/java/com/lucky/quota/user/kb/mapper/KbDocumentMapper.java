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
}
