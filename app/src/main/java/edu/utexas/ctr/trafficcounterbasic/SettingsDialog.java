package edu.utexas.ctr.trafficcounterbasic;

import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.RadialGradient;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by kuhrja on 4/8/2016.
 */
public class SettingsDialog extends DialogFragment {

    String currentDb = "";
    View rootView;
    String databasename;


    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.settings_dialog, null, false);
        getDialog().setTitle("Database Settings");
        final Context ctx = getActivity();
        databasename = ((MainActivity) getActivity()).GetCurrentDataBaseName();
        setCurrentdatabase();

        String trackingdatabasepath = ((MainActivity) getActivity()).getTrackingdatabasepath();
        SQLiteDatabase mydatabase = SQLiteDatabase.openOrCreateDatabase(trackingdatabasepath, null);

        Cursor RS = mydatabase.rawQuery("SELECT name from databases", null);
        Spinner spinner = (Spinner) rootView.findViewById(R.id.spinner);

        ArrayList<String> mArrayList = new ArrayList<String>();
        for (RS.moveToFirst(); !RS.isAfterLast(); RS.moveToNext()) {
            // The Cursor is now set to the right position
            mArrayList.add(RS.getString(0));
        }

        if (mArrayList != null) {

            // Create an ArrayAdapter using the string array and a default spinner layout
            ArrayAdapter<String> arrayadapter = new ArrayAdapter<String>(ctx, android.R.layout.simple_spinner_item, mArrayList);
            // Specify the layout to use when the list of choices appears
            arrayadapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            // Apply the adapter to the spinner
            spinner.setAdapter(arrayadapter);
        }

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                databasename = parentView.getItemAtPosition(position).toString();// your code here
                setCurrentdatabase();
                //returncurrentdatabase();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                return;
            }

        });

        return rootView;
    }

    public void setCurrentdatabase(){
        TextView currentDatabase = (TextView) rootView.findViewById(R.id.CurrentDatabaseTextView);
        currentDatabase.setText(databasename);
        }

    private OnCompleteListener mListener;

    public void returncurrentdatabase(){
        this.mListener.onComplete(databasename);
    }

    public static interface OnCompleteListener {
        public abstract void onComplete(String newdatabasename);

    }


        public String getCurrentDb(){
        return currentDb;
       }

}

