package com.bixolon.labelprintersample;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.bixolon.commonlib.BXLCommonConst;
import com.bixolon.commonlib.common.BXLFileHelper;
import com.bixolon.commonlib.downloader.XDownloader;
import com.bixolon.commonlib.log.LogService;
import com.bixolon.labelprinter.BixolonLabelPrinter;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileUtils;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.StrictMode;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import kotlin.Unit;

@SuppressLint({"HandlerLeak", "NewApi"})
public class MainActivity extends ListActivity {
	private static final String[] FUNCTIONS = {"drawText", "drawVectorFontText", "draw1dBarcode", "drawMaxicode", "drawPdf417", "drawQrCode", "drawDataMatrix", "drawBlock", "drawCircle", "setCharacterSet", "setPrintingType", "setMargin", "setLength", "setWidth", "setBufferMode", "clearBuffer", "setSpeed", "setDensity", "setOrientation", "setOffset", "setCutterPosition", "drawBitmap", "initializePrinter", "printInformation", "setAutoCutter", "getStatus", "getPrinterInformation", "executeDirectIo", "printSample", "drawPDF", "setupRFID", "calibrateRFID", "setRFIDPosition", "writeRFID", "emptyLabelPrint", "disableInactivityTime", "setLeftMarginPosition", "transferFile", "printerFirmwareDownload", "wlanFirmwareDownload", "WLAN Setting"};

	// Name of the connected device
	private String mConnectedDeviceName = null;
	private final int REQUEST_PERMISSION = 0;
	private final int REQUEST_FIRMWARE_SELECT = 10;
	private final int REQUEST_TRANSFER_FILE = 11;
	private final int REQUEST_WLAN_FIRMWARE = 12;
	private ListView mListView;

	private boolean mIsConnected;

	static BixolonLabelPrinter mBixolonLabelPrinter;
	static int count = 1;
	private boolean checkedManufacture = false;

	public Handler m_hHandler = null;
	public BluetoothAdapter m_BluetoothAdapter = null;
	public BluetoothLeScanner mLEScanner = null;
	public ScanSettings settings = null;
	public List<ScanFilter> filters;
	public ArrayAdapter<String> adapter = null;
	public ArrayList<BluetoothDevice> m_LeDevices;

	private ScanCallback mScanCallback;
	private final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
	private PendingIntent mPermissionIntent;
	private UsbManager usbManager;
	private UsbDevice device;
	private boolean tryedAutoConnect = true;

	private AlertDialog progressDialog;

	private String[] permissions = {
			Manifest.permission.READ_EXTERNAL_STORAGE,
			Manifest.permission.WRITE_EXTERNAL_STORAGE,
			Manifest.permission.ACCESS_COARSE_LOCATION,
			Manifest.permission.ACCESS_FINE_LOCATION,
	};

	private static Set<BluetoothDevice> discoverDevices = new HashSet<>();
	private static Context mContext = null;

	static {
		try {
			System.loadLibrary("bxl_common");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mContext = getApplicationContext();
		MainActivity.count = 1;
		ArrayList<String> list = new ArrayList<>();
		for (int i = 0; i < FUNCTIONS.length; i++) {
			list.add(FUNCTIONS[i]);
		}
		ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, list);
		mListView = findViewById(android.R.id.list);
		mListView.setAdapter(adapter);
//		mListView.setEnabled(false);

		mBixolonLabelPrinter = new BixolonLabelPrinter(this, mHandler, Looper.getMainLooper());
		LogService.InitDebugLog(true, true, BXLCommonConst._LOG_LEVEL_HIGH);

		final int ANDROID_NOUGAT = 24;
		if (Build.VERSION.SDK_INT >= ANDROID_NOUGAT) {
			StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
			StrictMode.setThreadPolicy(policy);
		}

		checkVerify();
		usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
		Thread.setDefaultUncaughtExceptionHandler(new SampleUncaughtExceptionHandler());

		String privatePath = getFilesDir().getPath();

		if (!BXLFileHelper.ExistDirectory(privatePath)) {
			BXLFileHelper.CreateDirectory(privatePath);
		}

		mBixolonLabelPrinter.setTransferFileListener(transferFileListener);
		isConnectedPrinter();
	}

	@Override
	public void onResume() {
		super.onResume();

//		isConnectedPrinter();
	}

	@Override
	public void onDestroy() {
		try {
			unregisterReceiver(mUsbReceiver);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
		mBixolonLabelPrinter.disconnect();
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (mIsConnected) {

		} else {
			menu.getItem(0).setEnabled(true);
			menu.getItem(1).setEnabled(true);
			menu.getItem(2).setEnabled(true);
			menu.getItem(3).setEnabled(true);
			menu.getItem(4).setEnabled(true);
			menu.getItem(5).setEnabled(false);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.item1:
				mBixolonLabelPrinter.findBluetoothPrinters();
				break;

			case R.id.item2:
				if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
					Toast.makeText(this, "BluetoothLE Not Supported", Toast.LENGTH_SHORT).show();
					return false;
				}

				final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
				m_BluetoothAdapter = bluetoothManager.getAdapter();

				// Checks if Bluetooth is supported on the device.
				if (m_BluetoothAdapter == null) {
					Toast.makeText(this, "Error Bluetooth Not Supported", Toast.LENGTH_SHORT).show();

					return false;
				}

				if (Build.VERSION.SDK_INT >= 21) {
					mLEScanner = m_BluetoothAdapter.getBluetoothLeScanner();
					settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_POWER).build();
					//settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_OPPORTUNISTIC).build();
					filters = new ArrayList<ScanFilter>();
				}

				m_hHandler = new Handler();
				adapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.select_dialog_singlechoice);
				m_LeDevices = new ArrayList<BluetoothDevice>();

				adapter.clear();
				m_LeDevices.clear();
				scanLeDevice(true);

				AlertDialog.Builder alertBuilder = new AlertDialog.Builder(MainActivity.this);
				alertBuilder.setIcon(R.drawable.ic_launcher);
				alertBuilder.setTitle("연결할 프린터를 선택하세요");

				alertBuilder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});

				alertBuilder.setAdapter(adapter, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						scanLeDevice(false);
						BluetoothDevice btDevice = m_LeDevices.get(id);
						mConnectedDeviceName = btDevice.getName();
						new Thread(() -> {
							mBixolonLabelPrinter.connect(btDevice.getAddress(), BXLCommonConst._PORT_BLUETOOTH_LE);
						}).start();
						dialog.dismiss();
					}
				});
				alertBuilder.show();
				break;

			case R.id.item3:
				mBixolonLabelPrinter.findNetworkPrinters(3);
				return true;

			case R.id.item4:
				DialogManager.showWifiDialog(MainActivity.this, mBixolonLabelPrinter);
				return true;

			case R.id.item5:
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
					mBixolonLabelPrinter.findUsbPrinters();
				}
				return true;

			case R.id.item6:
				mBixolonLabelPrinter.disconnect();

				setStatus(R.string.title_not_connected);
				//mListView.setEnabled(false);
				mIsConnected = false;
				invalidateOptionsMenu();
				return true;

		}
		return false;
	}

	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	public void scanLeDevice(final boolean bEnable) {
		if (bEnable) {
			if (Build.VERSION.SDK_INT >= 21) {
				try {
					setScanCallback();
				} catch (NoSuchMethodException e) {
					e.printStackTrace();
				}
			}
			// Stops scanning after a pre-defined scan period.
			m_hHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					if (Build.VERSION.SDK_INT < 21) {
						m_BluetoothAdapter.stopLeScan(m_LeScanCallback);
					} else {
						mLEScanner.stopScan(mScanCallback);
					}
				}
			}, 10000);

			if (Build.VERSION.SDK_INT < 21) {
				m_BluetoothAdapter.startLeScan(m_LeScanCallback);
			} else {
				mLEScanner.startScan(filters, settings, mScanCallback);
			}
		} else {
			if (Build.VERSION.SDK_INT < 21) {
				m_BluetoothAdapter.stopLeScan(m_LeScanCallback);
			} else {
				mLEScanner.stopScan(mScanCallback);
			}
		}
	}



	private void setScanCallback() throws NoSuchMethodException {
		mScanCallback = new ScanCallback() {
			@Override
			public void onScanResult(int callbackType, ScanResult result) {
				super.onScanResult(callbackType, result);

				Log.i("callbackType", String.valueOf(callbackType));
				Log.i("result", result.toString());
				BluetoothDevice btDevice = result.getDevice();

				if (!m_LeDevices.contains(btDevice) && (btDevice.getAddress().startsWith("74:F0:7D") || btDevice.getAddress().startsWith("40:19:20"))) {
					m_LeDevices.add(btDevice);
					adapter.add(btDevice.getName() + "\n" + btDevice.getAddress());
				} else {
					return;
				}
			}

			@Override
			public void onBatchScanResults(List<ScanResult> results) {
				super.onBatchScanResults(results);

				for (ScanResult sr : results) {
					Log.i("ScanResult - Results", sr.toString());
				}
			}

			@Override
			public void onScanFailed(int errorCode) {
				super.onScanFailed(errorCode);

				Log.e("Scan Failed", "Error Code: " + errorCode);
			}
		};
	}

	public BluetoothAdapter.LeScanCallback m_LeScanCallback = new BluetoothAdapter.LeScanCallback() {
		@Override
		public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (!m_LeDevices.contains(device)) {
						m_LeDevices.add(device);
						adapter.add(device.getName() + "\n" + device.getAddress());
					}
				}
			});
		}
	};

	Intent intent;

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		switch (position) {
			case 0: // drawText
				Intent intent = new Intent(MainActivity.this, DrawTextActivity.class);
				startActivity(intent);
				break;

			case 1: // drawVectorFontText
				intent = new Intent(MainActivity.this, DrawVectorTextActivity.class);
				startActivity(intent);
				break;

			case 2: // draw1dBarcode
				intent = new Intent(MainActivity.this, Draw1dBarcodeActivity.class);
				startActivity(intent);
				break;

			case 3: // drawMaxicode
				intent = new Intent(MainActivity.this, DrawMaxicodeActivity.class);
				startActivity(intent);
				break;

			case 4: // drawPdf417
				intent = new Intent(MainActivity.this, DrawPdf417Activity.class);
				startActivity(intent);
				break;

			case 5: // drawQrCode
				intent = new Intent(MainActivity.this, DrawQrCodeActivity.class);
				startActivity(intent);
				break;

			case 6: // drawDataMatrix
				intent = new Intent(MainActivity.this, DrawDataMatrixActivity.class);
				startActivity(intent);
				break;

			case 7: // drawBlock
				intent = new Intent(MainActivity.this, DrawBlockActivity.class);
				startActivity(intent);
				break;

			case 8: // drawCircle
				intent = new Intent(MainActivity.this, DrawCircleActivity.class);
				startActivity(intent);
				break;

			case 9: // setCharacterSet
				intent = new Intent(MainActivity.this, CharacterSetSelectionActivity.class);
				startActivity(intent);
				break;

			case 10: // setPrintingType
				DialogManager.showSetPrintingTypeDialog(MainActivity.this);
				break;

			case 11: // setMarginValue
				DialogManager.showSetMarginValueDialog(MainActivity.this);
				break;

			case 12: // setLabelLengthAndGap
				DialogManager.showSetLabelLengthAndGapDialog(MainActivity.this);
				break;

			case 13: // setLabelWidth
				DialogManager.showSetLabelWidthDialog(MainActivity.this);
				break;

			case 14: // setBufferMode
				DialogManager.showSetBufferModeDialog(MainActivity.this);
				break;

			case 15: // clearBuffer
				mBixolonLabelPrinter.clearBuffer();
				break;

			case 16: // setSpeed
				DialogManager.showSetSpeedDialog(MainActivity.this);
				break;

			case 17: // setDensity
				DialogManager.showSetDensityDialog(MainActivity.this);
				break;

			case 18: // setOrientation
				DialogManager.showSetOrientationDialog(MainActivity.this);
				break;

			case 19: // setOffsetBetweenBlackMark
				DialogManager.showSetOffsetBetweenBlackMarkDialog(MainActivity.this);
				break;

			case 20: // setCutterPosition
				DialogManager.showCutterPositionSettingDialog(MainActivity.this);
				break;

			case 21: // drawCompressionImage
				intent = new Intent(MainActivity.this, DrawBitmapActivity.class);
				startActivity(intent);
				break;

			case 22: // initializePrinter
				mBixolonLabelPrinter.initializePrinter();
				break;

			case 23: // printInformation
				mBixolonLabelPrinter.printInformation();
				break;

			case 24: // setAutoCutter
				DialogManager.showAutoCutterDialog(MainActivity.this);
				break;

			case 25: // getStatus
				mBixolonLabelPrinter.getStatus(false);
				break;

			case 26: // getPrinterInformation
				DialogManager.showPrinterInformationDialog(MainActivity.this, mBixolonLabelPrinter);
				break;

			case 27: // executeDirectIo
				intent = new Intent(MainActivity.this, DirectIoActivity.class);
				startActivity(intent);
				break;

			case 28: // Sample Receipt
//				printSampleReceipt();
//				printLabelSample();

				mBixolonLabelPrinter.executeDirectIo(new byte[] { 0x7E, 0x4A, 0x43, 0x0D, 0x0A }, false, 0);
				break;

			case 29:
				intent = new Intent(MainActivity.this, DrawPDFActivity.class);
				startActivity(intent);
				break;

			case 30:
				DialogManager.showSetupRFID(MainActivity.this);
				break;
			case 31:
				mBixolonLabelPrinter.calibrateRFID();
				break;
			case 32:
				DialogManager.showSetRFIDPosition(MainActivity.this);
				break;
			case 33:
				DialogManager.showWriteRFID(MainActivity.this);
				break;
			case 34:
				mBixolonLabelPrinter.print(1, 1);
				break;
			case 35: // disableInactivityTime
				mBixolonLabelPrinter.disableInactivityTime(false);
				break;
			case 36: // setLeftMarginPosition
				DialogManager.showSetLeftMarginPositionDialog(MainActivity.this);
				break;
			case 37:
				selectTransferFile();
				break;
			case 38: // firmwareDownload
				selectFirmwareFile();
				break;
			case 39: // wlan module Firmware Download
				selectWLANFirmwareFile();
				break;
			case 40: // wlan setting
				intent = new Intent(MainActivity.this, WlanSettingActivity.class);
				startActivity(intent);
				break;
		}
	}

	private void selectTransferFile() {
		if (!mBixolonLabelPrinter.isConnected()) {
			Log.e("SelectFirmware", "LabelPrinter not connected");
			Toast.makeText(getApplicationContext(), "Label Printer not connected", Toast.LENGTH_SHORT).show();
			return ;
		}
		Intent selectFileIntent = new Intent(Intent.ACTION_GET_CONTENT);
		selectFileIntent.setType("text/plain");
		selectFileIntent.setType("application/octet-stream");
		startActivityForResult(Intent.createChooser(selectFileIntent, "Select Firmware"), REQUEST_TRANSFER_FILE);
	}

	private void selectFirmwareFile() {
		if (!mBixolonLabelPrinter.isConnected()) {
			Log.e("SelectFirmware", "LabelPrinter not connected");
			Toast.makeText(getApplicationContext(), "Label Printer not connected", Toast.LENGTH_SHORT).show();
			return ;
		}
		progressDialog = new AlertDialog.Builder(MainActivity.this)
				.setTitle("Firmware Download")
				.setMessage("Downloading in progress...")
				.create();
		Intent selectFileIntent = new Intent(Intent.ACTION_GET_CONTENT);
		selectFileIntent.setType("application/octet-stream");
		startActivityForResult(Intent.createChooser(selectFileIntent, "Select Firmware"), REQUEST_FIRMWARE_SELECT);
	}

	private void selectWLANFirmwareFile() {
		if (!mBixolonLabelPrinter.isConnected()) {
			Log.e("SelectFirmware", "LabelPrinter not connected");
			Toast.makeText(getApplicationContext(), "Label Printer not connected", Toast.LENGTH_SHORT).show();
			return ;
		}
		progressDialog = new AlertDialog.Builder(MainActivity.this)
				.setTitle("WLAN Firmware Download")
				.setMessage("Downloading in progress...")
				.create();
		Intent selectFileIntent = new Intent(Intent.ACTION_GET_CONTENT);
		selectFileIntent.setType("application/octet-stream");
		startActivityForResult(Intent.createChooser(selectFileIntent, "Select Firmware"), REQUEST_WLAN_FIRMWARE);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode != Activity.RESULT_OK) {
			Log.e("ActivityResult", "fail: " + requestCode);
			return ;
		}
		if (requestCode == REQUEST_TRANSFER_FILE) {
			transferFile(data);
		} else if (requestCode == REQUEST_FIRMWARE_SELECT) {
			downloadFirmware(data);
		} else if (requestCode == REQUEST_WLAN_FIRMWARE) {
			downloadWlanFirmware(data);
		}
	}

	private void transferFile(Intent data) {
		if (data == null) {
			Log.e("ActivityResult", "data null");
			return ;
		}
		Uri fileUri = data.getData();
		if (fileUri == null)
		{
			Log.e("ActivityResult", "file uri null");
			return ;
		}
		String tempPath = getFileTempPath(fileUri, "file-", "");
		if (tempPath == null || tempPath.isEmpty()) {
			Log.e("ActivityResult", "temp firmware file path null or empty");
			return ;
		}
		mBixolonLabelPrinter.transferFile(tempPath);
	}

	private void downloadFirmware(Intent data) {
		if (data == null) {
			Log.e("ActivityResult", "data null");
			return ;
		}
		Uri fileUri = data.getData();
		if (fileUri == null)
		{
			Log.e("ActivityResult", "file uri null");
			return ;
		}
		String tempPath = getFileTempPath(fileUri, "firmware-", ".fls");
		if (tempPath == null || tempPath.isEmpty()) {
			Log.e("ActivityResult", "temp firmware file path null or empty");
			return ;
		}
		mBixolonLabelPrinter.firmwareDownload(tempPath, new XDownloader.FirmwareCallbackFunc() {
				@Override
				public void firmwareUpdateCompleted() {
					Message message =  mHandler.obtainMessage(BixolonLabelPrinter.MESSAGE_FIRMWARE_UPDATE);
					message.arg1 = BixolonLabelPrinter.FIRMWARE_COMPLETE;
					message.sendToTarget();
				}

				@Override
				public void progressUpdate(String s, int i, int i1) {
					Message message = mHandler.obtainMessage(BixolonLabelPrinter.MESSAGE_FIRMWARE_UPDATE);
					message.arg1 = BixolonLabelPrinter.FIRMWARE_PROGRESS;
					message.obj = s + " : " + i + "/" + i1;
					message.sendToTarget();
				}

				@Override
				public void delayUpdate(int i) {
					Message message = mHandler.obtainMessage(BixolonLabelPrinter.MESSAGE_FIRMWARE_UPDATE);
					message.arg1 = BixolonLabelPrinter.FIRMWARE_DELAY_UPDATE;
					message.obj = i;
					message.sendToTarget();
				}

				@Override
				public void exceptionUpdate(String s) {
					Message message = mHandler.obtainMessage(BixolonLabelPrinter.MESSAGE_FIRMWARE_UPDATE);
					message.arg1 = BixolonLabelPrinter.FIRMWARE_EXCEPTION;
					message.obj = s;
					message.sendToTarget();
				}
			}
		);
	}

	private void downloadWlanFirmware(Intent data) {
		if (data == null) {
			Log.e("ActivityResult", "data null");
			return ;
		}
		Uri fileUri = data.getData();
		if (fileUri == null)
		{
			Log.e("ActivityResult", "file uri null");
			return ;
		}
		String tempPath = getFileTempPath(fileUri, "wlan-", ".BIN");
		if (tempPath == null || tempPath.isEmpty()) {
			Log.e("ActivityResult", "temp firmware file path null or empty");
			return ;
		}

		mBixolonLabelPrinter.wlanFirmwareDownload(tempPath, new BixolonLabelPrinter.TransferFileCallback() {
			@Override
			public void onSending(int i, int i1) {
				Message message = mHandler.obtainMessage(BixolonLabelPrinter.MESSAGE_TRANSFER_FILE);
				message.arg1 = BixolonLabelPrinter.RC_PROGRESS;
				message.obj = i + "/" + i1;
				message.sendToTarget();
				Log.d("Testing", "OnSending: " + i + "/" + i1);
			}

			@Override
			public void onSuccess() {
				Message message = mHandler.obtainMessage(BixolonLabelPrinter.MESSAGE_TRANSFER_FILE);
				message.arg1 = BixolonLabelPrinter.RC_SUCCESS;
				message.sendToTarget();
				Log.d("Testing", "OnSuccess");
			}

			@Override
			public void onFailed(int i) {
				Message message = mHandler.obtainMessage(BixolonLabelPrinter.MESSAGE_TRANSFER_FILE);
				message.arg1 = BixolonLabelPrinter.RC_FAIL;
				message.arg2 = i;
				message.sendToTarget();
				Log.d("Testing", "OnFail: " + i);
			}
		});
	}

	private String getFileTempPath(Uri firmwareUri, String prefix, String suffix) {
		String tempPath = null;
		try {
			InputStream is = getApplicationContext().getContentResolver().openInputStream(firmwareUri);
			if (is == null) {
				Log.e("ActivityResult", "file InputStream open fail");
				return tempPath;
			}
			File tempFile = File.createTempFile(
					prefix,
					suffix,
					getApplicationContext().getCacheDir()
			);
			FileOutputStream fos = new FileOutputStream(tempFile);
			tempFile.deleteOnExit();
			Log.d("ActivityResult", "tempFile path: " + tempFile.getPath());
			FileUtils.copy(is, fos);
			is.close();
			fos.close();
			tempPath = tempFile.getPath();
		} catch (IOException e) {
			Log.e("ActivityResult", "temp file exception: " + e);
			Toast.makeText(getApplicationContext(), "tempFile exception", Toast.LENGTH_SHORT).show();
		} catch (Exception e) {
			Log.e("ActivityResult", "fail: " + e);
			Toast.makeText(getApplicationContext(), "exception", Toast.LENGTH_SHORT).show();
		}
		return tempPath;
	}

	public static Set<BluetoothDevice> getPairedDevices() throws IllegalStateException {
		BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		if (bluetoothAdapter == null) {
			throw new IllegalStateException("Bluetooth is not available.");
		}

		if (!bluetoothAdapter.isEnabled()) {
			throw new IllegalStateException("Bluetooth is not enabled.");
		}

		Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
		Set<BluetoothDevice> pairedDevices = new HashSet<>();

		for (BluetoothDevice device : bondedDevices) {
			/*if ((device.getAddress().startsWith(ADDRESS_PREFIX)))*/
			{
				pairedDevices.add(device);
			}
		}

		if (pairedDevices.size() > 0) {
			return Collections.unmodifiableSet(pairedDevices);
		} else {
			return null;
		}
	}

	public boolean detectDevicePairedState(String macAddress) {
		Set<BluetoothDevice> bondedDevices = getPairedDevices();

		if(bondedDevices != null) {
			for(BluetoothDevice device : bondedDevices) {
				if(macAddress.equals(device.getAddress())){
					return true;
				}
			}
		}
		return false;
	}

	private final void setStatus(int resId)
	{
		final ActionBar actionBar = getActionBar();
		actionBar.setSubtitle(resId);
	}

	private void printSampleReceipt()
	{
		mBixolonLabelPrinter.beginTransactionPrint();
		mBixolonLabelPrinter.drawText("75-C51", 50, 1200, BixolonLabelPrinter.FONT_SIZE_24, 1, 1, 0, BixolonLabelPrinter.ROTATION_270_DEGREES, false, true, BixolonLabelPrinter.TEXT_ALIGNMENT_NONE);
		mBixolonLabelPrinter.drawText("Guri-si", 35, 900, BixolonLabelPrinter.FONT_SIZE_15, 1, 1, 0, BixolonLabelPrinter.ROTATION_270_DEGREES, false, false, BixolonLabelPrinter.TEXT_ALIGNMENT_NONE);
		mBixolonLabelPrinter.drawText("Inchang-dong", 85, 900, BixolonLabelPrinter.FONT_SIZE_15, 1, 1, 0, BixolonLabelPrinter.ROTATION_270_DEGREES, false, false, BixolonLabelPrinter.TEXT_ALIGNMENT_NONE);

		mBixolonLabelPrinter.drawText("DLVY 1026-1287-1927", 160, 1200, BixolonLabelPrinter.FONT_SIZE_10, 1, 1, 0, BixolonLabelPrinter.ROTATION_270_DEGREES, false, false, BixolonLabelPrinter.TEXT_ALIGNMENT_NONE);
		mBixolonLabelPrinter.drawText("Date   2017/12/31", 190, 1200, BixolonLabelPrinter.FONT_SIZE_10, 1, 1, 0, BixolonLabelPrinter.ROTATION_270_DEGREES, false, false, BixolonLabelPrinter.TEXT_ALIGNMENT_NONE);
		mBixolonLabelPrinter.draw1dBarcode("123456789012", 160, 850, BixolonLabelPrinter.BARCODE_CODE128, 2, 10, 60, BixolonLabelPrinter.ROTATION_270_DEGREES, BixolonLabelPrinter.HRI_NOT_PRINTED, 0);

		mBixolonLabelPrinter.drawText("Bob      010-1234-5678", 230, 1200, BixolonLabelPrinter.FONT_SIZE_12, 1, 1, 0, BixolonLabelPrinter.ROTATION_270_DEGREES, false, false, BixolonLabelPrinter.TEXT_ALIGNMENT_NONE);
		mBixolonLabelPrinter.drawText("Gyunggi-do Guri-si Inchang-dong 3912", 260, 1200, BixolonLabelPrinter.FONT_SIZE_10, 1, 1, 0, BixolonLabelPrinter.ROTATION_270_DEGREES, false, false, BixolonLabelPrinter.TEXT_ALIGNMENT_NONE);
		mBixolonLabelPrinter.drawText("Apt 204dong / 702ho", 290, 1200, BixolonLabelPrinter.FONT_SIZE_10, 1, 1, 0, BixolonLabelPrinter.ROTATION_270_DEGREES, false, false, BixolonLabelPrinter.TEXT_ALIGNMENT_NONE);
		mBixolonLabelPrinter.drawText("Nike Sport   02-468-4317", 330, 1200, BixolonLabelPrinter.FONT_SIZE_12, 1, 1, 0, BixolonLabelPrinter.ROTATION_270_DEGREES, false, false, BixolonLabelPrinter.TEXT_ALIGNMENT_NONE);
		mBixolonLabelPrinter.drawText("Seoul-si Jonglo-gu Younji-dong 361-6", 360, 1200, BixolonLabelPrinter.FONT_SIZE_10, 1, 1, 0, BixolonLabelPrinter.ROTATION_270_DEGREES, false, false, BixolonLabelPrinter.TEXT_ALIGNMENT_NONE);
		mBixolonLabelPrinter.drawText("Nike mall 2F", 390, 1200, BixolonLabelPrinter.FONT_SIZE_10, 1, 1, 0, BixolonLabelPrinter.ROTATION_270_DEGREES, false, false, BixolonLabelPrinter.TEXT_ALIGNMENT_NONE);
		mBixolonLabelPrinter.drawText("Credit", 430, 1200, BixolonLabelPrinter.FONT_SIZE_12, 1, 1, 0, BixolonLabelPrinter.ROTATION_270_DEGREES, false, true, BixolonLabelPrinter.TEXT_ALIGNMENT_NONE);
		mBixolonLabelPrinter.drawText("Airmax GX-100", 480, 1200, BixolonLabelPrinter.FONT_SIZE_12, 1, 1, 0, BixolonLabelPrinter.ROTATION_270_DEGREES, false, true, BixolonLabelPrinter.TEXT_ALIGNMENT_NONE);

		mBixolonLabelPrinter.drawText("31-C1", 30, 600, BixolonLabelPrinter.FONT_SIZE_24, 1, 1, 0, BixolonLabelPrinter.ROTATION_270_DEGREES, false, true, BixolonLabelPrinter.TEXT_ALIGNMENT_NONE);
		mBixolonLabelPrinter.drawText("Guri-si", 120, 600, BixolonLabelPrinter.FONT_SIZE_12, 1, 1, 0, BixolonLabelPrinter.ROTATION_270_DEGREES, false, false, BixolonLabelPrinter.TEXT_ALIGNMENT_NONE);
		mBixolonLabelPrinter.drawText("Inchang-dong", 170, 600, BixolonLabelPrinter.FONT_SIZE_12, 1, 1, 0, BixolonLabelPrinter.ROTATION_270_DEGREES, false, false, BixolonLabelPrinter.TEXT_ALIGNMENT_NONE);
		mBixolonLabelPrinter.drawText("BIXOLON Apt 204dong / 702ho", 220, 600, BixolonLabelPrinter.FONT_SIZE_10, 1, 1, 0, BixolonLabelPrinter.ROTATION_270_DEGREES, false, false, BixolonLabelPrinter.TEXT_ALIGNMENT_NONE);
		mBixolonLabelPrinter.drawText("Bob  010-1234-5678", 300, 600, BixolonLabelPrinter.FONT_SIZE_10, 1, 1, 0, BixolonLabelPrinter.ROTATION_270_DEGREES, false, false, BixolonLabelPrinter.TEXT_ALIGNMENT_NONE);
		mBixolonLabelPrinter.drawText("BIXOLON Apt 204dong 702ho", 430, 600, BixolonLabelPrinter.FONT_SIZE_10, 1, 1, 0, BixolonLabelPrinter.ROTATION_270_DEGREES, false, false, BixolonLabelPrinter.TEXT_ALIGNMENT_NONE);
		mBixolonLabelPrinter.drawText("Gyunggi-do Guri-si Inchang-dong 3912", 260, 600, BixolonLabelPrinter.FONT_SIZE_10, 1, 1, 0, BixolonLabelPrinter.ROTATION_270_DEGREES, false, false, BixolonLabelPrinter.TEXT_ALIGNMENT_NONE);
		mBixolonLabelPrinter.drawText("Gyunggi-do Guri-si Inchang-dong 3912", 400, 600, BixolonLabelPrinter.FONT_SIZE_10, 1, 1, 0, BixolonLabelPrinter.ROTATION_270_DEGREES, false, false, BixolonLabelPrinter.TEXT_ALIGNMENT_NONE);

		mBixolonLabelPrinter.drawText("DLVY 1026-1287-1927", 30, 400, BixolonLabelPrinter.FONT_SIZE_10, 1, 1, 0, BixolonLabelPrinter.ROTATION_270_DEGREES, false, false, BixolonLabelPrinter.TEXT_ALIGNMENT_NONE);
		mBixolonLabelPrinter.drawText("Date   2017/12/31", 70, 400, BixolonLabelPrinter.FONT_SIZE_10, 1, 1, 0, BixolonLabelPrinter.ROTATION_270_DEGREES, false, false, BixolonLabelPrinter.TEXT_ALIGNMENT_NONE);

		mBixolonLabelPrinter.drawText("Bob 010-1234-5678", 120, 400, BixolonLabelPrinter.FONT_SIZE_12, 1, 1, 0, BixolonLabelPrinter.ROTATION_270_DEGREES, false, false, BixolonLabelPrinter.TEXT_ALIGNMENT_NONE);

		mBixolonLabelPrinter.drawText("Nike Sport", 300, 250, BixolonLabelPrinter.FONT_SIZE_10, 1, 1, 0, BixolonLabelPrinter.ROTATION_270_DEGREES, false, false, BixolonLabelPrinter.TEXT_ALIGNMENT_NONE);
		mBixolonLabelPrinter.drawText("02-648-4317", 330, 250, BixolonLabelPrinter.FONT_SIZE_10, 1, 1, 0, BixolonLabelPrinter.ROTATION_270_DEGREES, false, false, BixolonLabelPrinter.TEXT_ALIGNMENT_NONE);
		mBixolonLabelPrinter.drawText("Airmax GX-100", 360, 250, BixolonLabelPrinter.FONT_SIZE_10, 1, 1, 0, BixolonLabelPrinter.ROTATION_270_DEGREES, false, false, BixolonLabelPrinter.TEXT_ALIGNMENT_NONE);

		mBixolonLabelPrinter.drawQrCode("www.bixolon.com", 500, 200, BixolonLabelPrinter.QR_CODE_MODEL2, BixolonLabelPrinter.ECC_LEVEL_15, 4, BixolonLabelPrinter.ROTATION_270_DEGREES);

		mBixolonLabelPrinter.print(1, 1);
		mBixolonLabelPrinter.endTransactionPrint();
	}

	private void printLabelSample() {
		mBixolonLabelPrinter.beginTransactionPrint();

		mBixolonLabelPrinter.drawBlock(convert203To300DPI(40), convert203To300DPI(40), convert203To300DPI(800), convert203To300DPI(1200), BixolonLabelPrinter.BLOCK_OPTION_BOX, 8);
		mBixolonLabelPrinter.drawBlock(convert203To300DPI(40), convert203To300DPI(215), convert203To300DPI(800), convert203To300DPI(223), BixolonLabelPrinter.BLOCK_OPTION_LINE_OVERWRITING, 8);
		mBixolonLabelPrinter.drawBlock(convert203To300DPI(40), convert203To300DPI(279), convert203To300DPI(800), convert203To300DPI(287), BixolonLabelPrinter.BLOCK_OPTION_LINE_OVERWRITING, 8);
		mBixolonLabelPrinter.drawBlock(convert203To300DPI(40), convert203To300DPI(800), convert203To300DPI(800), convert203To300DPI(808), BixolonLabelPrinter.BLOCK_OPTION_LINE_OVERWRITING, 8);
		mBixolonLabelPrinter.drawBlock(convert203To300DPI(40), convert203To300DPI(1150), convert203To300DPI(800), convert203To300DPI(1158), BixolonLabelPrinter.BLOCK_OPTION_LINE_OVERWRITING, 8);
		mBixolonLabelPrinter.drawBlock(convert203To300DPI(190), convert203To300DPI(40), convert203To300DPI(198), convert203To300DPI(215), BixolonLabelPrinter.BLOCK_OPTION_LINE_OVERWRITING, 8);

		mBixolonLabelPrinter.drawText(
				"POSTAGE",
				convert203To300DPI(210),
				convert203To300DPI(60),
				BixolonLabelPrinter.FONT_SIZE_18,
				1, 1, 0, 0,
				false,
				true,
				BixolonLabelPrinter.TEXT_ALIGNMENT_LEFT
		);
		mBixolonLabelPrinter.drawVectorFontText("08/01/2022",
				convert203To300DPI(210),
				convert203To300DPI(90),
				BixolonLabelPrinter.VECTOR_FONT_ASCII,
				convert203To300DPI(20), convert203To300DPI(20),
				0,
				false,
				false,
				false,
				0,
				BixolonLabelPrinter.VECTOR_FONT_TEXT_ALIGNMENT_LEFT,
				BixolonLabelPrinter.VECTOR_FONT_TEXT_DIRECTION_LEFT_TO_RIGHT
		);
		mBixolonLabelPrinter.drawVectorFontText("From 06484",
				convert203To300DPI(210),
				convert203To300DPI(120),
				BixolonLabelPrinter.VECTOR_FONT_ASCII,
				convert203To300DPI(20), convert203To300DPI(20),
				0,
				false,
				false,
				false,
				0,
				BixolonLabelPrinter.VECTOR_FONT_TEXT_ALIGNMENT_LEFT,
				BixolonLabelPrinter.VECTOR_FONT_TEXT_DIRECTION_LEFT_TO_RIGHT
		);

		mBixolonLabelPrinter.drawVectorFontText("Zone 1",
				convert203To300DPI(210),
				convert203To300DPI(150),
				BixolonLabelPrinter.VECTOR_FONT_ASCII,
				convert203To300DPI(20), convert203To300DPI(20),
				0,
				false,
				false,
				false,
				0,
				BixolonLabelPrinter.VECTOR_FONT_TEXT_ALIGNMENT_LEFT,
				BixolonLabelPrinter.VECTOR_FONT_TEXT_DIRECTION_LEFT_TO_RIGHT
		);

		mBixolonLabelPrinter.draw1dBarcode(
				"978020137962",
				convert203To300DPI(420),
				convert203To300DPI(60),
				BixolonLabelPrinter.BARCODE_EAN13,
				convert203To300DPI(4), convert203To300DPI(5),
				convert203To300DPI(50),
				0,
				BixolonLabelPrinter.HRI_NOT_PRINT,0
		);

		mBixolonLabelPrinter.drawText("BIXOLON",
				convert203To300DPI(420),
				convert203To300DPI(120),
				BixolonLabelPrinter.FONT_SIZE_18,
				1, 1, 0, 0,
				false,
				true,
				BixolonLabelPrinter.TEXT_ALIGNMENT_LEFT
			);
		mBixolonLabelPrinter.drawVectorFontText("021B0050000378",
				convert203To300DPI(780),
				convert203To300DPI(120),
				BixolonLabelPrinter.VECTOR_FONT_ASCII,
				convert203To300DPI(20), convert203To300DPI(20),
				0,
				false,
				false,
				false,
				0,
				BixolonLabelPrinter.VECTOR_FONT_TEXT_ALIGNMENT_RIGHT,
				BixolonLabelPrinter.VECTOR_FONT_TEXT_DIRECTION_LEFT_TO_RIGHT
		);

		mBixolonLabelPrinter.drawText("ComPlsPrice",
				convert203To300DPI(420),
				convert203To300DPI(150),
				BixolonLabelPrinter.FONT_SIZE_18,
				1, 1, 0, 0,
				false,
				true,
				BixolonLabelPrinter.TEXT_ALIGNMENT_LEFT
		);
		mBixolonLabelPrinter.drawVectorFontText("Flat Rate Envelope",
				convert203To300DPI(420),
				convert203To300DPI(180),
				BixolonLabelPrinter.VECTOR_FONT_ASCII,
				convert203To300DPI(20), convert203To300DPI(20),
				0,
				true,
				false,
				false,
				0,
				BixolonLabelPrinter.VECTOR_FONT_TEXT_ALIGNMENT_LEFT,
				BixolonLabelPrinter.VECTOR_FONT_TEXT_DIRECTION_LEFT_TO_RIGHT
		);

		mBixolonLabelPrinter.drawVectorFontText(
				"PRIORITY MAIL 1-DAY",
				convert203To300DPI(400),
				convert203To300DPI(231),
				BixolonLabelPrinter.VECTOR_FONT_ASCII,
				convert203To300DPI(40),
				convert203To300DPI(40),
				0,
				true,
				false,
				false,
				0,
				BixolonLabelPrinter.VECTOR_FONT_TEXT_ALIGNMENT_CENTER,
				BixolonLabelPrinter.VECTOR_FONT_TEXT_DIRECTION_LEFT_TO_RIGHT
			);

		mBixolonLabelPrinter.drawBitmap(
				Bitmap.createScaledBitmap(
						base64ToBitmap("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"),
						convert203To300DPI(110),
						convert203To300DPI(140),
						true
				),
				convert203To300DPI(60),
				convert203To300DPI(60),
				convert203To300DPI(110),
				convert203To300DPI(140),
				true
		);
		mBixolonLabelPrinter.drawBitmap(
				Bitmap.createScaledBitmap(
						base64ToBitmap("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"),
						convert203To300DPI(720),
						convert203To300DPI(355),
						true
				),
				convert203To300DPI(60),
				convert203To300DPI(307),
				convert203To300DPI(720),
				convert203To300DPI(355),
				true
		);

		mBixolonLabelPrinter.drawVectorFontText(
				"TRACKING #",
				convert203To300DPI(400),
				convert203To300DPI(820),
				BixolonLabelPrinter.VECTOR_FONT_ASCII,
				convert203To300DPI(30),
				convert203To300DPI(30),
				0,
				true,
				false,
				false,
				0,
				BixolonLabelPrinter.VECTOR_FONT_TEXT_ALIGNMENT_CENTER,
				BixolonLabelPrinter.VECTOR_FONT_TEXT_DIRECTION_LEFT_TO_RIGHT
		);
		mBixolonLabelPrinter.draw1dBarcode("9405509202121001335231",
				convert203To300DPI(160),
				convert203To300DPI(870),
				BixolonLabelPrinter.BARCODE_CODE128,
				convert203To300DPI(4),
				convert203To300DPI(8),
				convert203To300DPI(200),
				0,
				BixolonLabelPrinter.HRI_NOT_PRINT,
				0
		);
		mBixolonLabelPrinter.drawVectorFontText(
				"9405 5092 0212 1001 3352 31",
				convert203To300DPI(400),
				convert203To300DPI(1080),
				BixolonLabelPrinter.VECTOR_FONT_ASCII,
				convert203To300DPI(30),
				convert203To300DPI(30),
				0,
				true,
				false,
				false,
				0,
				BixolonLabelPrinter.VECTOR_FONT_TEXT_ALIGNMENT_CENTER,
				BixolonLabelPrinter.VECTOR_FONT_TEXT_DIRECTION_LEFT_TO_RIGHT
		);

		mBixolonLabelPrinter.print(1, 1);
		mBixolonLabelPrinter.endTransactionPrint();
	}

	private Bitmap base64ToBitmap(String data) {
		if (data == null || data.isEmpty()) {
			return null;
		}
		byte[] decodedString = Base64.decode(data, Base64.NO_WRAP);
		return BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
	}

	private int convert203To300DPI(int value) {
		if (mBixolonLabelPrinter.getPrinterDpi() == 2) return value;
		else return (int) (((float) value / 203.0f) * 300);
	}

	private final void setStatus(CharSequence subtitle)
	{
		final ActionBar actionBar = getActionBar();
		actionBar.setSubtitle(subtitle);
	}

	@SuppressLint("HandlerLeak")
	private void dispatchMessage(Message msg)
	{
		switch (msg.arg1)
		{
			case BixolonLabelPrinter.PROCESS_GET_STATUS:
				byte[] report = (byte[]) msg.obj;
				StringBuffer buffer = new StringBuffer();

				if((report[0] == 0x00)) {
					buffer.append("Normal.\n");
				}

				if(report[0] == BixolonLabelPrinter.STATUS_1ST_BYTE_PAPER_EMPTY)
				{
					buffer.append("Paper Empty.\n");
				}
				if(report[0] == BixolonLabelPrinter.STATUS_1ST_BYTE_COVER_OPEN)
				{
					buffer.append("Cover open.\n");
				}
				if(report[0] == BixolonLabelPrinter.STATUS_1ST_BYTE_CUTTER_JAMMED)
				{
					buffer.append("Cutter jammed.\n");
				}
				if(report[0] == BixolonLabelPrinter.STATUS_1ST_BYTE_TPH_OVERHEAT)
				{
					buffer.append("TPH(thermal head) overheat.\n");
				}
				if(report[0] == BixolonLabelPrinter.STATUS_1ST_BYTE_AUTO_SENSING_FAILURE)
				{
					buffer.append("Gap detection error. (Auto-sensing failure)\n");
				}
				if(report[0] == BixolonLabelPrinter.STATUS_1ST_BYTE_RIBBON_END_ERROR)
				{
					buffer.append("Ribbon end error.\n");
				}

				if(report.length == 2)
				{
					if(report[1] == BixolonLabelPrinter.STATUS_2ND_BYTE_BUILDING_IN_IMAGE_BUFFER)
					{
						buffer.append("On building label to be printed in image buffer.\n");
					}
					if(report[1] == BixolonLabelPrinter.STATUS_2ND_BYTE_PRINTING_IN_IMAGE_BUFFER)
					{
						buffer.append("On printing label in image buffer.\n");
					}
					if(report[1] == BixolonLabelPrinter.STATUS_2ND_BYTE_PAUSED_IN_PEELER_UNIT)
					{
						buffer.append("Issued label is paused in peeler unit.\n");
					}
				}
				if(buffer.length() == 0)
				{
					buffer.append("No error");
				}
				Toast.makeText(getApplicationContext(), buffer.toString(), Toast.LENGTH_SHORT).show();
				break;
			case BixolonLabelPrinter.PROCESS_GET_INFORMATION_MODEL_NAME:
			case BixolonLabelPrinter.PROCESS_GET_INFORMATION_FIRMWARE_VERSION:
			case BixolonLabelPrinter.PROCESS_EXECUTE_DIRECT_IO:
				String data = new String((byte[])msg.obj);
				Toast.makeText(getApplicationContext(), data, Toast.LENGTH_SHORT).show();
				break;
			case BixolonLabelPrinter.PROCESS_OUTPUT_COMPLETE:
				//TODO Process after completion of sending print command
				Toast.makeText(getApplicationContext(), "Output Complete", Toast.LENGTH_SHORT).show();
				break;
		}
	}

	private final Handler mHandler = new Handler()
	{
		@Override
		public void handleMessage(Message msg)
		{
			switch (msg.what)
			{
				case BixolonLabelPrinter.MESSAGE_TRANSFER_FILE:
					switch (msg.arg1) {
						case BixolonLabelPrinter.RC_SUCCESS:
							if (progressDialog != null) progressDialog.dismiss();
							progressDialog = null;
							Toast.makeText(mContext, "file transfer success", Toast.LENGTH_SHORT).show();
							Log.d("Testing", "file transfer success");
							break;
						case BixolonLabelPrinter.RC_PROGRESS:
							if (progressDialog != null && !progressDialog.isShowing()) progressDialog.show();
							TextView message = progressDialog.findViewById(android.R.id.message);
							message.setText("WLAN Firmware Send: " + msg.obj.toString());
							Log.d("Testing", "file sending: " + msg.obj);
							break;
						case BixolonLabelPrinter.RC_FAIL:
							int errorCode = msg.arg2;
							if (progressDialog != null) progressDialog.dismiss();
							progressDialog = null;
							Toast.makeText(mContext, "file transfer fail: " + errorCode, Toast.LENGTH_SHORT).show();
							Log.e("Testing", "file transfer fail: " + errorCode);
							break;
					}
					break;
				case BixolonLabelPrinter.MESSAGE_FIRMWARE_UPDATE:
					switch (msg.arg1) {
						case BixolonLabelPrinter.FIRMWARE_COMPLETE:
							Log.d("Testing", "firmware download complete");
							if (progressDialog != null) progressDialog.dismiss();
							progressDialog = null;
							Toast.makeText(mContext, "firmware download complete", Toast.LENGTH_SHORT).show();
							break;
						case BixolonLabelPrinter.FIRMWARE_PROGRESS:
							if (progressDialog != null && !progressDialog.isShowing()) progressDialog.show();
							TextView message = progressDialog.findViewById(android.R.id.message);
							message.setText(msg.obj.toString());
							break;
						case BixolonLabelPrinter.FIRMWARE_DELAY_UPDATE:
							if (msg.obj != null) Log.d("Testing", "delay: " + msg.obj);
							break;
						case BixolonLabelPrinter.FIRMWARE_EXCEPTION:
							if (msg.obj != null) Log.e("Testing", "exception :" + msg.obj);
							Toast.makeText(mContext, "firmware Download fail: " + msg.obj, Toast.LENGTH_SHORT).show();
							break;
					}
				case BixolonLabelPrinter.MESSAGE_STATE_CHANGE:
					switch (msg.arg1)
					{
						case BixolonLabelPrinter.STATE_CONNECTED:
							setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
							mListView.setEnabled(true);
							mIsConnected = true;
							invalidateOptionsMenu();
							break;

						case BixolonLabelPrinter.STATE_CONNECTING:
							setStatus(R.string.title_connecting);
							break;

						case BixolonLabelPrinter.STATE_NONE:
							Log.e("NONE", msg.toString());
							setStatus(R.string.title_not_connected);
							//mListView.setEnabled(false);
							mIsConnected = false;
							invalidateOptionsMenu();
							break;
					}
					break;

				case BixolonLabelPrinter.MESSAGE_READ:
					MainActivity.this.dispatchMessage(msg);
					break;

				case BixolonLabelPrinter.MESSAGE_DEVICE_NAME:
					mConnectedDeviceName = msg.getData().getString(BixolonLabelPrinter.DEVICE_NAME);
					Toast.makeText(getApplicationContext(), mConnectedDeviceName, Toast.LENGTH_LONG).show();
					break;

				case BixolonLabelPrinter.MESSAGE_TOAST:
					//mListView.setEnabled(false);
					Toast.makeText(getApplicationContext(), msg.getData().getString(BixolonLabelPrinter.TOAST), Toast.LENGTH_SHORT).show();
					break;

				case BixolonLabelPrinter.MESSAGE_LOG:
					Toast.makeText(getApplicationContext(), msg.getData().getString(BixolonLabelPrinter.LOG), Toast.LENGTH_SHORT).show();
					break;

				case BixolonLabelPrinter.MESSAGE_BLUETOOTH_DEVICE_SET:
					if(msg.obj == null)
					{
						Toast.makeText(getApplicationContext(), "No paired device", Toast.LENGTH_SHORT).show();
					}
					else
					{
						DialogManager.showBluetoothDialog(MainActivity.this, (Set<BluetoothDevice>) msg.obj);
					}
					break;

				case BixolonLabelPrinter.MESSAGE_USB_DEVICE_SET:
					if(msg.obj == null)
					{
						Toast.makeText(getApplicationContext(), "No connected device", Toast.LENGTH_SHORT).show();
					}
					else
					{
						DialogManager.showUsbDialog(MainActivity.this, (Set<UsbDevice>) msg.obj, mUsbReceiver);
					}
					break;

				case BixolonLabelPrinter.MESSAGE_NETWORK_DEVICE_SET:
					if(msg.obj == null)
					{
						Toast.makeText(getApplicationContext(), "No connectable device", Toast.LENGTH_SHORT).show();
					}
					DialogManager.showNetworkDialog(MainActivity.this, msg.obj.toString());
					break;
			}
		}
	};


	private BroadcastReceiver mUsbReceiver = new BroadcastReceiver()
	{

		@Override
		public void onReceive(Context context, Intent intent)
		{
			String action = intent.getAction();

			if(UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action))
			{
				if(mBixolonLabelPrinter != null && !mBixolonLabelPrinter.isConnected()) {
					isConnectedPrinter();
					Toast.makeText(getApplicationContext(), "Found USB device", Toast.LENGTH_SHORT).show();
				}

			}
			else if(UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action))
			{
				mBixolonLabelPrinter.disconnect();
				Toast.makeText(getApplicationContext(), "USB device removed", Toast.LENGTH_SHORT).show();
			} else if(ACTION_USB_PERMISSION.equals(action)){
				if(device != null) {
					mBixolonLabelPrinter.connect(device);
					device = null;
				}
			}
		}
	};

	private void isConnectedPrinter(){
		HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
		Set<String> keys = deviceList.keySet();
		UsbDevice device = null;

		if(deviceList.size() > 0) {
			for(String key : keys) {
				device = deviceList.get(key);
				if(device.getVendorId() == 5380) {
					IntentFilter filter = new IntentFilter(
							ACTION_USB_PERMISSION);
					filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
					filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
						registerReceiver(mUsbReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
					} else {
						registerReceiver(mUsbReceiver, filter);
					}
					usbManager.requestPermission(device, mPermissionIntent);
				}
			}
		}

		if(device != null) {
			if(tryedAutoConnect) {
				mBixolonLabelPrinter.connect(device);
			}
		}
	}

	public void checkVerify()
	{
		int result = 0;
		ArrayList<String> permissionList = new ArrayList<String>();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
			mPermissionIntent = PendingIntent.getBroadcast(this, 0,
					new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_ALLOW_UNSAFE_IMPLICIT_INTENT);
		} else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
			mPermissionIntent = PendingIntent.getBroadcast(this, 0,
					new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
		} else {
			mPermissionIntent = PendingIntent.getBroadcast(this, 0,
					new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_UPDATE_CURRENT);
		}
		if (Build.VERSION.SDK_INT >= 33) {
			// 13 이상의 경우 BLUETOOTH_SCAN, CONNECT
			if (!Environment.isExternalStorageManager()) {
				Intent allFileAccessIntent = new Intent();
				allFileAccessIntent.setAction(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
				Uri uri = Uri.fromParts("package", this.getPackageName(), null);
				allFileAccessIntent.setData(uri);
				try {
					startActivity(allFileAccessIntent);
				} catch (Exception e) {
					Log.e("SelectFirmware", "activity start fail: " + e);
				}
			}
			permissions = new String[] {
					Manifest.permission.NEARBY_WIFI_DEVICES,
					Manifest.permission.BLUETOOTH_ADMIN,
					Manifest.permission.ACCESS_COARSE_LOCATION,
					Manifest.permission.ACCESS_FINE_LOCATION,
					Manifest.permission.BLUETOOTH_SCAN,
					Manifest.permission.BLUETOOTH_CONNECT
			};
		} else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
			// Android 11 (SDK 31)
			permissions = new String[]{
					Manifest.permission.READ_EXTERNAL_STORAGE,
					Manifest.permission.WRITE_EXTERNAL_STORAGE,
					Manifest.permission.ACCESS_COARSE_LOCATION,
					Manifest.permission.ACCESS_FINE_LOCATION,
					Manifest.permission.BLUETOOTH_SCAN,
					Manifest.permission.BLUETOOTH_CONNECT
			};
		}

		for(int i = 0; i < permissions.length; i++)
		{
			result = ContextCompat.checkSelfPermission(getApplicationContext(), permissions[i]);
			if(result != PackageManager.PERMISSION_GRANTED) {
				permissionList.add(permissions[i]);
			}
		}

		if(permissionList.size() > 0)
		{
			String[] reqPermissionArray = new String[permissionList.size()];
			reqPermissionArray = permissionList.toArray(reqPermissionArray);
			requestPermissions(reqPermissionArray, REQUEST_PERMISSION);
		}
	}


	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);

		if (requestCode == REQUEST_PERMISSION)
		{
			if (grantResults.length > 0)
			{
				for (int i=0; i<grantResults.length; ++i)
				{
					if (grantResults[i] == PackageManager.PERMISSION_DENIED)
					{
						new AlertDialog.Builder(this).setTitle(getResources().getString(R.string.notice)).setMessage(getResources().getString(R.string.grant_permission))
								.setPositiveButton(getResources().getString(R.string.exit), new DialogInterface.OnClickListener(){
									@Override
									public void onClick(DialogInterface dialog, int which) {
										dialog.dismiss();
										finish();
									}
								}).setNegativeButton(getResources().getString(R.string.set_permission), new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								dialog.dismiss();
								Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
										.setData(Uri.parse("package:" + getApplicationContext().getPackageName()));
								intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
								getApplicationContext().startActivity(intent);
							}
						}).setCancelable(false).show();
						return;
					}
				}
			}
			isConnectedPrinter();
		}
	}

    public class SampleUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler
    {
        @Override
        public void uncaughtException(Thread thread, final Throwable ex)
        {
            // 여기서 에러를 처리
            finish();
        }
    }

	private BixolonLabelPrinter.TransferFileListener transferFileListener = new BixolonLabelPrinter.TransferFileListener() {
		@Override
		public void onSuccess() {
			LogService.LogD(BXLCommonConst._LOG_LEVEL_HIGH, "TAG", "File transfer success");
		}

		@Override
		public void onFailed(int i) {
			LogService.LogE(BXLCommonConst._LOG_LEVEL_HIGH, "TAG", "File transfer failed with error : " + i);
		}
	};
}
