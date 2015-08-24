package com.i_echo.lpc.i_echo.Utils;

import android.app.AlertDialog;
import android.app.Service;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

import com.i_echo.lpc.i_echo.Constants;

/**
 * Created by LPC-Home1 on 7/7/2015.
 */
public class UtilsLocationTracker extends Service implements LocationListener {
    public static String LOGTAG = "UtilsLocationTracker";

    public static final String ACTION_BROADCAST_SPEED = "com.i_echo.action.SPEED_UPDATE";
    public static final String ACTION_BROADCAST_LOCATION = "com.i_echo.action.LOCATION_UPDATE";
    public static final String ACTION_STOP_LOCATION_TRACKER = "com.i_echo.action.STOP_LOCATION_TRACKER";
    public static final int MAX_SPEED_VECTOR = 10;                   // max speed vector averaged over 10*1 minutes
    private static final long MIN_DISTANCE_FOR_UPDATES = 10;        // 10 meters
    private static final long MIN_TIME_BETWEEN_UPDATES = 1000 * 60 * 1; // 1 minute
    private static final int TWO_MINUTES = 1000 * 60 * 2;

    private Location mLocation;                             // location
    public Location mPreviousBestLocation = null;
    private int[] mSpeed;                                   // speed in meters/hour
    private int mNumSpeedIdxLow;
    private int mNumSpeedIdxHigh;
    private boolean mIsGPSEnabled = false;                  // flag for GPS status
    private boolean mIsNetworkEnabled = false;              // flag for network status
    private boolean mCanGetLocation = false;                // flag for GPS status
    private LocationManager mLocationManager = null;         // Declaring a Location Manager
    private Intent mIntent;                                 // for communicating with/broadcasting to activity main

    @Override
    public void onCreate() {
        super.onCreate();

        mIntent = new Intent(ACTION_BROADCAST_SPEED);
        mSpeed = new int[MAX_SPEED_VECTOR];                 // all zeros
        mNumSpeedIdxLow = 0;
        mNumSpeedIdxHigh = 0;

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();

        if (null == intent || null == action) {
            getLocation();       // initialize location tracking and get the latest location
        } else if (ACTION_STOP_LOCATION_TRACKER.equals(intent.getAction())) {
            if (Constants.DEBUGGING)
                Log.d(LOGTAG, "Commanded to stop location service");
            stopLocationTracker();
        }

        return START_STICKY;        // or return START_NOT_STICKY;
    }

    @Override
    public void onLocationChanged(Location loc) {
        if (null != loc) {
            if (isBetterLocation(loc, mLocation))
                mLocation = loc;
            updateSpeedVector(toKphSpeed(mLocation.getSpeed()));
            mIntent.putExtra("Speed", getAverageSpeed());
            mIntent.putExtra("Latitude", mLocation.getLatitude());
            mIntent.putExtra("Longitude", mLocation.getLongitude());
            mIntent.putExtra("Provider", mLocation.getProvider());
            sendBroadcast(mIntent);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (Constants.DEBUGGING)
            Log.d(LOGTAG, "DONE location tracker");
        mLocationManager.removeUpdates(this);
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }


    public Location getLocation() {
        try {
            if (null == mLocationManager) {
                mLocationManager = (LocationManager) getApplicationContext().
                        getSystemService(LOCATION_SERVICE);
                // getting GPS and network location status
                mIsGPSEnabled = mLocationManager.
                        isProviderEnabled(LocationManager.GPS_PROVIDER);
                mIsNetworkEnabled = mLocationManager.
                        isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            }

            if (!mIsGPSEnabled && !mIsNetworkEnabled) {
                mLocation = null;
            } else {
                this.mCanGetLocation = true;
                // if GPS Enabled get lat/long using GPS Services
                if (mIsGPSEnabled) {
                    mLocationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            MIN_TIME_BETWEEN_UPDATES, MIN_DISTANCE_FOR_UPDATES, this);
                    Log.d(LOGTAG, "GPS enabled location");
                    if (null != mLocationManager) {
                        Location loc = mLocationManager
                                .getLastKnownLocation(LocationManager.GPS_PROVIDER);
                        if (null != loc) {
                            if (isBetterLocation(loc, mLocation))
                                mLocation = loc;
                            updateSpeedVector(toKphSpeed(mLocation.getSpeed()));
                        }
                    }
                } else if (mIsNetworkEnabled) {
                    mLocationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            MIN_TIME_BETWEEN_UPDATES, MIN_DISTANCE_FOR_UPDATES, this);
                    if (Constants.DEBUGGING)
                        Log.d(LOGTAG, "Network location");
                    if (null != mLocationManager) {
                        mLocation = mLocationManager.
                                getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                        if (null != mLocation) {
                            updateSpeedVector(toKphSpeed(mLocation.getSpeed()));
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return mLocation;
    }

    private void updateSpeedVector(int speed) {
        mSpeed[mNumSpeedIdxHigh] =speed;

        if (mNumSpeedIdxLow <= mNumSpeedIdxHigh) {
            if (mNumSpeedIdxHigh < MAX_SPEED_VECTOR-1)
                mNumSpeedIdxHigh++;
            else {
                mNumSpeedIdxHigh = 0;   // wrap-around
                if (mNumSpeedIdxHigh ==mNumSpeedIdxLow)
                    mNumSpeedIdxLow++;  // remove lowest
            }
        }
        else {
            if (mNumSpeedIdxHigh <mNumSpeedIdxLow -1) {
            }
            else {
                mNumSpeedIdxLow++;  // remove lowest
            }
            mNumSpeedIdxHigh++;
        }
    }

    private int getAverageSpeed() {
        int result =0, numTotal;
        if (mNumSpeedIdxLow <= mNumSpeedIdxHigh) {
            numTotal =mNumSpeedIdxHigh -mNumSpeedIdxLow;
            for (int i = mNumSpeedIdxLow; i< mNumSpeedIdxHigh; i++)
                result += mSpeed[i];
        }
        else {
            numTotal =(MAX_SPEED_VECTOR -mNumSpeedIdxHigh)
                        + (mNumSpeedIdxLow+1);
            for (int i = mNumSpeedIdxHigh; i< MAX_SPEED_VECTOR-1; i++)
                result += mSpeed[i];
            for (int i = 0; i<= mNumSpeedIdxLow; i++)
                result += mSpeed[i];
        }
        return Math.round(result/numTotal);
    }

    private int toKphSpeed (float mpsecSpeed) {
        return Math.round((mpsecSpeed/1000)*(60*60));
    }

    /**
     * Stop using GPS listener
     * Calling this function will stop using GPS in your app
     * */
    public void stopLocationTracker(){
        if(mLocationManager != null){
            mLocationManager.removeUpdates(UtilsLocationTracker.this);
        }
    }
    /**
     * Function to check GPS/wifi enabled
     * @return boolean
     * */
    public boolean mCanGetLocation() {
        return this.mCanGetLocation;
    }

    /**
     * Function to show settings alert dialog
     * On pressing Settings button will launch Settings Options
     * */
    public void showSettingsAlert(){
        AlertDialog.Builder alertDialog =
                new AlertDialog.Builder(getApplicationContext());

        // Setting Dialog Title
        alertDialog.setTitle("GPS is settings");

        // Setting Dialog Message
        alertDialog.setMessage("GPS is not enabled. Do you want to go to settings menu?");

        // On pressing Settings button
        alertDialog.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,int which) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                getApplicationContext().startActivity(intent);
            }
        });

        // on pressing cancel button
        alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        // Showing Alert Message
        alertDialog.show();
    }

    protected static boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    /** Checks whether two providers are the same */
    private static boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }


}
