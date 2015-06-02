/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sample;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Loader;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.yahoo.squidb.data.DatabaseDao;
import com.yahoo.squidb.data.SquidCursor;
import com.yahoo.squidb.sample.adapter.TaskListAdapter;
import com.yahoo.squidb.sample.models.Task;
import com.yahoo.squidb.sample.utils.TaskUtils;
import com.yahoo.squidb.utility.SquidCursorLoader;

public class TaskListActivity extends Activity implements LoaderManager.LoaderCallbacks<SquidCursor<Task>> {

    private static final int LOADER_ID_TASKS = 1;

    private DatabaseDao mDatabaseDao;

    private ListView mTaskListView;
    private TaskListAdapter mTaskListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_list);
        mTaskListView = (ListView) findViewById(R.id.task_list);

        mTaskListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                // TODO: Handle long click
                return false;
            }
        });

        mTaskListAdapter = new TaskListAdapter(this, new Task());
        mTaskListView.setAdapter(mTaskListAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getLoaderManager().restartLoader(LOADER_ID_TASKS, null, this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_task_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<SquidCursor<Task>> onCreateLoader(int id, Bundle args) {
        return new SquidCursorLoader<Task>(this, mDatabaseDao, Task.class, TaskUtils.TASKS_WITH_TAGS);
    }

    @Override
    public void onLoaderReset(Loader<SquidCursor<Task>> loader) {
        mTaskListAdapter.swapCursor(null);
    }

    @Override
    public void onLoadFinished(Loader<SquidCursor<Task>> loader, SquidCursor<Task> data) {
        mTaskListAdapter.swapCursor(data);
    }
}
