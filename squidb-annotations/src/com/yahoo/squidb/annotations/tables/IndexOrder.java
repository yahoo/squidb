/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.annotations.tables;

/**
 * Enum for explicitly specifying ASC or DESC in {@link com.yahoo.squidb.annotations.tables.constraints.PrimaryKey}
 * or {@link com.yahoo.squidb.annotations.tables.constraints.Unique} annotations. Most applications will probably not
 * need to use this enum.
 */
public enum IndexOrder {
    UNSPECIFIED,
    ASC,
    DESC
}
