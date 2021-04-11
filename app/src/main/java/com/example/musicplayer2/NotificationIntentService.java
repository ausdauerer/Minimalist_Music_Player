package com.example.musicplayer2;

import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class NotificationIntentService extends IntentService {

    public NotificationIntentService() {
        super("notificationIntentService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        switch (intent.getAction()) {
            case "playpause":
                Log.d("dbg","play button of notification clickes");
                Handler playHandler = new Handler(Looper.getMainLooper());
                playHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getBaseContext(), "You clicked the Play Pause Button", Toast.LENGTH_LONG).show();
                    }
                });
                break;
            case "previous":
                Log.d("dbg","play button of notification clickes");
                Handler prevHandler = new Handler(Looper.getMainLooper());
                prevHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getBaseContext(), "You clicked the Previous Button", Toast.LENGTH_LONG).show();
                    }
                });
                break;
            case "next":
                Log.d("dbg","play button of notification clickes");
                Handler nextHandler = new Handler(Looper.getMainLooper());
                nextHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getBaseContext(), "You clicked the Next Button", Toast.LENGTH_LONG).show();
                        Intent broadcastIntent = new Intent();
                        broadcastIntent.setAction("NEXT_BUTTON_CLICKED");
                        getApplicationContext().sendBroadcast(broadcastIntent);
                    }
                });
                break;
        }
    }
}