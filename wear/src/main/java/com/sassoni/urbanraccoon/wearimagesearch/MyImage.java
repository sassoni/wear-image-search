package com.sassoni.urbanraccoon.wearimagesearch;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.wearable.Asset;

public class MyImage implements Parcelable {

    private int index;
    private String pageUrl;
    private Asset asset;

    public MyImage(int index, String pageUrl, Asset asset) {
        this.index = index;
        this.pageUrl = pageUrl;
        this.asset = asset;
    }

    public int getIndex() {
        return index;
    }

    public String getPageUrl() {
        return pageUrl;
    }

    public Asset getAsset() {
        return asset;
    }

    private MyImage(Parcel in) {
        index = in.readInt();
        pageUrl = in.readString();
        asset = in.readParcelable(Asset.class.getClassLoader());
    }

    public static final Parcelable.Creator<MyImage> CREATOR = new Parcelable.Creator<MyImage>() {
        public MyImage createFromParcel(Parcel in) {
            return new MyImage(in);
        }

        public MyImage[] newArray(int size) {
            return new MyImage[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(index);
        dest.writeString(pageUrl);
        dest.writeParcelable(asset, 0);
    }
}
