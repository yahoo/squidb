package com.yahoo.squidb.data;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;

/*package*/ class SqlValidatorFactory {

    interface SqlValidator {

        void compileStatement(SQLiteDatabase db, String sql);
    }

    private static final SqlValidator INSTANCE;

    static {
        int version = VERSION.SDK_INT;
        if (version >= VERSION_CODES.JELLY_BEAN) {
            INSTANCE = new DefaultSqlValidator();
        } else if (version >= VERSION_CODES.ICE_CREAM_SANDWICH) {
            INSTANCE = new IcsSqlValidator();
        } else {
            // included for forks that reduce minSdk below 14
            INSTANCE = new DefaultSqlValidator();
        }
    }

    private SqlValidatorFactory() {
        //no instance
    }

    static SqlValidator getValidator() {
        return INSTANCE;
    }

    private static class DefaultSqlValidator implements SqlValidator {

        @Override
        public void compileStatement(SQLiteDatabase db, String sql) {
            db.compileStatement(sql);
        }
    }

    private static class IcsSqlValidator implements SqlValidator {

        @Override
        public void compileStatement(SQLiteDatabase db, String sql) {
            Cursor c = db.rawQuery(sql, null);
            if (c != null) {
                c.close();
            }
        }
    }
}

