/*
 * Copyright (C) 2013 Keisuke Suzuki
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Sample code for FPGA Magazine vol.2
 */
package com.physicaloid.android2fpga;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.ftdi.j2xx.D2xxManager;
import com.ftdi.j2xx.FT_Device;
import com.physicaloid.ftdififoavalonstsample.R;

public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getSimpleName();

    Button      btOpen;
    Button      btClose;
    Button      btWrite;
    EditText    etAddress;
    EditText    etData;
    Button      btRead;
    TextView    tvRead;

    Handler mTvReadHandler  = new Handler();

    private D2xxManager ftD2xx  = null;
    private FT_Device   ftDev   = null;

    private static final int    USB_OPEN_INDEX  = 0;
    private static final int    MAX_READBUF_SIZE = 256;
    private static final int    READ_WAIT_MS    = 10;

    byte[] rbuf = new byte[MAX_READBUF_SIZE];
    int     mReadSize=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btOpen = (Button) findViewById(R.id.btOpen);
        btClose = (Button) findViewById(R.id.btClose);
        btWrite = (Button) findViewById(R.id.btWrite);
        btRead = (Button) findViewById(R.id.btRead);

        tvRead = (TextView) findViewById(R.id.tvRead);

        etAddress = (EditText) findViewById(R.id.etAddress);
        etData = (EditText) findViewById(R.id.etData);

        updateView(false);

        try {
            ftD2xx = D2xxManager.getInstance(this);
        } catch (D2xxManager.D2xxException ex) {
            Log.e(TAG,ex.toString());
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        closeDevice();
    }

    /**
     * Event when tapping "open" button
     * @param v view
     */
    public void onClickOpen(View v) {
        openDevice();
    }

    /**
     * Event when tapping "close" button
     * @param v view
     */
    public void onClickClose(View v) {
        closeDevice();
    }

    /**
     * Event when tapping "write" button
     * @param v view
     */
    public void onClickWrite(View v) {

        byte[] b;
        AvalonSTParser avalonSTParser = new AvalonSTParser();
        try {
            b = avalonSTParser.createPacket(etAddress.getText().toString(), etData.getText().toString());
        } catch (Exception e) {
            Toast.makeText(this, "Fail to create a packet", Toast.LENGTH_LONG).show();
            return;
        }

        synchronized (ftDev) {
            ftDev.write(b, b.length);
        }
    }

    /**
     * Event when tapping "read" button
     * @param v view
     */
    public void onClickRead(View v) {
        if(ftDev==null) return;

        mReadSize = ftDev.getQueueStatus();
        if(mReadSize <= 0) return;  // If no data then no operation / negative number is error

        if(mReadSize > MAX_READBUF_SIZE) mReadSize = MAX_READBUF_SIZE;
        synchronized (ftDev) {
            mReadSize = ftDev.read(rbuf,mReadSize,READ_WAIT_MS); // You might want to set wait_ms.
        }

        mTvReadHandler.post(new Runnable() {
            @Override
            public void run() {
                tvRead.append("("+mReadSize+") "+toHexStr(rbuf, mReadSize)+"\n");
            }
        });
    }

    /**
     * Opens the device
     */
    private void openDevice() {
        if(ftDev == null) {
            int devCount = 0;
            devCount = ftD2xx.createDeviceInfoList(this);

            Log.d(TAG, "Device number : "+ Integer.toString(devCount));

            D2xxManager.FtDeviceInfoListNode[] deviceList = new D2xxManager.FtDeviceInfoListNode[devCount];
            ftD2xx.getDeviceInfoList(devCount, deviceList);

            if(devCount <= 0) {
                return;
            }

            ftDev = ftD2xx.openByIndex(this, USB_OPEN_INDEX);
        } else {
            synchronized(ftDev) {
                ftDev = ftD2xx.openByIndex(this, USB_OPEN_INDEX);
            }
        }

        if(ftDev.isOpen()) {
            updateView(true);
            ftDev.resetDevice(); // flush any data from the device buffers
        } else {
            Toast.makeText(this, "Cannot open.", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Closes the device
     */
    private void closeDevice() {
        if(ftDev != null) {
            synchronized (ftDev) {
                ftDev.close();
            }
            updateView(false);
        }

    }

    /**
     * Updates buttons enable or disable
     * @param on true: when the device is opened , false : when the device is closed
     */
    private void updateView(boolean on) {
        if(on) {
            btOpen.setEnabled(false);
            btClose.setEnabled(true);
            btWrite.setEnabled(true);
            btRead.setEnabled(true);
        } else {
            btOpen.setEnabled(true);
            btClose.setEnabled(false);
            btWrite.setEnabled(false);
            btRead.setEnabled(false);
        }
    }

    /**
     * Converts byte array to String
     * @param b byte array
     * @param length byte number to convert
     * @return hex String
     */
    private String toHexStr(byte[] b, int length) {
        String str="";
        for(int i=0; i<length; i++) {
            str += String.format("0x%02x ", b[i]);
        }
        return str;
    }
}
