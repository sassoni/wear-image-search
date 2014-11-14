package com.sassoni.urbanraccoon.wearimagesearch;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
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
public class MListenerService extends WearableListenerService {

    private static final String TAG = "----- PHONE: " + MListenerService.class.getSimpleName();
    GoogleApiClient mGoogleApiClient;
    RequestQueue queue;

    List<MGoogleImage> imageList;

    @Override
    public void onCreate() {
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


//        GoogleApiClient mGoogleApiClient = new GoogleApiClient.Builder(this)
//                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
//                    @Override
//                    public void onConnected(Bundle connectionHint) {
//                        Log.d(TAG, "onConnected: " + connectionHint);
//                        // Now you can use the data layer API
//                    }
//
//                    @Override
//                    public void onConnectionSuspended(int cause) {
//                        Log.d(TAG, "onConnectionSuspended: " + cause);
//                    }
//                })
//                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
//                    @Override
//                    public void onConnectionFailed(ConnectionResult result) {
//                        Log.d(TAG, "onConnectionFailed: " + result);
//                    }
//                })
//                .addApi(Wearable.API)
//                .build();


        if (mGoogleApiClient == null || !mGoogleApiClient.isConnected()) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Wearable.API)
                    .build();
        }
        ConnectionResult connectionResult = mGoogleApiClient.blockingConnect(30, TimeUnit.SECONDS);

        if (!connectionResult.isSuccess()) {
            Log.e(TAG, String.format("GoogleApiClient connect failed with error code %d", connectionResult.getErrorCode()));
        }
        // else return


        imageList = new ArrayList<MGoogleImage>();

        Log.i(TAG, "Message received: " + new String(messageEvent.getData()));

        // Do the request here
        queue = Volley.newRequestQueue(this);

        String encodedPhrase = "";
        try {
            encodedPhrase = URLEncoder.encode(new String(messageEvent.getData()), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String url = "https://www.googleapis.com/customsearch/v1?" + encodedPhrase;

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject jsonObject) {
                        Log.i(TAG, "Got json response back");
                        try {
                            JSONArray items = jsonObject.getJSONArray("items");

                            // TODO This should be the num of the request
                            for (int i = 0; i < 10; i++) {
                                JSONObject item = items.getJSONObject(i);
                                JSONObject image = item.getJSONObject("image");
                                MGoogleImage googleImage = new MGoogleImage(i, image.getString("thumbnailLink"), image.getString("contextLink"));
                                imageList.add(googleImage);
                                Log.i("TAG", image.toString());
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        // TODO We can do the request directly in the previous loop
                        downloadImages();

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {

                    }
                });

        queue.add(jsonObjectRequest);


/*        // Send the data
        if (mGoogleApiClient == null || !mGoogleApiClient.isConnected()) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Wearable.API)
                    .build();

            ConnectionResult connectionResult = mGoogleApiClient.blockingConnect(30, TimeUnit.SECONDS);

            if (!connectionResult.isSuccess()) {
                Log.e(TAG, String.format("GoogleApiClient connect failed with error code %d", connectionResult.getErrorCode()));
            } else {
                Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.debug_background_4);

                PutDataMapRequest dataMap = PutDataMapRequest.create("/image");

                dataMap.getDataMap().putAsset("image", toAsset(icon));
                dataMap.getDataMap().putInt("index", 4);
                dataMap.getDataMap().putLong("time", new Date().getTime());

                PutDataRequest request = dataMap.asPutDataRequest();
                Wearable.DataApi.putDataItem(mGoogleApiClient, request)
                        .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                            @Override
                            public void onResult(DataApi.DataItemResult dataItemResult) {
                                Log.i(TAG, "Sending image was successful: " + dataItemResult.getStatus()
                                        .isSuccess());
                            }
                        });
            }
        }*/


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
                    // ignore
                }
            }
        }
    }

    private void downloadImages() {
        Log.i(TAG, "Starting image download");
        for (final MGoogleImage image : imageList) {
            String imageUrl = image.getThumbnailLink();
            Log.i(TAG, "URL: " + imageUrl);
            ImageRequest request = new ImageRequest(imageUrl,
                    new Response.Listener<Bitmap>() {
                        @Override
                        public void onResponse(Bitmap bitmap) {
                            //mImageView.setImageBitmap(bitmap);
                            image.setThumbnail(bitmap);
                            sendImageToWatch(image);
                        }
                    }, 0, 0, null,
                    new Response.ErrorListener() {
                        public void onErrorResponse(VolleyError error) {
//                            mImageView.setImageResource(R.drawable.image_load_error);
                        }
                    });
            queue.add(request);
        }
    }

    private void sendImageToWatch(MGoogleImage image) {
        Log.i(TAG, "Sending image");
        // Send the data
//        if (mGoogleApiClient == null || !mGoogleApiClient.isConnected()) {
//            mGoogleApiClient = new GoogleApiClient.Builder(this)
//                    .addApi(Wearable.API)
//                    .build();
//
//            ConnectionResult connectionResult = mGoogleApiClient.blockingConnect(30, TimeUnit.SECONDS);
//
//            if (!connectionResult.isSuccess()) {
//                Log.e(TAG, String.format("GoogleApiClient connect failed with error code %d", connectionResult.getErrorCode()));
//            } //else {
        // Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.debug_background_4);

        PutDataMapRequest dataMap = PutDataMapRequest.create("/image");

        dataMap.getDataMap().putAsset("image", toAsset(image.getThumbnail()));
        dataMap.getDataMap().putInt("index", image.getIndex());
        dataMap.getDataMap().putLong("time", new Date().getTime());

        PutDataRequest request = dataMap.asPutDataRequest();
        Wearable.DataApi.putDataItem(mGoogleApiClient, request)
                .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                    @Override
                    public void onResult(DataApi.DataItemResult dataItemResult) {
                        Log.i(TAG, "Sending image was successful: " + dataItemResult.getStatus()
                                .isSuccess());
                    }
                });
        //}
        // }
    }
}
