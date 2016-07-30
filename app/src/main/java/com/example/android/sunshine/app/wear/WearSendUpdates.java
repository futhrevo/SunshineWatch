package com.example.android.sunshine.app.wear;

import android.app.IntentService;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.Target;
import com.example.android.sunshine.app.Utility;
import com.example.android.sunshine.app.data.WeatherContract;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.ExecutionException;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
public class WearSendUpdates extends IntentService implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = WearSendUpdates.class.getSimpleName();
    private Cursor cursor;
    private GoogleApiClient googleApiClient;
    private static final String[] FORECAST_COLUMNS = {
            WeatherContract.WeatherEntry.TABLE_NAME + "." + WeatherContract.WeatherEntry._ID,
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP
    };
    // these indices must match the projection
    static final int INDEX_WEATHER_ID = 0;
    static final int INDEX_WEATHER_CONDITION_ID = 1;
    static final int INDEX_WEATHER_DESC = 2;
    static final int INDEX_WEATHER_MAX_TEMP = 3;
    static final int INDEX_WEATHER_MIN_TEMP = 4;

    private int weatherId;
    private double maxTemp;
    private double minTemp;

    private static final String PATH = "/wear-weather";
    private static final String PATH_MAXTEMP = "maxTemp";
    private static final String PATH_MINTEMP = "minTemp";
    private static final String PATH_ICON = "wearIcon";

    public WearSendUpdates() {
        super("WearSendUpdates");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(googleApiClient.isConnected()){
            googleApiClient.disconnect();
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.i(TAG, "on Handle Intent");
        if (intent != null) {
           String location = Utility.getPreferredLocation(this);
            Uri uri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(location, System.currentTimeMillis());
            cursor = getContentResolver().query(uri, FORECAST_COLUMNS, null, null,
                    WeatherContract.WeatherEntry.COLUMN_DATE+ " ASC");

            if(cursor == null){
                return;
            }

            if(!cursor.moveToFirst()){
                cursor.close();
                return;
            }
            weatherId = cursor.getInt(INDEX_WEATHER_CONDITION_ID);
            maxTemp = cursor.getDouble(INDEX_WEATHER_MAX_TEMP);
            minTemp = cursor.getDouble(INDEX_WEATHER_MIN_TEMP);

            Log.i(TAG, String.valueOf(maxTemp));
            cursor.close();
            if(googleApiClient == null){
                googleApiClient = new GoogleApiClient.Builder(this)
                        .addApi(Wearable.API)
                        .addConnectionCallbacks(this)
                        .addOnConnectionFailedListener(this)
                        .build();
            }

            if(!googleApiClient.isConnected()){
                    googleApiClient.connect();
            }
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i(TAG, "Googleclient connected");
        int weatherArtResourceId = Utility.getIconResourceForWeatherCondition(weatherId);
        Bitmap weatherArtImage = null;
        if ( !Utility.usingLocalGraphics(this) ) {
            String weatherArtResourceUrl = Utility.getArtUrlForWeatherCondition(
                    this, weatherId);
            try {
                weatherArtImage = Glide.with(this)
                        .load(weatherArtResourceUrl)
                        .asBitmap()
                        .error(weatherArtResourceId)
                        .into(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL).get();
            } catch (InterruptedException | ExecutionException e) {
                Log.e(TAG, "Error retrieving large icon from " + weatherArtResourceUrl, e);
            }
        }
        Asset asset;
        if (weatherArtImage != null) {
            asset = createAssetFromBitmap(weatherArtImage);
        }else{
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), weatherArtResourceId);
            asset = createAssetFromBitmap(bitmap);
        }
        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(PATH);
        putDataMapRequest.setUrgent();

        putDataMapRequest.getDataMap().putString(PATH_MAXTEMP, Utility.formatTemperature(this, maxTemp));
        putDataMapRequest.getDataMap().putString(PATH_MINTEMP, Utility.formatTemperature(this, minTemp));
        putDataMapRequest.getDataMap().putAsset(PATH_ICON, asset);
        putDataMapRequest.getDataMap().putString("Time", Long.toString(System.currentTimeMillis()));
        // Prepare the data map for the request
        PutDataRequest request = putDataMapRequest.asPutDataRequest();

        Wearable.DataApi.putDataItem(googleApiClient, request)
                .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                    @Override
                    public void onResult(@NonNull DataApi.DataItemResult dataItemResult) {
                        Log.i(TAG, "Wearable data sent out");
                    }
                });
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Googleclient connection suspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i(TAG, "Googleclient connection failed");
    }

    private static Asset createAssetFromBitmap(Bitmap bitmap) {
        final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
        return Asset.createFromBytes(byteStream.toByteArray());
    }
}

