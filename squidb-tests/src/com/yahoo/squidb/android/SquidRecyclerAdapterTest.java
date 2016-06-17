/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.android;

import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.yahoo.squidb.data.SquidCursor;
import com.yahoo.squidb.recyclerview.SquidRecyclerAdapter;
import com.yahoo.squidb.recyclerview.SquidViewHolder;
import com.yahoo.squidb.sql.Function;
import com.yahoo.squidb.sql.Property.LongProperty;
import com.yahoo.squidb.sql.Query;
import com.yahoo.squidb.test.DatabaseTestCase;
import com.yahoo.squidb.test.TestModel;

public class SquidRecyclerAdapterTest extends DatabaseTestCase {

    @Override
    protected void setupDatabase() {
        super.setupDatabase();
        insertBasicTestModel("Alan", "Turing", DateUtils.DAY_IN_MILLIS);
        insertBasicTestModel("Linus", "Torvalds", DateUtils.YEAR_IN_MILLIS);
    }

    private interface RecyclerAdapterTest {

        void testRecyclerAdapter(TestRecyclerAdapter adapter);
    }

    public void testNoIdProperty() {
        testRecyclerAdapterInternal(null, new RecyclerAdapterTest() {

            @Override
            public void testRecyclerAdapter(TestRecyclerAdapter adapter) {
                assertFalse(adapter.hasStableIds());
                assertEquals(RecyclerView.NO_ID, adapter.getItemId(0));
                assertEquals(RecyclerView.NO_ID, adapter.getItemId(1));
            }
        });
    }

    public void testIdProperty() {
        testRecyclerAdapterInternal(TestModel.ID, new RecyclerAdapterTest() {

            @Override
            public void testRecyclerAdapter(TestRecyclerAdapter adapter) {
                assertTrue(adapter.hasStableIds());
                assertEquals(1, adapter.getItemId(0));
                assertEquals(2, adapter.getItemId(1));
            }
        });
    }

    public void testCustomIdProperty() {
        Function<Long> idSquared = Function.rawFunction("_id * _id");
        LongProperty idSquaredProperty = LongProperty.fromFunction(idSquared, "idSquared");
        testRecyclerAdapterInternal(idSquaredProperty, new RecyclerAdapterTest() {

            @Override
            public void testRecyclerAdapter(TestRecyclerAdapter adapter) {
                assertTrue(adapter.hasStableIds());
                assertEquals(1, adapter.getItemId(0));
                assertEquals(4, adapter.getItemId(1));
            }
        });
    }

    public void testViewHolderItemBinding() {
        final TestModel model1 = database.fetch(TestModel.class, 1, TestModel.PROPERTIES);
        final TestModel model2 = database.fetch(TestModel.class, 2, TestModel.PROPERTIES);

        testRecyclerAdapterInternal(TestModel.ID, new RecyclerAdapterTest() {

            @Override
            public void testRecyclerAdapter(TestRecyclerAdapter adapter) {
                FrameLayout parent = new FrameLayout(ContextProvider.getContext());
                TestViewHolder holder = adapter.onCreateViewHolder(parent, adapter.getItemViewType(0));

                adapter.onBindViewHolder(holder, 0);
                assertEquals(model1, holder.item);
                assertEquals(model1.getDisplayName(), holder.textView.getText().toString());
                adapter.onBindViewHolder(holder, 1);
                assertEquals(model2, holder.item);
                assertEquals(model2.getDisplayName(), holder.textView.getText().toString());
            }
        });
    }

    private void testRecyclerAdapterInternal(LongProperty idProperty, RecyclerAdapterTest test) {
        Query query = Query.select(TestModel.PROPERTIES)
                .orderBy(TestModel.BIRTHDAY.asc())
                .limit(2);
        if (idProperty != null) {
            query.selectMore(idProperty);
        }
        SquidCursor<TestModel> cursor = database.query(TestModel.class, query);

        TestRecyclerAdapter adapter = new TestRecyclerAdapter(idProperty);
        adapter.changeCursor(cursor);
        try {
            test.testRecyclerAdapter(adapter);
        } finally {
            cursor.close();
        }
    }

    // -- test adapter implementation

    static class TestRecyclerAdapter extends SquidRecyclerAdapter<TestModel, TestViewHolder> {

        public TestRecyclerAdapter(LongProperty idProperty) {
            super(idProperty);
        }

        @Override
        public TestViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new TestViewHolder(new TextView(parent.getContext()));
        }

        @Override
        public void onBindSquidViewHolder(TestViewHolder holder, int position) {
            holder.textView.setText(holder.item.getDisplayName());
        }

        @Override
        public SquidCursor<TestModel> getCursor() {
            return (SquidCursor<TestModel>) super.getCursor();
        }
    }

    static class TestViewHolder extends SquidViewHolder<TestModel> {

        TextView textView;

        public TestViewHolder(TextView itemView) {
            super(itemView, new TestModel());
            textView = itemView;
        }
    }
}
