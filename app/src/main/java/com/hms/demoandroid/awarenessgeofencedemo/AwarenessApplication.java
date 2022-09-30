package com.hms.demoandroid.awarenessgeofencedemo;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.huawei.hms.maps.MapsInitializer;

public class AwarenessApplication extends Application {
    public static final String GEOFENCE_CHANNEL_ID= "myGeofenceChannel";

    @Override
    public void onCreate() {
        MapsInitializer.setApiKey("DAEDAEXOlbCKmcccWX3fStm5qxHJkkaqtZ6KSu0igpTl2vbLKQPkmHlvXii6rigSnUZuZhqxR41ddUErhVLgDHlNwxOIiM4ow/FLKw==");
        MapsInitializer.initialize(this);
        createNotificationChannel();
        super.onCreate();

    }

    private void createNotificationChannel() {
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
            NotificationChannel channel=new NotificationChannel(GEOFENCE_CHANNEL_ID,"Geofence Notifications", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Receive alerts when entering on the geofence");
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);
        }
    }
}
