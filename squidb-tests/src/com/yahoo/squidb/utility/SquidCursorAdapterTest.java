package com.yahoo.squidb.utility;

import android.view.View;
import android.view.ViewGroup;

import com.yahoo.squidb.android.SquidCursorAdapter;
import com.yahoo.squidb.data.AbstractModel;
import com.yahoo.squidb.data.SquidCursor;
import com.yahoo.squidb.sql.Function;
import com.yahoo.squidb.sql.Property;
import com.yahoo.squidb.sql.Property.LongProperty;
import com.yahoo.squidb.sql.Query;
import com.yahoo.squidb.test.DatabaseTestCase;
import com.yahoo.squidb.test.Employee;
import com.yahoo.squidb.test.TestModel;
import com.yahoo.squidb.test.TestViewModel;

public class SquidCursorAdapterTest extends DatabaseTestCase {

    @Override
    protected void setupDatabase() {
        super.setupDatabase();
        insertBasicTestModel("Sam", "Bosley", 1);
        insertBasicTestModel("Jonathan", "Koren", 2);

        database.persist(new Employee().setName("Big Bird").setIsHappy(true));
        database.persist(new Employee().setName("Oscar").setIsHappy(false));
    }

    private interface CursorAdapterTest {

        void testCursorAdapter(SquidCursorAdapter<AbstractModel> adapter);
    }

    public void testReusableModel() {
        Query query = Query.select(TestModel.PROPERTIES).orderBy(TestModel.ID.asc());
        testCursorAdapterInternal(new TestModel(), null, query, new CursorAdapterTest() {
            @Override
            public void testCursorAdapter(SquidCursorAdapter<AbstractModel> adapter) {
                AbstractModel first = adapter.getItem(0);
                AbstractModel second = adapter.getItem(1);
                assertEquals(first, second);
            }
        });
    }

    public void testIdColumnForTableModels() {
        Query query = Query.select(TestModel.PROPERTIES).orderBy(TestModel.ID.asc());
        testCursorAdapterInternal(new TestModel(), null, query, new CursorAdapterTest() {
            @Override
            public void testCursorAdapter(SquidCursorAdapter<AbstractModel> adapter) {
                assertTrue(adapter.hasStableIds());
                assertEquals(1, adapter.getItemId(0));
                assertEquals(2, adapter.getItemId(1));
            }
        });
    }

    public void testNoIdColumnForNonTableModels() {
        Query query = Query.select(TestViewModel.PROPERTIES);
        testCursorAdapterInternal(new TestViewModel(), null, query, new CursorAdapterTest() {
            @Override
            public void testCursorAdapter(SquidCursorAdapter<AbstractModel> adapter) {
                assertFalse(adapter.hasStableIds());
                assertEquals(0, adapter.getItemId(0));
                assertEquals(0, adapter.getItemId(1));
            }
        });
    }

    public void testCustomIdColumn() {
        Function<Long> idSquared = Function.rawFunction("_id * _id");
        LongProperty idSquaredProperty = LongProperty.fromFunction(idSquared, "idSquared");
        Query query = Query.select(TestModel.PROPERTIES).selectMore(idSquaredProperty).orderBy(TestModel.ID.asc());
        testCursorAdapterInternal(new TestModel(), idSquaredProperty, query, new CursorAdapterTest() {
            @Override
            public void testCursorAdapter(SquidCursorAdapter<AbstractModel> adapter) {
                assertTrue(adapter.hasStableIds());
                assertEquals(1, adapter.getItemId(0));
                assertEquals(4, adapter.getItemId(1));
            }
        });
    }

    private void testCursorAdapterInternal(AbstractModel model, LongProperty idColumn, Query query,
            CursorAdapterTest test) {
        TestAdapter adapter;
        if (idColumn == null) {
            adapter = new TestAdapter(model);
        } else {
            adapter = new TestAdapter(model, idColumn);
        }

        SquidCursor<? extends AbstractModel> cursor = database.query(model.getClass(), query);
        try {
            adapter.swapCursor(cursor);
            test.testCursorAdapter(adapter);
        } finally {
            cursor.close();
        }
    }

    public void testSwapCursorDoesNotCloseOldCursor() {
        TestAdapter adapter = new TestAdapter(new TestModel());

        SquidCursor<TestModel> cursor1 = database.query(TestModel.class, Query.select());
        try {
            adapter.swapCursor(cursor1);
            SquidCursor<TestModel> cursor2 = database.query(TestModel.class, Query.select().where(TestModel.ID.eq(1)));
            try {
                SquidCursor<?> swappedCursor = adapter.swapCursor(cursor2);
                assertFalse(swappedCursor.isClosed());
            } finally {
                adapter.swapCursor(null);
                cursor2.close();
            }
        } finally {
            cursor1.close();
        }
    }

    public void testChangeCursorClosesOldCursor() {
        TestAdapter adapter = new TestAdapter(new TestModel());

        SquidCursor<TestModel> cursor1 = database.query(TestModel.class, Query.select());
        adapter.swapCursor(cursor1);
        SquidCursor<TestModel> cursor2 = database.query(TestModel.class, Query.select().where(TestModel.ID.eq(1)));

        adapter.changeCursor(cursor2);
        assertTrue(cursor1.isClosed());
        adapter.changeCursor(null);
        cursor2.close();
    }

    static class TestAdapter extends SquidCursorAdapter<AbstractModel> {

        public TestAdapter(AbstractModel model) {
            super(model);
        }

        public TestAdapter(AbstractModel model, Property<Long> columnForId) {
            super(model, columnForId);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return null;
        }
    }
}
