package org.baobab.foodcoapp;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

public class StretchableGrid extends LinearLayout {
    public static final String namespace = "http://schemas.android.com/apk/res-auto";
    private int columnCount;
    private int rowCount;

    public StretchableGrid(Context context, int rows, int cols) {
        super(context);
        rowCount = rows;
        columnCount = cols;
        init();
    }

    public StretchableGrid(Context context, AttributeSet attrs) {
        super(context, attrs);
        rowCount = attrs.getAttributeIntValue(namespace, "rowCount", 2);
        columnCount = attrs.getAttributeIntValue(namespace, "columnCount", 2);
        init();
    }

    private void init() {
        setOrientation(VERTICAL);
        for(int i = 0; i < rowCount; i++) {
            LinearLayout row = new LinearLayout(getContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setLayoutParams(new LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    0, 1f));
            super.addView(row, i);
            for(int j = 0; j < columnCount; j++) {
                row.addView(new FrameLayout(getContext()),
                        j, new LayoutParams(
                           LayoutParams.MATCH_PARENT,
                           LayoutParams.MATCH_PARENT, 1f));
            }
        }
    }

    @Override
    public void addView(View view, int index) {
        index = index - 1;
        if (index < 0) {
            return;
        }
        int rowIdx = index / columnCount;
        int colIdx = index % columnCount;
        LinearLayout row = (LinearLayout) getChildAt(rowIdx);
        row.removeViewAt(colIdx);
        row.addView(view, colIdx, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT, 1f));
    }

}
