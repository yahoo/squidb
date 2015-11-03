/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.sample.ios;

import com.yahoo.squidb.data.ISQLiteOpenHelper;
import com.yahoo.squidb.data.SquidDatabase;
import com.yahoo.squidb.ios.IOSOpenHelper;
import com.yahoo.squidb.sample.database.OpenHelperCreator;

public class IOSOpenHelperCreator extends OpenHelperCreator {

    @Override
    protected ISQLiteOpenHelper createOpenHelper(String databaseName, SquidDatabase.OpenHelperDelegate delegate,
            int version) {
        return new IOSOpenHelper(getDatabasePath(), databaseName, delegate, version);
    }

    public static native String getDatabasePath() /*-[
        NSArray *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
        NSString * documentsDirectory = [paths objectAtIndex:0];
        return [documentsDirectory stringByAppendingPathComponent:@"/Databases"];
    ]-*/;
}
