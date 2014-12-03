package com.sassoni.urbanraccoon.wearimagesearch;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.support.wearable.view.CircledImageView;
import android.support.wearable.view.GridViewPager;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.sassoni.urbanraccoon.wearimagesearch.common.Constants;
import com.sassoni.urbanraccoon.wearimagesearch.common.GZipUtils;
import com.sassoni.urbanraccoon.wearimagesearch.common.WearImage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public class WMainActivity extends Activity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        WGridPagerAdapter.ButtonClickedListener,
        MessageApi.MessageListener {

    private static final String TAG = "***** WEAR: " + WMainActivity.class.getSimpleName();

    private static final int SPEECH_REQUEST_CODE = 42;

    private int requestIndex = 1;

    private GoogleApiClient googleApiClient;
    private WGridPagerAdapter adapter;
    private List<WearImage> imagesList;
    private String searchTerm;

    private LinearLayout tapToSearchLayout;
    private GridViewPager gridViewPager;
    private TextView errorMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tapToSearchLayout = (LinearLayout) findViewById(R.id.tap_to_search_layout);
        gridViewPager = (GridViewPager) findViewById(R.id.main_grid_view_pager);
        errorMessage = (TextView) findViewById(R.id.error_message);

        CircledImageView tapToSearchBtn = (CircledImageView) findViewById(R.id.tap_to_search_circle);
        tapToSearchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSpeechRecognizer();
            }
        });

        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        imagesList = new ArrayList<WearImage>();
        growListBy(Constants.MAX_IMAGES_PER_REQUEST);

        adapter = new WGridPagerAdapter(this, imagesList);
        gridViewPager.setAdapter(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        googleApiClient.connect();
    }

    @Override
    protected void onStop() {
        if (googleApiClient != null && googleApiClient.isConnected()) {
            Wearable.MessageApi.removeListener(googleApiClient, this);
            googleApiClient.disconnect();
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
        Wearable.MessageApi.addListener(googleApiClient, this);
    }

    @Override
    public void onConnectionSuspended(int cause) {
        showErrorMessage();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        showErrorMessage();
    }

    // ----- Speech Request ----- //

    private void startSpeechRecognizer() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
//        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
//        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Testing prompt");  // not working?
        startActivityForResult(intent, SPEECH_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
            switchToGridView();

            List<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            searchTerm = results.get(0);
            Log.i(TAG, searchTerm);

            new GetNodesAndSendRequest().execute();
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    // ----- Sending data ----- //

    private class GetNodesAndSendRequest extends AsyncTask<Void, Void, Void> {
        Collection<String> nodes;

        @Override
        protected Void doInBackground(Void... args) {
            Log.i(TAG, "Retrieving nodes...");
            nodes = getNodes();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (nodes.isEmpty()) {
                showErrorMessage();
            } else {
                for (String node : nodes) {
                    Log.i(TAG, "Sending message to: " + node);
                    sendRequestForSearchTerm(node);
                }
            }
        }
    }

    private void sendRequestForSearchTerm(String node) {
        Log.i(TAG, "Sending message");

        String specificPath = Constants.PATH_SEARCH + "/" + requestIndex;
        Wearable.MessageApi.sendMessage(googleApiClient, node, specificPath, searchTerm.getBytes())
                .setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
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
        NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(googleApiClient).await();

        if (nodes != null) {
            for (Node node : nodes.getNodes()) {
                results.add(node.getId());
            }
        }

        return results;
    }

    // ----- Receiving data ----- //

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.i(TAG, "Received message from phone!");

        if (messageEvent.getPath().contains(Constants.PATH_IMAGE)) {
            WearImage wearImage = new WearImage(messageEvent.getData());
            new DecodeAndShowImageTask().execute(wearImage);
        } else if (messageEvent.getPath().contains(Constants.PATH_ERROR)) {
            showErrorMessage();
        }
    }

    // ----- Buttons in gridpager actions ----- //

    @Override
    public void onButtonClicked(int buttonId, int row) {
        if (buttonId == WGridPagerAdapter.BUTTON_LOAD_MORE) {
            loadMore();
        }

        // === Open on phone feature === //
//        else if (buttonId == WGridPagerAdapter.BUTTON_OPEN_ON_PHONE) {
//            openOnPhone(row);
//        }
    }

    private void loadMore() {
        requestIndex += Constants.MAX_IMAGES_PER_REQUEST;

        growListBy(Constants.MAX_IMAGES_PER_REQUEST);

        // See http://stackoverflow.com/questions/24742427/dynamically-adding-items-to-fragmentgridpageradapter
        adapter = null;
        adapter = new WGridPagerAdapter(this, imagesList);
        gridViewPager.setAdapter(adapter);

        // See https://code.google.com/p/android/issues/detail?id=75309
        Runnable setCurrentItemDelayed = new Runnable() {
            @Override
            public void run() {
                gridViewPager.setCurrentItem(requestIndex - 1, 0, false);
            }
        };
        Handler handler = new Handler();
        handler.postDelayed(setCurrentItemDelayed, 100);

        new GetNodesAndSendRequest().execute();
    }

    private void growListBy(int howMany) {
        for (int i = 0; i < howMany; i++) {
            imagesList.add(null);
        }
    }

    // === Open on phone feature === //
//    private void openOnPhone(int rowClicked) {
//        WearImage image = imagesList.get(rowClicked);
//        String link = image.getContextLink();
//        new SendOpenOnPhoneMessageTask().execute(link);
//    }

    // === Open on phone feature === //
//    private class SendOpenOnPhoneMessageTask extends AsyncTask<String, Void, Void> {
//        @Override
//        protected Void doInBackground(String... args) {
//            String link = args[0];
//            Log.i(TAG, "Link:---------" + link);
//            Log.i(TAG, "Start sending message...");
//            Collection<String> nodes = getNodes();
//            if (nodes.isEmpty()) {
//                showErrorMessage();
//            } else {
//                for (String node : nodes) {
//                    Log.i(TAG, "... to node: " + node);
//                    sendOpenOnPhoneMessage(node, link);
//                }
//            }
//            return null;
//        }
//    }

    // === Open on phone feature === //
//    private void sendOpenOnPhoneMessage(String node, String link) {
//        Log.i(TAG, "Sending message");
//
//        Wearable.MessageApi.sendMessage(googleApiClient, node, Constants.PATH_OPEN, link.getBytes())
//                .setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
//                                       @Override
//                                       public void onResult(MessageApi.SendMessageResult sendMessageResult) {
//                                           if (!sendMessageResult.getStatus().isSuccess()) {
//                                               Log.i(TAG, "Failed to send message with status code: "
//                                                       + sendMessageResult.getStatus().getStatusCode());
//                                           } else {
//                                               Log.i(TAG, "Sent message successfully ");
//                                           }
//                                       }
//                                   }
//                );
//    }

    // ----- UI changes ----- //

    private void showErrorMessage() {
        tapToSearchLayout.setVisibility(View.GONE);
        gridViewPager.setVisibility(View.GONE);
        errorMessage.setVisibility(View.VISIBLE);
    }

    private void switchToGridView() {
        tapToSearchLayout.setVisibility(View.GONE);
        gridViewPager.setVisibility(View.VISIBLE);
    }

    private class DecodeAndShowImageTask extends AsyncTask<WearImage, Void, Bitmap> {
        WearImage image;

        protected Bitmap doInBackground(WearImage... images) {
            image = images[0];

            byte[] compressedImageBytes = image.getImageData();
            byte[] decompressedImageBytes;

            try {
                decompressedImageBytes = GZipUtils.decompress(image.getImageData());
                Log.i(TAG, "Size decompressed " + decompressedImageBytes.length);
            } catch (IOException e) {
                Log.i(TAG, "Decompressed null");
                decompressedImageBytes = compressedImageBytes;
                e.printStackTrace();
            }

            Bitmap bitmap = BitmapFactory.decodeByteArray(decompressedImageBytes, 0, decompressedImageBytes.length);

            Log.i(TAG, "Size bitmap " + bitmap.getByteCount());

            return bitmap;
        }

        protected void onPostExecute(Bitmap bitmap) {
            Drawable drawable = new BitmapDrawable(getResources(), bitmap);
            image.setDrawable(drawable);
            // We use image.getPosition() instead of image index so that
            // the images are shown based on the order they get downloaded
            imagesList.set(image.getPosition(), image);
            adapter.notifyDataSetChanged();
        }
    }

}