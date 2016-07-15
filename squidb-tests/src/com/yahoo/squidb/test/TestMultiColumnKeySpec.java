package com.yahoo.squidb.test;

import com.yahoo.squidb.annotations.ColumnSpec;
import com.yahoo.squidb.annotations.TableModelSpec;

@TableModelSpec(className = "TestMultiColumnKey", tableName = "multiColumnKey",
        tableConstraint = "PRIMARY KEY(keyCol1, keyCol2, keyCol3)", noRowIdAlias = true)
public class TestMultiColumnKeySpec {

    @ColumnSpec(constraints = "NOT NULL")
    String keyCol1;

    @ColumnSpec(constraints = "NOT NULL")
    String keyCol2;

    @ColumnSpec(constraints = "NOT NULL")
    String keyCol3;

    String otherData;
}
