/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.sql.opensearch.data.type;

import lombok.Getter;
import org.opensearch.sql.data.type.ExprCoreType;
import org.opensearch.sql.data.type.ExprType;

/**
 * The SQL-plugin type for a field mapped {@code multi_value: true}. Such a field is stored by the
 * analytics engine as a Parquet LIST column, so it must surface as a Calcite {@code ARRAY} for the
 * multi-value operators ({@code array_length}, {@code mvjoin}, {@code mvindex}, {@code mvexpand},
 * ...) to type-check against it.
 *
 * <p>Unlike a bare {@link ExprCoreType#ARRAY}, this type retains the {@link #elementType} so the
 * array can be converted to a typed Calcite {@code ARRAY<T>} (e.g. {@code ARRAY<BIGINT>} for a
 * {@code long} field) rather than {@code ARRAY<ANY>}. Preserving the element type lets element-
 * sensitive paths (numeric aggregation after {@code mvexpand}, typed {@code mvindex}, comparisons)
 * bind and evaluate correctly for non-keyword scalar types.
 */
public class OpenSearchArrayType extends OpenSearchDataType {

  /** The type of the array's elements (the field's underlying scalar mapping type). */
  @Getter private final OpenSearchDataType elementType;

  public OpenSearchArrayType(OpenSearchDataType elementType) {
    super(ExprCoreType.ARRAY);
    this.elementType = elementType;
  }

  @Override
  public ExprType getExprType() {
    return this;
  }

  /** Exposes the element type so the Calcite conversion can build a typed {@code ARRAY<T>}. */
  @Override
  public java.util.Optional<ExprType> getArrayElementType() {
    return java.util.Optional.of(elementType.getExprType());
  }

  @Override
  public OpenSearchDataType cloneEmpty() {
    return new OpenSearchArrayType(elementType.cloneEmpty());
  }

  @Override
  public String typeName() {
    return "array";
  }

  @Override
  public String legacyTypeName() {
    return "array";
  }
}
