package com.yahoo.squidb.test;

import com.yahoo.squidb.annotations.TableModelSpec;
import com.yahoo.squidb.annotations.tables.ColumnName;
import com.yahoo.squidb.annotations.tables.constraints.NotNull;
import com.yahoo.squidb.annotations.tables.constraints.PrimaryKey;

@TableModelSpec(className = "TestNonIntegerPrimaryKey", tableName = "testNonIntegerPrimaryKey")
public class TestNonIntegerPrimaryKeySpec {

    @PrimaryKey
    @ColumnName("keyCol")
    @NotNull
    String key;

    String value;

}
