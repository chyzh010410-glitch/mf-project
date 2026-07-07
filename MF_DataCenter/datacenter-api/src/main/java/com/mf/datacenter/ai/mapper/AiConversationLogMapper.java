package com.mf.datacenter.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mf.datacenter.ai.entity.AiConversationLogEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface AiConversationLogMapper extends BaseMapper<AiConversationLogEntity> {

    @Select("""
            SELECT question, COUNT(*) AS total
            FROM dc_ai_conversation_log
            GROUP BY question
            ORDER BY total DESC
            LIMIT 8
            """)
    List<Map<String, Object>> frequentQuestions();
}
