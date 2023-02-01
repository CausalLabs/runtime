package io.causallabs.mustache;

import java.util.Collection;

/**
 * Represents a type of metric in Causal. Can be session, impression, or one of the @per metrics.
 */
public interface MetricTable {

    /**
     * The name of the associated metric table in the warehouse.
     * (https://tech.causallabs.io/docs/data-warehouse/metrics) Basically session_metrics,
     * impression_metrics, or @per_metrics.
     */
    public String getMetricTableName();

    /**
     * All the fields that are in the table. These do NOT include the partition columns, which are
     * always ds, hh, and metric_id
     */
    public Collection<WarehouseColumn> getWarehouseColumns();

}
