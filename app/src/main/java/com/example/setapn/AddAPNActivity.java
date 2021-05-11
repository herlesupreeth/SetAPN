package com.example.setapn;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

public class AddAPNActivity extends Activity {

    private TelephonyManager mTelephonyManager;
    public static final String EXTRA_MCC = "mcc";
    public static final String EXTRA_MNC = "mnc";

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_apn);
    }

    /** Called when the user taps the Add APN button */
    public void addAPN(View view) {
        int id = -1, carrier_id = -1;

        EditText mccText = findViewById(R.id.add_mcc);
        String extraMcc = mccText.getText().toString();
        EditText mncText = findViewById(R.id.add_mnc);
        String extraMnc = mncText.getText().toString();

        if (extraMcc.trim().equals("") || extraMcc.trim().length() < 3) {
            Toast.makeText(getApplicationContext(), "Invalid MCC", Toast.LENGTH_SHORT).show();
            return;
        }
        if (extraMnc.trim().equals("") || extraMnc.trim().length() < 2) {
            Toast.makeText(getApplicationContext(), "Invalid MNC", Toast.LENGTH_SHORT).show();
            return;
        }

        mTelephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            carrier_id = mTelephonyManager.getSimCarrierId();
        }

        ContentResolver resolver = this.getContentResolver();
        ContentValues values = new ContentValues();

        EditText name, apn_type;
        name = findViewById(R.id.add_apn_name);
        apn_type = findViewById(R.id.add_apn_type);

        values.put("name", name.getText().toString());
        values.put("apn", name.getText().toString());
        values.put("mcc", extraMcc);
        values.put("mnc", extraMnc);
        values.put("numeric", extraMcc+extraMnc);
        values.put("type", apn_type.getText().toString());
        values.put("carrier_enabled", 1);
        values.put("current", 1);
        values.put("carrier_id", carrier_id);
        values.put("edited", 0);
        // Forcing to go on IPv4 only rather than IPv4v6
        values.put("protocol", "IP");
        values.put("roaming_protocol", "IP");

        Cursor c = null;
        try {
            Uri newRow = resolver.insert(APN_TABLE_URI, values);
            if (newRow != null) {
                c = resolver.query(newRow, null, null, null, null);

                // Obtain the apn id
                int id_index = c.getColumnIndex("_id");
                c.moveToFirst();
                id = c.getShort(id_index);
                Log.d(TAG, "New ID: " + id + ": Inserting new APN succeeded!");
            }
        } catch (SQLException e) {
            Log.d(TAG, e.getMessage());
        }

        if (c != null)
            c.close();

        CheckBox preferred_apn = findViewById(R.id.preferred_apn);

        if (preferred_apn.isChecked() && id != -1) {
            ContentResolver d_resolver = this.getContentResolver();
            ContentValues d_values = new ContentValues();

            // See /etc/apns-conf.xml. The TelephonyProvider uses this file to
            // provide
            // content://telephony/carriers/preferapn URI mapping
            d_values.put("apn_id", id);
            try {
                d_resolver.update(PREFERRED_APN_URI, d_values, null, null);
                Cursor dc = resolver.query(PREFERRED_APN_URI, new String[] { "name",
                        "apn" }, "_id=" + id, null, null);
                if (dc != null) {
                    dc.close();
                }
            } catch (SQLException e) {
                Log.d(TAG, e.getMessage());
            }
        }

        Intent intent = new Intent(this, ListActivity.class);
        intent.putExtra(EXTRA_MCC, extraMcc);
        intent.putExtra(EXTRA_MNC, extraMnc);
        startActivity(intent);

        return;
    }
}
