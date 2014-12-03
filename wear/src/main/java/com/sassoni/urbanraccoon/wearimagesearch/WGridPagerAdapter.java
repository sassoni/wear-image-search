package com.sassoni.urbanraccoon.wearimagesearch;

import android.app.Activity;
import android.content.Context;
import android.graphics.PorterDuff;
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

import com.sassoni.urbanraccoon.wearimagesearch.common.Constants;
import com.sassoni.urbanraccoon.wearimagesearch.common.WearImage;

import java.util.List;

public class WGridPagerAdapter extends GridPagerAdapter {

    private static final String TAG = "***** WEAR: " + WGridPagerAdapter.class.getSimpleName();

    public static final int BUTTON_LOAD_MORE = 11;
    // === Open on phone feature === //
//    public static final int BUTTON_OPEN_ON_PHONE = 12;

    private Context context;
    private List<WearImage> list;

    public interface ButtonClickedListener {
        public void onButtonClicked(int buttonId, int row);
    }

    ButtonClickedListener buttonClickedListener;

    public WGridPagerAdapter(Activity activity, List<WearImage> list) {
        this.context = activity;
        this.list = list;
        buttonClickedListener = (ButtonClickedListener) activity;
    }

    @Override
    public int getRowCount() {
        // Stop showing 'more' when images images reach Constants.MAX_IMAGES_TOTAL
        if (list.size() < Constants.MAX_IMAGES_TOTAL) {
            return list.size() + 1;
        } else {
            return list.size();
        }
    }

    @Override
    public int getColumnCount(int row) {
        // === Open on phone feature === //
        // No need for second column in the 'more' row
//        if (row == list.size()) {
//            return 1;
//        } else {
//            return 2;
//        }
        return 1;
    }

    @Override
    protected Object instantiateItem(ViewGroup viewGroup, final int row, int col) {
        Log.i(TAG, "instantiateItem in row:" + row);

        View view = LayoutInflater.from(context.getApplicationContext()).inflate(R.layout.grid_pager_image, viewGroup, false);

        RelativeLayout gridPagerView = (RelativeLayout) view.findViewById(R.id.grid_pager_view_layout);
//        LinearLayout openOnPhoneLayout = (LinearLayout) view.findViewById(R.id.open_on_phone_layout);
        LinearLayout loadMoreLayout = (LinearLayout) view.findViewById(R.id.load_more_layout);
        ImageView imageView = (ImageView) view.findViewById(R.id.image_view);
//        View overlayView = view.findViewById(R.id.overlay_view);
        ProgressBar progressBar = (ProgressBar) view.findViewById(R.id.progress_bar);
        progressBar.getIndeterminateDrawable().setColorFilter(R.color.material_red, PorterDuff.Mode.SRC_IN);

        if (row < list.size()) {
            loadMoreLayout.setVisibility(View.GONE);

            if (list.get(row) != null) {  // If there is an image downloaded
                imageView.setBackground(list.get(row).getDrawable());
                imageView.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);

                // === Open on phone feature === //
//                if (col == 0) {
//                    overlayView.setVisibility(View.GONE);
//                    openOnPhoneLayout.setVisibility(View.GONE);
//                } else {
//                    overlayView.setVisibility(View.VISIBLE);
//                    openOnPhoneLayout.setVisibility(View.VISIBLE);
//
//                    CircledImageView openOnPhoneBtn = (CircledImageView) view.findViewById(R.id.open_on_phone_circle);
//                    openOnPhoneBtn.setOnClickListener(new View.OnClickListener() {
//                        @Override
//                        public void onClick(View v) {
//                            if (buttonClickedListener != null) {
//                                buttonClickedListener.onButtonClicked(BUTTON_OPEN_ON_PHONE, row);
//                            }
//                        }
//                    });
//                }
            } else {
                imageView.setVisibility(View.GONE);
//                overlayView.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
//                openOnPhoneLayout.setVisibility(View.GONE);
            }

        } else {  // Final row
            imageView.setVisibility(View.GONE);
//            overlayView.setVisibility(View.GONE);
            progressBar.setVisibility(View.GONE);
//            openOnPhoneLayout.setVisibility(View.GONE);
            loadMoreLayout.setVisibility(View.VISIBLE);

            CircledImageView loadMoreBtn = (CircledImageView) view.findViewById(R.id.load_more_circle);
            loadMoreBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.i(TAG, "Pressed!");
                    if (buttonClickedListener != null) {
                        buttonClickedListener.onButtonClicked(BUTTON_LOAD_MORE, row);
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
