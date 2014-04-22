package org.baobab.pos;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

public class StretchableGrid extends LinearLayout {
    public static final String namespace = "http://schemas.android.com/apk/res-auto";
    private int rowCount;
    private int columnCount;
    private int elementsCount = 0;

    public StretchableGrid(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(VERTICAL);
        rowCount = attrs.getAttributeIntValue(namespace, "rowCount", 2);
        columnCount = attrs.getAttributeIntValue(namespace, "columnCount", 2);
        for(int i = 0; i < rowCount; i++) {
            LinearLayout row = new LinearLayout(getContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setLayoutParams(new LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    0, 1f));
            super.addView(row);
            for(int j = 0; j < columnCount; j++) {
                row.addView(new FrameLayout(getContext()),
                        j, new LayoutParams(
                           LayoutParams.MATCH_PARENT,
                           LayoutParams.MATCH_PARENT, 1f));
            }
        }
    }

    @Override
    public void addView(View element) {
        int rowIdx = elementsCount / columnCount;
        int colIdx = elementsCount % columnCount;
        LinearLayout row = (LinearLayout) getChildAt(rowIdx);
        row.removeViewAt(colIdx);
        row.addView(element, colIdx, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT, 1f));
        elementsCount++;
    }

}
