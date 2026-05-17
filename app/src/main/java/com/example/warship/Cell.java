package com.example.warship;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

public class Cell {

    public static final int Oval = 0;      // ספינה
    public static final int EmptyVal = 1;  // ריק
    public static final int Hit = 2;       // פגיעה
    public static final int Miss = 3;      // החטאה

    private int x, y;
    private int cellWidth;
    private Bitmap bitmapO;
    private int val;
    private Paint p = new Paint();

    public Cell(int x, int y, Bitmap bitmapO, int cellWidth) {
        this.x = x;
        this.y = y;
        this.bitmapO = bitmapO;
        this.cellWidth = cellWidth;
        this.val = EmptyVal;
    }

    public void draw(Canvas canvas) {

        // מסגרת התא
        p.setStrokeWidth(6);
        p.setStyle(Paint.Style.STROKE);
        p.setColor(Color.BLACK);
        canvas.drawRect(x, y, x + cellWidth, y + cellWidth, p);

        // ספינה רגילה
        if (val == Oval) {
            canvas.drawBitmap(bitmapO, x + 5, y + 5, null);
        }

        // 🔴 פגיעה
        else if (val == Hit) {
            p.setStyle(Paint.Style.FILL);
            p.setColor(Color.RED);
            canvas.drawCircle(x + cellWidth / 2f, y + cellWidth / 2f, cellWidth / 4f, p);
        }

        // ⚪ החטאה
        else if (val == Miss) {
            p.setStyle(Paint.Style.FILL);
            p.setColor(Color.LTGRAY);
            canvas.drawCircle(x + cellWidth / 2f, y + cellWidth / 2f, cellWidth / 6f, p);
        }
    }

    public boolean isEmpty() {
        return val == EmptyVal;
    }

    public void forceSetVal(int v) {
        val = v;
    }

    public void clear() {
        val = EmptyVal;
    }
}