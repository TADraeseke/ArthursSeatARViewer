package td.com.nautilus.arthursseatarviewer;

import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

//Defines GPS as a class which implements LocationListener
public class GPS extends Service implements LocationListener {

    //Define program context
    private final Context mContext;

    //isGPSEnabled toggle
    boolean isGPSEnabled = false;

    //isNetworkEnabled toggle
    boolean isNetworkEnabled = false;

    //canGetLocation toggle
    boolean canGetLocation = false;

    //Instantiate location, lat, long
    Location location;
    double latitude;
    double longitude;

    //Min distance moved before location updates
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10; // 10 meters

    //Min time between location updates
    private static final long MIN_TIME_BW_UPDATES = 1000 * 60 * 1; // 1 minute

    //Instantiate locationManager
    protected LocationManager locationManager;

    //Constructor
    public GPS(Context context) {
        this.mContext = context;
        getLocation();
    }

    //Location getter
    public Location getLocation() {
        try {
            locationManager = (LocationManager) mContext
                    .getSystemService(LOCATION_SERVICE);

            //Get GPS status
            isGPSEnabled = locationManager
                    .isProviderEnabled(LocationManager.GPS_PROVIDER);

            //Get network provider status
            isNetworkEnabled = locationManager
                    .isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            //If isGPSEnabled or isNetworkEnabled is true
            if (isGPSEnabled || isNetworkEnabled) {
                //Set canGetLocation toggle to true
                this.canGetLocation = true;
                //If isNetworkEnabled is true, get location via NETWORK_PROVIDER
                if (isNetworkEnabled) {
                    locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER, MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                    Log.d("Network", "Network");
                    if (locationManager != null) {
                        location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                        if (location != null) {
                            latitude = location.getLatitude();
                            longitude = location.getLongitude();
                        }
                    }
                }
                // if isGPSEnabled is true, get location via GPS_PROVIDER
                if (isGPSEnabled) {
                    if (location == null) {
                        locationManager.requestLocationUpdates(
                                LocationManager.GPS_PROVIDER, MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                        Log.d("GPS Enabled", "GPS Enabled");
                        if (locationManager != null) {
                            location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                            if (location != null) {
                                latitude = location.getLatitude();
                                longitude = location.getLongitude();
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return location;
    }

    //Stops GPS
    public void stopUsingGPS(){
        if(locationManager != null){
            locationManager.removeUpdates(GPS.this);
        }
    }

    //Latitude getter
    public double getLatitude(){
        if(location != null){
            latitude = location.getLatitude();
        }
        return latitude;
    }

    //Longtitude getter
    public double getLongitude(){
        if(location != null){
            longitude = location.getLongitude();
        }
        return longitude;
    }

    //canGetLocation toggle getter
    public boolean canGetLocation() {
        return this.canGetLocation;
    }

    @Override
    //When location change detected, get lat long.
    public void onLocationChanged(Location location) {
        getLatitude();
        getLongitude();
    }

    @Override
    //Required function of LocationListener. Unused.
    public void onProviderDisabled(String provider) {
    }

    @Override
    //Required function of LocationListener. Unused.
    public void onProviderEnabled(String provider) {
    }

    @Override
    //Required function of LocationListener. Unused.
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    //Binds the service
    public IBinder onBind(Intent arg0) {
        return null;
    }
}