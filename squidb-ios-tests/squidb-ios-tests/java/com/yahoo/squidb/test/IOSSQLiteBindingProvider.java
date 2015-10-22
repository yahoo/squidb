/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the Apache 2.0 License.
 * See the accompanying LICENSE file for terms.
 */
package com.yahoo.squidb.test;

import com.yahoo.squidb.data.ISQLiteOpenHelper;
import com.yahoo.squidb.data.SquidDatabase;
import com.yahoo.squidb.ios.IOSOpenHelper;

public class IOSSQLiteBindingProvider extends SQLiteBindingProvider {
    
    public ISQLiteOpenHelper createOpenHelper(String databaseName, SquidDatabase.OpenHelperDelegate delegate, int version) {
        return new IOSOpenHelper(getDatabasePath(), databaseName, delegate, version);
    }
    
    public static native String getDatabasePath() /*-[
        NSArray *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);         
        NSString * documentsDirectory = [paths objectAtIndex:0];
        return [documentsDirectory stringByAppendingPathComponent:@"/Databases"];
    ]-*/;
}