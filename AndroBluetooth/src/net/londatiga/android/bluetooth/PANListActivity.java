package net.londatiga.android.bluetooth;

import net.londatiga.android.bluetooth.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.provider.Settings.Secure;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.ListView;
import android.widget.Toast;

/**
 * Device list.
 * 
 * @author Lorensius W. L. T <lorenz@londatiga.net>
 *
 */
public class PANListActivity extends Activity {
	private ListView mListView;
	private PANListAdapter mAdapter;
	private ArrayList<BluetoothDevice> mDeviceList;
	
	private static final String ACTIVITY_TAG="bluet";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_pan_devices);
		
		mDeviceList		= getIntent().getExtras().getParcelableArrayList("device.list");
		
		mListView		= (ListView) findViewById(R.id.lv_paned);
		
		mAdapter		= new PANListAdapter(this);
		
		mAdapter.setData(mDeviceList);
		
		
		mAdapter.setListener(new PANListAdapter.OnPANButtonClickListener() {			
			@Override
			public void onPANButtonClick(int position) {
				BluetoothDevice device = mDeviceList.get(position);
				String normalAddress = device.getAddress();
				String[] partAddress = normalAddress.split(":");
				String reverseAddress = partAddress[5] + ":" + partAddress[4] + ":" + partAddress[3] + ":" + partAddress[2] + ":" + partAddress[1] + ":" + partAddress[0];
				ProcessBuilder processBuilder = new ProcessBuilder("su","-c pand --show");
				StringBuffer commandOutput = new StringBuffer();
        		try {
					Process process = processBuilder.start();
					InputStreamReader reader = new InputStreamReader(process.getInputStream());
	        		BufferedReader bufferedReader = new BufferedReader(reader);
	        		int numRead;
	        		char[] buffer = new char[5000];
	        		while ((numRead = bufferedReader.read(buffer)) > 0) {
	        			commandOutput.append(buffer, 0, numRead);
	        	    }
	        	    bufferedReader.close();
	        	    System.out.println(commandOutput.toString());
	        	    System.out.println("result length:" + commandOutput.length());
	        	    try {
						process.waitFor();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
        		String commandResult = commandOutput.toString();
				
				
				if (commandResult.toLowerCase().contains(reverseAddress.toLowerCase())) {
					unconnectDevice(device);
				} else {
					showToast("Connecting...");	
					connectDevice(device);
				}
			}
		});
		
		mListView.setAdapter(mAdapter);
		
		registerReceiver(mPANReceiver, new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED));
		registerReceiver(mPANReceiver, new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED));
	}
	
	@Override
	public void onDestroy() {
		unregisterReceiver(mPANReceiver);
		
		super.onDestroy();
	}
	
	
	private void showToast(String message) {
		Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
	}
	
    private void connectDevice(BluetoothDevice device) {
        try {
        	try {
        		String deviceaddr = device.getAddress();
        		Method m = device.getClass().getMethod("createRfcommSocket", new Class[] {int.class});
        		BluetoothSocket btSocket = (BluetoothSocket) m.invoke(device, 1);
        		btSocket.connect();
        		InputStream inStream = btSocket.getInputStream();
        		OutputStream outStream = btSocket.getOutputStream();
        		String message = "Hello from Android.\n";
        		byte[] msgBuffer = message.getBytes();
        		outStream.write(msgBuffer);
        		byte[] incoming = new byte[1024];
        		int ret = inStream.read(incoming);
        		StringBuilder sb = new StringBuilder(ret);
        	    for (int i = 0; i < ret; ++ i) {
        	        if (incoming[i] < 0) throw new IllegalArgumentException();
        	        sb.append((char) incoming[i]);
        	    }
        	    String ipresult = sb.toString();
        	    System.out.println(ipresult);
        	    ProcessBuilder writeIP = new ProcessBuilder("su","-c echo \"" + ipresult + "\" > /mnt/sdcard/ip.txt");
        		Process execWriteIP = writeIP.start();
        		execWriteIP.waitFor();
        		inStream.close();
        		outStream.close();
        		btSocket.close();
        		
        		ProcessBuilder processBuilder = new ProcessBuilder("su","-c id ; pand -n --connect " + deviceaddr +  " " + "--service NAP" + "; ip link set bnep0 up ;" + "ip addr add 192.168.13.2 dev bnep0 ;" + "ip route add 192.168.13.0/24 via 192.168.13.2");
        		//ProcessBuilder processBuilder = new ProcessBuilder("su","-c id");
        		Process process = processBuilder.start();
        		
        		InputStreamReader reader = new InputStreamReader(process.getInputStream());
        		BufferedReader bufferedReader = new BufferedReader(reader);
        		int numRead;
        		char[] buffer = new char[5000];
        		StringBuffer commandOutput = new StringBuffer();
        		while ((numRead = bufferedReader.read(buffer)) > 0) {
        			commandOutput.append(buffer, 0, numRead);
        	    }
        	    bufferedReader.close();
        	    System.out.println(commandOutput.toString());
        	    System.out.println("result length:" + commandOutput.length());
        	    process.waitFor();
        		
        		
        	    NetworkInterface btface = NetworkInterface.getByName("bnep0");
        	    
        	    if(btface != null)
        	    {
        	    	showToast("Connected");
        	    }
        	    else
        	    {
        	    	showToast("Connect Failed. Please Retry.");
        	    }
        	    
        	    
        	} catch (InterruptedException e) {
        	    throw new RuntimeException(e);
        	}
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void unconnectDevice(BluetoothDevice device) {
        try {
        	String deviceaddr = device.getAddress();
    		ProcessBuilder processBuilder = new ProcessBuilder("su","-c id ; pand -k " + deviceaddr);
    		Process process = processBuilder.start();
    		process.waitFor();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private final BroadcastReceiver mPANReceiver = new BroadcastReceiver() {
	    public void onReceive(Context context, Intent intent) {
	        String action = intent.getAction();
	        
	        if(BluetoothDevice.ACTION_ACL_CONNECTED.equals(action))
	        {
	        	showToast("Connected");
	        	//BluetoothDevice connectedDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
	        	try {
					NetworkInterface btface = NetworkInterface.getByName("bnep0");
				} catch (SocketException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	        }
	        else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action))
	        {
	        	showToast("Disconnected");
	        	BluetoothDevice disconnectedDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
	        	try {
					NetworkInterface btface = NetworkInterface.getByName("bnep0");
				} catch (SocketException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
                
	        }
	        mAdapter.notifyDataSetChanged();
	    }
	};
}