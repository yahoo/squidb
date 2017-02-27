package com.yahoo.squidb.test;

import com.yahoo.squidb.annotations.ConstraintSql;
import com.yahoo.squidb.annotations.TableModelSpec;

@TableModelSpec(className = "TestMultiColumnKey", tableName = "multiColumnKey",
        tableConstraint = "PRIMARY KEY(keyCol1, keyCol2, keyCol3)")
public class TestMultiColumnKeySpec {

    @ConstraintSql("NOT NULL")
    String keyCol1;

    @ConstraintSql("NOT NULL")
    String keyCol2;

    @ConstraintSql("NOT NULL")
    String keyCol3;

    String otherData;
}
