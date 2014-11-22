package com.sassoni.urbanraccoon.wearimagesearch;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.wearable.view.CircledImageView;
import android.support.wearable.view.GridPagerAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import java.util.ArrayList;
import java.util.List;

public class WImagesGridPagerAdapter extends GridPagerAdapter {

    private static final String TAG = "***** WEAR: " + WImagesGridPagerAdapter.class.getSimpleName();

    private Context context;
    private static List<Drawable> imagesList;

    public WImagesGridPagerAdapter(Context context) {
        this.context = context;
        imagesList = new ArrayList<Drawable>();
        for (int i = 0; i < 10; i++) {
            imagesList.add(null);
        }
    }

    @Override
    public int getRowCount() {
        return 11;
    }

    @Override
    public int getColumnCount(int i) {
        return 2;
    }

    @Override
    protected Object instantiateItem(ViewGroup viewGroup, int row, int col) {
        Log.i(TAG, "instantiateItem");

        View view;

        if (row != 10) {
            view = LayoutInflater.from(context.getApplicationContext()).inflate(R.layout.grid_pager_image, null);
//        final View view = LayoutInflater.from(context.getApplicationContext()).inflate(R.layout.grid_pager_image, viewGroup, false);
            ImageView imageView = (ImageView) view.findViewById(R.id.imageView);
            CircledImageView circledImageView = (CircledImageView) view.findViewById(R.id.pager_visit_page_btn);
            ProgressBar progressBar = (ProgressBar) view.findViewById(R.id.pager_progress_bar);
            if (imagesList.get(row) != null) {
                imageView.setVisibility(View.VISIBLE);
                imageView.setBackground(imagesList.get(row));
                progressBar.setVisibility(View.GONE);
            } else {
                imageView.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
            }
            if (col == 0) {
                imageView.setAlpha(1.0f);
                circledImageView.setVisibility(View.GONE);
            } else {
                imageView.setAlpha(0.4f);
                circledImageView.setVisibility(View.VISIBLE);
            }
        } else {
            view = LayoutInflater.from(context.getApplicationContext()).inflate(R.layout.grid_pager_load_more, null);
            CircledImageView loadMoreBtn = (CircledImageView) view.findViewById(R.id.pager_load_more_btn);
            loadMoreBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.i(TAG, "Pressed!");
                }
            });
        }

        viewGroup.addView(view);
        return view;
    }

    @Override
    protected void destroyItem(ViewGroup viewGroup, int i, int i2, Object o) {
        viewGroup.removeView((View) o);
    }

    @Override
    public boolean isViewFromObject(View view, Object o) {
        return view == o;
    }

    public void updateImageWithIndex(int index, Drawable drawable) {
        WImagesGridPagerAdapter.imagesList.set(index, drawable);
        notifyDataSetChanged();
    }

}
