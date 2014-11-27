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
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;
import com.sassoni.urbanraccoon.wearimagesearch.common.Constants;
import com.sassoni.urbanraccoon.wearimagesearch.common.Keys;
import com.sassoni.urbanraccoon.wearimagesearch.common.MGoogleImage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
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
    private String keyword;
    //private int searchStartIndex = 1;

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

            Log.i(TAG, "YES!");

            if (googleApiClient == null || !googleApiClient.isConnected()) {
                googleApiClient = new GoogleApiClient.Builder(this)
                        .addApi(Wearable.API)
                        .build();
            }
            ConnectionResult connectionResult = googleApiClient.blockingConnect(30, TimeUnit.SECONDS);

            if (!connectionResult.isSuccess()) {
                Log.i(TAG, "GoogleApiClient connect failed with error code " + connectionResult.getErrorCode());
                // anything else?
            } else {
                //(x)1->(y)1
                //2->11
                //3->21
                //y=1+(x-1)*10
                String[] splitPath = path.split("/");
                int searchStartIndex = Integer.parseInt(splitPath[2])/*1 + ( Integer.parseInt(splitPath[2]) - 1 ) * 10*/;

                Log.i(TAG, searchStartIndex + "");

                requestQueue = Volley.newRequestQueue(this);
                keyword = new String(messageEvent.getData());
                requestImagesFor(keyword, searchStartIndex);
            }
        } else if (path.contains(Constants.PATH_OPEN)) {
//            if (googleApiClient == null || !googleApiClient.isConnected()) {
//                googleApiClient = new GoogleApiClient.Builder(this)
//                        .addApi(Wearable.API)
//                        .build();
//            }
//            ConnectionResult connectionResult = googleApiClient.blockingConnect(30, TimeUnit.SECONDS);
//
//            if (!connectionResult.isSuccess()) {
//                Log.i(TAG, "GoogleApiClient connect failed with error code " + connectionResult.getErrorCode());
//                // anything else?
//            } else {
//            }

            Log.i(TAG, "Opening something");

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
        } else

        {
            Log.i(TAG, "NO!");
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

                        List<MGoogleImage> thumbnailList = new ArrayList<MGoogleImage>();

                        try {
                            JSONArray items = jsonObject.getJSONArray("items");

                            for (int i = 0; i < items.length(); i++) {
                                Log.i(TAG, "Items length: " + items.length());
                                JSONObject item = items.getJSONObject(i);
                                JSONObject image = item.getJSONObject("image");
                                MGoogleImage googleImage = new MGoogleImage(searchStartIndex + i - 1, image.getString("thumbnailLink"), image.getString("contextLink"));
                                Log.i(TAG, "######## link: " + image.getString("contextLink"));
//   MGoogleImage googleImage = new MGoogleImage(i, item.getString("link"), image.getString("contextLink"));
                                thumbnailList.add(googleImage);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        downloadImages(thumbnailList);
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

    private void downloadImages(List<MGoogleImage> imageList) {
        Log.i(TAG, "Starting image download");

        for (final MGoogleImage image : imageList) {
            String imageUrl = image.getThumbnailLink();
            Log.i(TAG, "URL: " + imageUrl);
            ImageRequest request = new ImageRequest(imageUrl,
                    new Response.Listener<Bitmap>() {
                        @Override
                        public void onResponse(Bitmap bitmap) {
                            Log.i("BYTES", bitmap.getByteCount() + "");
                            image.setThumbnail(bitmap);
                            sendImageToWatch(image);
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

    private void sendImageToWatch(MGoogleImage image) {
        Log.i(TAG, "Sending image");

        PutDataMapRequest dataMap = PutDataMapRequest.create(Constants.PATH_IMAGE);

        dataMap.getDataMap().putAsset(Constants.DMAP_KEY_IMAGE, toAsset(image.getThumbnail()));
        dataMap.getDataMap().putInt(Constants.DMAP_KEY_INDEX, image.getIndex());
        dataMap.getDataMap().putString(Constants.DMAP_KEY_SEARCH_TERM, keyword);
        dataMap.getDataMap().putString(Constants.DMAP_KEY_CONTEXT_URL, image.getContextLink());
        dataMap.getDataMap().putLong(Constants.DMAP_KEY_TIME, new Date().getTime());

        PutDataRequest request = dataMap.asPutDataRequest();
        Wearable.DataApi.putDataItem(googleApiClient, request)
                .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                    @Override
                    public void onResult(DataApi.DataItemResult dataItemResult) {
                        Log.i(TAG, "Sending image success: " + dataItemResult.getStatus().isSuccess());
                    }
                });
    }

    private static Asset toAsset(Bitmap bitmap) {
        ByteArrayOutputStream byteStream = null;
        try {
            byteStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
            return Asset.createFromBytes(byteStream.toByteArray());
        } finally {
            if (null != byteStream) {
                try {
                    byteStream.close();
                } catch (IOException e) {
                    // what?
                }
            }
        }
    }
}
