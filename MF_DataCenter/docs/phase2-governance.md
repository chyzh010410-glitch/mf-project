# Phase 2 Governance

MF_DataCenter Phase 2 turns the dashboard from a read-only aggregation surface into a lightweight data governance console.

## Implemented Capabilities

- Scheduled snapshots
  - Spring Scheduler is enabled by `datacenter.scheduler.enabled`.
  - Local profile enables the scheduler.
  - Hourly snapshots run at minute 5 of every hour.
  - Daily snapshots run at 00:10.

- Metric dictionary
  - Table: `dc_metric_definition`.
  - APIs:
    - `GET /api/metrics/dictionary`
    - `POST /api/metrics/dictionary`
    - `PUT /api/metrics/dictionary/{id}`
    - `PATCH /api/metrics/dictionary/{id}/enabled`
  - Tracks metric code, name, source table, formula, period, owner, description, and enabled state.

- Data quality checks
  - Table: `dc_data_quality_check`.
  - Rule table: `dc_quality_rule`.
  - Issue table: `dc_quality_issue`.
  - API:
    - `GET /api/data-quality/summary`
    - `GET /api/data-quality/checks`
    - `POST /api/data-quality/run`
    - `GET /api/data-quality/rules`
    - `POST /api/data-quality/rules`
    - `PUT /api/data-quality/rules/{id}`
    - `PATCH /api/data-quality/rules/{id}/enabled`
    - `GET /api/data-quality/issues`
    - `PATCH /api/data-quality/issues/{id}/status`
  - Current rules cover missing snapshots, hourly freshness, negative values, and today's daily snapshot presence.
  - Failed checks open quality issues. Passing checks auto-resolve open issues for the same check and metric.

- Standard metric API
  - `GET /api/metrics/latest`
  - `GET /api/metrics/query?code=gmv_total&period=daily`
  - `GET /api/metrics/query?code=gmv_total,order_total&period=daily&dimensionKey=global&dimensionValue=all&limit=100`
  - Existing snapshot refresh APIs remain available:
    - `POST /api/metrics/snapshots/hourly/refresh`
    - `POST /api/metrics/snapshots/daily/refresh`

## Frontend Surfaces

- `/metric-governance`
  - Metric dictionary table.
  - Metric create, edit, enable, and disable actions.
  - Manual hourly/daily snapshot refresh.
  - Standard metric query.
  - Latest snapshot list.

- `/data-quality`
  - Quality summary.
  - Latest quality check table.
  - Quality rule create, edit, enable, and disable actions.
  - Quality issue list with processing, resolved, and ignored transitions.
  - Manual quality check trigger.

## Database Migration

Phase 2 adds:

- `V4__metric_governance.sql`
  - `dc_metric_definition`
  - `dc_data_quality_check`
  - seed rows for the current platform, commerce, merchant, category, and AI metrics.
- `V5__quality_rules_and_issues.sql`
  - `dc_quality_rule`
  - `dc_quality_issue`
  - seed rows for the current lightweight quality rules.
  - corrected Chinese labels for seeded metric definitions.

## Guardrails

- MF_EP remains read-only.
- DataCenter writes only to `mf_datacenter`.
- Metric definitions are governance metadata; they do not modify MF_EP business logic.
- Current scheduler is intentionally lightweight. Quartz or external orchestration can be added later if cluster coordination is required.
