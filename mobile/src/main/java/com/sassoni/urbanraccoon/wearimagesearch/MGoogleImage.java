package com.sassoni.urbanraccoon.wearimagesearch;


import android.graphics.Bitmap;

public class MGoogleImage {

    int index;
    int thumbnailWidth;
    int getThumbnailHeight;
    int height;
    String thumbnailLink;
    String contextLink;
    int byteSize;
    int width;
    Bitmap thumbnail;

    public MGoogleImage(int index, String thumbnailLink, String contextLink) {
        this.index = index;
        this.thumbnailLink = thumbnailLink;
        this.contextLink = contextLink;
    }

    int getIndex() {
        return index;
    }

    String getThumbnailLink() {
        return thumbnailLink;
    }

    String getContextLink() {
        return thumbnailLink;
    }

    Bitmap getThumbnail() {
        return thumbnail;
    }

    void setThumbnail(Bitmap thumbnail) {
        this.thumbnail = thumbnail;
    }
}
