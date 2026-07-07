package com.mf.agentservice.tools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ToolExecutionContext {
    private final List<ToolExecutionRecord> records = new ArrayList<>();

    public void add(ToolExecutionRecord record) {
        records.add(record);
    }

    public List<ToolExecutionRecord> records() {
        return Collections.unmodifiableList(records);
    }

    public List<ToolCallSummary> summaries() {
        return records.stream().map(ToolExecutionRecord::summary).toList();
    }
}
