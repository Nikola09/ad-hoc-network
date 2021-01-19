package com.example.adhocnetwork.adhocnetwork;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.IBinder;

import com.google.android.gms.nearby.connection.Payload;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SharingService extends Service {
    private long initTime = 2;
    private long updateTime = 6;
    private ScheduledExecutorService executor;
    private ConnectionService mConnectionService;
    private MyData myData;
    private MediaPlayer mediaPlayer;

    private final Runnable sharer = new Runnable() {
        public void run() {
            try {
                System.out.println("Running scheduled update check ");

                myData.updateFakeSensors();
                Payload payload = Payload.fromBytes(MyData.serialize(myData.getMyDataTransfer()));
                mConnectionService.send(payload);

                if (myData.getHealthAlarm())
                {
                    if (mediaPlayer.isPlaying())
                        mediaPlayer.stop();
                    mediaPlayer.start();

                }

            } catch ( Exception e ) {
                System.out.println( "ERROR - unexpected exception: " + e.getMessage() );
            }

        }
    };

    public SharingService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mConnectionService = ConnectionService.getInstance();
        myData = MyData.getInstance();
        mediaPlayer  = MediaPlayer.create(getApplicationContext(), R.raw.alarm);

        executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleWithFixedDelay( sharer, initTime, updateTime, TimeUnit.SECONDS);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }


}