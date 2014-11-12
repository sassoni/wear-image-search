package com.sassoni.urbanraccoon.wearimagesearch;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.view.GridViewPager;
import android.util.Log;

public class WImagesGridActivity extends Activity {

    private static final String TAG = "***** WEAR: " + WImagesGridActivity.class.getSimpleName();

    WImagesGridPagerAdapter adapter;
    private DataChangedBroadcastReceiver dataChangedBroadcastReceiver;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);
//        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
//        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
//            @Override
//            public void onLayoutInflated(WatchViewStub stub) {
//                mTextView = (TextView) stub.findViewById(R.id.text);
//            }
//        });
        dataChangedBroadcastReceiver = new DataChangedBroadcastReceiver();

        adapter = new WImagesGridPagerAdapter(WImagesGridActivity.this);

        final GridViewPager pager = (GridViewPager) findViewById(R.id.main_grid_view_pager);
        pager.setAdapter(adapter);
    }

    @Override
    protected void onStart() {
        LocalBroadcastManager.getInstance(this).registerReceiver(dataChangedBroadcastReceiver, new IntentFilter("data_changed"));
        super.onStart();
    }

    @Override
    protected void onStop() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(dataChangedBroadcastReceiver);
        super.onStop();
    }

    private class DataChangedBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("---------WEAR ", "Data Change broadcast!!");
            adapter.notifyDataSetChanged();
        }
    }
}
