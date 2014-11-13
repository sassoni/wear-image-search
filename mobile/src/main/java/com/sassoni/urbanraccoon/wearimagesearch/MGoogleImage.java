package com.sassoni.urbanraccoon.wearimagesearch;


public class MGoogleImage {

    int index;
    int thumbnailWidth;
    int getThumbnailHeight;
    int height;
    String thumbnailLink;
    String contextLink;
    int byteSize;
    int width;

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
}
