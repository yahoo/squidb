/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.android;

import com.yahoo.squidb.test.DatabaseTestCase;
import com.yahoo.squidb.test.Thing;
import com.yahoo.squidb.utility.VersionCode;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

public class SquidDatabaseAndroidTest extends DatabaseTestCase {

    public void testVersionForCustomBinding() {
        if (SquidTestRunner.selectedBinding == SquidTestRunner.SquidbBinding.SQLITE) {
            assertEquals(VersionCode.LATEST, database.getSqliteVersion());
        }
    }

    public void testConcurrencyStressTest() {
        int numThreads = 20;
        final AtomicReference<Exception> exception = new AtomicReference<Exception>();
        List<Thread> workers = new ArrayList<Thread>();
        for (int i = 0; i < numThreads; i++) {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    concurrencyStressTest(exception);
                }
            });
            t.start();
            workers.add(t);
        }
        for (Thread t : workers) {
            try {
                t.join();
            } catch (Exception e) {
                exception.set(e);
            }
        }
        assertNull(exception.get());
    }

    private void concurrencyStressTest(AtomicReference<Exception> exception) {
        try {
            Random r = new Random();
            int numOperations = 100;
            Thing t = new Thing();
            for (int i = 0; i < numOperations; i++) {
                int rand = r.nextInt(10);
                if (rand == 0) {
                    database.close();
                } else if (rand == 1) {
                    database.clear();
                } else if (rand == 2) {
                    database.recreate();
                } else if (rand == 3) {
                    database.beginTransactionNonExclusive();
                    try {
                        for (int j = 0; j < 20; j++) {
                            t.setFoo(Integer.toString(j))
                                    .setBar(-j);
                            database.createNew(t);
                        }
                        database.setTransactionSuccessful();
                    } finally {
                        database.endTransaction();
                    }
                } else {
                    t.setFoo(Integer.toString(i))
                            .setBar(-i);
                    database.createNew(t);
                }
            }
        } catch (Exception e) {
            exception.set(e);
        }
    }
}
