package com.iobeam.samples.android.wifirssi;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.iobeam.api.ApiException;
import com.iobeam.api.client.DataCallback;
import com.iobeam.api.client.Iobeam;
import com.iobeam.api.client.RegisterCallback;
import com.iobeam.api.client.RestRequest;
import com.iobeam.api.resource.DataPoint;

import java.util.concurrent.TimeUnit;

/**
 * This is an example activity of how to use the iobeam Java client library. This example will
 * measure the RSSI of the WiFi at periodic intervals, batch them into an import request, and import
 * that data periodically. By default, readings are taken every 20s and are send to the iobeam Cloud
 * once there are 3 readings.
 */
public class IobeamActivity extends ActionBarActivity implements Handler.Callback {

    private static final String LOG_TAG = "IobeamActivity";

    private static final String SERIES_NAME = "rssi";
    private static final String KEY_DEVICE_ID = "device_id";

    private static final int MSG_GET_RSSI = 0;
    private static final int MSG_SEND_SUCCESS = 1;  // sent if data upload succeeds
    private static final int MSG_SEND_FAILURE = 2;  // sent if data upload fails
    private static final int MSG_REGISTER_SUCCESS = 3;  // sent if registration succeeds
    private static final int MSG_REGISTER_FAILURE = 4;  // sent if registration fails
    private static final long DELAY = TimeUnit.SECONDS.toMillis(20);  // take measurement every 20s

    private WifiManager mWifiManager;
    private Handler mHandler;
    private DataCallback mDataCallback;
    private String mDeviceId;
    private boolean mCanSend = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initIobeam();

        // Setup app to get RSSI measurements and kick off the measurement loop. The Handler also
        // will be used for callbacks from registration (if needed) or from sending data.
        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        mHandler = new Handler(this);
        mHandler.sendEmptyMessage(MSG_GET_RSSI);

        initDataCallback();
        setContentView(R.layout.activity_main);
    }

    /**
     * Initializes the Iobeam client library, including registering for a new device ID if needed.
     */
    private void initIobeam() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String path = getFilesDir().getAbsolutePath();
        // projectId and token are defined in the res/values/iobeam_config.xml file.
        int projectId = getResources().getInteger(R.integer.iobeam_project_id);
        String token = getString(R.string.iobeam_project_token);
        mDeviceId = prefs.getString(KEY_DEVICE_ID, null);

        try {
            Iobeam.init(path, projectId, token, mDeviceId);
            mDeviceId = Iobeam.getDeviceId();

            // If the device ID has not been set for this device yet, register for one. The callback
            // will send messages to mHandler to process.
            if (mDeviceId == null) {
                RegisterCallback cb = new RegisterCallback() {
                    @Override
                    public void onSuccess(String deviceId) {
                        Message m = new Message();
                        m.what = MSG_REGISTER_SUCCESS;
                        m.getData().putString(KEY_DEVICE_ID, deviceId);
                        mHandler.sendMessage(m);
                        mCanSend = true;
                        mDeviceId = deviceId;
                    }

                    @Override
                    public void onFailure(Throwable throwable, RestRequest restRequest) {
                        mHandler.sendEmptyMessage(MSG_REGISTER_FAILURE);
                        mCanSend = false;
                        mDeviceId = null;
                    }
                };
                Iobeam.registerDeviceAsync(cb);
            }
        } catch (ApiException e) {
            e.printStackTrace();
            mCanSend = false;
        }
    }


    private void initDataCallback() {
        // This callback notifies mHandler of success or failure.
        mDataCallback = new DataCallback() {
            @Override
            public void onSuccess() {
                mHandler.sendEmptyMessage(MSG_SEND_SUCCESS);
            }

            @Override
            public void onFailure(Throwable throwable, RestRequest restRequest) {
                mHandler.sendEmptyMessage(MSG_SEND_FAILURE);
            }
        };
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeMessages(MSG_GET_RSSI);
        mHandler.removeMessages(MSG_SEND_SUCCESS);
        mHandler.removeMessages(MSG_SEND_FAILURE);
        mHandler = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Adds a DataPoint to an ongoing ImportTask, launching or creating the task as necessary.
     * @param d Data to be added.
     */
    private void addDataPoint(DataPoint d) {
        Log.v(LOG_TAG, "data: " + d);
        Iobeam.addData(SERIES_NAME, d);

        if (mCanSend && Iobeam.getDataSize(SERIES_NAME) >= 3) {
            try {
                Iobeam.sendAsync(mDataCallback);
            } catch (ApiException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean handleMessage(Message m) {
        switch (m.what) {
            case MSG_GET_RSSI:
                mHandler.removeMessages(MSG_GET_RSSI);  // remove spurious messages
                // Get current RSSI value, add to import task, then schedule next reading.
                int rssi = mWifiManager.getConnectionInfo().getRssi();
                DataPoint d = new DataPoint(System.currentTimeMillis(), rssi);
                addDataPoint(d);
                mHandler.sendEmptyMessageDelayed(MSG_GET_RSSI, DELAY);
                return true;
            case MSG_SEND_SUCCESS:
                Log.d(LOG_TAG, "Send succeeded");
                return true;
            case MSG_SEND_FAILURE:
                Log.d(LOG_TAG, "Send failed.");
                return true;
            case MSG_REGISTER_SUCCESS:
                // The Iobeam client persists the device ID, but we do as well in case we need it.
                String deviceId = m.getData().getString(KEY_DEVICE_ID);
                PreferenceManager.getDefaultSharedPreferences(this).edit()
                        .putString(KEY_DEVICE_ID, deviceId).apply();
                Log.d(LOG_TAG, "Registered device: " + deviceId);
                return true;
            case MSG_REGISTER_FAILURE:
                Log.d(LOG_TAG, "Register failed.");
                return true;
            default:
                return false;
        }
    }
}
