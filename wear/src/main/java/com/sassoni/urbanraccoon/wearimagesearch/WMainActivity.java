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
import android.os.Parcel;
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
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.sassoni.urbanraccoon.wearimagesearch.common.Constants;
import com.sassoni.urbanraccoon.wearimagesearch.common.ParcelableUtil;
import com.sassoni.urbanraccoon.wearimagesearch.common.WearImage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.zip.GZIPInputStream;

// TODO Test while disconnected
// TODO onDestroy delete data in storage?
// TODO Remove + from buildfile
// TODO stop requests on destroy
// TODO add timeout in request
// TODO Is it better to convert views to a new fragment?
// TODO Test which language model works better
// TODO Phone should send the search ter mback to check against it
public class WMainActivity extends Activity implements DataApi.DataListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        WImagesGridPagerAdapter.ButtonClickedListener,
        MessageApi.MessageListener {

    private static final String TAG = "***** WEAR: " + WMainActivity.class.getSimpleName();

    private static final int SPEECH_REQUEST_CODE = 42;

    private int positionInGrid = 0;
    private int requestIndex = 1;

    private GoogleApiClient mGoogleApiClient;
    private WImagesGridPagerAdapter adapter;
    private int imagesIndices[];
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

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        imagesIndices = new int[Constants.MAX_IMAGES_TOTAL];

        imagesList = new ArrayList<WearImage>();
        growListBy(Constants.MAX_IMAGES_PER_REQUEST);

//        adapter = new WImagesGridPagerAdapter(this, imagesList);
        adapter = new WImagesGridPagerAdapter(this, imagesList);
        gridViewPager.setAdapter(adapter);
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
            Wearable.MessageApi.removeListener(mGoogleApiClient, this);
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
        Wearable.MessageApi.addListener(mGoogleApiClient, this);
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
        switchToGridView();
        searchTerm = "george papandreou";
        Log.i(TAG, searchTerm);

        new SendRequestForSearchTermTask().execute();
        super.onActivityResult(requestCode, resultCode, data);
//        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
//            switchToGridView();
//
//            List<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
//            searchTerm = results.get(0);
//            Log.i(TAG, searchTerm);
//
//            new SendRequestForSearchTermTask().execute();
//        } else {
//            showErrorMessage();
//        }
//        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.i(TAG, "Received message from phone!");

        if (messageEvent.getPath().contains(Constants.PATH_IMAGE)) {
            Log.i(TAG, "YEs!");
            WearImage wearImage = ParcelableUtil.unmarshall(messageEvent.getData(), WearImage.CREATOR);
            new DecodeAndShowImageTask().execute(wearImage);
        } else {
            Log.i(TAG, "No!");
        }
    }

    // ----- Sending data ----- //

    private class SendRequestForSearchTermTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... args) {
            Log.i(TAG, "Start sending message...");
            Collection<String> nodes = getNodes();
            if (nodes.isEmpty()) {
                showErrorMessage();
            } else {
                for (String node : nodes) {
                    Log.i(TAG, "... to node: " + node);
                    sendRequestForSearchTerm(node);
                }
            }
            return null;
        }
    }

    private void sendRequestForSearchTerm(String node) {
        Log.i(TAG, "Sending message");

        String specificPath = Constants.PATH_SEARCH + "/" + requestIndex;
        Wearable.MessageApi.sendMessage(mGoogleApiClient, node, specificPath, searchTerm.getBytes())
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
        NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();

        if (nodes != null) {
            for (Node node : nodes.getNodes()) {
                results.add(node.getId());
            }
        }

        return results;
    }

    // ----- Receiving data ----- //

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.i(TAG, "Data Changed!");
//        for (DataEvent event : dataEvents) {
//            if (event.getType() == DataEvent.TYPE_CHANGED &&
//                    event.getDataItem().getUri().getPath().equals(Constants.PATH_IMAGE)) {
//                DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
//
//                String searchTermForData = dataMapItem.getDataMap().getString(Constants.DMAP_KEY_SEARCH_TERM);
//
//                if (searchTermForData.equals(searchTerm)) {  // Ignore any old requests
//                    Asset profileAsset = dataMapItem.getDataMap().getAsset(Constants.DMAP_KEY_IMAGE);
//                    int imageIndex = dataMapItem.getDataMap().getInt(Constants.DMAP_KEY_INDEX);
//                    String contextUrl = dataMapItem.getDataMap().getString(Constants.DMAP_KEY_CONTEXT_URL);
//
//                    byte[] compressedImage = dataMapItem.getDataMap().getByteArray("compressed");
//
//                    Log.i(TAG, "We got new image for index: " + imageIndex);
//                    Log.i(TAG, "wih context urlx: " + contextUrl);
//                    WearImage wearImage = new WearImage(imageIndex, null, null, contextUrl);
//                    wearImage.setAsset(profileAsset);
//                    wearImage.setImageData(compressedImage);
////                MyImage image = new MyImage(imageIndex, "url", profileAsset);
//                    new DecodeAndShowImageTask().execute(wearImage);
//                }
//            }
//        }
    }

    // ----- Buttons in gridpager actions ----- //

    @Override
    public void onButtonClicked(int buttonId, int row) {

        if (buttonId == WImagesGridPagerAdapter.BUTTON_LOAD_MORE) {
            loadMore();
        } else if (buttonId == WImagesGridPagerAdapter.BUTTON_OPEN_ON_PHONE) {
            openOnPhone(row);
        }
    }

    private void loadMore() {
        requestIndex += Constants.MAX_IMAGES_PER_REQUEST;

        growListBy(Constants.MAX_IMAGES_PER_REQUEST);

        // See http://stackoverflow.com/questions/24742427/dynamically-adding-items-to-fragmentgridpageradapter
        adapter = null;
//        adapter = new WImagesGridPagerAdapter(this, imagesList);
        adapter = new WImagesGridPagerAdapter(this, imagesList);
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

        new SendRequestForSearchTermTask().execute();
    }

    private void openOnPhone(int rowClicked) {
        int imageIndex = imagesIndices[rowClicked];
        WearImage image = imagesList.get(imageIndex);
        String link = image.getContextLink();

        new SendOpenOnPhoneMessageTask().execute(link);
    }

    private class SendOpenOnPhoneMessageTask extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... args) {
            String link = args[0];
            Log.i(TAG, "Link:---------" + link);
            Log.i(TAG, "Start sending message...");
            Collection<String> nodes = getNodes();
            if (nodes.isEmpty()) {
                showErrorMessage();
            } else {
                for (String node : nodes) {
                    Log.i(TAG, "... to node: " + node);
                    sendOpenOnPhoneMessage(node, link);
                }
            }
            return null;
        }
    }

    private void sendOpenOnPhoneMessage(String node, String link) {
        Log.i(TAG, "Sending message");

        Wearable.MessageApi.sendMessage(mGoogleApiClient, node, Constants.PATH_OPEN, link.getBytes())
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

    private void growListBy(int howMany) {
        for (int i = 0; i < howMany; i++) {
            imagesList.add(null);
        }
    }

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
            byte[] decompressedImageBytes = null;

            try {
                decompressedImageBytes = decompress(image.getImageData());
                Log.i(TAG, "Size decompressed " + decompressedImageBytes.length);
            } catch (IOException e) {
                Log.i(TAG, "Decompressed null");
                decompressedImageBytes = compressedImageBytes;
                e.printStackTrace();
            }

            Bitmap bitmap = BitmapFactory.decodeByteArray(decompressedImageBytes, 0, decompressedImageBytes.length);

            Log.i(TAG, "Size bitmap " + bitmap.getByteCount());

            return bitmap;

            //            Log.i(TAG, "Size compressed " + image.getImageData().length);


//            Asset asset = image.getAsset();
//            if (asset == null) {
//                return null;
//            }
//
//            InputStream assetInputStream = Wearable.DataApi.getFdForAsset(mGoogleApiClient, asset).await().getInputStream();
//            if (assetInputStream == null) {
//                return null;
//            }
//
//            return BitmapFactory.decodeStream(assetInputStream);

        }

        protected void onPostExecute(Bitmap bitmap) {


//            Drawable drawable2 = new BitmapDrawable(getResources(), bitmap2);

            Drawable drawable = new BitmapDrawable(getResources(), bitmap);
            // We use positionInGrid instead of image index so that
            // the images are shown based on the order they came
            image.setDrawable(drawable);
            imagesList.set(image.getPosition(), image);
            adapter.notifyDataSetChanged();

            imagesIndices[positionInGrid] = image.getIndex();
            positionInGrid++;
        }
    }

    public static byte[] decompress(byte[] compressed) throws IOException {
        final int BUFFER_SIZE = 32;
        ByteArrayInputStream is = new ByteArrayInputStream(compressed);
        GZIPInputStream gis = new GZIPInputStream(is, BUFFER_SIZE);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] data = new byte[BUFFER_SIZE];
        int bytesRead;
        while ((bytesRead = gis.read(data)) != -1) {
            out.write(data, 0, bytesRead);
        }
        gis.close();
        is.close();
        return out.toByteArray();
    }

}