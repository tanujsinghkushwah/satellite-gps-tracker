package com.android.satellite_gps_tracker;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import java.util.HashMap;
import java.util.Map;

public class LocationService extends Service implements gettingData {

    String lSatellites, User_Name;

    private LocationCallback locationCallback = new LocationCallback(){
        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);
            if(locationResult != null && locationResult.getLastLocation()!=null){
                double latitude = locationResult.getLastLocation().getLatitude();
                double longitude = locationResult.getLastLocation().getLongitude();
                String Android_ID = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
                sendLocationData(latitude, longitude, Android_ID);
            }
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private void sendLocationData(double send_Latitude , double send_Longitude, String ID) {

        final String Latitude = String.valueOf(send_Latitude);
        final String Longitude = String.valueOf(send_Longitude);
        final String phone_id = ID;
        final String Name_User = User_Name;
        final String list_Satellites = lSatellites;

        StringRequest stringRequest = new StringRequest(Request.Method.POST, "https://script.google.com/macros/s/AKfycbzvBH76uNPsfTEHtYYoDU-Xw-87JVhMmS4ajwjMll4bVFjVZmQ/exec",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Toast.makeText(LocationService.this,response,Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(getApplicationContext(),MainActivity.class);
                        intent.putExtra("key_name", User_Name);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> parmas = new HashMap<>();

                //here we pass params
                parmas.put("latitude",Latitude);
                parmas.put("longitude",Longitude);
                parmas.put("android_id",phone_id);
                parmas.put("Satellites", list_Satellites);
                parmas.put("Name", Name_User);
                return parmas;
            }
        };

        int socketTimeOut = 50000;// u can change this .. here it is 50 seconds

        RetryPolicy retryPolicy = new DefaultRetryPolicy(socketTimeOut, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        stringRequest.setRetryPolicy(retryPolicy);

        RequestQueue queue = Volley.newRequestQueue(this);

        queue.add(stringRequest);
    }

    private void startLocationService(){
        String channelID = "location_notification_channel";
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Intent resultIntent = new Intent();
        PendingIntent pendingIntent = PendingIntent.getActivity(
                getApplicationContext(),
                0,
                resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
        );
        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                getApplicationContext(),
                channelID
        );
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setContentTitle("Location Service");
        builder.setDefaults(NotificationCompat.DEFAULT_ALL);
        builder.setContentText("Running");
        builder.setContentIntent(pendingIntent);
        builder.setAutoCancel(false);
        builder.setPriority(NotificationCompat.PRIORITY_MAX);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            if(notificationManager!=null
            && notificationManager.getNotificationChannel(channelID) == null){
                NotificationChannel notificationChannel = new NotificationChannel(
                        channelID,
                        "Location Service",
                        NotificationManager.IMPORTANCE_HIGH
                );
                notificationChannel.setDescription("This channel is used by location service.");
                notificationManager.createNotificationChannel(notificationChannel);
            }
        }

        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(300000);
        locationRequest.setFastestInterval(300000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationServices.getFusedLocationProviderClient(this)
                .requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        startForeground(Constants.LOCATION_SERVICE_ID, builder.build());
    }

    private void stopLocationService(){
        LocationServices.getFusedLocationProviderClient(this)
                .removeLocationUpdates(locationCallback);
        stopForeground(true);
        stopSelf();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        lSatellites = intent.getExtras().getString("Satellites");
        User_Name = intent.getExtras().getString("Name");

        if(intent != null){
            String action = intent.getAction();
            if(action!=null){
                if(action.equals(Constants.ACTION_START_LOCATION_SERVICE)){
                    startLocationService();
                } else if(action.equals(Constants.ACTION_STOP_LOCATION_SERVICE)){
                    stopLocationService();
                }
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }
}
