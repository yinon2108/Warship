package com.example.warship;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class GameActivity extends AppCompatActivity {
    private TextView txtStatus;
    private Button btnStart;
    private BoardGame myBoard;
    private BoardGame oppBoard;
    private FbModule fb;
    private String mySlot = null;
    private String state = "SETUP";
    private String currentTurn = null;
    private boolean lockSetup = false;
    private boolean myReadySent = false;
    private boolean myBoardReady = false;
    private boolean oppBoardReady = false;
    private boolean setupStarted = false;
    private ArrayList<Ship> ships = new ArrayList<>();
    private Ship selectedShip = null;
    private int[][] myShots = new int[9][9];
    private int[][] shotsOnMe = new int[9][9];
    private long lastShotSeqHandled = 0;
    private long pendingSeq = -1;
    private int pendingLine = -1;
    private int pendingCol = -1;
    private int hitsIHit = 0;
    private int hitsOnMe = 0;
    private boolean gameEnded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
        txtStatus = findViewById(R.id.txtStatus);
        btnStart = findViewById(R.id.btnStart);
        FrameLayout myContainer = findViewById(R.id.myBoardContainer);
        FrameLayout oppContainer = findViewById(R.id.oppBoardContainer);
        myBoard = new BoardGame(this, BoardGame.BOARD_MY); // יצירת הלוח שלי
        oppBoard = new BoardGame(this, BoardGame.BOARD_OPP); // יצירת לוח היריב
        FrameLayout.LayoutParams params1 = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                );
        FrameLayout.LayoutParams params2 = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                );
        myContainer.addView(myBoard, params1); // הכנסת הלוח שלי למסך
        oppContainer.addView(oppBoard, params2);// הכנסת לוח היריב למסך
        fb = new FbModule(this);

        fb.getRoot().addListenerForSingleValueEvent(new ValueEventListener() { // שיוך מכשיר לp1/p2
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String p1Id = snapshot.child("p1").child("id").getValue(String.class);
                String p2Id = snapshot.child("p2").child("id").getValue(String.class);
                String myId = fb.getMyId();
                if (p1Id == null || p1Id.equals(myId)) {
                    mySlot = "p1";
                    fb.getRoot().child("p1").child("id").setValue(myId);
                    fb.getRoot().child("p1").child("ready").setValue(false);
                    fb.getRoot().child("p1").child("ships").removeValue();
                    fb.getRoot().child("state").removeValue();
                    fb.getRoot().child("currentTurn").removeValue();
                    fb.getRoot().child("shot").removeValue();
                    fb.getRoot().child("shotResult").removeValue();
                }
                else if (p2Id == null || p2Id.equals(myId)) {
                    mySlot = "p2";
                    fb.getRoot().child("p2").child("id").setValue(myId);
                    fb.getRoot().child("p2").child("ready").setValue(false);
                    fb.getRoot().child("p2").child("ships").removeValue();
                    fb.getRoot().child("state").removeValue();
                    fb.getRoot().child("currentTurn").removeValue();
                    fb.getRoot().child("shot").removeValue();
                    fb.getRoot().child("shotResult").removeValue();
                }
                if (mySlot == null) {
                    Toast.makeText(GameActivity.this, "כבר יש 2 שחקנים.", Toast.LENGTH_LONG).show();
                    btnStart.setEnabled(false);
                    return;
                }
                btnStart.setEnabled(true);
                Toast.makeText(GameActivity.this, "אתה: " + mySlot, Toast.LENGTH_SHORT).show();
                updateStatusText();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {} // מופעל במקרה של שגיאה בגישה לפיירבייס
        });

        fb.getRoot().child("state").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot s) {
                String newState = s.getValue(String.class);
                if (newState == null) {
                    newState = "SETUP";
                }
                if ("PLAY".equals(newState) && !myReadySent) {
                    state = "SETUP";
                    updateStatusText();
                    return;
                }
                state = newState;
                if ("PLAY".equals(state)) {
                    lockSetup = true;
                    btnStart.setEnabled(false);
                    btnStart.setText("PLAYING");
                }
                updateStatusText();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        fb.getRoot().child("currentTurn").addValueEventListener(new ValueEventListener() { //עדכון תור המשחק
            @Override
            public void onDataChange(@NonNull DataSnapshot s) {
                currentTurn = s.getValue(String.class);
                updateStatusText();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        fb.getRoot().addValueEventListener(new ValueEventListener() { // הפעלת המשחק כאשר שני השחקנים מוכנים
            @Override
            public void onDataChange(@NonNull DataSnapshot s) {
                Boolean r1 = s.child("p1").child("ready").getValue(Boolean.class);
                Boolean r2 = s.child("p2").child("ready").getValue(Boolean.class);
                boolean bothReady = (r1 != null && r1) && (r2 != null && r2);
                if (bothReady && myReadySent) {
                    fb.getRoot().child("state").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            String currentState = snapshot.getValue(String.class);
                            if (currentState == null) {
                                fb.getRoot().child("state").setValue("PLAY");
                                fb.getRoot().child("currentTurn").setValue("p1");
                                fb.getRoot().child("shot").removeValue();
                                fb.getRoot().child("shotResult").removeValue();
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
                    });
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        fb.getRoot().child("shot").addValueEventListener(new ValueEventListener() { // קבלת יריות מהיריב ובדיקת פגיעה או החטאה בלוח שלי
            @Override
            public void onDataChange(@NonNull DataSnapshot s) {
                if (gameEnded) {return;}
                FbModule.Shot shot = s.getValue(FbModule.Shot.class);
                if (shot == null) {return;}
                if (mySlot == null) {return;}
                if (shot.by == null) {return;}
                if (shot.by.equals(mySlot)) {return;}
                if (shot.seq <= lastShotSeqHandled) {return;}

                lastShotSeqHandled = shot.seq;
                boolean hit = isHitOnMyShips(shot.line, shot.col);
                if (hit) {
                    if (shotsOnMe[shot.line][shot.col] != 2) {
                        hitsOnMe++;
                    }
                    shotsOnMe[shot.line][shot.col] = 2;
                    myBoard.setCell(shot.line, shot.col, Cell.Hit);
                    myBoard.animateHit(shot.line, shot.col);
                }
                else {
                    shotsOnMe[shot.line][shot.col] = 1;
                    myBoard.setCell(shot.line, shot.col, Cell.Miss);
                }

                fb.sendShotResult(shot.seq, hit);

                if (hitsOnMe >= 12) { // האם הפסדתי
                    gameEnded = true;
                    btnStart.setText("YOU LOST");
                    txtStatus.setText("הפסדת!");
                    showEndDialog(false);
                    return;
                }
                fb.setTurn(mySlot);
                Toast.makeText(GameActivity.this, hit ? "ירו עליך: פגיעה!" : "ירו עליך: החטאה", Toast.LENGTH_SHORT).show();
                updateStatusText();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

            fb.getRoot().child("shotResult").addValueEventListener(new ValueEventListener() { //קבלת תוצאת הירייה שלי
            @Override
            public void onDataChange(@NonNull DataSnapshot s) {
                if (gameEnded) return;
                if (pendingSeq == -1) return;
                FbModule.ShotResult result = s.getValue(FbModule.ShotResult.class);
                if (result == null) return;
                if (result.seq != pendingSeq) return;

                if (result.hit) {
                    if (myShots[pendingLine][pendingCol] != 2) {
                        hitsIHit++;
                    }
                    myShots[pendingLine][pendingCol] = 2;
                    oppBoard.animateHit(pendingLine, pendingCol);
                    Toast.makeText(GameActivity.this, "פגעת!", Toast.LENGTH_SHORT).show();
                }
                else {
                    myShots[pendingLine][pendingCol] = 1;
                    Toast.makeText(GameActivity.this, "החטאת!", Toast.LENGTH_SHORT).show();
                }

                redrawOppShots();

                pendingSeq = -1;
                pendingLine = -1;
                pendingCol = -1;

                if (hitsIHit >= 12) { // האם ניצחתי
                    gameEnded = true;
                    btnStart.setText("YOU WON");
                    txtStatus.setText("ניצחת!");
                    showEndDialog(true);
                    return;
                }
                updateStatusText();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        btnStart.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                if (!"SETUP".equals(state)) return;
                if (mySlot == null) return;

                myReadySent = true;
                lockSetup = true;
                selectedShip = null;
                btnStart.setEnabled(false);
                btnStart.setText("Waiting...");
                fb.setReady(mySlot, ships);
                Toast.makeText(GameActivity.this, "שלחת READY", Toast.LENGTH_SHORT).show();
                updateStatusText();
            }
        });
        updateStatusText();
    }
    private void showEndDialog(boolean win) {
        String title;
        String message;
        if (win) {
            title = "ניצחת!";
            message = "כל הכבוד, הטבעת את כל הצוללות של היריב.";
        }
        else {
            title = "הפסדת!";
            message = "היריב הטביע את כל הצוללות שלך.";
        }

        new AlertDialog.Builder(this).setTitle(title).setMessage(message).setCancelable(false)
                .setPositiveButton("סיום", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        finish();
                    }
                }).show();
    }

    @Override
    protected void onDestroy() { // ניקוי נתוני השחקן מפיירבייס בעת סגירת המסך
        super.onDestroy();
        if (fb != null && mySlot != null) {
            fb.clearMySlot(mySlot);
        }
    }

    public void onBoardReady(int boardType) { // עדכון שBoardGame סיים להיטען והכנה לשימוש
        if (boardType == BoardGame.BOARD_MY) {myBoardReady = true;}
        if (boardType == BoardGame.BOARD_OPP) {oppBoardReady = true;}
        if (myBoardReady && oppBoardReady && !setupStarted) {
            setupStarted = true;
            startSetup();
        }
    }

    public void onBoardTouch(int boardType, int line, int col) { // טיפול בלחיצות על הלוח בזמן סידור הספינות או במהלך המשחק
        if (gameEnded) {return;}
        if ("SETUP".equals(state)) {
            if (boardType == BoardGame.BOARD_MY) {
                handleSetupTouch(line, col);
            }
            return;
        }
        if ("PLAY".equals(state)) {
            if (boardType == BoardGame.BOARD_OPP) {
                handlePlayShot(line, col);
            }
        }
    }

    private void startSetup() { // אתחול שלב סידור הספינות לפני תחילת המשחק
        ships.clear();
        selectedShip = null;
        lockSetup = false;
        myReadySent = false;
        gameEnded = false;

        hitsIHit = 0;
        hitsOnMe = 0;
        pendingSeq = -1;
        pendingLine = -1;
        pendingCol = -1;
        lastShotSeqHandled = 0;

        myShots = new int[9][9];
        shotsOnMe = new int[9][9];

        ships.add(new Ship(1, 1, "v"));
        ships.add(new Ship(1, 3, "v"));
        ships.add(new Ship(1, 5, "v"));
        ships.add(new Ship(1, 7, "v"));
        redrawMyShips();
        redrawOppShots();
        updateStatusText();
    }

    private void handleSetupTouch(int line, int col) { // טיפול בלחיצת המשתמש בזמן סידור הספינות
        if (lockSetup) {return;}

        Ship ship = getShipByCenter(line, col);
        if (ship != null) {
            if (selectedShip == ship) {
                String newOri = ship.ori.equals("v") ? "h" : "v";
                if (canShipBeAt(ship, ship.centerLine, ship.centerCol, newOri)) {
                    ship.toggleOri();
                    redrawMyShips();
                }
                else {
                    Toast.makeText(this, "אין מקום לסיבוב", Toast.LENGTH_SHORT).show();
                }
                return;
            }
            selectedShip = ship;
            Toast.makeText(this, "נבחרה ספינה. לחץ על תא ריק להזזה או שוב לסיבוב", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedShip != null) {
            if (canShipBeAt(selectedShip, line, col, selectedShip.ori)) {
                selectedShip.centerLine = line;
                selectedShip.centerCol = col;
                redrawMyShips();
            }
            else {
                Toast.makeText(this, "אין מקום להזזה", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void redrawMyShips() { // ציור מחדש של הספינות שלי על הלוח לאחר שינוי מיקום או כיוון
        myBoard.clearAll();
        for (Ship s : ships) { //עבור על כל ספינה s שנמצאת ברשימת ships
            myBoard.setCell(s.centerLine, s.centerCol, Cell.Oval);
            if (s.ori.equals("v")) {
                myBoard.setCell(s.centerLine - 1, s.centerCol, Cell.Oval);
                myBoard.setCell(s.centerLine + 1, s.centerCol, Cell.Oval);
            }
            else {
                myBoard.setCell(s.centerLine, s.centerCol - 1, Cell.Oval);
                myBoard.setCell(s.centerLine, s.centerCol + 1, Cell.Oval);
            }
        }

        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                if (shotsOnMe[i][j] == 1) {
                    myBoard.setCell(i, j, Cell.Miss);
                }
                if (shotsOnMe[i][j] == 2) {
                    myBoard.setCell(i, j, Cell.Hit);
                }
            }
        }
    }
    private void handlePlayShot(int line, int col) { // טיפול בלחיצה של השחקן על לוח היריב
        if (gameEnded) return;
        if (mySlot == null) return;
        if (!"PLAY".equals(state)) return;
        if (!mySlot.equals(currentTurn)) {
            Toast.makeText(this, "לא התור שלך", Toast.LENGTH_SHORT).show();
            return;
        }
        if (myShots[line][col] != 0) {
            Toast.makeText(this, "כבר ירית כאן", Toast.LENGTH_SHORT).show();
            return;
        }
        if (pendingSeq != -1) {
            Toast.makeText(this, "חכה לתוצאה של הירייה הקודמת", Toast.LENGTH_SHORT).show();
            return;
        }
        long seq = System.currentTimeMillis();
        pendingSeq = seq;
        pendingLine = line;
        pendingCol = col;
        fb.fireShot(mySlot, line, col, seq);
        Toast.makeText(this, "ירית! מחכה לתוצאה...", Toast.LENGTH_SHORT).show();
    }

    private void redrawOppShots() { // עדכון תצוגת היריות שבוצעו על לוח היריב
        oppBoard.clearAll();
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                if (myShots[i][j] == 1) {oppBoard.setCell(i, j, Cell.Miss);}
                if (myShots[i][j] == 2) {oppBoard.setCell(i, j, Cell.Hit);}
            }
        }
    }
    private void updateStatusText() { // עדכון הודעת המצב המוצגת לשחקן
        if (txtStatus == null) return;
        if (gameEnded) return;
        String text = "State: " + state;
        if (mySlot != null) {
            text += " | You: " + mySlot;
        }
        if ("SETUP".equals(state)) {
            if (lockSetup) {
                text += " | READY sent";
            }
            else {
                text += " | Arrange ships";
            }
        }
        if ("PLAY".equals(state)) {
            boolean myTurn = mySlot != null && mySlot.equals(currentTurn);
            text += " | " + (myTurn ? "התור שלך" : "תור היריב");
        }
        txtStatus.setText(text);
    }
    private Ship getShipByCenter(int line, int col) { // איתור ספינה לפי תא המרכז שלה
        for (Ship s : ships) { //עבור על כל ספינה s שנמצאת ברשימת ships
            if (s.centerLine == line && s.centerCol == col) {
                return s;
            }
        }
        return null;
    }
    private boolean canShipBeAt(Ship ship, int newLine, int newCol, String newOri) { // בדיקה האם ניתן למקם ספינה במיקום ובכיוון המבוקשים
        int[][] cells;
        if (newOri.equals("v")) {
            cells = new int[][]{
                    {newLine, newCol},
                    {newLine - 1, newCol},
                    {newLine + 1, newCol}
            };
        }
        else {
            cells = new int[][]{
                    {newLine, newCol},
                    {newLine, newCol - 1},
                    {newLine, newCol + 1}
            };
        }
        for (int[] c : cells) { //עבור על כל תא במערך דו מימדי cells, ושמור אותו כמערך חד מימדי c

            if (c[0] < 0 || c[0] > 8 || c[1] < 0 || c[1] > 8) { // c[0] = שורה, c[1] = עמודה
                return false;
            }
        }
        for (Ship other : ships) { //עבור על כל ספינה s שנמצאת ברשימת ships

            if (other == ship) { // דילוג על הספינה הנוכחית כדי לא לבדוק התנגשות עם עצמה
                continue;
            }
            for (int[] c : cells) { //עבור על כל תא במערך דו מימדי cells, ושמור אותו כמערך חד מימדי c
                if (isCellOfShip(other, c[0], c[1])) {
                    return false;
                }
            }
        }
        return true;
    }
    private boolean isCellOfShip(Ship s, int line, int col) { // בדיקה האם תא מסוים שייך לספינה נתונה
        if (s.centerLine == line && s.centerCol == col) {
            return true;
        }
        if (s.ori.equals("v")) {
            return (s.centerLine - 1 == line && s.centerCol == col) || (s.centerLine + 1 == line && s.centerCol == col);
        }
        else {
            return (s.centerLine == line && s.centerCol - 1 == col) || (s.centerLine == line && s.centerCol + 1 == col);
        }
    }
    private boolean isHitOnMyShips(int line, int col) { // בדיקה האם ירייה פגעה באחת מהספינות שלי
        for (Ship s : ships) {
            if (isCellOfShip(s, line, col)) {
                return true;
            }
        }
        return false;
    }
}