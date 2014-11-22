package com.sassoni.urbanraccoon.wearimagesearch.common;


import android.graphics.Bitmap;

import com.google.android.gms.wearable.Asset;

public class MGoogleImage {

    int index;
    int thumbnailWidth;
    int getThumbnailHeight;
    int height;
    private String thumbnailLink;
    private String contextLink;
    int byteSize;
    int width;
    private Bitmap thumbnail;
    private Asset asset;

    public MGoogleImage(int index, String thumbnailLink, String contextLink) {
        this.index = index;
        this.thumbnailLink = thumbnailLink;
        this.contextLink = contextLink;
    }

    public int getIndex() {
        return index;
    }

    public String getThumbnailLink() {
        return thumbnailLink;
    }

    public String getContextLink() {
        return thumbnailLink;
    }

    public Bitmap getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(Bitmap thumbnail) {
        this.thumbnail = thumbnail;
    }

    public void setAsset(Asset asset) {
        this.asset = asset;
    }

    public Asset getAsset() {
        return asset;
    }
}
