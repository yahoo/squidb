package com.yahoo.squidb.test;

import com.yahoo.squidb.annotations.TableModelSpec;
import com.yahoo.squidb.annotations.tables.constraints.NotNull;
import com.yahoo.squidb.annotations.tables.constraints.PrimaryKeyColumns;

@TableModelSpec(className = "TestMultiColumnKey", tableName = "multiColumnKey")
@PrimaryKeyColumns(columns = {"keyCol1", "keyCol2", "keyCol3"})
public class TestMultiColumnKeySpec {

    @NotNull
    String keyCol1;

    @NotNull
    String keyCol2;

    @NotNull
    String keyCol3;

    String otherData;
}
