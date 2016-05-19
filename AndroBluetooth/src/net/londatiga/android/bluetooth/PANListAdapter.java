package net.londatiga.android.bluetooth;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import net.londatiga.android.bluetooth.R;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

/**
 * Device list adapter.
 * 
 * @author Lorensius W. L. T <lorenz@londatiga.net>
 *
 */
public class PANListAdapter extends BaseAdapter{
	private LayoutInflater mInflater;	
	private List<BluetoothDevice> mData;
	private OnPANButtonClickListener mListener;
	
	public PANListAdapter(Context context) { 
        mInflater = LayoutInflater.from(context);        
    }
	
	public void setData(List<BluetoothDevice> data) {
		mData = data;
	}
	
	
	
	public void setListener(OnPANButtonClickListener listener) {
		mListener = listener;
	}
	
	public int getCount() {
		return (mData == null) ? 0 : mData.size();
	}

	public Object getItem(int position) {
		return null;
	}

	public long getItemId(int position) {
		return position;
	}

	public View getView(final int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		if (convertView == null) {			
			convertView			=  mInflater.inflate(R.layout.list_pan_device, null);
			
			holder 				= new ViewHolder();
			
			holder.nameTv		= (TextView) convertView.findViewById(R.id.tv_name);
			holder.addressTv 	= (TextView) convertView.findViewById(R.id.tv_address);
			holder.panBtn		= (Button) convertView.findViewById(R.id.btn_pan);
			
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		
		BluetoothDevice device	= mData.get(position);
			
		holder.nameTv.setText(device.getName());
		holder.addressTv.setText(device.getAddress());
		
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
		
		
		
		
		if(commandResult.toLowerCase().contains(reverseAddress.toLowerCase()))
		{
			holder.panBtn.setText("Disconnect");
		}
		else
		{
			holder.panBtn.setText("Connect");
		}
		
		holder.panBtn.setOnClickListener(new View.OnClickListener() {			
			@Override
			public void onClick(View v) {
				if (mListener != null) {
					mListener.onPANButtonClick(position);
				}
			}
		});
		
        return convertView;
	}

	static class ViewHolder {
		TextView nameTv;
		TextView addressTv;
		TextView panBtn;
	}
	
	public interface OnPANButtonClickListener {
		public abstract void onPANButtonClick(int position);
	}
}