package com.hms.demoandroid.awarenessgeofencedemo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.huawei.hms.kit.awareness.barrier.BarrierStatus;

public class GeofenceEventReceiver extends BroadcastReceiver {
    private static final String TAG="GeofenceEvent";
    private GeofenceStatusListener listener;
    //Replace the prefix with your own package name
    public static final String RECEIVER_ACTION="com.hms.demoandroid.awarenessgeofencedemo.LOCATION_BARRIER_RECEIVER_ACTION";
    public static final String GEOFENCE_LABEL="location enter barrier";

    public void register(Context context){
        IntentFilter intentFilter=new IntentFilter(RECEIVER_ACTION);
        context.registerReceiver(this,intentFilter);
    }

    public void setListener(GeofenceStatusListener listener) {
        this.listener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        BarrierStatus barrierStatus = BarrierStatus.extract(intent);
        String label = barrierStatus.getBarrierLabel();
        switch(barrierStatus.getPresentStatus()) {
            case BarrierStatus.TRUE:
                Log.i(TAG, label + " status:true");
                if(listener!=null) listener.statusIn();
                break;
            case BarrierStatus.FALSE:
                Log.i(TAG, label + " status:false");
                if(listener!=null) listener.statusOut();
                break;
            case BarrierStatus.UNKNOWN:
                Log.i(TAG, label + " status:unknown");
                if(listener!=null) listener.statusUnknown();
                break;
        }
    }



    public interface GeofenceStatusListener{
        void statusIn();
        void statusOut();
        void statusUnknown();
    }
}
