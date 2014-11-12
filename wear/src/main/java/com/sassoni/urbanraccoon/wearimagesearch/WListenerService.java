package com.sassoni.urbanraccoon.wearimagesearch;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.WearableListenerService;

public class WListenerService extends WearableListenerService {

    private static final String TAG = "***** WEAR: " + WListenerService.class.getSimpleName();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.i(TAG, "Data Changed!");
        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED && event.getDataItem().getUri().getPath().equals("/image")) {
                DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                Asset profileAsset = dataMapItem.getDataMap().getAsset("image");
                int imageIndex = dataMapItem.getDataMap().getInt("index");

                Intent broadcastIntent = new Intent(WConstants.ACTION_DATA_CHANGE);
                MyImage image = new MyImage(imageIndex, "url", profileAsset);
                broadcastIntent.putExtra(WConstants.KEY_MY_IMAGE, image);

                LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
            }
        }
    }

}
