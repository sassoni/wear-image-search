package com.sassoni.urbanraccoon.wearimagesearch;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.wearable.view.CircledImageView;
import android.support.wearable.view.GridPagerAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import java.util.List;

public class WImagesGridPagerAdapter extends GridPagerAdapter {

    private static final String TAG = "***** WEAR: " + WImagesGridPagerAdapter.class.getSimpleName();

    private Context context;
    private List<Drawable> list;
    private int numOfCols = 2;

    public interface MoreButtonClickedListener {
        public void onMoreButtonClicked();
    }

    MoreButtonClickedListener moreButtonClickedListener;

    public WImagesGridPagerAdapter(Activity activity, List<Drawable> list) {
        this.context = activity;
        this.list = list;
        moreButtonClickedListener = (MoreButtonClickedListener) activity;
    }

    @Override
    public int getRowCount() {
        return list.size();
    }

    @Override
    public int getColumnCount(int row) {
        if (row != list.size() - 1) {
            return 2;
        } else {
            return 1;
        }
    }

    @Override
    protected Object instantiateItem(ViewGroup viewGroup, int row, int col) {
        Log.i(TAG, "instantiateItem in row:" + row);

        View view = LayoutInflater.from(context.getApplicationContext()).inflate(R.layout.grid_pager_image, viewGroup, false);

        RelativeLayout gridPagerView = (RelativeLayout) view.findViewById(R.id.grid_pager_view_layout);
        LinearLayout openOnPhoneLayout = (LinearLayout) view.findViewById(R.id.open_on_phone_layout);
        LinearLayout loadMoreLayout = (LinearLayout) view.findViewById(R.id.load_more_layout);
        ImageView imageView = (ImageView) view.findViewById(R.id.imageView);
        View overlayView = view.findViewById(R.id.overlay_view);
        ProgressBar progressBar = (ProgressBar) view.findViewById(R.id.progress_bar);

        gridPagerView.setBackgroundColor(context.getResources().getColor(R.color.material_light_gray));

        if (row != list.size() - 1) {
            loadMoreLayout.setVisibility(View.GONE);

            if (list.get(row) != null) {
                imageView.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
                imageView.setBackground(list.get(row));
            } else {
                imageView.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
            }

            if (col == 0) {
                imageView.setVisibility(View.VISIBLE);
                overlayView.setVisibility(View.GONE);
                openOnPhoneLayout.setVisibility(View.GONE);
            } else {
                imageView.setVisibility(View.GONE);
                overlayView.setVisibility(View.VISIBLE);
                openOnPhoneLayout.setVisibility(View.VISIBLE);
            }
        } else {  // Final row
            imageView.setVisibility(View.GONE);
            overlayView.setVisibility(View.GONE);
            progressBar.setVisibility(View.GONE);
            openOnPhoneLayout.setVisibility(View.GONE);
            loadMoreLayout.setVisibility(View.VISIBLE);
            gridPagerView.setBackgroundColor(context.getResources().getColor(R.color.material_gray));

            CircledImageView loadMoreBtn = (CircledImageView) view.findViewById(R.id.load_more_circle);
            loadMoreBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.i(TAG, "Pressed!");
                    if (moreButtonClickedListener != null) {
                        moreButtonClickedListener.onMoreButtonClicked();
                    }
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

}
