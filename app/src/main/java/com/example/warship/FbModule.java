package com.example.warship;

import android.content.Context;
import android.provider.Settings;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class FbModule {

    private final DatabaseReference root;
    private final String myId;

    public FbModule(Context context) {
        FirebaseDatabase db = FirebaseDatabase.getInstance("https://warship-7e855-default-rtdb.firebaseio.com/"); // חיבור לFirebase
        root = db.getReference("play"); // מצביע לצומת play
        myId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    public interface SlotCallback {
        void onSlot(String slot); // מחזיר p1 / p2 / null
    }

    public void claimSlot(SlotCallback cb) { // ניסיון לחבר שחקן ל-p1/p2
        root.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                String p1Id = snapshot.child("p1").child("id").getValue(String.class);
                String p2Id = snapshot.child("p2").child("id").getValue(String.class);

                if (p1Id == null || p1Id.equals(myId)) {
                    prepareSlot("p1", cb);
                    return;
                }

                if (p2Id == null || p2Id.equals(myId)) {
                    prepareSlot("p2", cb);
                    return;
                }

                cb.onSlot(null);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                cb.onSlot(null);
            }// מקרה שגיאה
        });
    }

    private void prepareSlot(String slot, SlotCallback cb) {// הכנת מקום בפיירבייס לשחקן
        root.child(slot).child("id").setValue(myId);
        root.child(slot).child("ready").setValue(false);
        root.child(slot).child("ships").removeValue();

        root.child("state").removeValue();
        root.child("currentTurn").removeValue();
        root.child("shot").removeValue();
        root.child("shotResult").removeValue();

        cb.onSlot(slot);
    }

    public void clearMySlot(String mySlot) { // מרוקן מקום של שחקן בפיירבייס
        if (mySlot == null) return;

        root.child(mySlot).removeValue();
        root.child("state").removeValue();
        root.child("currentTurn").removeValue();
        root.child("shot").removeValue();
        root.child("shotResult").removeValue();
    }

    public void setReady(String mySlot, ArrayList<Ship> ships) { // סימון שהשחקן מוכן
        if (mySlot == null) return;

        root.child(mySlot).child("ships").setValue(ships);
        root.child(mySlot).child("ready").setValue(true);
    }

    public interface BothReadyListener {
        void onBothReady(boolean bothReady); // מחזיר האם שני השחקנים מוכנים
    }

    public void listenBothReady(BothReadyListener l) { // בודק אם שני השחקנים מוכנים
        root.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot s) {
                Boolean r1 = s.child("p1").child("ready").getValue(Boolean.class);
                Boolean r2 = s.child("p2").child("ready").getValue(Boolean.class);

                boolean both = (r1 != null && r1) && (r2 != null && r2);
                l.onBothReady(both);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    public void tryStartGameIfNeeded() { // מתחיל משחק
        root.child("state").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String currentState = snapshot.getValue(String.class);

                if (currentState == null) {
                    root.child("state").setValue("PLAY");
                    root.child("currentTurn").setValue("p1");
                    root.child("shot").removeValue();
                    root.child("shotResult").removeValue();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    public interface StateListener {
        void onState(String state); // מחזיר את מצב המשחק setup/play
    }

    public void listenState(StateListener l) {
        root.child("state").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot s) { // מופעל כאשר state משתנה
                String state = s.getValue(String.class);
                if (state == null) state = "SETUP";
                l.onState(state); // שולח את מצב המשחק לGameActivity
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    public interface TurnListener {
        void onTurn(String turn); // מחזיר של מי התור
    }

    public void listenTurn(TurnListener l) {
        root.child("currentTurn").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot s) { // מופעל כאשר התור משתנה
                String turn = s.getValue(String.class);
                l.onTurn(turn); // שולח לGameActivity את התור הנוכחי
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    public static class Shot {
        public String by;
        public int line;
        public int col;
        public long seq;

        public Shot() { // בנאי ריק בשביל Firebase
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

        public ShotResult() { // בנאי ריק בשביל Firebase
        }

        public ShotResult(long seq, boolean hit) {
            this.seq = seq;
            this.hit = hit;
        }
    }

    public interface ShotListener {
        void onShot(Shot shot);// מחזיר את פרטי הירייה שהתקבלה
    }

    public void listenShot(ShotListener l) { // מאזין ליריות חדשות בפיירבייס
        root.child("shot").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot s) { // מופעל כאשר יש ירייה חדשה
                Shot shot = s.getValue(Shot.class);
                if (shot != null) l.onShot(shot);// שולח את פרטי הירייה לGameActivity
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    public interface ShotResultListener {
        void onShotResult(ShotResult result); // מחזיר את תוצאת הירייה
    }

    public void listenShotResult(ShotResultListener l) {
        root.child("shotResult").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot s) { // מופעל כאשר מתקבלת תוצאת ירייה
                ShotResult result = s.getValue(ShotResult.class);
                if (result != null) l.onShotResult(result); // שולח את התוצאה לGameActivity
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    public void fireShot(String mySlot, int line, int col, long seq) { // יצירת ירייה חדשה ושליחתה לפיירבייס
        root.child("shot").setValue(new Shot(mySlot, line, col, seq));
    }

    public void sendShotResult(long seq, boolean hit) { // שולח את תוצאת הירייה לפיירבייס
        root.child("shotResult").setValue(new ShotResult(seq, hit));
    }

    public void setTurn(String slot) { // משנה את התור הנוכחי במשחק
        root.child("currentTurn").setValue(slot);
    }
}