/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.android;

import com.yahoo.squidb.sql.Table;
import com.yahoo.squidb.test.TestDatabase;

public class AndroidTestDatabase extends TestDatabase {

    @Override
    protected Table[] getTables() {
        Table[] tables = super.getTables();
        Table[] allTables = new Table[tables.length + 1];
        System.arraycopy(tables, 0, allTables, 0, tables.length);
        allTables[tables.length] = AndroidTestModel.TABLE;
        return allTables;
    }
}
