package com.yahoo.squidb.sql;

import com.yahoo.squidb.data.DatabaseDao;
import com.yahoo.squidb.data.SquidCursor;
import com.yahoo.squidb.test.DatabaseTestCase;
import com.yahoo.squidb.test.TestDatabase;
import com.yahoo.squidb.test.TestModel;

import java.util.concurrent.atomic.AtomicReference;

public class AttachDetachTest extends DatabaseTestCase {

    private TestModel model1;
    private TestModel model2;
    private TestModel model3;

    private TestDatabase database2;
    private DatabaseDao dao2;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        database2 = new TestDatabase(getContext()) {
            @Override
            public String getName() {
                return "db2.db";
            }
        };

        model1 = insertBasicTestModel("Guy 1", "Lname1", System.currentTimeMillis() - 5);
        model2 = insertBasicTestModel("Guy 2", "Lname2", System.currentTimeMillis() - 4);
        model3 = insertBasicTestModel("Guy 3", "Lname3", System.currentTimeMillis() - 3);

        dao2 = new DatabaseDao(database2);
        database2.clear();
        assertEquals(0, dao2.count(TestModel.class, Criterion.all));
    }

    public void testAttachDetach() {
        String attachedAs = database2.attachDatabase(database);
        Insert insert = Insert.into(TestModel.TABLE).columns(TestModel.PROPERTIES)
                .select(Query.select(TestModel.PROPERTIES)
                        .from(TestModel.TABLE.qualifiedFromDatabase(attachedAs)));
        dao2.beginTransaction();
        try {
            database2.tryExecStatement(insert);
            dao2.setTransactionSuccessful();
        } finally {
            dao2.endTransaction();
            database2.detachDatabase(database);
        }

        SquidCursor<TestModel> cursor = dao2
                .query(TestModel.class,
                        Query.select(TestModel.ID, TestModel.FIRST_NAME, TestModel.LAST_NAME, TestModel.BIRTHDAY));
        try {
            assertEquals(3, cursor.getCount());
            cursor.moveToFirst();
            assertEquals(model1, new TestModel(cursor));
            cursor.moveToNext();
            assertEquals(model2, new TestModel(cursor));
            cursor.moveToNext();
            assertEquals(model3, new TestModel(cursor));
        } finally {
            cursor.close();
        }

        assertFalse(database2.tryExecStatement(insert)); // Should fail after detatch
    }

    public void testAttachWhileOtherDbInTransactionThrowsException() {
        testThrowsException(new Runnable() {
            @Override
            public void run() {
                database.beginTransaction();
                try {
                    database2.attachDatabase(database);
                } finally {
                    database.endTransaction();
                }
            }
        }, IllegalStateException.class);
    }

    public void testAttachBlocksNewTransactions() {
        try {
            testAttachDetachConcurrency(false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void testInProgressTransactionsBlockAttach() {
        try {
            testAttachDetachConcurrency(true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void testAttachDetachConcurrency(final boolean transactionBeforeAttach) throws Exception {
        final AtomicReference<Exception> threadFailed = new AtomicReference<Exception>(null);

        Thread anotherThread = new Thread() {
            @Override
            public void run() {
                try {
                    dao.beginTransaction();
                    try {
                        if (transactionBeforeAttach) {
                            sleep(2000L);
                        }
                        dao.beginTransaction(); // Test with nested xact
                        try {
                            insertBasicTestModel("New", "Guy", System.currentTimeMillis() - 1);
                            dao.setTransactionSuccessful();
                        } finally {
                            dao.endTransaction();
                        }
                        dao.setTransactionSuccessful();
                    } finally {
                        dao.endTransaction();
                    }
                } catch (Exception e) {
                    threadFailed.set(e);
                }
            }
        };

        if (transactionBeforeAttach) {
            anotherThread.start();
            Thread.sleep(1000L);
        }
        String attachedAs = database2.attachDatabase(database);
        dao2.beginTransaction();
        try {
            database2.tryExecStatement(Insert.into(TestModel.TABLE).columns(TestModel.PROPERTIES)
                    .select(Query.select(TestModel.PROPERTIES)
                            .from(TestModel.TABLE.qualifiedFromDatabase(attachedAs))));
            if (!transactionBeforeAttach) {
                anotherThread.start();
                Thread.sleep(2000);
            }
            dao2.setTransactionSuccessful();
        } finally {
            dao2.endTransaction();
            database2.detachDatabase(database);
        }

        try {
            anotherThread.join();
        } catch (InterruptedException e) {
            fail();
        }

        if (threadFailed.get() != null) {
            throw threadFailed.get();
        }
        assertEquals(4, dao.count(TestModel.class, Criterion.all));
        assertEquals(3 + (transactionBeforeAttach ? 1 : 0), dao2.count(TestModel.class, Criterion.all));
    }

}
