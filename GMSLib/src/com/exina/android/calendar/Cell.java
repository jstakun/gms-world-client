/*
 * Copyright (C) 2011 Chris Gao <chris@exina.net>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.exina.android.calendar;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;

public class Cell {
    //private static final String TAG = "Cell";

    protected Rect mBound = null;
    protected int mDayOfMonth = 1;	// from 1 to 31
    protected Paint mPaint = new Paint(Paint.SUBPIXEL_TEXT_FLAG
            | Paint.ANTI_ALIAS_FLAG);
    private int dx, dy;
    private boolean selected;

    public Cell(int dayOfMon, Rect rect, float textSize, boolean bold, boolean selected) {
        mDayOfMonth = dayOfMon;
        mBound = rect;
        mPaint.setTextSize(textSize/*26f*/);
        mPaint.setColor(Color.BLACK);
        if (bold) {
            mPaint.setFakeBoldText(true);
        }

        dx = (int) mPaint.measureText(String.valueOf(mDayOfMonth)) / 2;
        dy = (int) (-mPaint.ascent() + mPaint.descent()) / 2;

        this.selected = selected;
    }

    public Cell(int dayOfMon, Rect rect, float textSize, boolean selected) {
        this(dayOfMon, rect, textSize, false, selected);
    }

    protected void draw(Canvas canvas) {
        canvas.drawText(String.valueOf(mDayOfMonth), mBound.centerX() - dx, mBound.centerY() + dy, mPaint);
        if (selected) {
            drawTriangle(canvas);
        }
    }

    public int getDayOfMonth() {
        return mDayOfMonth;
    }

    public boolean hitTest(int x, int y) {
        return mBound.contains(x, y);
    }

    public Rect getBound() {
        return mBound;
    }

    @Override
    public String toString() {
        return String.valueOf(mDayOfMonth) + "(" + mBound.toString() + ")";
    }

    private void drawTriangle(Canvas canvas) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStrokeWidth(2);
        paint.setColor(android.graphics.Color.RED);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        paint.setAntiAlias(true);
        Path path = new Path();
        path.setFillType(Path.FillType.EVEN_ODD);
        path.moveTo(mBound.right - 11, mBound.top + 5);
        path.lineTo(mBound.right - 1, mBound.top + 5);
        path.lineTo(mBound.right - 1, mBound.top + 15);
        path.lineTo(mBound.right - 11, mBound.top + 5);
        path.close();
        canvas.drawPath(path, paint);
    }
}
