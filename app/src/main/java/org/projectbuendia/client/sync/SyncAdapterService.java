// Copyright 2015 The Project Buendia Authors
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may not
// use this file except in compliance with the License.  You may obtain a copy
// of the License at: http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distrib-
// uted under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
// OR CONDITIONS OF ANY KIND, either express or implied.  See the License for
// specific language governing permissions and limitations under the License.

package org.projectbuendia.client.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/** A service that holds a singleton SyncAdapter and provides it to the OS on request. */
public class SyncAdapterService extends Service {

    private static SyncAdapter syncAdapter = null;
    private static final Object lock = new Object();

    public static SyncAdapter getSyncAdapter() {
        return syncAdapter;
    }

    @Override public void onCreate() {
        Log.i("SyncAdapterService", "onCreate");
        super.onCreate();
        synchronized (lock) {
            if (syncAdapter == null) {
                syncAdapter = new SyncAdapter(getApplicationContext(), true);
            }
        }
    }

    @Override public IBinder onBind(Intent intent) {
        Log.i("SyncAdapter", "onBind");
        return syncAdapter.getSyncAdapterBinder();
    }
}
