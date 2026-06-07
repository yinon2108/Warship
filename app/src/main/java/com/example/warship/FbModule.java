package com.example.warship;

import android.content.Context;
import android.provider.Settings;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.ArrayList;

public class FbModule {
    private final DatabaseReference root;
    private final String myId;
    public FbModule(Context context) {
        FirebaseDatabase db = FirebaseDatabase.getInstance("https://warship-7e855-default-rtdb.firebaseio.com/");
        root = db.getReference("play");
        myId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }
    public DatabaseReference getRoot() {
        return root;
    }
    public String getMyId() {
        return myId;
    }
    public void clearMySlot(String mySlot) {
        if (mySlot == null) {return;}
        root.child(mySlot).removeValue();
        root.child("state").removeValue();
        root.child("currentTurn").removeValue();
        root.child("shot").removeValue();
        root.child("shotResult").removeValue();
    }
    public void setReady(String mySlot, ArrayList<Ship> ships) {
        if (mySlot == null) {return;}
        root.child(mySlot).child("ships").setValue(ships);
        root.child(mySlot).child("ready").setValue(true);
    }
    public void fireShot(String mySlot, int line, int col, long seq) { // שליחת ירייה של השחקן לפיירבייס
        Shot shot = new Shot(mySlot, line, col, seq);
        root.child("shot").setValue(shot);
    }

    public void sendShotResult(long seq, boolean hit) {
        ShotResult result = new ShotResult(seq, hit);
        root.child("shotResult").setValue(result);
    }

    public void setTurn(String slot) {
        root.child("currentTurn").setValue(slot);
    }

    public static class Shot {
        public String by;
        public int line;
        public int col;
        public long seq;
        public Shot() {
        }
        public Shot(String by, int line, int col, long seq) {
            this.by = by;
            this.line = line;
            this.col = col;
            this.seq = seq;
        }
    }
    public static class ShotResult {
        public long seq;
        public boolean hit;
        public ShotResult() {
        }
        public ShotResult(long seq, boolean hit) {
            this.seq = seq;
            this.hit = hit;
        }
    }
}