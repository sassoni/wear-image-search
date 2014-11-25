package com.sassoni.urbanraccoon.wearimagesearch;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.wearable.view.CircledImageView;
import android.support.wearable.view.GridViewPager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.sassoni.urbanraccoon.wearimagesearch.common.Constants;
import com.sassoni.urbanraccoon.wearimagesearch.common.MGoogleImage;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

// We don't really handle connection errors with the google api client here. Just because??
// TODO Should we show something in case of any error?
// TODO Test while disconnected
// TODO onDestroy delete data in storage?
// TODO Remove + from buildfile
public class WMainActivity extends Activity implements DataApi.DataListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        WImagesGridPagerAdapter.MoreButtonClickedListener {

    private static final String TAG = "***** WEAR: " + WMainActivity.class.getSimpleName();

    private GoogleApiClient mGoogleApiClient;
    private WImagesGridPagerAdapter adapter;
    private static final int SPEECH_REQUEST_CODE = 10;

    private CircledImageView tapToSearchBtn;
    private TextView tapToSearchLabel;
    private GridViewPager gridViewPager;

    private int positionInGrid = 0;
    private int requestIndex = 1;

    private String spokenText;

    private static List<Drawable> imagesList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        tapToSearchBtn = (CircledImageView) findViewById(R.id.main_tap_to_search_btn);
        tapToSearchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSpeechRecognizer();
            }
        });

        tapToSearchLabel = (TextView) findViewById(R.id.main_tap_to_search_label);

        imagesList = new ArrayList<Drawable>();
        addToList(10);

        gridViewPager = (GridViewPager) findViewById(R.id.main_grid_view_pager);
        adapter = new WImagesGridPagerAdapter(this, imagesList);
        gridViewPager.setAdapter(adapter);
    }

    private void addToList(int howMany){
        for (int i = 0; i < howMany; i++) {
            imagesList.add(null);
        }
    }

    private void updateImageWithIndex(int index, Drawable drawable) {
//        imagesList.set(index, drawable);
        imagesList.set(index, drawable);
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            Wearable.DataApi.removeListener(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    // ----- Google Api Client ----- //

    @Override
    public void onConnected(Bundle bundle) {
        Wearable.DataApi.addListener(mGoogleApiClient, this);

        // Delete all previous data
        Uri uri = new Uri.Builder().scheme(PutDataRequest.WEAR_URI_SCHEME).path(Constants.PATH_IMAGE).build();

        PendingResult<DataApi.DeleteDataItemsResult> deleteDataItemsResultPendingResult =
                Wearable.DataApi.deleteDataItems(mGoogleApiClient, uri);
        deleteDataItemsResultPendingResult.setResultCallback(new ResultCallback<DataApi.DeleteDataItemsResult>() {
            @Override
            public void onResult(DataApi.DeleteDataItemsResult deleteDataItemsResult) {

            }
        }, 2, TimeUnit.SECONDS);
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
    }

    // ----- Speech Request ----- //

    private void startSpeechRecognizer() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        // TODO Test which language model works better
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
//        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
//        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Testing prompt");  // not working?
        startActivityForResult(intent, SPEECH_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
            // TODO Is it better to convert this to a new fragment?
            switchToGridView();
            List<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            spokenText = results.get(0);

            Log.i(TAG, spokenText);

            // Send the query to the phone
            requestIndex = 1;
            new SendKeywordToPhoneTask().execute(spokenText);
        } // else show an error?
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onMoreButtonClicked() {
//        adapter.increaseSize();
        gridViewPager.getAdapter().notifyDataSetChanged();
        gridViewPager.setOffscreenPageCount(20);

//        gridViewPager.setAdapter(adapter);
        //adapter.notifyDataSetChanged();
        sendRequestForMore();
    }

    // ----- Keyword to Phone ----- //

    private class SendKeywordToPhoneTask extends AsyncTask<String, Void, Void> {
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

    private void sendRequestForMore() {
        Log.i(TAG, "Requesting more");
        requestIndex++;
        new SendKeywordToPhoneTask().execute(spokenText);
    }

    private void sendMessage(String node, String message) {
        Log.i(TAG, "Sending message");

        String specificPath = Constants.SEARCH_KEY_PATH + "/" + requestIndex;
        Wearable.MessageApi.sendMessage(
                mGoogleApiClient, node, specificPath, message.getBytes()).setResultCallback(
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

    // ----- Receiving data ----- //

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.i(TAG, "Data Changed!");
        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED && event.getDataItem().getUri().getPath().equals(Constants.PATH_IMAGE)) {
                Log.i(TAG, "!!!!!!!!!!!Deleted!!!!!!!!!!!");
            }

            if (event.getType() == DataEvent.TYPE_CHANGED && event.getDataItem().getUri().getPath().equals(Constants.PATH_IMAGE)) {
                DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                Asset profileAsset = dataMapItem.getDataMap().getAsset("image");
                int imageIndex = dataMapItem.getDataMap().getInt("index");

                Log.i(TAG, "We got new image for index: " + imageIndex);

                MGoogleImage mGoogleImage = new MGoogleImage(imageIndex, null, "url");
                mGoogleImage.setAsset(profileAsset);
//                MyImage image = new MyImage(imageIndex, "url", profileAsset);
                new SendImageToAdapterTask().execute(mGoogleImage);
            }
        }
    }

    private void switchToGridView() {
        tapToSearchBtn.setVisibility(View.GONE);
        tapToSearchLabel.setVisibility(View.GONE);
        gridViewPager.setVisibility(View.VISIBLE);
    }

    private class SendImageToAdapterTask extends AsyncTask<MGoogleImage, Void, Bitmap> {
        MGoogleImage image;

        protected Bitmap doInBackground(MGoogleImage... images) {
            image = images[0];
            Asset asset = image.getAsset();
            if (asset == null) {
                // TODO Probably just return null here
                throw new IllegalArgumentException("Asset must be non-null");
            }

            InputStream assetInputStream = Wearable.DataApi.getFdForAsset(mGoogleApiClient, asset).await().getInputStream();

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
            Log.i(TAG, "Position in grid: " + positionInGrid);
//            adapter.updateImageWithIndex(/*image.getIndex()*/positionInGrid, drawable);
            updateImageWithIndex(/*image.getIndex()*/positionInGrid, drawable);
            positionInGrid++;
        }
    }

}

