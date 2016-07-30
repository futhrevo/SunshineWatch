package com.example.android.sunshine.app.wear;

import android.content.Intent;
import android.util.Log;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

public class WearListenerService extends WearableListenerService {
    private static final String TAG = WearListenerService.class.getSimpleName();

    private static final String PATH_REQ = "/wear-weather-req";
    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.i(TAG, "New Message received at phone");
        String path = messageEvent.getPath();
        if(path.equals(PATH_REQ)){
            startService(new Intent(getApplicationContext(), WearSendUpdates.class));
        }
    }
}
