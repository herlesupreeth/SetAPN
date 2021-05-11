package com.example.setapn;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.provider.Telephony;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends Activity {
    /*
     * Information of all APNs Details can be found in
     * com.android.providers.telephony.TelephonyProvider
     */
    public static final Uri APN_TABLE_URI = Uri
            .parse("content://telephony/carriers");
    /*
     * Information of the preferred APN
     */
    public static final Uri PREFERRED_APN_URI = Uri
            .parse("content://telephony/carriers/preferapn");
    private static final String TAG = "APN_SETTINGS";
    public static final String NEW_APN = "internet";

    private int getDefaultAPN() {
        Cursor c = this.getContentResolver().query(PREFERRED_APN_URI,
                new String[] { "_id", "name" }, null, null, null);
        int id = -1;
        if (c != null) {
            try {
                if (c.moveToFirst())
                    id = c.getInt(c.getColumnIndex("_id"));
            } catch (SQLException e) {
                Log.d(TAG, e.getMessage());
            }
            c.close();
        }
        return id;
    }

    /*
     * Set an apn to be the default apn for web traffic Require an input of the
     * apn id to be set
     */
    public boolean setDefaultAPN(int id) {
        boolean res = false;
        ContentResolver resolver = this.getContentResolver();
        ContentValues values = new ContentValues();

        // See /etc/apns-conf.xml. The TelephonyProvider uses this file to
        // provide
        // content://telephony/carriers/preferapn URI mapping
        values.put("apn_id", id);
        try {
            resolver.update(PREFERRED_APN_URI, values, null, null);
            Cursor c = resolver.query(PREFERRED_APN_URI, new String[] { "name",
                    "apn" }, "_id=" + id, null, null);
            if (c != null) {
                res = true;
                c.close();
            }
        } catch (SQLException e) {
            Log.d(TAG, e.getMessage());
        }
        return res;
    }

    private int checkNewAPN(String name, String apn, String type, String mcc, String mnc) {
        int id = -1;
        // MNC and MCC based search works only until Android API level 28, deprecated in 29
        Cursor c = this.getContentResolver().query(APN_TABLE_URI,
                new String[] { "_id", "name",  "apn", "type", "mcc", "mnc" }, "name=? AND apn=? AND type=? AND mcc=? AND mnc=?",
                new String[] { name, apn, type, mcc, mnc }, null);
        if (c == null) {
            id = -1;
        } else {
            int record_cnt = c.getCount();
            if (record_cnt == 0) {
                id = -1;
            } else if (c.moveToFirst()) {
                if (c.getString(c.getColumnIndex("name")).equalsIgnoreCase(
                        name) &&
                    c.getString(c.getColumnIndex("apn")).equalsIgnoreCase(
                            apn) &&
                    c.getString(c.getColumnIndex("type")).equalsIgnoreCase(
                                type) &&
                    c.getString(c.getColumnIndex("mcc")).equals(mcc) &&
                    c.getString(c.getColumnIndex("mnc")).equals(mnc)) {
                    id = c.getInt(c.getColumnIndex("_id"));
                }
            }
            c.close();
        }
        return id;
    }

    /*
     *  Print all data records associated with Cursor c.
     *  Return a string that contains all record data.
     *  For some weird reason, Android SDK Log class cannot print very long string message.
     *  Thus we have to log record-by-record.
     */
    private String printAllData() {

        Cursor c = this.getContentResolver().query(APN_TABLE_URI,
                null, null, null, null);
        if(c == null) return null;
        String s = "";
        int record_cnt = c.getColumnCount();
        Log.d(TAG, "Total # of records: " + record_cnt);

        if(c.moveToFirst())
        {
            String[] columnNames = c.getColumnNames();
            String row = "";
            for(String columnName:columnNames)
            {
                row += columnName+":\t";
            }
            Log.d(TAG, row);
            s += columnNames;
            do{
                row = "";
                for(String columnIndex:columnNames)
                {
                    int i = c.getColumnIndex(columnIndex);
                    row += c.getString(i)+":\t";
                }
                row += "\n";
                Log.d(TAG, row);
                s += row;
            }while(c.moveToNext());
            Log.d(TAG,"End Of Records");
        }
        return s;
    }

    public int addNewAPN(String name, String apn, String type, String mcc, String mnc) {
        int id = -1;
        ContentResolver resolver = this.getContentResolver();
        ContentValues values = new ContentValues();

        values.put("name", name);
        values.put("apn", apn);
        values.put("mcc", mcc);
        values.put("mnc", mnc);
        values.put("numeric", mcc+mnc);
        values.put("type", type);
        values.put("carrier_enabled", 1);
        // Forcing to go on IPv4 only rather than IPv4v6
        values.put("protocol", "IP");
        values.put("modem_cognitive", 1);

        Cursor c = null;
        try {
            Uri newRow = resolver.insert(APN_TABLE_URI, values);
            if (newRow != null) {
                c = resolver.query(newRow, null, null, null, null);

                // Obtain the apn id
                int idindex = c.getColumnIndex("_id");
                c.moveToFirst();
                id = c.getShort(idindex);
                Log.d(TAG, "New ID: " + id + ": Inserting new APN succeeded!");
            }
        } catch (SQLException e) {
            Log.d(TAG, e.getMessage());
        }

        if (c != null)
            c.close();
        return id;
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Adding default APN to access internet
        int id = checkNewAPN("internet", "internet", "default", "001", "01");
        if (id == -1) {
            id = addNewAPN("internet", "internet", "default", "001", "01");
        }
        if (setDefaultAPN(id)) {
            Log.i(TAG, NEW_APN
                    + " set new default APN successfully and Default id is "
                    + id);
        }
        // Adding APN for IMS
        id = checkNewAPN("ims", "ims", "ims", "001", "01");
        if (id == -1) {
            id = addNewAPN("ims", "ims", "ims", "001", "01");
        }
    }
}