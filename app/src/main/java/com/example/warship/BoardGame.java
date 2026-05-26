package com.example.warship;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;

public class BoardGame extends View {
    public static final int BOARD_MY = 0;
    public static final int BOARD_OPP = 1;
    public Cell[][] arr;
    private int cellSize;
    private boolean created = false;
    private final Context context;
    private final int boardType;// אישי/יריב

    public BoardGame(Context context, int boardType) {
        super(context);
        this.context = context;
        this.boardType = boardType;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) { // מופעל כשגודל הView נקבע
        super.onSizeChanged(w, h, oldw, oldh);

        int boardSize = Math.min(w, h);
        cellSize = boardSize / 9;

        Bitmap bitmapCircle = BitmapFactory.decodeResource(getResources(), R.drawable.o);
        bitmapCircle = Bitmap.createScaledBitmap(bitmapCircle, cellSize - 10, cellSize - 10, false);

        arr = new Cell[9][9];

        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                arr[i][j] = new Cell(j * cellSize, i * cellSize, bitmapCircle, cellSize);
            }
        }

        if (!created) {
            created = true;
            ((GameActivity) context).onBoardReady(boardType);
        }

        invalidate();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        if (arr == null) return;

        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                arr[i][j].draw(canvas);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (arr == null) return true;

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            int col = (int) (event.getX() / cellSize);
            int line = (int) (event.getY() / cellSize);

            if (line >= 0 && line < 9 && col >= 0 && col < 9) {
                ((GameActivity) context).onBoardTouch(boardType, line, col);
            }

            return true;
        }

        return false;
    }

    public void clearAll() { // מאפס את כל הלוח
        if (arr == null) return;

        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                arr[i][j].clear();
            }
        }

        invalidate();
    }

    public void setCell(int line, int col, int val) {
        if (arr == null) return;
        if (line < 0 || line > 8 || col < 0 || col > 8) return;

        arr[line][col].forceSetVal(val);
        invalidate();
    }
    public void animateHit(int line, int col) { // מפעיל אנימציית פגיעה 🔴
        if (arr == null) return;
        arr[line][col].startExplosionAnimation(this);
    }
}
    public void clearAll() {
        if (arr == null) return;

        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                arr[i][j].clear();
            }
        }

        invalidate();
    }

    public void setCell(int line, int col, int val) {
        if (arr == null) return;
        if (line < 0 || line > 8 || col < 0 || col > 8) return;

        arr[line][col].forceSetVal(val);
        invalidate();
    }
}