package com.example.android.sunshine.app.wear;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.android.sunshine.app.sync.SunshineSyncAdapter;

public class WearReceiver extends BroadcastReceiver {
    public WearReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // New data arrived
        if(SunshineSyncAdapter.ACTION_DATA_UPDATED.equals(intent.getAction())){
            context.startService(new Intent(context, WearSendUpdates.class));
        }
    }
}
