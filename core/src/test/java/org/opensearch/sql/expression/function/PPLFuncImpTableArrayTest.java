/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.sql.expression.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opensearch.sql.calcite.utils.OpenSearchTypeFactory.TYPE_FACTORY;

import org.apache.calcite.rex.RexBuilder;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.type.SqlTypeName;
import org.junit.jupiter.api.Test;

class PPLFuncImpTableArrayTest {
  private final RexBuilder rexBuilder = new RexBuilder(TYPE_FACTORY);

  @Test
  void resolvesCustomerVisibleArrayFunctions() {
    RexNode array =
        PPLFuncImpTable.INSTANCE.resolve(
            rexBuilder, "array", rexBuilder.makeLiteral("prod"), rexBuilder.makeLiteral("blue"));

    RexNode contains =
        PPLFuncImpTable.INSTANCE.resolve(
            rexBuilder, "array_contains", array, rexBuilder.makeLiteral("prod"));
    RexNode cardinality = PPLFuncImpTable.INSTANCE.resolve(rexBuilder, "cardinality", array);
    RexNode joined =
        PPLFuncImpTable.INSTANCE.resolve(
            rexBuilder, "array_join", array, rexBuilder.makeLiteral(","));

    assertEquals(SqlKind.ARRAY_CONTAINS, contains.getKind());
    assertEquals(SqlTypeName.BOOLEAN, contains.getType().getSqlTypeName());
    assertEquals(SqlKind.ARRAY_LENGTH, cardinality.getKind());
    assertEquals(SqlTypeName.INTEGER, cardinality.getType().getSqlTypeName());
    assertEquals(SqlKind.ARRAY_JOIN, joined.getKind());
    assertEquals(SqlTypeName.VARCHAR, joined.getType().getSqlTypeName());
  }

  @Test
  void resolvesElementWiseStringMapperToStringArray() {
    RexNode array =
        PPLFuncImpTable.INSTANCE.resolve(
            rexBuilder, "array", rexBuilder.makeLiteral("prod"), rexBuilder.makeLiteral("blue"));

    RexNode mapped =
        PPLFuncImpTable.INSTANCE.resolve(
            rexBuilder,
            BuiltinFunctionName.INTERNAL_ARRAY_MAP_STRING,
            array,
            rexBuilder.makeLiteral("upper"));

    assertEquals(SqlTypeName.ARRAY, mapped.getType().getSqlTypeName());
    assertEquals(SqlTypeName.VARCHAR, mapped.getType().getComponentType().getSqlTypeName());
  }

  @Test
  void resolvesElementWiseIntegerMapperToIntegerArray() {
    RexNode array =
        PPLFuncImpTable.INSTANCE.resolve(
            rexBuilder, "array", rexBuilder.makeLiteral("prod"), rexBuilder.makeLiteral("blue"));

    RexNode mapped =
        PPLFuncImpTable.INSTANCE.resolve(
            rexBuilder,
            BuiltinFunctionName.INTERNAL_ARRAY_MAP_INTEGER,
            array,
            rexBuilder.makeLiteral("length"));

    assertEquals(SqlTypeName.ARRAY, mapped.getType().getSqlTypeName());
    assertEquals(SqlTypeName.INTEGER, mapped.getType().getComponentType().getSqlTypeName());
  }

  @Test
  void resolvesArrayNullifAndCoalesceToArrayReturnType() {
    RexNode array =
        PPLFuncImpTable.INSTANCE.resolve(
            rexBuilder, "array", rexBuilder.makeLiteral("prod"), rexBuilder.makeLiteral("blue"));

    RexNode nullif =
        PPLFuncImpTable.INSTANCE.resolve(
            rexBuilder,
            BuiltinFunctionName.INTERNAL_ARRAY_NULLIF,
            array,
            rexBuilder.makeLiteral("prod"));
    RexNode coalesce =
        PPLFuncImpTable.INSTANCE.resolve(
            rexBuilder,
            BuiltinFunctionName.INTERNAL_ARRAY_COALESCE,
            array,
            rexBuilder.makeLiteral("fallback"));

    assertEquals(SqlTypeName.ARRAY, nullif.getType().getSqlTypeName());
    assertEquals(SqlTypeName.ARRAY, coalesce.getType().getSqlTypeName());
  }
}
