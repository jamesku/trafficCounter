package edu.utexas.ctr.trafficcounterbasic;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.sql.ResultSet;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity implements  SettingsDialog.OnCompleteListener {
    SQLiteDatabase mydatabase;
    Vibrator vibe;
    String databasename = "Intersection";
    static String databasepath;
    boolean writing = false;
    boolean emailRequest = false;
    String trackingdatabasepath = "";
    int uploadcounter=0;
    int[] counter = new int[24];

    //define where the backup database is going to go
    File newDbPath = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //setup main view and toolbar
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //enable vibration
        vibe = (Vibrator) MainActivity.this.getSystemService(Context.VIBRATOR_SERVICE);

        TextView intersection = (TextView) findViewById(R.id.intersection);
        intersection.setText(databasename);
        //getDatabaseName();
        intersection.setText(databasename);
        //openTrackingDatabase(databasename);
        openDatabase(databasename);
        countUpdate();
    }

    public void getDatabaseName(){
        mydatabase = openOrCreateDatabase("trackingdatabase", SQLiteDatabase.CREATE_IF_NECESSARY, null);
        mydatabase.execSQL("CREATE TABLE IF NOT EXISTS selected(name TEXT);");
        Cursor RS = mydatabase.rawQuery("SELECT name from databases", null);
        RS.moveToFirst();
        if (RS.getString(0)!= null) {
            databasename = RS.getString(0);
        }

    }

    public String GetCurrentDataBaseName(){
        return databasename;
         }

    public void setDatabaseName(String databasename){
        mydatabase = openOrCreateDatabase("trackingdatabase", SQLiteDatabase.CREATE_IF_NECESSARY, null);
        mydatabase.execSQL("CREATE TABLE IF NOT EXISTS selected(name TEXT);");
        Cursor RS = mydatabase.rawQuery("SELECT name from databases", null);
        mydatabase.execSQL("insert into selected (name) Values ("+databasename+");");
    }


    public void onComplete(String newdatabasename) {
        databasename = newdatabasename;
        openTrackingDatabase(databasename);
        openDatabase(databasename);
        countUpdate();
    }

    public void openTrackingDatabase(String localdatabasename) {
        mydatabase = openOrCreateDatabase("trackingdatabase", SQLiteDatabase.CREATE_IF_NECESSARY, null);
        mydatabase.execSQL("CREATE TABLE IF NOT EXISTS databases(name TEXT);");
        //this query checks to see if the database name already exists and then adds it if it doesnt.
        mydatabase.execSQL("INSERT INTO databases(name) SELECT '" + localdatabasename + "' WHERE NOT EXISTS(SELECT 1 FROM databases WHERE name = '" + localdatabasename + "');");

        mydatabase.close();
        trackingdatabasepath = mydatabase.getPath();
    }

    public String getTrackingdatabasepath() {
        return trackingdatabasepath;
    }

    public void CreateNewDatabase(){

    }

    public void clearDatabase(){

        mydatabase = openOrCreateDatabase(databasename, SQLiteDatabase.CREATE_IF_NECESSARY, null);
        mydatabase.execSQL("Delete FROM Movements;");
        Arrays.fill(counter,0);
        resetButtons();

    }

    public void openDatabase(String localdatabasename) {
        //open or create a private database
        mydatabase = openOrCreateDatabase(localdatabasename, SQLiteDatabase.CREATE_IF_NECESSARY, null);
        databasename = localdatabasename;
        databasepath = mydatabase.getPath();

        //make sure all the movements have a table
        mydatabase.execSQL("CREATE TABLE IF NOT EXISTS Counts(Time VARCHAR, Movement VARCHAR);");
           }

    //export button has to ask permission to write to external storage in Android 6.0
    public void exportResults(View view) {
        checkPerm();
    }

    public void checkPerm() {
        //check version, if its less than 6.0 dont ask for specific permission
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            String[] perms = {"android.permission.WRITE_EXTERNAL_STORAGE"};
            int permsRequestCode = 200;
            requestPermissions(perms, permsRequestCode);
        } else {
            exportDB();
        }
    }

    //automatically called from android's permission function when permission is granted
    public void onRequestPermissionsResult(int permsRequestCode, String[] permissions, int[] grantResults) {
        switch (permsRequestCode) {
            case 200:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    exportDB();
                    //UploadFile uploadfile = new uploadFile();
                } else {
                    Toast.makeText(this, "Datbase NOT Exported", Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.email:
                emailResultsInit();
                return true;
            case R.id.action_export:
                checkPerm();
            case R.id.cleardb:
                clearDatabase();
           /* case R.id.action_settings:
                SettingsDialog settingsdialog = new SettingsDialog();
                settingsdialog.show(getSupportFragmentManager(), "SettingsDialog");
                return true;*/
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void emailResultsInit() {
        emailRequest = true;
        checkPerm();
    }


    public void emailResults() {

        File attachment = new File(newDbPath, databasename);
        if (attachment.exists()) {
            final Intent email = new Intent(Intent.ACTION_SEND);
            email.setType("vnd.android.cursor.dir/email");
            email.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(attachment));
            startActivity(Intent.createChooser(email, "Send Email"));
        }

    }

    private void settingsDialog() {
        SettingsDialog DeleteDialog = new SettingsDialog();
    }

    private void exportDB() {
        try {
            FileInputStream in = new FileInputStream(databasepath);
            FileOutputStream out = new FileOutputStream(newDbPath + "/" + databasename);
            FileChannel inChannel = new FileInputStream(databasepath).getChannel();
            FileChannel outChannel = new FileOutputStream(newDbPath + "/" + databasename).getChannel();

            inChannel.transferTo(0, inChannel.size(), outChannel);
            in.close();
            out.flush();
            out.close();
            Toast.makeText(this, "DB Exported!", Toast.LENGTH_LONG).show();

            if (emailRequest = true) {
                emailResults();
                emailRequest = false;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getDatabaseFolderPath() {
        String databasefolderpath = databasepath.replace(databasename, "");
        return databasefolderpath;
    }

    public void DeleteDatabase(View view) {

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                this);

        // set title
        alertDialogBuilder.setTitle("Delete Database");

        // set dialog message
        alertDialogBuilder
                .setMessage("Are you sure you want to delete this database")
                .setCancelable(true)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // if this button is clicked, close
                        // current activity
                        MainActivity.this.finish();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // if this button is clicked, just close
                        // the dialog box and do nothing
                        dialog.cancel();
                    }
                });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
    }


    public void countUpdate(){
       String[] movements = {"NorthThrough","SouthThrough","EastThrough","WestThrough","NorthThroughHeavy",
               "SouthThroughHeavy","EastThroughHeavy","WestThroughHeavy",
               "NorthLeft","SouthLeft","EastLeft","WestLeft","NorthLeftHeavy","SouthLeftHeavy","EastLeftHeavy",
               "WestLeftHeavy","NorthRight","SouthRight","EastRight","WestRight","NorthRightHeavy","SouthRightHeavy",
               "EastRightHeavy","WestRightHeavy"};

        for(int i=0; i< movements.length; i++)
        counter[i] = (int) (long) (DatabaseUtils.queryNumEntries(mydatabase, movements[i]));
    }

    public void uploadCount(){
        uploadcounter++;
                if (uploadcounter>400){
                    uploadDb();
                    uploadcounter=0;
        }
    }

    public void uploadDb(){

    }

    /* Buttons writing to Database
    ************************************************************************************************************************************* */



    public void setNorthThrough(View view){
        mydatabase.execSQL("INSERT INTO Counts (Time, Movement) VALUES(DateTime('now','localtime'), 'NorthThrough');");
        vibe.vibrate(8);
        String reference = "NorthThrough";
        Button myButton = (Button) findViewById(R.id.northThrough);
        //myButton.setText(""+count(reference));
        counter[0]++;
        myButton.setText(""+ counter[0]);
    }

    public void setSouthThrough(View view){
        String reference = "SouthThrough";
        mydatabase.execSQL("INSERT INTO Counts (Time, Movement) VALUES(DateTime('now','localtime'), 'SouthThrough');");
        vibe.vibrate(8);

        Button myButton = (Button) findViewById(R.id.southThrough);
        //myButton.setText(""+count(reference));
        counter[1]++;
        myButton.setText(""+ counter[1]);
    }

    public void setEastThrough(View view){
        String reference = "EastThrough";
        mydatabase.execSQL("INSERT INTO Counts (Time, Movement) VALUES(DateTime('now','localtime'), 'EastThrough');");
        vibe.vibrate(8);

        Button myButton = (Button) findViewById(R.id.eastThrough);
        //myButton.setText("" + count(reference));
        counter[2]++;
        myButton.setText(""+ counter[2]);
    }

    public void setWestThrough(View view){
        String reference = "WestThrough";
        mydatabase.execSQL("INSERT INTO Counts (Time, Movement) VALUES(DateTime('now','localtime'), 'WestThrough');");
        vibe.vibrate(8);

        Button myButton = (Button) findViewById(R.id.westThrough);
        //myButton.setText("" + count(reference));
        counter[3]++;
        myButton.setText(""+ counter[3]);
    }

    public void setNorthThroughHeavy(View view){
        String reference = "NorthThroughHeavy";
        mydatabase.execSQL("INSERT INTO Counts (Time, Movement) VALUES(DateTime('now','localtime'), 'NorthThroughHeavy');");
        vibe.vibrate(8);

        Button myButton = (Button) findViewById(R.id.northThroughHeavy);
        //myButton.setText("" + count(reference));
        counter[4]++;
        myButton.setText(""+ counter[4]);
    }

    public void setSouthThroughHeavy(View view){
        String reference = "SouthThroughHeavy";
        mydatabase.execSQL("INSERT INTO Counts (Time, Movement) VALUES(DateTime('now','localtime'), 'SouthThroughHeavy');");
        vibe.vibrate(8);

        Button myButton = (Button) findViewById(R.id.southThroughHeavy);
        //myButton.setText("" + count(reference));
        counter[5]++;
        myButton.setText(""+ counter[5]);
    }

    public void setEastThroughHeavy(View view){
        String reference = "EastThroughHeavy";
        mydatabase.execSQL("INSERT INTO Counts (Time, Movement) VALUES(DateTime('now','localtime'), 'EastThroughHeavy');");
        vibe.vibrate(8);

        Button myButton = (Button) findViewById(R.id.eastThroughHeavy);
        //myButton.setText("" + count(reference));
        counter[6]++;
        myButton.setText(""+ counter[6]);
    }

    public void setWestThroughHeavy(View view){
        String reference = "WestThroughHeavy";
        mydatabase.execSQL("INSERT INTO Counts (Time, Movement) VALUES(DateTime('now','localtime'), 'WestThroughHeavy');");
        vibe.vibrate(8);

        Button myButton = (Button) findViewById(R.id.westThroughHeavy);
        //myButton.setText("" + count(reference));
        counter[7]++;
        myButton.setText(""+ counter[7]);
    }

////////////////////////////////////////////////////////////////////////////////////////


    public void setNorthLeft(View view){
        String reference = "NorthLeft";
        mydatabase.execSQL("INSERT INTO Counts (Time, Movement) VALUES(DateTime('now','localtime'), 'NorthLeft');");
        vibe.vibrate(8);

        Button myButton = (Button) findViewById(R.id.northLeft);
        //myButton.setText("" + count(reference));
        counter[8]++;
        myButton.setText(""+ counter[8]);
    }

    public void setSouthLeft(View view){
        String reference = "SouthLeft";
        mydatabase.execSQL("INSERT INTO Counts (Time, Movement) VALUES(DateTime('now','localtime'), 'SouthLeft');");
        vibe.vibrate(8);

        Button myButton = (Button) findViewById(R.id.southLeft);
        //myButton.setText("" + count(reference));
        counter[9]++;
        myButton.setText(""+ counter[9]);
    }

    public void setEastLeft(View view){
        String reference = "EastLeft";
        mydatabase.execSQL("INSERT INTO Counts (Time, Movement) VALUES(DateTime('now','localtime'), 'EastLeft');");
        vibe.vibrate(8);

        Button myButton = (Button) findViewById(R.id.eastLeft);
       // myButton.setText("" + count(reference));
        counter[10]++;
        myButton.setText(""+ counter[10]);
    }

    public void setWestLeft(View view){
        String reference = "WestLeft";
        mydatabase.execSQL("INSERT INTO Counts (Time, Movement) VALUES(DateTime('now','localtime'), 'WestLeft');");
        vibe.vibrate(8);

        Button myButton = (Button) findViewById(R.id.westLeft);
       // myButton.setText("" + count(reference));
        counter[11]++;
        myButton.setText(""+ counter[11]);
    }

    public void setNorthLeftHeavy(View view){
        String reference = "NorthLeftHeavy";
        mydatabase.execSQL("INSERT INTO Counts (Time, Movement) VALUES(DateTime('now','localtime'), 'NorthLeftHeavy');");
        vibe.vibrate(8);

        Button myButton = (Button) findViewById(R.id.northLeftHeavy);
       // myButton.setText("" + count(reference));
        counter[12]++;
        myButton.setText(""+ counter[12]);
    }

    public void setSouthLeftHeavy(View view){
        String reference = "SouthLeftHeavy";
        mydatabase.execSQL("INSERT INTO Counts (Time, Movement) VALUES(DateTime('now','localtime'), 'SouthLeftHeavy');");
        vibe.vibrate(8);

        Button myButton = (Button) findViewById(R.id.southLeftHeavy);
       // myButton.setText("" + count(reference));
        counter[13]++;
        myButton.setText(""+ counter[13]);
    }

    public void setEastLeftHeavy(View view){
        String reference = "EastLeftHeavy";
        mydatabase.execSQL("INSERT INTO Counts (Time, Movement) VALUES(DateTime('now','localtime'), 'EastLeftHeavy');");
        vibe.vibrate(8);

        Button myButton = (Button) findViewById(R.id.eastLeftHeavy);
       // myButton.setText("" + count(reference));
        counter[14]++;
        myButton.setText(""+ counter[14]);
    }

    public void setWestLeftHeavy(View view){
        String reference = "WestLeftHeavy";
        mydatabase.execSQL("INSERT INTO Counts (Time, Movement) VALUES(DateTime('now','localtime'), 'WestLeftHeavy');");
        vibe.vibrate(8);

        Button myButton = (Button) findViewById(R.id.westLeftHeavy);
       // myButton.setText("" + count(reference));
        counter[15]++;
        myButton.setText(""+ counter[15]);
    }

    /////////////////////////////////////////////////////////////////////////////////////

    public void setNorthRight(View view){
        String reference = "NorthRight";
        mydatabase.execSQL("INSERT INTO Counts (Time, Movement) VALUES(DateTime('now','localtime'), 'NorthRight');");
        vibe.vibrate(8);

        Button myButton = (Button) findViewById(R.id.northRight);
      //  myButton.setText("" + count(reference));
        counter[16]++;
        myButton.setText(""+ counter[16]);
    }

    public void setSouthRight(View view){
        String reference = "SouthRight";
        mydatabase.execSQL("INSERT INTO Counts (Time, Movement) VALUES(DateTime('now','localtime'), 'SouthRight');");
        vibe.vibrate(8);

        Button myButton = (Button) findViewById(R.id.southRight);
      //  myButton.setText("" + count(reference));
        counter[17]++;
        myButton.setText(""+ counter[17]);
    }

    public void setEastRight(View view){
        String reference = "EastRight";
        mydatabase.execSQL("INSERT INTO Counts (Time, Movement) VALUES(DateTime('now','localtime'), 'EastRight');");
        vibe.vibrate(8);

        Button myButton = (Button) findViewById(R.id.eastRight);
      // myButton.setText("" + count(reference));
        counter[18]++;
        myButton.setText(""+ counter[18]);
    }

    public void setWestRight(View view){
        String reference = "WestRight";
        mydatabase.execSQL("INSERT INTO Counts (Time, Movement) VALUES(DateTime('now','localtime'), 'WestRight');");
        vibe.vibrate(8);

        Button myButton = (Button) findViewById(R.id.westRight);
      //  myButton.setText("" + count(reference));
        counter[19]++;
        myButton.setText(""+ counter[19]);
    }

    public void setNorthRightHeavy(View view){
        String reference = "NorthRightHeavy";
        mydatabase.execSQL("INSERT INTO Counts (Time, Movement) VALUES(DateTime('now','localtime'), 'NorthRightHeavy');");
        vibe.vibrate(8);

        Button myButton = (Button) findViewById(R.id.northRightHeavy);
       // myButton.setText("" + count(reference));
        counter[20]++;
        myButton.setText(""+ counter[20]);
    }

    public void setSouthRightHeavy(View view){
        String reference = "SouthRightHeavy";
        mydatabase.execSQL("INSERT INTO Counts (Time, Movement) VALUES(DateTime('now','localtime'), 'SouthRightHeavy');");
        vibe.vibrate(8);

        Button myButton = (Button) findViewById(R.id.southRightHeavy);
       // myButton.setText("" + count(reference));
        counter[21]++;
        myButton.setText(""+ counter[21]);
    }

    public void setEastRightHeavy(View view){
        String reference = "EastRightHeavy";
        mydatabase.execSQL("INSERT INTO Counts (Time, Movement) VALUES(DateTime('now','localtime'), 'EastRightHeavy');");
        vibe.vibrate(8);

        Button myButton = (Button) findViewById(R.id.eastRightHeavy);
       // myButton.setText("" + count(reference));
        counter[22]++;
        myButton.setText(""+ counter[22]);
    }

    public void setWestRightHeavy(View view){
        String reference = "WestRightHeavy";
        mydatabase.execSQL("INSERT INTO Counts (Time, Movement) VALUES(DateTime('now','localtime'), 'WestRightHeavy');");
        vibe.vibrate(8);

        Button myButton = (Button) findViewById(R.id.westRightHeavy);
      //  myButton.setText("" + count(reference));
        counter[23]++;
        myButton.setText(""+ counter[23]);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }


    public void resetButtons(){
            Button myButton = (Button) findViewById(R.id.northThrough);
            myButton.setText(""+ counter[0]);
            myButton = (Button) findViewById(R.id.southThrough);
            myButton.setText(""+ counter[1]);
            myButton = (Button) findViewById(R.id.eastThrough);
            myButton.setText(""+ counter[2]);
            myButton = (Button) findViewById(R.id.westThrough);
            myButton.setText(""+ counter[3]);
            myButton = (Button) findViewById(R.id.northThroughHeavy);
            myButton.setText(""+ counter[4]);
            myButton = (Button) findViewById(R.id.southThroughHeavy);
            myButton.setText(""+ counter[5]);
            myButton = (Button) findViewById(R.id.eastThroughHeavy);
            myButton.setText(""+ counter[6]);
            myButton = (Button) findViewById(R.id.westThroughHeavy);
            myButton.setText(""+ counter[7]);
            myButton = (Button) findViewById(R.id.northLeft);
            myButton.setText(""+ counter[8]);
            myButton = (Button) findViewById(R.id.southLeft);
            myButton.setText(""+ counter[9]);
            myButton = (Button) findViewById(R.id.eastLeft);
            myButton.setText(""+ counter[10]);
            myButton = (Button) findViewById(R.id.westLeft);
            myButton.setText(""+ counter[11]);
            myButton = (Button) findViewById(R.id.northLeftHeavy);
            myButton.setText(""+ counter[12]);
            myButton = (Button) findViewById(R.id.southLeftHeavy);
            myButton.setText(""+ counter[13]);
            myButton = (Button) findViewById(R.id.eastLeftHeavy);
            myButton.setText(""+ counter[14]);
            myButton = (Button) findViewById(R.id.westLeftHeavy);
            myButton.setText(""+ counter[15]);
            myButton = (Button) findViewById(R.id.northRight);
            myButton.setText(""+ counter[16]);
            myButton = (Button) findViewById(R.id.southRight);
            myButton.setText(""+ counter[17]);
            myButton = (Button) findViewById(R.id.eastRight);
            myButton.setText(""+ counter[18]);
            myButton = (Button) findViewById(R.id.westRight);
            myButton.setText(""+ counter[19]);
            myButton = (Button) findViewById(R.id.northRightHeavy);
            myButton.setText(""+ counter[20]);
            myButton = (Button) findViewById(R.id.southRightHeavy);
            myButton.setText(""+ counter[21]);
            myButton = (Button) findViewById(R.id.eastRightHeavy);
            myButton.setText(""+ counter[22]);
            myButton = (Button) findViewById(R.id.westRightHeavy);
            myButton.setText(""+ counter[23]);
        }

}
