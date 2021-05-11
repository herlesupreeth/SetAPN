package com.example.setapn;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;


public class SearchActivity extends Activity {

    private TelephonyManager mTelephonyManager;
    public static final String EXTRA_MCC = "mcc";
    public static final String EXTRA_MNC = "mnc";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
    }

    /** Called when the user taps the Search APNs button */
    public void searchAPNs(View view) {
        EditText mccText = findViewById(R.id.search_mcc);
        String extraMcc = mccText.getText().toString();
        EditText mncText = findViewById(R.id.search_mnc);
        String extraMnc = mncText.getText().toString();

        if (extraMcc.trim().equals("") || extraMcc.trim().length() < 3) {
            Toast.makeText(getApplicationContext(), "Invalid MCC", Toast.LENGTH_SHORT).show();
            return;
        }
        if (extraMnc.trim().equals("") || extraMnc.trim().length() < 2) {
            Toast.makeText(getApplicationContext(), "Invalid MNC", Toast.LENGTH_SHORT).show();
            return;
        }
        if (checkCarrierPrivileges() < 0) {
           return;
        }

        Intent intent = new Intent(this, ListActivity.class);
        intent.putExtra(EXTRA_MCC, extraMcc);
        intent.putExtra(EXTRA_MNC, extraMnc);

        startActivity(intent);
    }

    private boolean isSimCardPresent() {
        return mTelephonyManager.getPhoneType() != TelephonyManager.PHONE_TYPE_NONE &&
                mTelephonyManager.getSimState() != TelephonyManager.SIM_STATE_ABSENT;
    }

    private void displayError(String error) {
        Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
    }

    private int checkCarrierPrivileges() {
        findViewById(R.id.loadingPanel).setVisibility(View.VISIBLE);

        mTelephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        int tries = 5;
        while(true) {
            if (!isSimCardPresent() &&
                    mTelephonyManager.getSimState() != TelephonyManager.SIM_STATE_READY &&
                    tries >= 0) {
                tries--;
                try {
                    Thread.sleep(2000);

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                break;
            }
        }
        if (tries < 0) {
            displayError("Waited for 10 seconds, but SIM was not ready. Try relaunching the app.");
            findViewById(R.id.loadingPanel).setVisibility(View.GONE);
            return -1;
        }

        tries = 5;
        while(true) {
            if (mTelephonyManager.hasCarrierPrivileges() == false && tries >= 0) {
                tries--;
                try {
                    Thread.sleep(2000);

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                break;
            }
        }
        if (tries < 0) {
            displayError("App does not have Carrier Privileges");
            findViewById(R.id.loadingPanel).setVisibility(View.GONE);
            return -1;
        }

        findViewById(R.id.loadingPanel).setVisibility(View.GONE);
        return 0;
    }
}
