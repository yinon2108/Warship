package com.example.warship;

public class Ship {
    public int centerLine;
    public int centerCol;
    public String ori; // "v" / "h"

    public Ship() {
    }

    public Ship(int centerLine, int centerCol, String ori) {
        this.centerLine = centerLine;
        this.centerCol = centerCol;
        this.ori = ori;
    }

    public void toggleOri() {
        ori = ori.equals("v") ? "h" : "v";
    }
}