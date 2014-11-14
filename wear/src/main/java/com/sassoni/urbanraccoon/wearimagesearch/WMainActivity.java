package com.sassoni.urbanraccoon.wearimagesearch;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.view.CircledImageView;
import android.support.wearable.view.GridViewPager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

// We don't really handle connection errors with the google api client here. Just because??
// TODO Should we show something in case of any error?
// TODO Test while disconnected
public class WMainActivity extends Activity {

    private static final String TAG = "***** WEAR: " + WMainActivity.class.getSimpleName();

    private GoogleApiClient mGoogleApiClient;
    private WImagesGridPagerAdapter adapter;
    private DataChangedBroadcastReceiver dataChangedBroadcastReceiver;
    private static final int SPEECH_REQUEST_CODE = 10;

    private CircledImageView tapToSearchBtn;
    private TextView tapToSearchLabel;
    private GridViewPager gridViewPager;

    private int positionInGrid = 0;

    // TODO Make a common module for constants
    public static String SEARCH_PATH = "/search";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
//        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
//            @Override
//            public void onLayoutInflated(WatchViewStub stub) {
//                mTextView = (TextView) stub.findViewById(R.id.text);
//            }
//        });
        dataChangedBroadcastReceiver = new DataChangedBroadcastReceiver();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle bundle) {
                    }

                    @Override
                    public void onConnectionSuspended(int i) {
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult connectionResult) {
                    }
                })
                .build();

        tapToSearchBtn = (CircledImageView) findViewById(R.id.main_tap_to_search_btn);
        tapToSearchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSpeechRecognizer();
            }
        });

        tapToSearchLabel = (TextView) findViewById(R.id.main_tap_to_search_label);

        gridViewPager = (GridViewPager) findViewById(R.id.main_grid_view_pager);
        adapter = new WImagesGridPagerAdapter(this);
        gridViewPager.setAdapter(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
        LocalBroadcastManager.getInstance(this).registerReceiver(dataChangedBroadcastReceiver, new IntentFilter(WConstants.ACTION_DATA_CHANGE));
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(dataChangedBroadcastReceiver);
        super.onStop();
    }

    private void sendMessage(String node, String message) {
        Log.i(TAG, "Sending message");
        Wearable.MessageApi.sendMessage(
                mGoogleApiClient, node, SEARCH_PATH, message.getBytes()).setResultCallback(
                new ResultCallback<MessageApi.SendMessageResult>() {
                    @Override
                    public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                        if (!sendMessageResult.getStatus().isSuccess()) {
                            Log.i(TAG, "Failed to send message with status code: "
                                    + sendMessageResult.getStatus().getStatusCode());
                        } else {
                            Log.i(TAG, "Sent message successfully ");
                        }
                    }
                }
        );
    }

    private void startSpeechRecognizer() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        // TODO Test which language model works better
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
//        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Testing prompt");  // not working?
        startActivityForResult(intent, SPEECH_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
            // TODO Is it better to convert this to a new fragment?
            switchToGridView();
            List<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            String spokenText = results.get(0);

            Log.i(TAG, spokenText);

            // Send the query to the phone
            new SendMessageTask().execute(spokenText);

//            Intent myIntent = new Intent(WMainActivity.this, WImagesGridActivity.class);
//            //myIntent.putExtra("key", value); //put the text as parameter
//            startActivity(myIntent);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private Collection<String> getNodes() {
        HashSet<String> results = new HashSet<String>();
        NodeApi.GetConnectedNodesResult nodes =
                Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();

        //  nodes might be null here?
        for (Node node : nodes.getNodes()) {
            results.add(node.getId());
        }

        return results;
    }

    private class SendMessageTask extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... args) {
            Log.i(TAG, "Start sending message...");
            Collection<String> nodes = getNodes();
            for (String node : nodes) {
                Log.i(TAG, "... to node: " + node);
                sendMessage(node, args[0]);
            }
            return null;
        }
    }

    private void switchToGridView() {
        tapToSearchBtn.setVisibility(View.GONE);
        tapToSearchLabel.setVisibility(View.GONE);
        gridViewPager.setVisibility(View.VISIBLE);
    }

    private class DataChangedBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "Data Change broadcast received!");
            MyImage image = (MyImage) intent.getParcelableExtra(WConstants.KEY_MY_IMAGE);
            new SendImageToAdapterTask().execute(image);
        }
    }

    private class SendImageToAdapterTask extends AsyncTask<MyImage, Void, Bitmap> {
        MyImage image;

        protected Bitmap doInBackground(MyImage... images) {
            image = images[0];
            Asset asset = image.getAsset();
            if (asset == null) {
                // TODO Probably just return null here
                throw new IllegalArgumentException("Asset must be non-null");
            }
            ConnectionResult result = mGoogleApiClient.blockingConnect(10000, TimeUnit.MILLISECONDS);
            if (!result.isSuccess()) {
                return null;
            }
            // convert asset into a file descriptor and block until it's ready
            InputStream assetInputStream = Wearable.DataApi.getFdForAsset(mGoogleApiClient, asset).await().getInputStream();
            mGoogleApiClient.disconnect();

            if (assetInputStream == null) {
                Log.i(TAG, "Requested an unknown Asset.");
                return null;
            }
            // decode the stream into a bitmap
            return BitmapFactory.decodeStream(assetInputStream);
        }

        protected void onPostExecute(Bitmap bitmap) {
            //TODO Check if bitmap is null
            Drawable drawable = new BitmapDrawable(getResources(), bitmap);
            adapter.updateImageWithIndex(/*image.getIndex()*/positionInGrid, drawable);
            positionInGrid++;
        }
    }

}

