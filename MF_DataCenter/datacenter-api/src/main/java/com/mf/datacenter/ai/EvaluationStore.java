package com.mf.datacenter.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mf.datacenter.ai.entity.*;
import com.mf.datacenter.ai.mapper.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;

@Component
public class EvaluationStore {
    private final AtomicLong suiteIds = new AtomicLong(1);
    private final AtomicLong caseIds = new AtomicLong(1);
    private final AtomicLong resultIds = new AtomicLong(1);
    private final List<EvaluationRecords.Suite> suites = new ArrayList<>();
    private final List<EvaluationRecords.EvaluationCase> cases = new ArrayList<>();
    private final List<EvaluationRecords.Result> results = new ArrayList<>();
    private final boolean mysqlEnabled;
    private final EvaluationSuiteMapper suiteMapper;
    private final EvaluationCaseMapper caseMapper;
    private final EvaluationResultMapper resultMapper;

    public EvaluationStore(@Value("${datacenter.mysql.enabled:false}") boolean mysqlEnabled,
            ObjectProvider<EvaluationSuiteMapper> suiteMapper, ObjectProvider<EvaluationCaseMapper> caseMapper,
            ObjectProvider<EvaluationResultMapper> resultMapper) {
        this.mysqlEnabled = mysqlEnabled; this.suiteMapper = suiteMapper.getIfAvailable();
        this.caseMapper = caseMapper.getIfAvailable(); this.resultMapper = resultMapper.getIfAvailable();
    }

    public synchronized EvaluationRecords.Suite createSuite(EvaluationRecords.CreateSuiteRequest request) {
        if (mysqlEnabled) { var e = new EvaluationSuiteEntity(); e.setName(request.name()); e.setDescription(request.description()); e.setCreateTime(LocalDateTime.now()); suiteMapper.insert(e); return suite(e); }
        var suite = new EvaluationRecords.Suite(suiteIds.getAndIncrement(), request.name(), request.description(), LocalDateTime.now());
        suites.add(suite); return suite;
    }
    public synchronized List<EvaluationRecords.Suite> suites() { return mysqlEnabled ? suiteMapper.selectList(new LambdaQueryWrapper<EvaluationSuiteEntity>().orderByDesc(EvaluationSuiteEntity::getId)).stream().map(this::suite).toList() : List.copyOf(suites); }
    public synchronized EvaluationRecords.EvaluationCase createCase(EvaluationRecords.CreateCaseRequest request) {
        if (suites().stream().noneMatch(item -> item.id().equals(request.suiteId()))) throw new IllegalArgumentException("evaluation suite not found");
        if (mysqlEnabled) { var e = new EvaluationCaseEntity(); e.setSuiteId(request.suiteId()); e.setQuestion(request.question()); e.setExpectedIntent(request.expectedIntent()); e.setExpectedTool(request.expectedTool()); e.setExpectedSafetyResult(request.expectedSafetyResult()); e.setTags(request.tags()); e.setEnabled(!Boolean.FALSE.equals(request.enabled())); e.setCreateTime(LocalDateTime.now()); caseMapper.insert(e); return evaluationCase(e); }
        var item = new EvaluationRecords.EvaluationCase(caseIds.getAndIncrement(), request.suiteId(), request.question(), request.expectedIntent(), request.expectedTool(), request.expectedSafetyResult(), request.tags(), !Boolean.FALSE.equals(request.enabled()), LocalDateTime.now());
        cases.add(item); return item;
    }
    public synchronized List<EvaluationRecords.EvaluationCase> cases(Long suiteId) { return mysqlEnabled ? caseMapper.selectList(new LambdaQueryWrapper<EvaluationCaseEntity>().eq(suiteId != null, EvaluationCaseEntity::getSuiteId, suiteId)).stream().map(this::evaluationCase).toList() : cases.stream().filter(item -> suiteId == null || suiteId.equals(item.suiteId())).toList(); }
    public synchronized EvaluationRecords.Result record(EvaluationRecords.CreateResultRequest request) {
        if (cases(null).stream().noneMatch(item -> item.id().equals(request.caseId()))) throw new IllegalArgumentException("evaluation case not found");
        if (mysqlEnabled) { var e = new EvaluationResultEntity(); e.setCaseId(request.caseId()); e.setActualIntent(request.actualIntent()); e.setActualTools(request.actualTools()); e.setActualFallbackReason(request.actualFallbackReason()); e.setAnswerSnapshot(request.answerSnapshot()); e.setPassed(Boolean.TRUE.equals(request.passed())); e.setFailureReason(request.failureReason()); e.setExecutedAt(LocalDateTime.now()); resultMapper.insert(e); return result(e); }
        var result = new EvaluationRecords.Result(resultIds.getAndIncrement(), request.caseId(), request.actualIntent(), request.actualTools(), request.actualFallbackReason(), request.answerSnapshot(), Boolean.TRUE.equals(request.passed()), request.failureReason(), LocalDateTime.now());
        results.add(result); return result;
    }
    public synchronized List<EvaluationRecords.Result> results() { return mysqlEnabled ? resultMapper.selectList(new LambdaQueryWrapper<EvaluationResultEntity>().orderByDesc(EvaluationResultEntity::getExecutedAt)).stream().map(this::result).toList() : results.stream().sorted(Comparator.comparing(EvaluationRecords.Result::executedAt).reversed()).toList(); }
    public synchronized EvaluationRecords.Summary summary() {
        long total = results.size(); long passed = results.stream().filter(EvaluationRecords.Result::passed).count();
        return new EvaluationRecords.Summary(total, passed, total == 0 ? 0 : Math.round(passed * 10000.0 / total) / 100.0,
                results().stream().filter(item -> !item.passed()).limit(20).toList());
    }
    private EvaluationRecords.Suite suite(EvaluationSuiteEntity e) { return new EvaluationRecords.Suite(e.getId(), e.getName(), e.getDescription(), e.getCreateTime()); }
    private EvaluationRecords.EvaluationCase evaluationCase(EvaluationCaseEntity e) { return new EvaluationRecords.EvaluationCase(e.getId(), e.getSuiteId(), e.getQuestion(), e.getExpectedIntent(), e.getExpectedTool(), e.getExpectedSafetyResult(), e.getTags(), e.getEnabled(), e.getCreateTime()); }
    private EvaluationRecords.Result result(EvaluationResultEntity e) { return new EvaluationRecords.Result(e.getId(), e.getCaseId(), e.getActualIntent(), e.getActualTools(), e.getActualFallbackReason(), e.getAnswerSnapshot(), e.getPassed(), e.getFailureReason(), e.getExecutedAt()); }
}
