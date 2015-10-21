/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sql;

import com.yahoo.squidb.data.SquidCursor;
import com.yahoo.squidb.test.DatabaseTestCase;
import com.yahoo.squidb.test.TestDatabase;
import com.yahoo.squidb.test.TestModel;
import com.yahoo.squidb.test.TestVirtualModel;

import java.util.concurrent.atomic.AtomicReference;

public class AttachDetachTest extends DatabaseTestCase {

    private TestModel model1;
    private TestModel model2;
    private TestModel model3;
    private TestVirtualModel virtualModel;

    private TestDatabase database2;

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

        virtualModel = new TestVirtualModel().setTestNumber(1L).setTitle("A").setBody("B");
        database.persist(virtualModel);

        database2.clear();
        assertEquals(0, database2.countAll(TestModel.class));
    }

    public void testAttachDetach() {
        String attachedAs = database2.attachDatabase(database);
        Insert insert = Insert.into(TestModel.TABLE).columns(TestModel.PROPERTIES)
                .select(Query.select(TestModel.PROPERTIES)
                        .from(TestModel.TABLE.qualifiedFromDatabase(attachedAs)));
        Insert insertVirtual = Insert.into(TestVirtualModel.TABLE).columns(TestVirtualModel.PROPERTIES)
                .select(Query.select(TestVirtualModel.PROPERTIES)
                        .from(TestVirtualModel.TABLE.qualifiedFromDatabase(attachedAs)));
        database2.beginTransaction();
        try {
            database2.tryExecStatement(insert);
            database2.tryExecStatement(insertVirtual);
            database2.setTransactionSuccessful();
        } finally {
            database2.endTransaction();
            database2.detachDatabase(database);
        }

        SquidCursor<TestModel> cursor = database2
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
        assertEquals(virtualModel,
                database2.fetch(TestVirtualModel.class, virtualModel.getId(), TestVirtualModel.PROPERTIES));

        assertFalse(database2.tryExecStatement(insert)); // Should fail after detach
    }

    public void testAttacheeWithTransactionOnSameThreadThrowsException() {
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

    public void testAttacherWithTransactionOnSameThreadThrowsException() {
        testThrowsException(new Runnable() {
            public void run() {
                database2.beginTransaction();
                try {
                    database2.attachDatabase(database);
                } finally {
                    database2.endTransaction();
                }
            }
        }, IllegalStateException.class);
    }

    public void testAttachBlocksNewTransactionsInAttachee() {
        try {
            testAttachDetachConcurrency(false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void testAttacheeWithTransactionOnOtherThreadBlocksAttach() {
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
                    database.beginTransaction();
                    try {
                        if (transactionBeforeAttach) {
                            sleep(2000L);
                        }
                        database.beginTransaction(); // Test with nested transaction
                        try {
                            insertBasicTestModel("New", "Guy", System.currentTimeMillis() - 1);
                            database.setTransactionSuccessful();
                        } finally {
                            database.endTransaction();
                        }
                        database.setTransactionSuccessful();
                    } finally {
                        database.endTransaction();
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
        database2.beginTransaction();
        try {
            database2.tryExecStatement(Insert.into(TestModel.TABLE).columns(TestModel.PROPERTIES)
                    .select(Query.select(TestModel.PROPERTIES)
                            .from(TestModel.TABLE.qualifiedFromDatabase(attachedAs))));
            if (!transactionBeforeAttach) {
                anotherThread.start();
                Thread.sleep(2000L);
            }
            database2.setTransactionSuccessful();
        } finally {
            database2.endTransaction();
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
        assertEquals(4, database.countAll(TestModel.class));
        assertEquals(3 + (transactionBeforeAttach ? 1 : 0), database2.countAll(TestModel.class));
    }

    /*
     * NOTE: This test is only relevant if write ahead logging (WAL) is enabled on the attacher. If the attacher does
     * not have WAL enabled, this test should always pass.
     */
    public void testAttacherInTransactionOnAnotherThread() throws Exception {
        final AtomicReference<Exception> threadFailed = new AtomicReference<Exception>(null);

        Thread anotherThread = new Thread() {
            @Override
            public void run() {
                try {
                    database2.beginTransaction();
                    try {
                        sleep(2000L);
                        database2.persist(new TestModel().setFirstName("Alan").setLastName("Turing"));
                        database2.setTransactionSuccessful();
                    } finally {
                        database2.endTransaction();
                    }
                } catch (Exception e) {
                    threadFailed.set(e);
                }
            }
        };

        anotherThread.start();
        Thread.sleep(1000L);

        String attachedAs = database2.attachDatabase(database);
        database2.beginTransaction();
        try {
            database2.tryExecStatement(Insert.into(TestModel.TABLE).columns(TestModel.FIRST_NAME, TestModel.LAST_NAME)
                    .select(Query.select(TestModel.FIRST_NAME, TestModel.LAST_NAME)
                            .from(TestModel.TABLE.qualifiedFromDatabase(attachedAs))));
            database2.setTransactionSuccessful();
        } finally {
            database2.endTransaction();
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
        assertEquals(4, database2.countAll(TestModel.class));
    }

}
