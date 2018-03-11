package damian.arduinoradar;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.content.ContentValues.TAG;

public class MainActivity extends Activity implements View.OnClickListener {

    public static int angle = 30;
    public static int distance = 0;
    public static int scale = 50;
    public static boolean connected = false;

    Button bOn, bOff, bList, bFind, bRadar, bDC;
    TextView text, sbValue;
    ListView lv;
    SeekBar sb;
    private ArrayList<String> list;
    private ArrayAdapter<String> adapter;
    private BluetoothAdapter BA;
    private ConnectThread connection;
    InputStream mmInputStream = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        text = (TextView)findViewById(R.id.textView);
        sbValue = (TextView)findViewById(R.id.textView2);
        bOn = (Button)findViewById(R.id.button);
        bOff = (Button)findViewById(R.id.button2);
        bList = (Button)findViewById(R.id.button3);
        bFind = (Button)findViewById(R.id.button4);
        bRadar = (Button)findViewById(R.id.button5);
        bDC = (Button)findViewById(R.id.button6);
        lv = (ListView) findViewById(R.id.listView);
        sb = (SeekBar) findViewById(R.id.seekBar);
        list = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, list);
        BA = BluetoothAdapter.getDefaultAdapter();

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        this.registerReceiver(mReceiver, filter);

        bOn.setOnClickListener(this);
        bOff.setOnClickListener(this);
        bList.setOnClickListener(this);
        bFind.setOnClickListener(this);
        bRadar.setOnClickListener(this);
        bDC.setOnClickListener(this);

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (!connected){
                    String deviceAddress = (lv.getItemAtPosition(position).toString());
                    Log.i(TAG, "Device address: " + deviceAddress);
                    Pattern pat = Pattern.compile("\\((.{17})\\)");
                    Matcher mat = pat.matcher(deviceAddress);
                    if (mat.find()) {
                        deviceAddress = mat.group(1);
                        BluetoothDevice device = BA.getRemoteDevice(deviceAddress);
                        text.setText(device.getName());
                        connection = new ConnectThread(device);
                        connection.start();
                    } else {
                        System.out.println("NO MATCH");
                    }
                } else {
                    Toast.makeText(getApplicationContext(),"Already connected to device", Toast.LENGTH_SHORT).show();
                }
            }
        });

        sb.setProgress(50);
        sb.incrementProgressBy(50);
        sb.setMax(400);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progress = progress / 50;
                progress = progress * 50;
                if (progress < 50) {
                    progress = 50;
                }
                sb.setProgress(progress);
                sbValue.setText(String.valueOf(progress));
                scale = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
}

    //The BroadcastReceiver that listens for bluetooth broadcasts
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                list.add(device.getName() + " (" + device.getAddress() + ")");
                lv.setAdapter(adapter);
            }
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Toast.makeText(getApplicationContext(),"Searching complete", Toast.LENGTH_SHORT).show();
            }
            else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                Toast.makeText(getApplicationContext(),"Connected to device", Toast.LENGTH_SHORT).show();
                text.setTextColor(Color.GREEN);
                connected = true;
            }
            else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                Toast.makeText(getApplicationContext(),"Disconnected from device", Toast.LENGTH_SHORT).show();
                text.setTextColor(Color.BLACK);
                connected = false;
            }
            else if (BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(action)) {
                Toast.makeText(getApplicationContext(),"Disconnect Request!", Toast.LENGTH_SHORT).show();
                text.setTextColor(Color.BLACK);
                connected = false;
            }
        }
    };

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.button:
                if (!BA.isEnabled()) {
                    Intent turnOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(turnOn, 0);
                }
                else {
                    Toast.makeText(getApplicationContext(),"Bluetooth already ON",Toast.LENGTH_SHORT).show();
                }
                break;

            case R.id.button2:
                if (BA.isEnabled()) {
                    BA.disable();
                    list.clear();
                    lv.setAdapter(adapter);
                    Toast.makeText(getApplicationContext(),"Bluetooth OFF", Toast.LENGTH_SHORT).show();
                }
                else {
                    Toast.makeText(getApplicationContext(),"Bluetooth already OFF", Toast.LENGTH_SHORT).show();
                }
                break;

            case R.id.button3:
                if (BA.isEnabled()) {
                    if (BA.isDiscovering()) {
                        BA.cancelDiscovery();
                    }
                    list.clear();
                    Set<BluetoothDevice> pairedDevices = BA.getBondedDevices();
                    for (BluetoothDevice device : pairedDevices) {
                        list.add(device.getName() + " (" + device.getAddress() + ")");
                    }
                    lv.setAdapter(adapter);
                }
                else {
                    Toast.makeText(getApplicationContext(),"Turn ON Bluetooth!", Toast.LENGTH_SHORT).show();
                }
                break;

            case R.id.button4:
                if (BA.isEnabled()) {
                    if (!BA.isDiscovering()){
                        list.clear();
                        lv.setAdapter(adapter);
                        BA.startDiscovery();
                        Toast.makeText(getApplicationContext(),"Searching BT devices", Toast.LENGTH_LONG).show();
                    }
                    else {
                        BA.cancelDiscovery();
                        Toast.makeText(getApplicationContext(),"Searching cancel", Toast.LENGTH_SHORT).show();
                    }
                }
                else {
                    Toast.makeText(getApplicationContext(),"Turn ON Bluetooth!", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.button5:
                if (connected){
                    startRadar(v);
                } else {
                    Toast.makeText(getApplicationContext(),"Connect to device first!", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.button6:
                if (connected){
                    connection.cancel();
                    connection.interrupt();
                } else {
                    Toast.makeText(getApplicationContext(),"No devices connected!", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    public void startRadar (View view){
        Intent intent = new Intent(this, RadarActivity.class);
        startActivity(intent);
    }

    private class ConnectThread extends Thread {

        private byte[] mmBuffer;
        private final BluetoothSocket bSocket;

        private ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            BluetoothSocket tmp = null;
            final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                Log.i(TAG, "Device Name: " + device.getName());
                Log.i(TAG, "Device UUID: " + device.getUuids()[0].getUuid());
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }

            bSocket = tmp;
        }

        public void run() {
            mmBuffer = new byte[1024];
            int numBytes; // bytes returned from read()

            // Cancel discovery because it otherwise slows down the connection.
            if (BA.isDiscovering()) {
                BA.cancelDiscovery();
            }

            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                bSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    Log.d(TAG, "closing connection");
                    bSocket.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
            }
            //Log.i(TAG, "CONNECTED");
            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            // new ManageConnection(bSocket, mHandler);

            try {
                mmInputStream = bSocket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating input stream", e);
            }
            String message = "";
            while (true) {
                try {
                    // Read from the InputStream.
                    int bytesAvailable = mmInputStream.available();
                    if(bytesAvailable > 0) {
                        numBytes = mmInputStream.read(mmBuffer);
                        String readMessage = new String(mmBuffer, 0, numBytes);
                        message += readMessage;
                        Pattern twopart = Pattern.compile("\\((\\d+):(\\d+)\\)");
                        Matcher m = twopart.matcher(message);
                        if (m.find()) {
                            angle = Integer.parseInt(m.group(1));
                            distance = Integer.parseInt(m.group(2));
                            message = "";
                        }
                    }
                } catch (IOException e) {
                    Log.d(TAG, "Input stream was disconnected", e);
                    break;
                }
            }

        }

        // Closes the client socket and causes the thread to finish.
        private void cancel() {
            try {
                bSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
            Log.i(TAG, "SOCKET CLOSED");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (BA.isDiscovering()) {
            BA.cancelDiscovery();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (BA.isDiscovering()) {
            BA.cancelDiscovery();
        }
        unregisterReceiver(mReceiver);
    }

}
