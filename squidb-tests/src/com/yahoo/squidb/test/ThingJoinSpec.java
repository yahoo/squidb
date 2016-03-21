package com.yahoo.squidb.test;

import com.yahoo.squidb.annotations.Constants;
import com.yahoo.squidb.annotations.ViewModelSpec;
import com.yahoo.squidb.annotations.ViewQuery;
import com.yahoo.squidb.sql.Function;
import com.yahoo.squidb.sql.Property;
import com.yahoo.squidb.sql.Query;
import com.yahoo.squidb.sql.Table;

@ViewModelSpec(className = "ThingJoin", viewName = "thingJoin", isSubquery = true)
public class ThingJoinSpec {

    private static final Table THING_2 = Thing.TABLE.as("thing2");
    private static final Table THING_3 = Thing.TABLE.as("thing3");

    @ViewQuery
    public static final Query QUERY = Query.select()
            .from(Thing.TABLE)
            .innerJoin(THING_2, Thing.ID.eq(Function.subtract(THING_2.qualifyField(Thing.ID), 1)))
            .innerJoin(THING_3, Thing.ID.eq(Function.subtract(THING_3.qualifyField(Thing.ID), 2)));

    public static final Property.LongProperty THING_1_ID = Thing.ID;
    public static final Property.LongProperty THING_2_ID = THING_2.qualifyField(Thing.ID);
    public static final Property.LongProperty THING_3_ID = THING_3.qualifyField(Thing.ID);

    public static final Property.StringProperty THING_1_FOO = Thing.FOO;
    public static final Property.StringProperty THING_2_FOO = THING_2.qualifyField(Thing.FOO);
    public static final Property.StringProperty THING_3_FOO = THING_3.qualifyField(Thing.FOO);

    public static final Property.IntegerProperty THING_1_BAR = Thing.BAR;
    public static final Property.IntegerProperty THING_2_BAR = THING_2.qualifyField(Thing.BAR);
    public static final Property.IntegerProperty THING_3_BAR = THING_3.qualifyField(Thing.BAR);

    @Constants
    public static class Const {
        public static final Table THING_2 = ThingJoinSpec.THING_2;
        public static final Table THING_3 = ThingJoinSpec.THING_3;
    }

}
