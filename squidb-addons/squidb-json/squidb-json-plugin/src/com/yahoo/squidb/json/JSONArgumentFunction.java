package com.yahoo.squidb.json;

import com.yahoo.squidb.sql.Function;
import com.yahoo.squidb.sql.SqlBuilder;
import com.yahoo.squidb.utility.VersionCode;

class JSONArgumentFunction<TYPE> extends Function<TYPE> {

    private static final VersionCode JSON1_MIN_VERSION = new VersionCode(3, 9, 0, 0);

    private final String functionName;
    private final Object jsonArg;
    private final Object[] additionalArgs;

    JSONArgumentFunction(String functionName, Object jsonArg, Object... additionalArgs) {
        super();
        this.functionName = functionName;
        this.jsonArg = jsonArg;
        this.additionalArgs = additionalArgs;
    }

    @Override
    protected void appendFunctionExpression(SqlBuilder builder, boolean forSqlValidation) {
        if (!builder.sqliteVersion.isAtLeast(JSON1_MIN_VERSION)) {
            throw new UnsupportedOperationException("The function " + functionName + " is not supported on SQLite "
                    + "version " + builder.sqliteVersion + " - requires version " + JSON1_MIN_VERSION + " or higher");
        }
        builder.sql.append(functionName).append("(");
        boolean needsSeparator = false;
        if (jsonArg != null) {
            builder.addValueToSql(jsonArg, forSqlValidation);
            needsSeparator = true;
        }
        if (additionalArgs != null && additionalArgs.length > 0) {
            for (Object additionalArg : additionalArgs) {
                if (needsSeparator) {
                    builder.sql.append(", ");
                }
                builder.addValueToSql(additionalArg, forSqlValidation);
                needsSeparator = true;
            }
        }
        builder.sql.append(")");
    }
}
