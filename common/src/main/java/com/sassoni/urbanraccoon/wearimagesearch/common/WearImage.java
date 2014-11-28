package com.sassoni.urbanraccoon.wearimagesearch.common;

import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;

public class WearImage implements Parcelable {

    private int index;  // The index that the image appears in search search results
    private int position;  // The position in the grid that the image will appear
    private String link;
    private String contextLink;
    private byte[] imageData;
    transient private Drawable drawable;

    public WearImage(int index, String link, String contextLink) {
        this.index = index;
        this.link = link;
        this.contextLink = contextLink;
    }

    public int getIndex() {
        return index;
    }

    public String getLink() {
        return link;
    }

    public String getContextLink() {
        return contextLink;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public int getPosition() {
        return position;
    }

    public void setImageData(byte[] imageData) {
        this.imageData = imageData;
    }

    public byte[] getImageData() {
        return imageData;
    }

    public void setDrawable(Drawable drawable) {
        this.drawable = drawable;
    }

    public Drawable getDrawable() {
        return drawable;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(index);
        out.writeInt(position);
        out.writeString(link);
        out.writeString(contextLink);
        out.writeByteArray(imageData);
    }

    public static final Parcelable.Creator<WearImage> CREATOR
            = new Parcelable.Creator<WearImage>() {
        public WearImage createFromParcel(Parcel in) {
            return new WearImage(in);
        }

        public WearImage[] newArray(int size) {
            return new WearImage[size];
        }
    };

    private WearImage(Parcel in) {
        index = in.readInt();
        position = in.readInt();
        link = in.readString();
        contextLink = in.readString();
        imageData = in.createByteArray();
    }
}
