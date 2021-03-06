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
package org.apache.calcite.adapter.spark;

import org.apache.calcite.adapter.enumerable.EnumerableConvention;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.convert.ConverterRule;
import org.apache.calcite.rel.core.RelFactories;
import org.apache.calcite.tools.RelBuilderFactory;

import java.util.function.Predicate;

/**
 * Rule to convert a relational expression from
 * {@link org.apache.calcite.adapter.jdbc.JdbcConvention} to
 * {@link SparkRel#CONVENTION Spark convention}.
 *
 * @deprecated Use {@link SparkRules#ENUMERABLE_TO_SPARK}.
 */
@Deprecated // to be removed before 1.25
public class EnumerableToSparkConverterRule extends ConverterRule {
  public static final EnumerableToSparkConverterRule INSTANCE =
      new EnumerableToSparkConverterRule(RelFactories.LOGICAL_BUILDER);

  /**
   * Creates an EnumerableToSparkConverterRule.
   *
   * @param relBuilderFactory Builder for relational expressions
   */
  public EnumerableToSparkConverterRule(RelBuilderFactory relBuilderFactory) {
    super(RelNode.class, (Predicate<RelNode>) r -> true,
        EnumerableConvention.INSTANCE, SparkRel.CONVENTION, relBuilderFactory,
        "EnumerableToSparkConverterRule");
  }

  public RelNode convert(RelNode rel) {
    RelTraitSet newTraitSet = rel.getTraitSet().replace(getOutTrait());
    return new EnumerableToSparkConverter(
        rel.getCluster(), newTraitSet, rel);
  }
}
