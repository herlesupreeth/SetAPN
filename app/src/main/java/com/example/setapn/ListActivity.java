package com.example.setapn;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class ListActivity extends Activity {

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
    private String op_mcc, op_mnc;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        Intent intent = getIntent();
        op_mcc = intent.getStringExtra(SearchActivity.EXTRA_MCC);
        op_mnc = intent.getStringExtra(SearchActivity.EXTRA_MNC);

        populateMccMncAPNs(op_mcc, op_mnc);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.floating_action_button);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), AddAPNActivity.class);
                startActivity(intent);
            }
        });
    }

    private int populateMccMncAPNs(String mcc, String mnc) {

        Cursor c = this.getContentResolver().query(APN_TABLE_URI,
                null,
                "mcc = ? AND mnc = ?",
                new String[] { mcc, mnc }, null);

        if(c == null || !c.moveToFirst()) {
            Toast.makeText(getApplicationContext(), "No records found", Toast.LENGTH_SHORT).show();
            if (c != null) {
                c.close();
            }
            return -1;
        }

        TableLayout tableLayout = findViewById(R.id.list_layout);
        TableRow tableRow;
        TextView textView;
        Button deleteButton;

        // Populate table headers
        tableRow = new TableRow(getApplicationContext());
        String[] columnNames = c.getColumnNames();

        for(String columnName: columnNames) {
            textView = new TextView(getApplicationContext());
            textView.setText(columnName);
            textView.setPadding(20, 20, 20, 20);
            tableRow.addView(textView);
        }
        tableLayout.addView(tableRow);

        int rec_count = c.getCount();
        Log.d(TAG, "Total # of records: " + rec_count);

        do {
            tableRow = new TableRow(getApplicationContext());
            for(String columnName: columnNames) {
                int i = c.getColumnIndex(columnName);
                textView = new TextView(getApplicationContext());
                textView.setText(c.getString(i));
                textView.setPadding(20, 20, 20, 20);
                tableRow.addView(textView);
            }
            deleteButton = new Button(getApplicationContext());
            deleteButton.setText("Delete");
            deleteButton.setId(Integer.parseInt(c.getString(c.getColumnIndex("_id"))));
            deleteButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View arg0) {
                    int row_id = arg0.getId();
                    if (deleteAPN(row_id) < 1) {
                        Toast.makeText(getApplicationContext(), "Failed to delete APN", Toast.LENGTH_SHORT).show();
                    }
                    refreshTable();
                }
            });
            tableRow.addView(deleteButton);

            tableLayout.addView(tableRow);
        } while(c.moveToNext());

        c.close();
        return rec_count;
    }

    private int deleteAPN(int rowId) {
        int dc = this.getContentResolver().delete(APN_TABLE_URI,
                "_id = ?",
                new String[] { String.valueOf(rowId) });
        return dc;
    }

    private void refreshTable() {
        TableLayout tableLayout = findViewById(R.id.list_layout);
        tableLayout.removeAllViewsInLayout();
        populateMccMncAPNs(op_mcc, op_mnc);
    }
}
