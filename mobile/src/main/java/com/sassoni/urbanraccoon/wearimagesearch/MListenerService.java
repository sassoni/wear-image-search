package com.sassoni.urbanraccoon.wearimagesearch;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.PowerManager;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;
import com.sassoni.urbanraccoon.wearimagesearch.common.Constants;
import com.sassoni.urbanraccoon.wearimagesearch.common.GZipUtils;
import com.sassoni.urbanraccoon.wearimagesearch.common.Keys;
import com.sassoni.urbanraccoon.wearimagesearch.common.ParcelableUtil;
import com.sassoni.urbanraccoon.wearimagesearch.common.WearImage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

// TODO Add check for the wearable api
// Check here https://developer.android.com/google/auth/api-client.html#WearableApi
// TODO Check how to handle all error cases
// TODO Add path checking onMessageReceived.
public class MListenerService extends WearableListenerService {

    private static final String TAG = "----- PHONE: " + MListenerService.class.getSimpleName();

    private GoogleApiClient googleApiClient;
    private RequestQueue requestQueue;
    private static int position = 0;
    private HashSet<String> nodes;

    @Override
    public void onCreate() {
        Log.i(TAG, "OnCreate");
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        super.onMessageReceived(messageEvent);
        Log.i(TAG, "Message received: " + new String(messageEvent.getData()));

        String path = messageEvent.getPath();

        if (path.contains(Constants.PATH_SEARCH)) {
            Log.i(TAG, "Message is for search!");

            if (googleApiClient == null) {
                googleApiClient = new GoogleApiClient.Builder(this)
                        .addApi(Wearable.API)
                        .build();
            }

            if (!googleApiClient.isConnected()){
                ConnectionResult connectionResult = googleApiClient.blockingConnect(10, TimeUnit.SECONDS);

                if (!connectionResult.isSuccess()) {
                    Log.i(TAG, "GoogleApiClient connect failed with error code " + connectionResult.getErrorCode());
                    // Send message to watch
                    return;
                }
            }


                nodes = new HashSet<String>();
                NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(googleApiClient).await();
                if (nodes != null) {
                    for (Node node : nodes.getNodes()) {
                        this.nodes.add(node.getId());
                    }
                }

                String[] splitPath = path.split("/");
                int searchStartIndex = Integer.parseInt(splitPath[2]);
                Log.i(TAG, searchStartIndex + "");

                // If it's the first time we are asking for this keyword
                // initialize position on grid
                if (searchStartIndex == 1) {
                    position = 0;
                }

                requestQueue = Volley.newRequestQueue(this);
                String keyword = new String(messageEvent.getData());
                requestImagesFor(keyword, searchStartIndex);

        } else if (path.contains(Constants.PATH_OPEN)) {
            Log.i(TAG, "Message is to open link.");

            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
            WifiManager.WifiLock wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "wifitag");
            PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                    "MyWakelockTag");
            wakeLock.acquire();
            wifiLock.acquire();
            String link = new String(messageEvent.getData());
            Log.i(TAG, "This link: " + link);
            Intent webIntent = new Intent(Intent.ACTION_VIEW);
            webIntent.setData(Uri.parse(link));
            webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            this.startActivity(webIntent);
            wakeLock.release();
            wifiLock.release();
        }
    }

    private void requestImagesFor(String keyword, final int searchStartIndex) {

        String encodedKeyword = "";
        try {
            encodedKeyword = URLEncoder.encode(keyword, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        String url = "https://www.googleapis.com/customsearch/v1?searchType=image&key=" + Keys.API_KEY
                + "&cx=" + Keys.CSE_CREATOR + ":" + Keys.CSE_ID
                + "&q=" + encodedKeyword
                + "&num=" + Constants.MAX_IMAGES_PER_REQUEST
                + "&start=" + searchStartIndex;

        Log.i(TAG, "url: " + url);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject jsonObject) {
                        Log.i(TAG, "Got json response back");

                        List<WearImage> wearImageList = new ArrayList<WearImage>();

                        try {
                            JSONArray items = jsonObject.getJSONArray("items");
                            Log.i(TAG, "Items length: " + items.length());

                            for (int i = 0; i < items.length(); i++) {
                                JSONObject item = items.getJSONObject(i);
                                JSONObject image = item.getJSONObject("image");

                                WearImage wearImage = new WearImage(searchStartIndex + i - 1, item.getString("link"), image.getString("contextLink"));
                                wearImageList.add(wearImage);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        downloadImages(wearImageList);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        // what?
                    }
                });

        requestQueue.add(jsonObjectRequest);
    }

    private void downloadImages(List<WearImage> imageList) {
        Log.i(TAG, "Starting image download");

//        final Collection<String> nodes = getNodes();
        // TODO the rest should happen if nodes are not empty

        if (nodes.isEmpty()) {
            Log.i(TAG, "Nodes are empty");
        } else {
            Log.i(TAG, "Nodes are not empty");
        }

        Log.i(TAG, "imagelist size: " + imageList.size());

        for (final WearImage image : imageList) {

            String imageUrl = image.getLink();  // image.getLink()
            Log.i(TAG, "URL: " + imageUrl);

            ImageRequest request = new ImageRequest(imageUrl,
                    new Response.Listener<Bitmap>() {
                        @Override
                        public void onResponse(Bitmap bitmap) {

                            // Scale the bitmap
                            // TODO Check scaling factors
                            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, 250, 250, false);

                            // Create byte array
                            ByteArrayOutputStream stream = new ByteArrayOutputStream();
                            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream);
                            byte[] byteArray = stream.toByteArray();

                            // Compress the byteArray
                            byte[] compressedByteArray = null;
                            try {
                                compressedByteArray = GZipUtils.compress(byteArray);

                            } catch (IOException e) {
                                compressedByteArray = byteArray;
                                e.printStackTrace();
                            }

                            Log.i("BYTES bitmap", bitmap.getByteCount() + "");
                            Log.i("BYTES scaledBitmap", scaledBitmap.getByteCount() + "");
                            Log.i("BYTES byteArray", byteArray.length + "");
                            Log.i("BYTES compressedByteArray", compressedByteArray.length + "");

                            image.setImageData(compressedByteArray);
                            Log.i(TAG, "position: " + position);
                            image.setPosition(position);
                            position++;
                            for (String node : nodes) {
                                sendImageToWatch(node, image);
                            }
                        }
                    }, 0, 0, null,
                    new Response.ErrorListener() {
                        public void onErrorResponse(VolleyError error) {
                            // what?
                        }
                    });
            requestQueue.add(request);
        }
    }

//    private class SendImageToWatchTask extends AsyncTask<Void, Void, Void> {
//        @Override
//        protected Void doInBackground(Void... args) {
//            Log.i(TAG, "Start sending message...");
//            Collection<String> nodes = getNodes();
//            if (nodes.isEmpty()) {
////                showErrorMessage();
//            } else {
//                for (String node : nodes) {
//                    Log.i(TAG, "... to node: " + node);
//                    sendImageToWatch(node);
//                }
//            }
//            return null;
//        }
//    }

    private void sendImageToWatch(String node, WearImage image) {
        Log.i(TAG, "Sending image");

        byte[] wearImageBytes = ParcelableUtil.marshall(image);
        Log.i(TAG, "After marshalling: " + wearImageBytes.length);

        Wearable.MessageApi.sendMessage(googleApiClient, node, Constants.PATH_IMAGE, wearImageBytes)
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
//
//        PutDataMapRequest dataMap = PutDataMapRequest.create(Constants.PATH_IMAGE);
//
//        dataMap.getDataMap().putByteArray(Constants.DMAP_KEY_IMAGE, image.getImageData());
//        dataMap.getDataMap().putInt(Constants.DMAP_KEY_INDEX, image.getIndex());
//        dataMap.getDataMap().putString(Constants.DMAP_KEY_SEARCH_TERM, keyword);
//        dataMap.getDataMap().putString(Constants.DMAP_KEY_CONTEXT_URL, image.getContextLink());
//        dataMap.getDataMap().putLong(Constants.DMAP_KEY_TIME, new Date().getTime());
//
//        PutDataRequest request = dataMap.asPutDataRequest();
//        Wearable.DataApi.putDataItem(googleApiClient, request)
//                .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
//                    @Override
//                    public void onResult(DataApi.DataItemResult dataItemResult) {
//                        Log.i(TAG, "Sending image success: " + dataItemResult.getStatus().isSuccess());
//                    }
//                });
    }

//    private Collection<String> getNodes() {
//        HashSet<String> nodes = new HashSet<String>();
//        NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(googleApiClient).await();
//
//        if (nodes != null) {
//            for (Node node : nodes.getNodes()) {
//                nodes.add(node.getId());
//            }
//        }
//
//        return nodes;
//    }

}
