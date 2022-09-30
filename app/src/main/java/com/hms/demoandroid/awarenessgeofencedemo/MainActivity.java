package com.hms.demoandroid.awarenessgeofencedemo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.huawei.hmf.tasks.OnFailureListener;
import com.huawei.hmf.tasks.OnSuccessListener;
import com.huawei.hms.kit.awareness.Awareness;
import com.huawei.hms.kit.awareness.barrier.AwarenessBarrier;
import com.huawei.hms.kit.awareness.barrier.BarrierUpdateRequest;
import com.huawei.hms.kit.awareness.barrier.LocationBarrier;
import com.huawei.hms.maps.HuaweiMap;
import com.huawei.hms.maps.MapView;
import com.huawei.hms.maps.model.Circle;
import com.huawei.hms.maps.model.CircleOptions;
import com.huawei.hms.maps.model.LatLng;

public class MainActivity extends AppCompatActivity implements GeofenceEventReceiver.GeofenceStatusListener {

    private MapView mapView;//MapView uses less resources than MapFragment
    private HuaweiMap huaweiMap;//Map instance
    private Circle graphicalGeofence;//Circle to display the geofence on the map
    private GeofenceEventReceiver barrierReceiver;//Receiver to capture the geofence entering event
    final static int LOCATION_REQUEST=100;//Request code for the location permissions
    final static double FIXED_RADIUS=2000.00;//Radius of an area. The unit is meter.
    private static final String MAPVIEW_BUNDLE_KEY = "MapViewBundleKey";
    private int notificationId=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mapView=findViewById(R.id.mapView);
        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle("MapViewBundleKey");
        }
        mapView.onCreate(mapViewBundle);
        mapView.getMapAsync(this::onMapReady);
    }

    private void onMapReady(HuaweiMap huaweiMap){
        this.huaweiMap=huaweiMap;//Recover your huawei map when loaded
        if(checkLocationPermission()){
            setupMap();//Enable the features based on location
        }else requestLocationPermissions();

    }

    private void requestLocationPermissions() {
        if(isBackgroundPermissionNeeded()){//Awareness kit requires Background location permissions on Android 10 and Higher
            requestPermissions (new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},LOCATION_REQUEST);
        }else requestPermissions (new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},LOCATION_REQUEST);
    }

    private boolean isBackgroundPermissionNeeded() {//check if the Android version is 10 or higher
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(checkLocationPermission()){
            setupMap();
        }
    }

    private void setupMap() {
        huaweiMap.setOnMapLongClickListener(this::onLongClick);//The long click event will be reported to the onLongClick method in this class
        huaweiMap.setMyLocationEnabled(true);//enable real time location
        huaweiMap.setWatermarkEnabled(true);//removes the Petal Map watermark
    }

    private boolean checkLocationPermission() {
        int locPermission=checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
        if(isBackgroundPermissionNeeded()){
            int backPermission=checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
            return locPermission== PackageManager.PERMISSION_GRANTED&&backPermission==PackageManager.PERMISSION_GRANTED;
        }
        else return locPermission== PackageManager.PERMISSION_GRANTED;
    }

    private void onLongClick(LatLng position){
        if(huaweiMap!=null){//Always check the map is not null
            //disable the previous geofence if exists
            removePreviousGeofence();
            //Add a new geofence
            configNewGeofence(position);
        }

    }

    @SuppressLint("MissingPermission") //This method will be only called after checking the permission
    private void configNewGeofence(LatLng position) {
        //Draw a circle on the map
        Log.e("Coordinates","Lat:"+position.latitude+"\tLon:"+position.longitude);
        CircleOptions circle=new CircleOptions();
        circle.center(position);
        circle.radius(FIXED_RADIUS);
        graphicalGeofence=huaweiMap.addCircle(circle);
        //Setup Awareness geofence
        AwarenessBarrier enterBarrier = LocationBarrier.enter(position.latitude,position.longitude, FIXED_RADIUS);
        //Define PendingIntent that will be triggered upon a barrier status change, and register a broadcast receiver to receive the broadcast.
        Intent intent = new Intent(GeofenceEventReceiver.RECEIVER_ACTION);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 1, intent,PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        barrierReceiver = new GeofenceEventReceiver();
        barrierReceiver.register(this);
        barrierReceiver.setListener(this);//the barrier receiver will send updates through the interface
        //add the barrier
        BarrierUpdateRequest.Builder builder = new BarrierUpdateRequest.Builder();
        BarrierUpdateRequest request = builder.addBarrier(GeofenceEventReceiver.GEOFENCE_LABEL, enterBarrier,pendingIntent).build();
        Awareness.getBarrierClient(this).updateBarriers(request)
                .addOnSuccessListener(aVoid->{
                    Snackbar.make(mapView,"Geofence enabled",Snackbar.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e->{
                    Log.e("MainActivity",e.getMessage());
                    Toast.makeText(this,"Failed to add the geofence, please try again",Toast.LENGTH_LONG).show();
                    removePreviousGeofence();
                });
    }

    private void removePreviousGeofence() {
        if(graphicalGeofence!=null){//Remove the circle from the map
            graphicalGeofence.remove();
        }
        //Disable the awareness geofence
        disableAwarenessGeofence();
    }

    private void disableAwarenessGeofence() {
        if(barrierReceiver!=null){
            //unregister your receiver
            unregisterReceiver(barrierReceiver);
            barrierReceiver=null;
            // Define a request for updating a barrier.
            BarrierUpdateRequest.Builder builder = new BarrierUpdateRequest.Builder();
            BarrierUpdateRequest request = builder.deleteBarrier(GeofenceEventReceiver.GEOFENCE_LABEL).build();
            Awareness.getBarrierClient(this).updateBarriers(request)
                    // Callback listener for execution success.
                    .addOnSuccessListener(aVoid -> Toast.makeText(getApplicationContext(), "delete barrier success", Toast.LENGTH_SHORT).show())
                    // Callback listener for execution failure.
                    .addOnFailureListener(e -> {
                        //Toast.makeText(getApplicationContext(), "delete barrier failed", Toast.LENGTH_SHORT).show();
                        Log.e("MainActivity", "delete barrier failed", e);
                    });
        }
    }

    @Override
    protected void onStart() {
        mapView.onStart();
        super.onStart();
    }

    @Override
    public void onLowMemory() {
        mapView.onLowMemory();
        super.onLowMemory();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        Bundle mapViewBundle = outState.getBundle(MAPVIEW_BUNDLE_KEY);
        if (mapViewBundle == null) {
            mapViewBundle = new Bundle();
            outState.putBundle(MAPVIEW_BUNDLE_KEY, mapViewBundle);
        }

        mapView.onSaveInstanceState(mapViewBundle);
    }

    @Override
    protected void onPause() {
        mapView.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {//Release resources when destroying the activity
        mapView.onDestroy();
        disableAwarenessGeofence();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        mapView.onResume();
        super.onResume();
    }

    @Override
    protected void onStop() {
        mapView.onStop();
        super.onStop();
    }

    @Override
    public void statusIn() {
        //do something when the user enters into the geofence
        displayNotification("You are inside the geofence");
    }

    @Override
    public void statusOut() {
        //do something when the user left the geofence
        Snackbar.make(mapView,"You are not in the geofence",Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void statusUnknown() {
        displayNotification("I don't know where you are");
    }

    private void displayNotification(String message){
        NotificationCompat.Builder builder=new NotificationCompat.Builder(this,AwarenessApplication.GEOFENCE_CHANNEL_ID);
        builder.setContentTitle("Geofence event");
        builder.setContentText(message);
        builder.setSmallIcon(R.mipmap.ic_launcher);
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.notify(notificationId,builder.build());
        notificationId+=1;
    }
}