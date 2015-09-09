/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.utility;

import android.text.format.DateUtils;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.yahoo.squidb.data.SquidCursor;
import com.yahoo.squidb.recyclerview.SquidRecyclerAdapter;
import com.yahoo.squidb.recyclerview.SquidViewHolder;
import com.yahoo.squidb.sql.Query;
import com.yahoo.squidb.test.DatabaseTestCase;
import com.yahoo.squidb.test.TestModel;

public class SquidRecyclerAdapterTest extends DatabaseTestCase {

    private TestRecyclerAdapter adapter;

    @Override
    protected void setupDatabase() {
        super.setupDatabase();
        insertBasicTestModel("Alan", "Turing", DateUtils.DAY_IN_MILLIS);
        insertBasicTestModel("Linus", "Torvalds", DateUtils.YEAR_IN_MILLIS);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        adapter = new TestRecyclerAdapter();
    }

    public void testViewHolderItemBinding() {
        SquidCursor<TestModel> cursor = database.query(TestModel.class,
                Query.select().orderBy(TestModel.BIRTHDAY.asc()));

        cursor.moveToFirst();
        TestModel model1 = new TestModel(cursor);
        cursor.moveToNext();
        TestModel model2 = new TestModel(cursor);

        adapter.changeCursor(cursor);
        FrameLayout parent = new FrameLayout(getContext());
        TestViewHolder holder = adapter.onCreateViewHolder(parent, adapter.getItemViewType(0));
        adapter.onBindViewHolder(holder, 0);
        assertEquals(model1, holder.item);
        assertEquals(model1.getDisplayName(), holder.textView.getText().toString());
        adapter.onBindViewHolder(holder, 1);
        assertEquals(model2, holder.item);
        assertEquals(model2.getDisplayName(), holder.textView.getText().toString());
    }

    // -- test adapter implementation

    static class TestRecyclerAdapter extends SquidRecyclerAdapter<TestModel, TestViewHolder> {

        @Override
        public TestViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new TestViewHolder(new TextView(parent.getContext()));
        }

        @Override
        public void onBindSquidViewHolder(TestViewHolder holder, int position) {
            holder.textView.setText(holder.item.getDisplayName());
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
