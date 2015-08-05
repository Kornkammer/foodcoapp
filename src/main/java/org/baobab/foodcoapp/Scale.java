package org.baobab.foodcoapp;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import java.util.HashMap;

public class Scale implements Runnable {

    private static final String TAG = "Scale";
    private static final int PID = 32773;
    private static final int VID = 2338;
    public static final String USB_PERMISSION = "org.baobab.foodcoapp.USB_PERMISSION";

    private final UsbManager mUsbManager;
    private PendingIntent mPermissionIntent;
    private final AppCompatActivity activity;
    private boolean mRunning;
    private UsbDevice mDevice;
    private UsbEndpoint mEndpointIntr;
    private UsbDeviceConnection mConnection;
    private int mWeightLimit;

    public interface ScaleListener {
        public void onWeight(String weight);
    }

    public Scale(AppCompatActivity activity) {
        mUsbManager = (UsbManager) activity.getSystemService(Context.USB_SERVICE);
        this.activity = activity;
        Intent intent = activity.getIntent();
        if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(intent.getAction())) {
            UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            Toast.makeText(activity, "ATTACH startet app", Toast.LENGTH_LONG).show();
            setDevice(device);
        } else {
            searchForDevice();
        }
    }

    public void registerForDetach() {
        IntentFilter filter = new IntentFilter("org.baobab.foodcoapp.USB_PERMISSION");
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        activity.registerReceiver(mUsbReceiver, filter);

    }
    private BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            //If our device is detached, disconnect
            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                Toast.makeText(activity, "DETACHED", Toast.LENGTH_LONG).show();
                Log.i(TAG, "Device Detached");
                if (mDevice != null && mDevice.equals(device)) {
                    setDevice(null);
                }
            }
            mPermissionIntent = PendingIntent.getBroadcast(activity, 0, new Intent("org.baobab.foodcoapp.USB_PERMISSION"), 0);
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                Toast.makeText(activity, "ATTACHED", Toast.LENGTH_LONG).show();
                Log.i(TAG, "Device Attached");
                mUsbManager.requestPermission(device, mPermissionIntent);
            }
            //If this is our permission request, check result
            if (USB_PERMISSION.equals(action)) {
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                        && device != null) {
                    //Connect to the device
                    setDevice(device);
                } else {
                    Toast.makeText(activity, "permission denied", Toast.LENGTH_LONG).show();
                    Log.d(TAG, "permission denied for device " + device);
                    setDevice(null);
                }
            }
        }
    };

    private void searchForDevice() {
        HashMap<String, UsbDevice> devices = mUsbManager.getDeviceList();
        UsbDevice selected = null;
        for (UsbDevice device : devices.values()) {
            Toast.makeText(activity, "searching usb scale ", Toast.LENGTH_LONG).show();
            if (device.getVendorId() == VID && device.getProductId() == PID) {
                selected = device;
                break;
            }
        }
        //Request a connection
        if (selected != null) {
            if (mUsbManager.hasPermission(selected)) {
                setDevice(selected);
            } else {
                Toast.makeText(activity, "request permission", Toast.LENGTH_LONG).show();
                mUsbManager.requestPermission(selected, mPermissionIntent);
            }
        }
    }

    private void setDevice(UsbDevice device) {
        if (device == null) {
            mConnection = null;
            mRunning = false;
            return;
        }
        Log.d(TAG, "setDevice " + device.getDeviceName());

        if (device.getInterfaceCount() != 1) {
            Toast.makeText(activity, "no usb interface", Toast.LENGTH_LONG).show();
            Log.e(TAG, "no usb interface");
            return;
        }
        UsbInterface intf = device.getInterface(0);
        if (intf.getEndpointCount() != 1) {
            Toast.makeText(activity, "no usb interface endpoint", Toast.LENGTH_LONG).show();
            Log.e(TAG, "no usb interface endpoint");
            return;
        }
        UsbEndpoint ep = intf.getEndpoint(0);
        if (ep.getType() != UsbConstants.USB_ENDPOINT_XFER_INT
                || ep.getDirection() != UsbConstants.USB_DIR_IN) {
            Toast.makeText(activity, "endpoint is not Interrupt IN", Toast.LENGTH_LONG).show();
            Log.e(TAG, "endpoint is not Interrupt IN");
            return;
        }
        mDevice = device;
        mEndpointIntr = ep;

        UsbDeviceConnection connection = mUsbManager.openDevice(device);
        if (connection != null && connection.claimInterface(intf, true)) {
            mConnection = connection;
            updateAttributesData(mConnection);
            updateLimitData(mConnection);

            mRunning = true;
            Thread thread = new Thread(null, this, "ScaleMonitor");
            thread.start();
        } else {
            mConnection = null;
            mRunning = false;
        }
    }

    @Override
    public void run() {
//        byte[] buffer = new byte[6];
        while (mRunning) {
            int requestType = 0xA1; // 1010 0001b
            int request = 0x01; //HID GET_REPORT
            int value = 0x0103; //Input report, ID = 3
            int index = 0; //Interface 0
            int length = 6;
            byte[] buffer = new byte[6];
            mConnection.controlTransfer(requestType, request, value, index, buffer, length, 2000);
            mHandler.sendMessage(Message.obtain(mHandler, MSG_DATA, buffer));

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Log.w(TAG, "Read Interrupted");
            }
        }
    }
    int counter = 0;
    private static final int MSG_DATA = 101;
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_DATA:
                    byte[] data = (byte[]) msg.obj;
                    Log.d(TAG, "Raw: " + bytesToHex(data));
                    byte reportId = data[0];
                    byte status = data[1];
                    int units = (data[2] & 0xFF);
                    byte scaling = data[3];

                    //Two byte value representing the weight itself
                    int weight = (data[5] & 0xFF) << 8;
                    weight += (data[4] & 0xFF);
                    ((ScaleListener) activity).onWeight(++counter + "status=" + status + " unit=" + units +" WEIGHT: " + weight);
                    Log.d(TAG, counter + "status=" + status + " unit=" + units +" WEIGHT: " + weight);


                    //Validate result
//                    if (mWeightLimit > 0 && weight > mWeightLimit) {
//                        Toast.makeText(activity, "TOO Much!!! " + weight, Toast.LENGTH_LONG).show();
//                        return;
//                    }

//                    switch (units) {
//                        case UNITS_GRAM:
//                            updateWeightGrams(weight);
//                            break;
//                        case UNITS_OUNCE:
//                            updateWeightPounds(weight);
//                            break;
//                        default:
//                            mWeight.setText("---");
//                            break;
//                    }
                    break;
                default:
                    break;
            }
        }
    };

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for(byte b : bytes) {
            Log.d("Foo", "byte: " + b);
            sb.append(String.format("%02X ", b));
        }
        return sb.toString();
    }

    private void updateAttributesData(UsbDeviceConnection connection) {
        int requestType = 0xA1; // 1010 0001b
        int request = 0x01; //HID GET_REPORT
        int value = 0x0301; //Feature report, ID = 1
        int index = 0; //Interface 0
        int length = 3;

        byte[] buffer = new byte[length];
        connection.controlTransfer(requestType, request, value, index, buffer, length, 2000);

        byte reportId = buffer[0]; //Always 0x01
        int scaleClass = (buffer[1] & 0xFF);
        int defaultUnits = (buffer[2] & 0xFF);

        String message = String.format("Class: %d, Default Units: %s", scaleClass, getUnitDisplayString(defaultUnits));
//        Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
        Log.d(TAG, message);
    }

    private void updateLimitData(UsbDeviceConnection connection) {
        int requestType = 0xA1; // 1010 0001b
        int request = 0x01; //HID GET_REPORT
        int value = 0x0305; //Feature report, ID = 5
        int index = 0; //Interface 0
        int length = 5;

        byte[] buffer = new byte[length];
        connection.controlTransfer(requestType, request, value, index, buffer, length, 2000);

        //Parse the data and fill the display
        byte reportId = buffer[0]; //Always 0x05
        //Maps to a units constant defined for HID POS
        int unit = (buffer[1] & 0xFF);
        //Scaling applied to the weight value, if any
        byte scaling = buffer[2];

        //Two byte value representing the weight itself
        int weight = (buffer[4] & 0xFF) << 8;
        weight += (buffer[3] & 0xFF);
        mWeightLimit = weight;

        String message = String.format("Weight Limit: %d %s", weight, getUnitDisplayString(unit));
//        Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
        Log.d(TAG, message);
    }

    private String getUnitDisplayString(int unitConstant) {
        switch (unitConstant) {
            case UNITS_MILLIGRAM:
                return "Milligrams";
            case UNITS_GRAM:
                return "Grams";
            case UNITS_KILOGRAM:
                return "Kilograms";
            case UNITS_CARAT:
                return "Carats";
            case UNITS_TAEL:
                return "Taels";
            case UNITS_GRAIN:
                return "Grains";
            case UNITS_PENNYWEIGHT:
                return "Pennyweights";
            case UNITS_MTON:
                return "Metric Tons";
            case UNITS_ATON:
                return "Avoir Tons";
            case UNITS_TROYOUNCE:
                return "Troy Ounces";
            case UNITS_OUNCE:
                return "Ounces";
            case UNITS_POUND:
                return "Pounds";
            default:
                return "";
        }
    }

    // USB HIS POS Constants
    private static final int UNITS_MILLIGRAM = 0x01;
    private static final int UNITS_GRAM = 0x02;
    private static final int UNITS_KILOGRAM = 0x03;
    private static final int UNITS_CARAT = 0x04;
    private static final int UNITS_TAEL = 0x05;
    private static final int UNITS_GRAIN = 0x06;
    private static final int UNITS_PENNYWEIGHT = 0x07;
    private static final int UNITS_MTON = 0x08;
    private static final int UNITS_ATON = 0x09;
    private static final int UNITS_TROYOUNCE = 0x0A;
    private static final int UNITS_OUNCE = 0x0B;
    private static final int UNITS_POUND = 0x0C;

    private static final int STATUS_FAULT = 0x01;
    private static final int STATUS_STABLEZERO = 0x02;
    private static final int STATUS_INMOTION = 0x03;
    private static final int STATUS_STABLE = 0x04;
    private static final int STATUS_UNDERZERO = 0x05;
    private static final int STATUS_OVERLIMIT = 0x06;
    private static final int STATUS_NEEDCAL = 0x07;
    private static final int STATUS_NEEDZERO = 0x08;

}
