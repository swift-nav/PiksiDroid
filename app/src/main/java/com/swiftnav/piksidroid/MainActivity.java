package com.swiftnav.piksidroid;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.ftdi.j2xx.D2xxManager;
import com.ftdi.j2xx.FT_Device;

import java.util.HashMap;
import java.util.Iterator;

public class MainActivity extends ActionBarActivity {
	public String TAG = "PiksiDroid";
	private static final String ACTION_USB_PERMISSION =
			"com.android.example.USB_PERMISSION";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		UsbManager mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
		PendingIntent mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
		IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
		registerReceiver(mUsbReceiver, filter);

		UsbDevice piksidev = null;
		HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
		Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
		while(deviceIterator.hasNext()){
			UsbDevice device = deviceIterator.next();
			if ((device.getVendorId() == 0x403) && (device.getProductId() == 0x6014))
				mUsbManager.requestPermission(device, mPermissionIntent);
				piksidev = device;
		}
		if (piksidev == null) {
			Log.d(TAG, "No Piksi connected!");
		}
		try {
			D2xxManager d2xx = D2xxManager.getInstance(this);

			int devCount = 0;
			devCount = d2xx.createDeviceInfoList(this);
			D2xxManager.FtDeviceInfoListNode[] devList = new D2xxManager.FtDeviceInfoListNode[devCount];
			d2xx.getDeviceInfoList(devCount, devList);
			FT_Device piksi = d2xx.openByIndex(this, 0);

			if (piksi == null) {
				D2xxManager.D2xxException myException = new D2xxManager.D2xxException("Cannot open device!");
				throw myException;
			}
			if (!piksi.setBaudRate(Utils.baudrate)) {
				D2xxManager.D2xxException myException = new D2xxManager.D2xxException("Cannot set baudrate!!");
				throw myException;
			}

			byte foo[] = new byte[100];
			int i = piksi.read(foo, 100, 5000);
			Log.d(TAG, "" + i);
			Log.d(TAG, HexDump.dumpHexString(foo));
		} catch (D2xxManager.D2xxException e) {
			Log.d(TAG, e.toString());
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		//noinspection SimplifiableIfStatement
		if (id == R.id.action_settings) {
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {

		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (ACTION_USB_PERMISSION.equals(action)) {
				synchronized (this) {
					UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

					if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
						if(device != null){
							Log.d(TAG, "We have OK!");
						}
					}
					else {
						Log.d(TAG, "permission denied for device " + device);
					}
				}
			}
		}
	};
}
