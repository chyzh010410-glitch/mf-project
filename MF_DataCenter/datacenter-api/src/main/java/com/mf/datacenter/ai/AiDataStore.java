package com.mf.datacenter.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mf.datacenter.common.PageResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mf.datacenter.ai.entity.AiConversationLogEntity;
import com.mf.datacenter.ai.entity.AiToolCallLogEntity;
import com.mf.datacenter.ai.entity.SampleCandidateEntity;
import com.mf.datacenter.ai.entity.UnresolvedQuestionEntity;
import com.mf.datacenter.ai.mapper.AiConversationLogMapper;
import com.mf.datacenter.ai.mapper.AiHistoryAuditLogMapper;
import com.mf.datacenter.ai.mapper.AiToolCallLogMapper;
import com.mf.datacenter.ai.mapper.SampleCandidateMapper;
import com.mf.datacenter.ai.mapper.UnresolvedQuestionMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Component
public class AiDataStore {

    private final ObjectMapper objectMapper;
    private final Path dataFile;
    private final boolean mysqlEnabled;
    private final String jdbcUrl;
    private final AiConversationLogMapper conversationMapper;
    private final AiHistoryAuditLogMapper historyAuditLogMapper;
    private final AiToolCallLogMapper toolCallMapper;
    private final UnresolvedQuestionMapper unresolvedMapper;
    private final SampleCandidateMapper sampleMapper;
    private final AtomicLong conversationIds = new AtomicLong(1000);
    private final AtomicLong toolCallIds = new AtomicLong(1500);
    private final AtomicLong unresolvedIds = new AtomicLong(2000);
    private final AtomicLong sampleIds = new AtomicLong(3000);
    private final List<AiRecords.Conversation> conversations = new ArrayList<>();
    private final List<AiRecords.ToolCall> toolCalls = new ArrayList<>();
    private final List<AiRecords.UnresolvedQuestion> unresolvedQuestions = new ArrayList<>();
    private final List<AiRecords.SampleCandidate> sampleCandidates = new ArrayList<>();

    public AiDataStore(
            ObjectMapper objectMapper,
            @Value("${datacenter.storage.ai-data-file:data/ai-store.json}") String dataFile,
            @Value("${datacenter.mysql.enabled:false}") boolean mysqlEnabled,
            @Value("${spring.datasource.url:}") String jdbcUrl,
            ObjectProvider<AiConversationLogMapper> conversationMapper,
            ObjectProvider<AiHistoryAuditLogMapper> historyAuditLogMapper,
            ObjectProvider<AiToolCallLogMapper> toolCallMapper,
            ObjectProvider<UnresolvedQuestionMapper> unresolvedMapper,
            ObjectProvider<SampleCandidateMapper> sampleMapper
    ) {
        this.objectMapper = objectMapper.findAndRegisterModules();
        this.dataFile = Path.of(dataFile);
        this.mysqlEnabled = mysqlEnabled;
        this.jdbcUrl = jdbcUrl;
        this.conversationMapper = conversationMapper.getIfAvailable();
        this.historyAuditLogMapper = historyAuditLogMapper.getIfAvailable();
        this.toolCallMapper = toolCallMapper.getIfAvailable();
        this.unresolvedMapper = unresolvedMapper.getIfAvailable();
        this.sampleMapper = sampleMapper.getIfAvailable();
        loadOrSeed();
    }

    private void loadOrSeed() {
        if (mysqlEnabled) {
            ensureMysqlReady();
            return;
        }
        if (Files.exists(dataFile)) {
            load();
            return;
        }
        var firstConversation = createConversation(new AiRecords.CreateConversationRequest(
                "MF_AgentService", "demo-session-1", "u-1001", "consumer",
                "番茄叶片发黄应该补什么肥？", "建议先确认土壤湿度，再补充含镁水溶肥。", "fertilizer_advice", true, 5
        ));
        conversations.add(firstConversation);
        unresolvedQuestions.add(createUnresolved(new AiRecords.CreateUnresolvedQuestionRequest(
                firstConversation.id(), "葡萄裂果和补钙时机怎么判断？", "缺少区域作物案例", "pending", "high", null, "content-ops", "待补充百科", "补充区域作物案例"
        )));
        sampleCandidates.add(createSample(new AiRecords.CreateSampleCandidateRequest(
                firstConversation.id(), firstConversation.question(), firstConversation.answer(), "conversation", "good", "pending", "", "", false
        )));
        toolCalls.add(createToolCall(new AiRecords.CreateToolCallRequest(
                firstConversation.id(), "fertilizer_knowledge_search", "番茄 黄叶 补肥", "命中 3 条百科片段", true, "", 126L
        )));
        save();
    }

    public synchronized AiRecords.Conversation addConversation(AiRecords.CreateConversationRequest request) {
        if (mysqlEnabled) {
            var entity = new AiConversationLogEntity();
            entity.setSource(request.source());
            entity.setSessionId(request.sessionId());
            entity.setUserId(request.userId());
            entity.setUserType(request.userType());
            entity.setQuestion(request.question());
            entity.setAnswer(request.answer());
            entity.setIntent(request.intent());
            entity.setResolved(request.resolved());
            entity.setSatisfaction(request.satisfaction());
            entity.setCreateTime(LocalDateTime.now());
            conversationMapper.insert(entity);
            return toConversation(entity);
        }
        var record = createConversation(request);
        conversations.add(record);
        save();
        return record;
    }

    public synchronized AiRecords.Conversation updateSatisfaction(Long id, Integer satisfaction) {
        if (satisfaction == null || satisfaction < 1 || satisfaction > 5) throw new IllegalArgumentException("satisfaction must be between 1 and 5");
        if (mysqlEnabled) {
            var entity = conversationMapper.selectById(id);
            if (entity == null) throw new IllegalArgumentException("conversation not found");
            entity.setSatisfaction(satisfaction); conversationMapper.updateById(entity); return toConversation(entity);
        }
        for (int index = 0; index < conversations.size(); index++) {
            var item = conversations.get(index);
            if (item.id().equals(id)) {
                var updated = new AiRecords.Conversation(item.id(), item.source(), item.sessionId(), item.userId(), item.userType(), item.question(), item.answer(), item.intent(), item.resolved(), satisfaction, item.createTime());
                conversations.set(index, updated); save(); return updated;
            }
        }
        throw new IllegalArgumentException("conversation not found");
    }

    public synchronized List<AiRecords.Conversation> conversations() {
        if (mysqlEnabled) {
            return conversationMapper.selectList(new LambdaQueryWrapper<AiConversationLogEntity>()
                            .isNull(AiConversationLogEntity::getUserDeletedAt)
                            .orderByDesc(AiConversationLogEntity::getCreateTime)
                            .orderByDesc(AiConversationLogEntity::getId))
                    .stream()
                    .map(this::toConversation)
                    .toList();
        }
        return conversations.stream()
                .sorted(Comparator.comparing(AiRecords.Conversation::createTime).reversed())
                .toList();
    }

    public synchronized List<AiRecords.Conversation> conversations(String source, String intent, LocalDateTime startTime, LocalDateTime endTime) {
        return conversations().stream()
                .filter(item -> !hasText(source) || source.equals(item.source()))
                .filter(item -> !hasText(intent) || intent.equals(item.intent()))
                .filter(item -> startTime == null || !item.createTime().isBefore(startTime))
                .filter(item -> endTime == null || !item.createTime().isAfter(endTime))
                .toList();
    }

    public synchronized PageResult<AiRecords.Conversation> conversationPage(String source, String intent, long pageNo, long pageSize) {
        var safePageNo = Math.max(pageNo, 1); var safePageSize = Math.min(Math.max(pageSize, 1), 100);
        if (mysqlEnabled) {
            var query = new LambdaQueryWrapper<AiConversationLogEntity>().eq(hasText(source), AiConversationLogEntity::getSource, source)
                    .eq(hasText(intent), AiConversationLogEntity::getIntent, intent).orderByDesc(AiConversationLogEntity::getCreateTime).orderByDesc(AiConversationLogEntity::getId);
            query.isNull(AiConversationLogEntity::getUserDeletedAt);
            var page = conversationMapper.selectPage(new Page<>(safePageNo, safePageSize), query);
            return PageResult.of(page.getRecords().stream().map(this::toConversation).toList(), page.getTotal(), safePageNo, safePageSize);
        }
        return page(conversations(source, intent, null, null), safePageNo, safePageSize);
    }

    public synchronized AiRecords.ConversationTrace conversationTrace(Long conversationId) {
        var conversation = conversations().stream().filter(item -> item.id().equals(conversationId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("conversation not found"));
        return new AiRecords.ConversationTrace(
                conversation,
                toolCalls().stream().filter(item -> conversationId.equals(item.conversationId())).toList(),
                unresolvedQuestions(null, null).stream().filter(item -> conversationId.equals(item.conversationId())).toList(),
                sampleCandidates(null, null).stream().filter(item -> conversationId.equals(item.conversationId())).toList()
        );
    }

    public synchronized AiRecords.UserConversationPage userConversations(String userId, String sessionId, long page, long pageSize) {
        var safePage = Math.max(page, 1); var safeSize = Math.min(Math.max(pageSize, 1), 100);
        if (mysqlEnabled) {
            if (sessionBelongsToOtherUser(userId, sessionId)) { audit("read", "forbidden", userId, sessionId, "session belongs to another user"); throw new HistoryAccessService.HistoryForbiddenException("会话不属于当前用户"); }
            var query = new LambdaQueryWrapper<AiConversationLogEntity>().eq(AiConversationLogEntity::getUserId, userId).eq(AiConversationLogEntity::getSessionId, sessionId)
                    .isNull(AiConversationLogEntity::getUserDeletedAt).orderByAsc(AiConversationLogEntity::getCreateTime).orderByAsc(AiConversationLogEntity::getId);
            var result = conversationMapper.selectPage(new Page<>(safePage, safeSize), query);
            audit("read", "ok", userId, sessionId, null);
            return new AiRecords.UserConversationPage(sessionId, result.getRecords().stream().map(this::toUserConversation).toList(), safePage, safeSize, result.getTotal());
        }
        var rows = conversations.stream().filter(item -> userId.equals(item.userId()) && sessionId.equals(item.sessionId())).sorted(java.util.Comparator.comparing(AiRecords.Conversation::createTime)).toList();
        return new AiRecords.UserConversationPage(sessionId, rows.stream().skip((safePage - 1) * safeSize).limit(safeSize).map(this::toUserConversation).toList(), safePage, safeSize, rows.size());
    }

    public synchronized AiRecords.DeleteConversationResult deleteUserConversations(String userId, String sessionId) {
        if (mysqlEnabled) {
            if (sessionBelongsToOtherUser(userId, sessionId)) { audit("delete", "forbidden", userId, sessionId, "session belongs to another user"); throw new HistoryAccessService.HistoryForbiddenException("会话不属于当前用户"); }
            var count = conversationMapper.update(null, new LambdaUpdateWrapper<AiConversationLogEntity>().eq(AiConversationLogEntity::getUserId, userId).eq(AiConversationLogEntity::getSessionId, sessionId)
                    .isNull(AiConversationLogEntity::getUserDeletedAt).set(AiConversationLogEntity::getUserDeletedAt, LocalDateTime.now()));
            audit("delete", "ok", userId, sessionId, "deleted=" + count);
            return new AiRecords.DeleteConversationResult(sessionId, count);
        }
        var count = conversations.removeIf(item -> userId.equals(item.userId()) && sessionId.equals(item.sessionId())) ? 1 : 0;
        return new AiRecords.DeleteConversationResult(sessionId, count);
    }

    public synchronized AiRecords.ToolCall addToolCall(AiRecords.CreateToolCallRequest request) {
        if (mysqlEnabled) {
            var entity = new AiToolCallLogEntity();
            entity.setConversationId(request.conversationId());
            entity.setToolName(request.toolName());
            entity.setRequestSummary(request.requestSummary());
            entity.setResponseSummary(request.responseSummary());
            entity.setSuccess(request.success());
            entity.setErrorMessage(request.errorMessage());
            entity.setDurationMs(request.durationMs());
            entity.setCreateTime(LocalDateTime.now());
            toolCallMapper.insert(entity);
            return toToolCall(entity);
        }
        var record = createToolCall(request);
        toolCalls.add(record);
        save();
        return record;
    }

    public synchronized List<AiRecords.ToolCall> toolCalls() {
        if (mysqlEnabled) {
            return toolCallMapper.selectList(new LambdaQueryWrapper<AiToolCallLogEntity>()
                            .orderByDesc(AiToolCallLogEntity::getCreateTime)
                            .orderByDesc(AiToolCallLogEntity::getId))
                    .stream()
                    .map(this::toToolCall)
                    .toList();
        }
        return toolCalls.stream()
                .sorted(Comparator.comparing(AiRecords.ToolCall::createTime).reversed())
                .toList();
    }

    public synchronized PageResult<AiRecords.ToolCall> toolCallPage(long pageNo, long pageSize) {
        var safePageNo = Math.max(pageNo, 1); var safePageSize = Math.min(Math.max(pageSize, 1), 100);
        if (mysqlEnabled) {
            var page = toolCallMapper.selectPage(new Page<>(safePageNo, safePageSize), new LambdaQueryWrapper<AiToolCallLogEntity>().orderByDesc(AiToolCallLogEntity::getCreateTime).orderByDesc(AiToolCallLogEntity::getId));
            return PageResult.of(page.getRecords().stream().map(this::toToolCall).toList(), page.getTotal(), safePageNo, safePageSize);
        }
        return page(toolCalls(), safePageNo, safePageSize);
    }

    public synchronized AiRecords.AiStats stats() {
        if (mysqlEnabled) {
            var conversations = conversationMapper.selectList(new LambdaQueryWrapper<AiConversationLogEntity>().isNull(AiConversationLogEntity::getUserDeletedAt));
            var activeUnresolved = unresolvedMapper.selectCount(new LambdaQueryWrapper<UnresolvedQuestionEntity>()
                    .in(UnresolvedQuestionEntity::getStatus, "pending", "processing"));
            var frequentQuestions = conversationMapper.frequentQuestions().stream()
                    .map(row -> new AiRecords.QuestionCount(
                            String.valueOf(row.get("question")),
                            ((Number) row.get("total")).longValue()))
                    .toList();
            return new AiRecords.AiStats(
                    conversations.size(),
                    (int) conversations.stream().map(AiConversationLogEntity::getUserId).filter(this::hasText).distinct().count(),
                    activeUnresolved,
                    Math.toIntExact(toolCallMapper.selectCount(null)),
                    Math.toIntExact(sampleMapper.selectCount(null)),
                    frequentQuestions
            );
        }
        var uniqueUsers = conversations.stream()
                .map(AiRecords.Conversation::userId)
                .filter(this::hasText)
                .collect(Collectors.toSet())
                .size();
        var activeUnresolved = unresolvedQuestions.stream()
                .filter(item -> List.of("pending", "processing").contains(item.status()))
                .count();
        var frequentQuestions = conversations.stream()
                .collect(Collectors.groupingBy(AiRecords.Conversation::question, Collectors.counting()))
                .entrySet()
                .stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(8)
                .map(item -> new AiRecords.QuestionCount(item.getKey(), item.getValue()))
                .toList();
        return new AiRecords.AiStats(conversations.size(), uniqueUsers, activeUnresolved, toolCalls.size(), sampleCandidates.size(), frequentQuestions);
    }

    public synchronized AiRecords.UnresolvedQuestion addUnresolved(AiRecords.CreateUnresolvedQuestionRequest request) {
        validateStatus(request.status(), List.of("pending", "processing", "resolved", "ignored"));
        if (mysqlEnabled) {
            var entity = new UnresolvedQuestionEntity();
            entity.setConversationId(request.conversationId());
            entity.setQuestion(request.question());
            entity.setReason(request.reason());
            entity.setStatus(request.status());
            entity.setPriority(normalizePriority(request.priority()));
            entity.setDueTime(request.dueTime());
            entity.setOwner(request.owner());
            entity.setRemark(request.remark());
            entity.setKnowledgeAction(request.knowledgeAction());
            entity.setCreateTime(LocalDateTime.now());
            entity.setUpdateTime(entity.getCreateTime());
            unresolvedMapper.insert(entity);
            return toUnresolved(entity);
        }
        var record = createUnresolved(request);
        unresolvedQuestions.add(record);
        save();
        return record;
    }

    public synchronized List<AiRecords.UnresolvedQuestion> unresolvedQuestions(String status, String keyword) {
        if (mysqlEnabled) {
            var query = new LambdaQueryWrapper<UnresolvedQuestionEntity>()
                    .eq(hasText(status), UnresolvedQuestionEntity::getStatus, status)
                    .and(hasText(keyword), wrapper -> wrapper
                            .like(UnresolvedQuestionEntity::getQuestion, keyword)
                            .or()
                            .like(UnresolvedQuestionEntity::getReason, keyword))
                    .orderByDesc(UnresolvedQuestionEntity::getUpdateTime)
                    .orderByDesc(UnresolvedQuestionEntity::getId);
            return unresolvedMapper.selectList(query).stream().map(this::toUnresolved).toList();
        }
        return unresolvedQuestions.stream()
                .filter(item -> !hasText(status) || item.status().equals(status))
                .filter(item -> contains(item.question(), keyword) || contains(item.reason(), keyword))
                .sorted(Comparator.comparing(AiRecords.UnresolvedQuestion::updateTime).reversed())
                .toList();
    }

    public synchronized PageResult<AiRecords.UnresolvedQuestion> unresolvedPage(String status, String keyword, long pageNo, long pageSize) {
        var safePageNo = Math.max(pageNo, 1); var safePageSize = Math.min(Math.max(pageSize, 1), 100);
        if (mysqlEnabled) {
            var query = new LambdaQueryWrapper<UnresolvedQuestionEntity>().eq(hasText(status), UnresolvedQuestionEntity::getStatus, status)
                    .and(hasText(keyword), wrapper -> wrapper.like(UnresolvedQuestionEntity::getQuestion, keyword).or().like(UnresolvedQuestionEntity::getReason, keyword))
                    .orderByDesc(UnresolvedQuestionEntity::getUpdateTime).orderByDesc(UnresolvedQuestionEntity::getId);
            var result = unresolvedMapper.selectPage(new Page<>(safePageNo, safePageSize), query);
            return PageResult.of(result.getRecords().stream().map(this::toUnresolved).toList(), result.getTotal(), safePageNo, safePageSize);
        }
        return page(unresolvedQuestions(status, keyword), safePageNo, safePageSize);
    }

    public synchronized AiRecords.UnresolvedQuestion updateUnresolved(Long id, AiRecords.UpdateUnresolvedStatusRequest request) {
        validateStatus(request.status(), List.of("pending", "processing", "resolved", "ignored"));
        if (mysqlEnabled) {
            var updated = unresolvedMapper.update(null, new LambdaUpdateWrapper<UnresolvedQuestionEntity>()
                    .eq(UnresolvedQuestionEntity::getId, id)
                    .set(UnresolvedQuestionEntity::getStatus, request.status())
                    .set(UnresolvedQuestionEntity::getPriority, normalizePriority(request.priority()))
                    .set(UnresolvedQuestionEntity::getDueTime, request.dueTime())
                    .set(UnresolvedQuestionEntity::getOwner, request.owner())
                    .set(UnresolvedQuestionEntity::getRemark, request.remark())
                    .set(UnresolvedQuestionEntity::getKnowledgeAction, request.knowledgeAction())
                    .set(UnresolvedQuestionEntity::getUpdateTime, LocalDateTime.now()));
            if (updated == 0) {
                throw new IllegalArgumentException("unresolved question not found");
            }
            return toUnresolved(unresolvedMapper.selectById(id));
        }
        for (int i = 0; i < unresolvedQuestions.size(); i++) {
            var item = unresolvedQuestions.get(i);
            if (item.id().equals(id)) {
                var updated = new AiRecords.UnresolvedQuestion(
                        item.id(), item.conversationId(), item.question(), item.reason(),
                        request.status(), normalizePriority(request.priority()), request.dueTime(), request.owner(), request.remark(), request.knowledgeAction(), item.createTime(), LocalDateTime.now()
                );
                unresolvedQuestions.set(i, updated);
                save();
                return updated;
            }
        }
        throw new IllegalArgumentException("unresolved question not found");
    }

    public synchronized AiRecords.SampleCandidate addSample(AiRecords.CreateSampleCandidateRequest request) {
        validateStatus(request.reviewStatus(), List.of("pending", "approved", "rejected"));
        if (mysqlEnabled) {
            var entity = new SampleCandidateEntity();
            entity.setConversationId(request.conversationId());
            entity.setQuestion(request.question());
            entity.setAnswer(request.answer());
            entity.setSource(request.source());
            entity.setQualityStatus(request.qualityStatus());
            entity.setReviewStatus(request.reviewStatus());
            entity.setReviewer(request.reviewer());
            entity.setReviewRemark(request.reviewRemark());
            entity.setRecommendedForKnowledge(Boolean.TRUE.equals(request.recommendedForKnowledge()));
            entity.setCreateTime(LocalDateTime.now());
            entity.setUpdateTime(entity.getCreateTime());
            sampleMapper.insert(entity);
            return toSample(entity);
        }
        var record = createSample(request);
        sampleCandidates.add(record);
        save();
        return record;
    }

    public synchronized List<AiRecords.SampleCandidate> sampleCandidates(String reviewStatus, String keyword) {
        if (mysqlEnabled) {
            var query = new LambdaQueryWrapper<SampleCandidateEntity>()
                    .eq(hasText(reviewStatus), SampleCandidateEntity::getReviewStatus, reviewStatus)
                    .and(hasText(keyword), wrapper -> wrapper
                            .like(SampleCandidateEntity::getQuestion, keyword)
                            .or()
                            .like(SampleCandidateEntity::getAnswer, keyword))
                    .orderByDesc(SampleCandidateEntity::getUpdateTime)
                    .orderByDesc(SampleCandidateEntity::getId);
            return sampleMapper.selectList(query).stream().map(this::toSample).toList();
        }
        return sampleCandidates.stream()
                .filter(item -> !hasText(reviewStatus) || item.reviewStatus().equals(reviewStatus))
                .filter(item -> contains(item.question(), keyword) || contains(item.answer(), keyword))
                .sorted(Comparator.comparing(AiRecords.SampleCandidate::updateTime).reversed())
                .toList();
    }

    public synchronized PageResult<AiRecords.SampleCandidate> samplePage(String reviewStatus, String keyword, long pageNo, long pageSize) {
        var safePageNo = Math.max(pageNo, 1); var safePageSize = Math.min(Math.max(pageSize, 1), 100);
        if (mysqlEnabled) {
            var query = new LambdaQueryWrapper<SampleCandidateEntity>().eq(hasText(reviewStatus), SampleCandidateEntity::getReviewStatus, reviewStatus)
                    .and(hasText(keyword), wrapper -> wrapper.like(SampleCandidateEntity::getQuestion, keyword).or().like(SampleCandidateEntity::getAnswer, keyword))
                    .orderByDesc(SampleCandidateEntity::getUpdateTime).orderByDesc(SampleCandidateEntity::getId);
            var result = sampleMapper.selectPage(new Page<>(safePageNo, safePageSize), query);
            return PageResult.of(result.getRecords().stream().map(this::toSample).toList(), result.getTotal(), safePageNo, safePageSize);
        }
        return page(sampleCandidates(reviewStatus, keyword), safePageNo, safePageSize);
    }

    public synchronized AiRecords.SampleCandidate reviewSample(Long id, AiRecords.ReviewSampleRequest request) {
        validateStatus(request.reviewStatus(), List.of("pending", "approved", "rejected"));
        if (mysqlEnabled) {
            var updated = sampleMapper.update(null, new LambdaUpdateWrapper<SampleCandidateEntity>()
                    .eq(SampleCandidateEntity::getId, id)
                    .set(SampleCandidateEntity::getReviewStatus, request.reviewStatus())
                    .set(SampleCandidateEntity::getReviewer, request.reviewer())
                    .set(SampleCandidateEntity::getReviewRemark, request.reviewRemark())
                    .set(SampleCandidateEntity::getRecommendedForKnowledge, Boolean.TRUE.equals(request.recommendedForKnowledge()))
                    .set(SampleCandidateEntity::getUpdateTime, LocalDateTime.now()));
            if (updated == 0) {
                throw new IllegalArgumentException("sample candidate not found");
            }
            return toSample(sampleMapper.selectById(id));
        }
        for (int i = 0; i < sampleCandidates.size(); i++) {
            var item = sampleCandidates.get(i);
            if (item.id().equals(id)) {
                var updated = new AiRecords.SampleCandidate(
                        item.id(), item.conversationId(), item.question(), item.answer(), item.source(),
                        item.qualityStatus(), request.reviewStatus(), request.reviewer(), request.reviewRemark(), Boolean.TRUE.equals(request.recommendedForKnowledge()),
                        item.createTime(), LocalDateTime.now()
                );
                sampleCandidates.set(i, updated);
                save();
                return updated;
            }
        }
        throw new IllegalArgumentException("sample candidate not found");
    }

    private void validateStatus(String status, List<String> allowed) {
        if (status == null || !allowed.contains(status)) {
            throw new IllegalArgumentException("invalid status: " + status);
        }
    }

    private void ensureMysqlReady() {
        if (!hasText(jdbcUrl)) {
            throw new IllegalStateException("spring.datasource.url is required when datacenter.mysql.enabled=true");
        }
        requireMappers();
        if (conversationMapper.selectCount(null) > 0) {
            return;
        }
        var firstConversation = addConversation(new AiRecords.CreateConversationRequest(
                "MF_AgentService", "demo-session-1", "u-1001", "consumer",
                "番茄叶片发黄应该补什么肥？", "建议先确认土壤湿度，再补充含镁水溶肥。", "fertilizer_advice", true, 5
        ));
        addUnresolved(new AiRecords.CreateUnresolvedQuestionRequest(
                firstConversation.id(), "葡萄裂果和补钙时机怎么判断？", "缺少区域作物案例", "pending", "high", null, "content-ops", "待补充百科", "补充区域作物案例"
        ));
        addSample(new AiRecords.CreateSampleCandidateRequest(
                firstConversation.id(), firstConversation.question(), firstConversation.answer(), "conversation", "good", "pending", "", "", false
        ));
        addToolCall(new AiRecords.CreateToolCallRequest(
                firstConversation.id(), "fertilizer_knowledge_search", "番茄 黄叶 补肥", "命中 3 条百科片段", true, "", 126L
        ));
    }

    private void requireMappers() {
        if (conversationMapper == null || toolCallMapper == null || unresolvedMapper == null || sampleMapper == null) {
            throw new IllegalStateException("MyBatis-Plus mappers are required when datacenter.mysql.enabled=true");
        }
    }

    private AiRecords.Conversation toConversation(AiConversationLogEntity entity) {
        return new AiRecords.Conversation(entity.getId(), entity.getSource(), entity.getSessionId(), entity.getUserId(), entity.getUserType(),
                entity.getQuestion(), entity.getAnswer(), entity.getIntent(), entity.getResolved(), entity.getSatisfaction(), entity.getCreateTime());
    }

    private AiRecords.UserConversation toUserConversation(AiConversationLogEntity entity) { return new AiRecords.UserConversation(entity.getId(), entity.getQuestion(), entity.getAnswer(), entity.getIntent(), entity.getCreateTime()); }
    private AiRecords.UserConversation toUserConversation(AiRecords.Conversation item) { return new AiRecords.UserConversation(item.id(), item.question(), item.answer(), item.intent(), item.createTime()); }

    private boolean sessionBelongsToOtherUser(String userId, String sessionId) { return conversationMapper.selectCount(new LambdaQueryWrapper<AiConversationLogEntity>().eq(AiConversationLogEntity::getSessionId, sessionId).ne(AiConversationLogEntity::getUserId, userId)) > 0; }
    private void audit(String action, String outcome, String userId, String sessionId, String detail) { if (historyAuditLogMapper == null) return; var row = new com.mf.datacenter.ai.entity.AiHistoryAuditLogEntity(); row.setAction(action); row.setOutcome(outcome); row.setActorUserId(userId); row.setSessionId(sessionId); row.setDetail(detail); row.setCreateTime(LocalDateTime.now()); historyAuditLogMapper.insert(row); }

    private AiRecords.ToolCall toToolCall(AiToolCallLogEntity entity) {
        return new AiRecords.ToolCall(entity.getId(), entity.getConversationId(), entity.getToolName(), entity.getRequestSummary(),
                entity.getResponseSummary(), entity.getSuccess(), entity.getErrorMessage(), entity.getDurationMs(), entity.getCreateTime());
    }

    private AiRecords.UnresolvedQuestion toUnresolved(UnresolvedQuestionEntity entity) {
        return new AiRecords.UnresolvedQuestion(entity.getId(), entity.getConversationId(), entity.getQuestion(), entity.getReason(),
                entity.getStatus(), entity.getPriority(), entity.getDueTime(), entity.getOwner(), entity.getRemark(), entity.getKnowledgeAction(), entity.getCreateTime(), entity.getUpdateTime());
    }

    private AiRecords.SampleCandidate toSample(SampleCandidateEntity entity) {
        return new AiRecords.SampleCandidate(entity.getId(), entity.getConversationId(), entity.getQuestion(), entity.getAnswer(),
                entity.getSource(), entity.getQualityStatus(), entity.getReviewStatus(), entity.getReviewer(), entity.getReviewRemark(),
                entity.getRecommendedForKnowledge(), entity.getCreateTime(), entity.getUpdateTime());
    }

    private AiRecords.Conversation createConversation(AiRecords.CreateConversationRequest request) {
        return new AiRecords.Conversation(conversationIds.incrementAndGet(), request.source(), request.sessionId(), request.userId(),
                request.userType(), request.question(), request.answer(), request.intent(), request.resolved(), request.satisfaction(), LocalDateTime.now());
    }

    private AiRecords.UnresolvedQuestion createUnresolved(AiRecords.CreateUnresolvedQuestionRequest request) {
        var now = LocalDateTime.now();
        return new AiRecords.UnresolvedQuestion(unresolvedIds.incrementAndGet(), request.conversationId(), request.question(), request.reason(),
                request.status(), normalizePriority(request.priority()), request.dueTime(), request.owner(), request.remark(), request.knowledgeAction(), now, now);
    }

    private AiRecords.ToolCall createToolCall(AiRecords.CreateToolCallRequest request) {
        return new AiRecords.ToolCall(toolCallIds.incrementAndGet(), request.conversationId(), request.toolName(), request.requestSummary(),
                request.responseSummary(), request.success(), request.errorMessage(), request.durationMs(), LocalDateTime.now());
    }

    private AiRecords.SampleCandidate createSample(AiRecords.CreateSampleCandidateRequest request) {
        var now = LocalDateTime.now();
        return new AiRecords.SampleCandidate(sampleIds.incrementAndGet(), request.conversationId(), request.question(), request.answer(),
                request.source(), request.qualityStatus(), request.reviewStatus(), request.reviewer(), request.reviewRemark(), Boolean.TRUE.equals(request.recommendedForKnowledge()), now, now);
    }

    private boolean contains(String value, String keyword) {
        return !hasText(keyword) || (value != null && value.contains(keyword));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String normalizePriority(String priority) {
        return priority != null && List.of("low", "normal", "high", "urgent").contains(priority) ? priority : "normal";
    }

    private <T> PageResult<T> page(List<T> rows, long pageNo, long pageSize) {
        var from = (int) Math.min((pageNo - 1) * pageSize, rows.size());
        var to = (int) Math.min(from + pageSize, rows.size());
        return PageResult.of(rows.subList(from, to), rows.size(), pageNo, pageSize);
    }

    private void load() {
        try {
            var state = objectMapper.readValue(dataFile.toFile(), StoredAiState.class);
            conversations.clear();
            toolCalls.clear();
            unresolvedQuestions.clear();
            sampleCandidates.clear();
            conversations.addAll(state.conversations() == null ? List.of() : state.conversations());
            toolCalls.addAll(state.toolCalls() == null ? List.of() : state.toolCalls());
            unresolvedQuestions.addAll(state.unresolvedQuestions() == null ? List.of() : state.unresolvedQuestions());
            sampleCandidates.addAll(state.sampleCandidates() == null ? List.of() : state.sampleCandidates());
            conversationIds.set(maxConversationId());
            toolCallIds.set(maxToolCallId());
            unresolvedIds.set(maxUnresolvedId());
            sampleIds.set(maxSampleId());
        } catch (IOException ex) {
            throw new IllegalStateException("failed to load AI data store: " + dataFile, ex);
        }
    }

    private void save() {
        try {
            var parent = dataFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(dataFile.toFile(),
                    new StoredAiState(conversations, toolCalls, unresolvedQuestions, sampleCandidates));
        } catch (IOException ex) {
            throw new IllegalStateException("failed to save AI data store: " + dataFile, ex);
        }
    }

    private long maxConversationId() {
        return conversations.stream().map(AiRecords.Conversation::id).max(Long::compareTo).orElse(1000L);
    }

    private long maxToolCallId() {
        return toolCalls.stream().map(AiRecords.ToolCall::id).max(Long::compareTo).orElse(1500L);
    }

    private long maxUnresolvedId() {
        return unresolvedQuestions.stream().map(AiRecords.UnresolvedQuestion::id).max(Long::compareTo).orElse(2000L);
    }

    private long maxSampleId() {
        return sampleCandidates.stream().map(AiRecords.SampleCandidate::id).max(Long::compareTo).orElse(3000L);
    }

    private record StoredAiState(
            List<AiRecords.Conversation> conversations,
            List<AiRecords.ToolCall> toolCalls,
            List<AiRecords.UnresolvedQuestion> unresolvedQuestions,
            List<AiRecords.SampleCandidate> sampleCandidates
    ) {
    }
}
