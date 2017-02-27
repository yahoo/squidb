package com.yahoo.squidb.test;

import com.yahoo.squidb.annotations.ColumnName;
import com.yahoo.squidb.annotations.ConstraintSql;
import com.yahoo.squidb.annotations.PrimaryKey;
import com.yahoo.squidb.annotations.TableModelSpec;

@TableModelSpec(className = "TestNonIntegerPrimaryKey", tableName = "testNonIntegerPrimaryKey")
public class TestNonIntegerPrimaryKeySpec {

    @PrimaryKey
    @ColumnName("keyCol")
    @ConstraintSql("NOT NULL")
    String key;

    String value;

}
