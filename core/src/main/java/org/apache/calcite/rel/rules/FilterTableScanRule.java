/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.calcite.rel.rules;

import org.apache.calcite.interpreter.Bindables;
import org.apache.calcite.plan.RelOptRule;
import org.apache.calcite.plan.RelOptRuleCall;
import org.apache.calcite.plan.RelOptRuleOperand;
import org.apache.calcite.plan.RelOptTable;
import org.apache.calcite.rel.core.Filter;
import org.apache.calcite.rel.core.RelFactories;
import org.apache.calcite.rel.core.TableScan;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.rex.RexUtil;
import org.apache.calcite.schema.FilterableTable;
import org.apache.calcite.schema.ProjectableFilterableTable;
import org.apache.calcite.tools.RelBuilderFactory;
import org.apache.calcite.util.ImmutableIntList;
import org.apache.calcite.util.mapping.Mapping;
import org.apache.calcite.util.mapping.Mappings;

import com.google.common.collect.ImmutableList;

/**
 * Planner rule that converts
 * a {@link org.apache.calcite.rel.core.Filter}
 * on a {@link org.apache.calcite.rel.core.TableScan}
 * of a {@link org.apache.calcite.schema.FilterableTable}
 * or a {@link org.apache.calcite.schema.ProjectableFilterableTable}
 * to a {@link org.apache.calcite.interpreter.Bindables.BindableTableScan}.
 *
 * <p>The {@link #INTERPRETER} variant allows an intervening
 * {@link org.apache.calcite.adapter.enumerable.EnumerableInterpreter}.
 *
 * @see org.apache.calcite.rel.rules.ProjectTableScanRule
 */
public abstract class FilterTableScanRule extends RelOptRule {
  @SuppressWarnings("Guava")
  @Deprecated // to be removed before 2.0
  public static final com.google.common.base.Predicate<TableScan> PREDICATE =
      FilterTableScanRule::test;

  /** @deprecated Use {@link CoreRules#FILTER_SCAN}. */
  @Deprecated // to be removed before 1.25
  public static final FilterTableScanRule INSTANCE =
      CoreRules.FILTER_SCAN;

  /** @deprecated Use {@link CoreRules#FILTER_INTERPRETER_SCAN}. */
  @Deprecated // to be removed before 1.25
  public static final FilterTableScanRule INTERPRETER =
      CoreRules.FILTER_INTERPRETER_SCAN;

  //~ Constructors -----------------------------------------------------------

  @Deprecated // to be removed before 2.0
  protected FilterTableScanRule(RelOptRuleOperand operand, String description) {
    this(operand, RelFactories.LOGICAL_BUILDER, description);
  }

  /** Creates a FilterTableScanRule. */
  protected FilterTableScanRule(RelOptRuleOperand operand,
      RelBuilderFactory relBuilderFactory, String description) {
    super(operand, relBuilderFactory, description);
  }

  //~ Methods ----------------------------------------------------------------

  public static boolean test(TableScan scan) {
    // We can only push filters into a FilterableTable or
    // ProjectableFilterableTable.
    final RelOptTable table = scan.getTable();
    return table.unwrap(FilterableTable.class) != null
        || table.unwrap(ProjectableFilterableTable.class) != null;
  }

  protected void apply(RelOptRuleCall call, Filter filter, TableScan scan) {
    final ImmutableIntList projects;
    final ImmutableList.Builder<RexNode> filters = ImmutableList.builder();
    if (scan instanceof Bindables.BindableTableScan) {
      final Bindables.BindableTableScan bindableScan =
          (Bindables.BindableTableScan) scan;
      filters.addAll(bindableScan.filters);
      projects = bindableScan.projects;
    } else {
      projects = scan.identity();
    }

    final Mapping mapping = Mappings.target(projects,
        scan.getTable().getRowType().getFieldCount());
    filters.add(
        RexUtil.apply(mapping.inverse(), filter.getCondition()));

    call.transformTo(
        Bindables.BindableTableScan.create(scan.getCluster(), scan.getTable(),
            filters.build(), projects));
  }
}
