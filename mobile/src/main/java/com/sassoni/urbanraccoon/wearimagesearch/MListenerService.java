package com.sassoni.urbanraccoon.wearimagesearch;

import android.content.Intent;
import android.graphics.Bitmap;
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
import java.util.concurrent.TimeUnit;

public class MListenerService extends WearableListenerService {

    private static final String TAG = "----- PHONE: " + MListenerService.class.getSimpleName();

    private GoogleApiClient googleApiClient;
    private RequestQueue requestQueue;
    private static int position = 0;
    private String watchNode;

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

        watchNode = messageEvent.getSourceNodeId();
        String path = messageEvent.getPath();

        if (path.contains(Constants.PATH_SEARCH)) {
            Log.i(TAG, "Message is for search!");

            if (googleApiClient == null) {
                googleApiClient = new GoogleApiClient.Builder(this)
                        .addApi(Wearable.API)
                        .build();
            }

            if (!googleApiClient.isConnected()) {
                ConnectionResult connectionResult = googleApiClient.blockingConnect(10, TimeUnit.SECONDS);

                if (!connectionResult.isSuccess()) {
                    Log.i(TAG, "GoogleApiClient connect failed with error code " + connectionResult.getErrorCode());
                    return;
                }
            }

            String[] splitPath = path.split("/");
            int searchStartIndex = Integer.parseInt(splitPath[splitPath.length - 1]);
            Log.i(TAG, "Search start index: " + searchStartIndex);

            // If it's the first time we are asking for this keyword
            // initialize position on grid
            if (searchStartIndex == 1) {
                position = 0;
            }

            requestQueue = Volley.newRequestQueue(this);
            String keyword = new String(messageEvent.getData());
            requestImagesFor(keyword, searchStartIndex);

        }
        // === Open on phone feature === //
//        else if (path.contains(Constants.PATH_OPEN)) {
//            Log.i(TAG, "Message is to open link.");
//            String link = new String(messageEvent.getData());
//            openLinkOnPhone(link);
//        }
    }

    // === Open on phone ===
//    private void openLinkOnPhone(String link) {
//        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
//        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WISWakelock");
//        wakeLock.acquire();
//        Intent webIntent = new Intent(Intent.ACTION_VIEW);
//        if (!link.startsWith("http://") || !link.startsWith("https://")) {
//            link = "http://" + link;
//        }
//        webIntent.setData(Uri.parse(link));
//        webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        this.startActivity(webIntent);
//        wakeLock.release();
//    }

    private void requestImagesFor(String keyword, final int searchStartIndex) {
        String encodedKeyword = "";
        try {
            encodedKeyword = URLEncoder.encode(keyword, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            encodedKeyword = keyword;
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

                        try {
                            JSONArray items = jsonObject.getJSONArray("items");
                            Log.i(TAG, "Items length: " + items.length());

                            for (int i = 0; i < items.length(); i++) {
                                JSONObject item = items.getJSONObject(i);
                                JSONObject image = item.getJSONObject("image");

                                WearImage wearImage = new WearImage(searchStartIndex + i - 1, item.getString("link"), image.getString("contextLink"));
                                downloadAndSendImage(wearImage);
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                            sendErrorMessageToWatch();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        sendErrorMessageToWatch();
                    }
                });

        requestQueue.add(jsonObjectRequest);
    }

    private void downloadAndSendImage(final WearImage image) {
        Log.i(TAG, "Starting image download");

        String imageUrl = image.getLink();
        Log.i(TAG, "URL: " + imageUrl);

        ImageRequest request = new ImageRequest(imageUrl,
                new Response.Listener<Bitmap>() {
                    @Override
                    public void onResponse(Bitmap bitmap) {

                        // Scale the bitmap
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
                        image.setPosition(position);
                        Log.i(TAG, "position: " + position);
                        position++;

                        sendImageToWatch(image);
                    }
                }, 0, 0, null,
                new Response.ErrorListener() {
                    public void onErrorResponse(VolleyError error) {
                        // Nothing
                    }
                });
        requestQueue.add(request);
    }

    private void sendImageToWatch(WearImage image) {
        Log.i(TAG, "Sending image");

        byte[] wearImageBytes = ParcelableUtil.marshall(image);
        Log.i(TAG, "After marshalling: " + wearImageBytes.length);

        Wearable.MessageApi.sendMessage(googleApiClient, watchNode, Constants.PATH_IMAGE, wearImageBytes)
                .setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                                       @Override
                                       public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                                           if (!sendMessageResult.getStatus().isSuccess()) {
                                               Log.i(TAG, "Failed to send message:" + sendMessageResult.getStatus().getStatusCode());
                                           } else {
                                               Log.i(TAG, "Sent message successfully ");
                                           }
                                       }
                                   }
                );
    }

    private void sendErrorMessageToWatch() {
        Wearable.MessageApi.sendMessage(googleApiClient, watchNode, Constants.PATH_ERROR, null)
                .setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                                       @Override
                                       public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                                           if (!sendMessageResult.getStatus().isSuccess()) {
                                               Log.i(TAG, "Failed to send message:" + sendMessageResult.getStatus().getStatusCode());
                                           } else {
                                               Log.i(TAG, "Sent message successfully ");
                                           }
                                       }
                                   }
                );
    }

}
