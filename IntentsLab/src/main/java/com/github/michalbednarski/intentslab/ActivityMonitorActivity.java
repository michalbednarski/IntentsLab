/*
 * IntentsLab - Android app for playing with Intents and Binder IPC
 * Copyright (C) 2014 Michał Bednarski
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.github.michalbednarski.intentslab;

import android.app.IActivityController;
import android.app.ListActivity;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcelable;
import android.os.RemoteException;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;
import com.github.michalbednarski.intentslab.editor.IntentEditorActivity;
import com.github.michalbednarski.intentslab.runas.IRemoteInterface;
import com.github.michalbednarski.intentslab.runas.RunAsManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mb on 21.09.13.
 */
public class ActivityMonitorActivity extends ListActivity {

    private static ArrayList<Intent> sStagingIntents = null;
    private static final Handler sHandler = new Handler();
    private static final ArrayList<ActivityMonitorActivity> sStagingListViewers = new ArrayList<ActivityMonitorActivity>();

    private Intent[] mRecordedIntents;
    private ArrayAdapter<Intent> mAdapter;
    private boolean mIsInStagingListViewers = false;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            final Parcelable[] recordedIntentsRaw = savedInstanceState.getParcelableArray("recordedIntents");
            if (recordedIntentsRaw != null) {
                mRecordedIntents = new Intent[recordedIntentsRaw.length];
                System.arraycopy(recordedIntentsRaw, 0, mRecordedIntents, 0, recordedIntentsRaw.length);
            }
        } else if (sStagingIntents == null) {
            startRecording();
        }
        if (mRecordedIntents == null && sStagingIntents == null) {
            finish();
            return;
        }
        getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Object item = parent.getItemAtPosition(position);
                if (item instanceof Intent) {
                    startActivity(
                            new Intent(ActivityMonitorActivity.this, IntentEditorActivity.class)
                            .putExtra(IntentEditorActivity.EXTRA_INTENT, (Intent) item)
                    );
                }
            }
        });
        chooseAndSetAdapter();
    }

    @Override
    protected void onDestroy() {
        addOrRemoveFromStagingListViewers(false);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.activity_monitor, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        boolean endedRecording = mRecordedIntents != null;
        menu.findItem(R.id.end_recording).setVisible(!endedRecording).setEnabled(!endedRecording);
        menu.findItem(R.id.save).setVisible(endedRecording).setEnabled(endedRecording);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.end_recording:
                endRecording();
                return true;
            // TODO: save
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mRecordedIntents != null) {
            outState.putParcelableArray("recordedIntents", mRecordedIntents);
        }
    }

    private void chooseAndSetAdapter() {
        if (mRecordedIntents != null) {
            mAdapter = new IntentArrayAdapter(this, mRecordedIntents);
            addOrRemoveFromStagingListViewers(false);
        } else {
            mAdapter = new IntentArrayAdapter(this, sStagingIntents);
            addOrRemoveFromStagingListViewers(true);
        }
        setListAdapter(mAdapter);
    }

    private void addOrRemoveFromStagingListViewers(boolean shouldBeInStagingListViewersList) {
        if (shouldBeInStagingListViewersList != mIsInStagingListViewers) {
            if (shouldBeInStagingListViewersList) {
                sStagingListViewers.add(this);
            } else {
                sStagingListViewers.remove(this);
            }
        }
        mIsInStagingListViewers = shouldBeInStagingListViewersList;
    }

    private void startRecording() {
        // Get remote interface
        IRemoteInterface mRemoteInterface = RunAsManager.getRemoteInterfaceForSystemDebuggingCommands();

        // Ensure it exists
        if (mRemoteInterface == null) {
            Toast.makeText(this, "No remote interface", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Set our watcher controller
        try {
            sStagingIntents = new ArrayList<Intent>();
            mRemoteInterface.setActivityController(new MyActivityController());
            startService(new Intent(this, ActivityRecorderService.class));
        } catch (RemoteException e) {
            Utils.toastException(this, e);
            finish();
        }
    }

    private void endRecording() {
        mRecordedIntents = sStagingIntents.toArray(new Intent[sStagingIntents.size()]);
        cancelRecording();
        stopService(new Intent(this, ActivityRecorderService.class));
        chooseAndSetAdapter();
        ActivityCompat.invalidateOptionsMenu(this);
    }

    private static void cancelRecording() {
        if (sStagingIntents == null) {
            return;
        }

        // Clear staging intents
        sStagingIntents = null;

        // Remove activity controller
        IRemoteInterface mRemoteInterface = RunAsManager.getRemoteInterfaceForSystemDebuggingCommands();
        try {
            if (mRemoteInterface != null) {
                mRemoteInterface.setActivityController(null);
            }
        } catch (RemoteException ignored) {
            // We cannot do anything to unregister, we have to keep controller which won't do anything
            // when sStagingIntents == null
        }
    }

    private static class IntentArrayAdapter extends ArrayAdapter<Intent> {

        public IntentArrayAdapter(Context context, List<Intent> objects) {
            super(context, 0, objects);
        }

        public IntentArrayAdapter(Context context, Intent[] objects) {
            super(context, 0, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                final LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(android.R.layout.simple_list_item_2, parent, false);
            }
            final Intent intent = getItem(position);
            CharSequence text1;
            if (!Utils.stringEmptyOrNull(intent.getAction())) {
                text1 = intent.getAction();
            } else {
                final PackageManager pm = getContext().getPackageManager();
                try {
                    final ActivityInfo activityInfo = pm.getActivityInfo(intent.getComponent(), 0);
                    text1 = activityInfo.loadLabel(pm);
                } catch (Exception e) {
                    text1 = "?";
                }
            }

            ((TextView) convertView.findViewById(android.R.id.text1)).setText(
                     text1
            );
            ((TextView) convertView.findViewById(android.R.id.text2)).setText(
                    intent.getComponent().flattenToShortString()
            );
            return convertView;
        }
    }

    private static class MyActivityController extends IActivityController.Stub {

        @Override
        public boolean activityStarting(final Intent intent, String pkg) throws RemoteException {
            // Log
            sHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (sStagingIntents != null) {
                        sStagingIntents.add(intent);
                        for (ActivityMonitorActivity activity : sStagingListViewers) {
                            activity.mAdapter.notifyDataSetChanged();
                        }

                    }
                }
            });

            return true; // Allow starting activity
        }

        @Override
        public boolean activityResuming(String pkg) throws RemoteException {
            return true; // Allow resuming activity
        }

        @Override
        public boolean appCrashed(String processName, int pid, String shortMsg, String longMsg, long timeMillis, String stackTrace) throws RemoteException {
            return true; // Prompt user, normal system behavior
        }

        @Override
        public int appEarlyNotResponding(String processName, int pid, String annotation) throws RemoteException {
            return 0;
        }

        @Override
        public int appNotResponding(String processName, int pid, String processStats) throws RemoteException {
            return 0; // Prompt user, normal system behavior
        }

        @Override
        public int systemNotResponding(String msg) throws RemoteException {
            return -1; // Restart, normal system behavior
        }
    }

    public static class ActivityRecorderService extends Service {
        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            startForeground(325,
                    new NotificationCompat.Builder(this)
                            .setOngoing(true)
                            .setSmallIcon(R.drawable.ic_launcher) // TODO
                            .setContentTitle("Recording activity intents...")
                            .setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, ActivityMonitorActivity.class), 0))
                            .build()
            );
            return START_NOT_STICKY;
        }

        @Override
        public void onDestroy() {
            cancelRecording();
        }

        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }
    }
}