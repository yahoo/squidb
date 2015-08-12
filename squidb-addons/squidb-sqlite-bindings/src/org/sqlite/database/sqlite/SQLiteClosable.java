/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
** Modified to support SQLite extensions by the SQLite developers: 
** sqlite-dev@sqlite.org.
*/

package org.sqlite.database.sqlite;

import java.io.Closeable;

/**
 * An object created from a SQLiteDatabase that can be closed.
 *
 * This class implements a primitive reference counting scheme for database objects.
 */
public abstract class SQLiteClosable implements Closeable {
    private int mReferenceCount = 1;

    /**
     * Called when the last reference to the object was released by
     * a call to {@link #releaseReference()} or {@link #close()}.
     */
    protected abstract void onAllReferencesReleased();

    /**
     * Called when the last reference to the object was released by
     * a call to {@link #releaseReferenceFromContainer()}.
     *
     * @deprecated Do not use.
     */
    @Deprecated
    protected void onAllReferencesReleasedFromContainer() {
        onAllReferencesReleased();
    }

    /**
     * Acquires a reference to the object.
     *
     * @throws IllegalStateException if the last reference to the object has already
     * been released.
     */
    public void acquireReference() {
        synchronized(this) {
            if (mReferenceCount <= 0) {
                throw new IllegalStateException(
                        "attempt to re-open an already-closed object: " + this);
            }
            mReferenceCount++;
        }
    }

    /**
     * Releases a reference to the object, closing the object if the last reference
     * was released.
     *
     * @see #onAllReferencesReleased()
     */
    public void releaseReference() {
        boolean refCountIsZero = false;
        synchronized(this) {
            refCountIsZero = --mReferenceCount == 0;
        }
        if (refCountIsZero) {
            onAllReferencesReleased();
        }
    }

    /**
     * Releases a reference to the object that was owned by the container of the object,
     * closing the object if the last reference was released.
     *
     * @see #onAllReferencesReleasedFromContainer()
     * @deprecated Do not use.
     */
    @Deprecated
    public void releaseReferenceFromContainer() {
        boolean refCountIsZero = false;
        synchronized(this) {
            refCountIsZero = --mReferenceCount == 0;
        }
        if (refCountIsZero) {
            onAllReferencesReleasedFromContainer();
        }
    }

    /**
     * Releases a reference to the object, closing the object if the last reference
     * was released.
     *
     * Calling this method is equivalent to calling {@link #releaseReference}.
     *
     * @see #releaseReference()
     * @see #onAllReferencesReleased()
     */
    public void close() {
        releaseReference();
    }
}
