package com.yahoo.squidb.utility;

import android.view.View;
import android.view.ViewGroup;

import com.yahoo.squidb.data.SquidCursor;
import com.yahoo.squidb.sql.Function;
import com.yahoo.squidb.sql.Property;
import com.yahoo.squidb.sql.Property.LongProperty;
import com.yahoo.squidb.sql.Query;
import com.yahoo.squidb.test.DatabaseTestCase;
import com.yahoo.squidb.test.TestModel;

public class SquidCursorAdapterTest extends DatabaseTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        insertBasicTestModel("Sam", "Bosley", 1);
        insertBasicTestModel("Jonathan", "Koren", 2);
    }

    private static interface CursorAdapterTest {

        public void testCursorAdapter(SquidCursorAdapter<TestModel> adapter);
    }

    public void testReusableModel() {
        testCursorAdapterInternal(null, new CursorAdapterTest() {
            @Override
            public void testCursorAdapter(SquidCursorAdapter<TestModel> adapter) {
                TestModel first = adapter.getItem(0);
                TestModel second = adapter.getItem(1);
                assertEquals(first, second);
            }
        });
    }

    public void testIdColumn() {
        testCursorAdapterInternal(null, new CursorAdapterTest() {
            @Override
            public void testCursorAdapter(SquidCursorAdapter<TestModel> adapter) {
                assertEquals(1, adapter.getItemId(0));
                assertEquals(2, adapter.getItemId(1));
            }
        });
    }

    public void testCustomIdColumn() {
        Function<Long> idSquared = Function.rawFunction("_id * _id");
        LongProperty idSquaredProperty = LongProperty.fromFunction(idSquared, "idSquared");
        testCursorAdapterInternal(idSquaredProperty, new CursorAdapterTest() {
            @Override
            public void testCursorAdapter(SquidCursorAdapter<TestModel> adapter) {
                assertEquals(1, adapter.getItemId(0));
                assertEquals(4, adapter.getItemId(1));
            }
        });
    }

    private void testCursorAdapterInternal(Property<Long> idColumn, CursorAdapterTest test) {
        Query query = Query.select(TestModel.PROPERTIES).orderBy(TestModel.ID.asc());
        if (idColumn != null) {
            query.selectMore(idColumn);
        }
        SquidCursor<TestModel> cursor = dao.query(TestModel.class, query);
        try {
            SquidCursorAdapter<TestModel> adapter = new SquidCursorAdapter<TestModel>(getContext(), new TestModel(),
                    idColumn) {
                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    return null;
                }
            };
            adapter.swapCursor(cursor);
            test.testCursorAdapter(adapter);
        } finally {
            cursor.close();
        }
    }

}
