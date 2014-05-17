package com.github.michalbednarski.intentslab.clipboard;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import com.github.michalbednarski.intentslab.R;

/**
 * Created by mb on 17.05.14.
 * TODO: Start this service when clipboard contents needs to be kept
 */
public class KeepAliveService extends Service {

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(236,
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentText("[Bound services]")
                        .setContentIntent(PendingIntent.getActivity(
                                this,
                                0,
                                new Intent(this, ClipboardActivity.class),
                                0
                        ))
                        .build());
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
