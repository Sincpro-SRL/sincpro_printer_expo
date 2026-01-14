package com.bixolon.labelprintersample;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.FileUtils;
import android.provider.OpenableColumns;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import com.bixolon.commonlib.BXLCommonConst;
import com.bixolon.commonlib.setting.wlan.WlanInfo;
import com.bixolon.commonlib.setting.wlan.enums.certificate.pemfiletype.PemFileType;
import com.bixolon.commonlib.setting.wlan.enums.wlaninfo.authmode.AuthMode;
import com.bixolon.commonlib.setting.wlan.enums.wlaninfo.cryptomode.CryptoMode;
import com.bixolon.commonlib.setting.wlan.enums.wlaninfo.ipconfigmode.IpConfigMode;
import com.bixolon.commonlib.setting.wlan.enums.wlaninfo.networkmode.NetworkMode;
import com.bixolon.commonlib.setting.wlan.enums.wlaninfo.usesuppoorted.UseSupported;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class WlanSettingActivity extends Activity implements View.OnClickListener {
    private final int REQUEST_NCT_FILE_UPLOAD = 1;
    private final int REQUEST_CERTIFICATE_FILE_UPLOAD = 2;
    private final int REQUEST_BIN_CERTIFICATE_FILE_UPLOAD = 3;
    private WlanInfo wlanInfo = null;

    private Button wlanFromPrinterBtn;
    private Button wlanFromFileBtn;
    // WLAN Basic Setting
    private EditText wlanSSID;
    private Button wlanMode;
    private String[] modeArray;
    private AlertDialog wlanModeSelectDialog;
    private DialogInterface.OnClickListener wlanModeClickListener = (dialogInterface, position) -> {
        // TODO: 선택 시 해당 모드로 세팅
        NetworkMode networkMode = null;
        switch (position) {
            case 0:
                networkMode = NetworkMode.INFRA;
                break;
            case 1:
                networkMode = NetworkMode.AD_HOC;
                break;
            case 2:
                networkMode = NetworkMode.P2P;
                break;
            case 3:
                networkMode = NetworkMode.SOFT_AP;
                break;
        }
        if (networkMode != null) {
            wlanInfo.setNetworkMode(networkMode);
            wlanMode.setText(modeArray[position]);
        }
    };

    // WLAN Security
    private Button wlanAuthMode;
    private String[] authModeArray;
    private AlertDialog wlanAuthModeSelectDialog;
    private DialogInterface.OnClickListener wlanAuthModeClickListener = (dialogInterface, position) -> {
        AuthMode authMode = null;
        switch (position) {
            case 0:
                authMode = AuthMode.OPEN;
                break;
            case 1:
                authMode = AuthMode.SHARED;
                break;
            case 2:
                authMode = AuthMode.WPA1PSK;
                break;
            case 3:
                authMode = AuthMode.WPA2PSD;
                break;
            case 4:
                authMode = AuthMode.WPA1EAP;
                break;
            case 5:
                authMode = AuthMode.WPA2EAP;
                break;
        }
        if (authMode != null) {
            wlanInfo.setAuthMode(authMode);
            wlanAuthMode.setText(authModeArray[position]);
        }
    };
    private Button wlanCryptoMode;
    private String[] cryptoModeArray;
    private AlertDialog wlanCryptoModeSelectDialog;
    private DialogInterface.OnClickListener wlanCryptoModeClickListener = (dialogInterface, position) -> {
        CryptoMode cryptoMode = null;
        switch (position) {
            case 0:
                cryptoMode = CryptoMode.NONE;
                break;
            case 1:
                cryptoMode = CryptoMode.WEP64_128;
                break;
            case 2:
                cryptoMode = CryptoMode.TKIP;
                break;
            case 3:
                cryptoMode = CryptoMode.AES;
                break;
            case 4:
                cryptoMode = CryptoMode.AES_TKIP;
                break;
        }
        if (cryptoMode != null) {
            wlanInfo.setCryptoMode(cryptoMode);
            wlanCryptoMode.setText(cryptoModeArray[position]);
        }
    };

    // IP Address Setting
    private Button wlanIpConfigMode;
    private String[] ipConfigModeArray;
    private AlertDialog wlanIpConfigModeSelectDialog;
    private DialogInterface.OnClickListener wlanIpConfigModeClickListener = (dialogInterface, position) -> {
        IpConfigMode configMode = null;
        switch (position) {
            case 0:
                configMode = IpConfigMode.STATIC;
                break;
            case 1:
                configMode = IpConfigMode.DHCP;
                break;
        }
        if (configMode != null) {
            wlanInfo.setIpConfigMode(configMode);
            wlanIpConfigMode.setText(ipConfigModeArray[position]);
        }
    };
    private EditText wlanIpAddress;
    private EditText wlanSubnetMask;
    private EditText wlanGateway;
    private EditText wlanPortNumber;
    private Button wlanPsk;

    // Protocol
    private Switch wlanProtocolHTTPS;
    private Switch wlanProtocolTELNET;
    private Switch wlanProtocolFTP;
    private Switch wlanProtocolSNMP;

    // System Setting
    private EditText wlanSystemName;
    private EditText wlanUserName;
    private Button wlanUserPassword;
    private EditText wlanInactivityTime;

    // Certificate
    private Button wlanPemMode;
    private String[] pemMode;
    private AlertDialog wlanPemModeSelectDialog;
    private DialogInterface.OnClickListener wlanPemModeClickListener = (dialogInterface, position) -> {
        wlanPemMode.setText(pemMode[position]);
    };

    private Button wlanCertificateFile;
    private Button wlanCertificateUploadBtn;
    private Button wlanSetBtn;
    private Button wlanBinaryCertificateUploadBtn;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wlan_setting);
        initView();
    }

    private void initView() {
        wlanFromFileBtn = findViewById(R.id.get_wlan_from_file_btn);
        wlanFromFileBtn.setOnClickListener(this);
        wlanFromPrinterBtn = findViewById(R.id.get_wlan_from_printer_btn);
        wlanFromPrinterBtn.setOnClickListener(this);
        // WLAN Basic
        wlanSSID = findViewById(R.id.wlan_network_ssid_field);
        wlanMode = findViewById(R.id.wlan_mode_selector);
        modeArray = this.getResources().getStringArray(R.array.wlan_mode);
        wlanMode.setOnClickListener(this);

        // WLAN Security
        wlanAuthMode = findViewById(R.id.wlan_auth_mode_selector);
        authModeArray = this.getResources().getStringArray(R.array.wlan_auth_mode);
        wlanAuthMode.setOnClickListener(this);
        wlanCryptoMode = findViewById(R.id.wlan_crypto_mode_selector);
        cryptoModeArray = this.getResources().getStringArray(R.array.wlan_crypto_mode);
        wlanCryptoMode.setOnClickListener(this);

        // IP Address Setting
        wlanIpConfigMode = findViewById(R.id.wlan_ip_config_mode_selector);
        ipConfigModeArray = this.getResources().getStringArray(R.array.wlan_ip_config_mode);
        wlanIpConfigMode.setOnClickListener(this);

        wlanIpAddress = findViewById(R.id.wlan_ip_address_field);
        wlanSubnetMask = findViewById(R.id.wlan_subnet_mask_field);
        wlanGateway = findViewById(R.id.wlan_gateway_field);
        wlanPortNumber = findViewById(R.id.wlan_port_number_field);
        wlanPsk = findViewById(R.id.wlan_psk_password_field);
        wlanPsk.setOnClickListener(this);

        // System Setting
        wlanSystemName = findViewById(R.id.wlan_system_name_field);
        wlanUserPassword = findViewById(R.id.wlan_user_password_field);
        wlanUserPassword.setOnClickListener(this);
        wlanUserName = findViewById(R.id.wlan_user_name_field);
        wlanInactivityTime = findViewById(R.id.wlan_inactivity_time_field);

        // Protocol
        wlanProtocolHTTPS = findViewById(R.id.wlan_protocol_https_switch);
        wlanProtocolTELNET = findViewById(R.id.wlan_protocol_telnet_switch);
        wlanProtocolFTP = findViewById(R.id.wlan_protocol_ftp_switch);
        wlanProtocolSNMP = findViewById(R.id.wlan_protocol_snmp_switch);

        // Certificate
        wlanPemMode = findViewById(R.id.wlan_pem_type_selector);
        wlanPemMode.setOnClickListener(this);
        pemMode = this.getResources().getStringArray(R.array.wlan_pem_mode);
        wlanCertificateFile = findViewById(R.id.wlan_certificate_select_btn);
        wlanCertificateFile.setOnClickListener(this);
        wlanCertificateUploadBtn = findViewById(R.id.wlan_certificate_upload_btn);
        wlanCertificateUploadBtn.setOnClickListener(this);
        wlanBinaryCertificateUploadBtn = findViewById(R.id.wlan_binary_certificate_upload_btn);
        wlanBinaryCertificateUploadBtn.setOnClickListener(this);

        wlanSetBtn = findViewById(R.id.set_wlan_btn);
        wlanSetBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if (!MainActivity.mBixolonLabelPrinter.isConnected()) {
            Log.e("WlanSettingActivity", "LabelPrinter not connected");
            Toast.makeText(this, "LabelPrinter not connected", Toast.LENGTH_SHORT).show();
            return;
        }
        int result = 0;
        switch (view.getId()) {
            case R.id.get_wlan_from_printer_btn:
                wlanInfo = MainActivity.mBixolonLabelPrinter.getWlanInfo();
                initWlanInfoView();
                break;
            case R.id.get_wlan_from_file_btn:
                // nct File Open
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                startActivityForResult(Intent.createChooser(intent, "Select NCT File"), REQUEST_NCT_FILE_UPLOAD);
                break;
            case R.id.wlan_mode_selector:
                if (wlanModeSelectDialog == null) {
                    wlanModeSelectDialog = new AlertDialog.Builder(this)
                            .setTitle(R.string.wlan_mode).setItems(modeArray, wlanModeClickListener).create();
                }
                wlanModeSelectDialog.show();
                break;
            case R.id.wlan_auth_mode_selector:
                if (wlanAuthModeSelectDialog == null) {
                    wlanAuthModeSelectDialog = new AlertDialog.Builder(this)
                            .setTitle(R.string.wlan_auth_mode).setItems(authModeArray, wlanAuthModeClickListener).create();
                }
                wlanAuthModeSelectDialog.show();
                break;
            case R.id.wlan_crypto_mode_selector:
                if (wlanCryptoModeSelectDialog == null) {
                    wlanCryptoModeSelectDialog = new AlertDialog.Builder(this)
                            .setTitle(R.string.wlan_crypto_mode).setItems(cryptoModeArray, wlanCryptoModeClickListener).create();
                }
                wlanCryptoModeSelectDialog.show();
                break;
            case R.id.wlan_ip_config_mode_selector:
                if (wlanIpConfigModeSelectDialog == null) {
                    wlanIpConfigModeSelectDialog = new AlertDialog.Builder(this)
                            .setTitle(R.string.wlan_ip_config).setItems(ipConfigModeArray, wlanIpConfigModeClickListener).create();
                }
                wlanIpConfigModeSelectDialog.show();
                break;
            case R.id.wlan_psk_password_field:
                setPskPassword();
                break;
            case R.id.wlan_user_password_field:
                setUserPassword();
                break;
            case R.id.wlan_pem_type_selector:
                if (wlanPemModeSelectDialog == null) {
                    wlanPemModeSelectDialog = new AlertDialog.Builder(this)
                            .setTitle(R.string.wlan_pem_type).setItems(pemMode, wlanPemModeClickListener).create();
                }
                wlanPemModeSelectDialog.show();
                break;
            case R.id.wlan_certificate_select_btn:
                // File Selector Open
                Intent certificateUploadIntent = new Intent(Intent.ACTION_GET_CONTENT);
                certificateUploadIntent.setType("*/*");
                startActivityForResult(Intent.createChooser(certificateUploadIntent, "Select PEM File"), REQUEST_CERTIFICATE_FILE_UPLOAD);
                break;
            case R.id.wlan_certificate_upload_btn:
                result = MainActivity.mBixolonLabelPrinter.updateCertificateFile();
                break;
            case R.id.set_wlan_btn:
                updateWlanInfo();
                result = MainActivity.mBixolonLabelPrinter.setWlanInfo(wlanInfo);
                break;
            case R.id.wlan_binary_certificate_upload_btn:
                Intent binaryCertificateIntent = new Intent(Intent.ACTION_GET_CONTENT);
                binaryCertificateIntent.setType("application/octet-stream");
                startActivityForResult(Intent.createChooser(binaryCertificateIntent, "Select bin File"), REQUEST_BIN_CERTIFICATE_FILE_UPLOAD);
                break;
            default:
                break;
        }
        if (result != BXLCommonConst._BXL_RC_SUCCESS) {
            Toast.makeText(this, "Error code: " + result, Toast.LENGTH_SHORT).show();
        }
    }

    private void initWlanInfoView() {
        if (wlanInfo == null) return;
        // WLAN Basic
        wlanSSID.setText(wlanInfo.getSSID());
        NetworkMode networkMode = wlanInfo.getNetworkMode();
        switch (networkMode) {
            case INFRA:
                wlanMode.setText(modeArray[0]);
                break;
            case AD_HOC:
                wlanMode.setText(modeArray[1]);
                break;
            case P2P:
                wlanMode.setText(modeArray[2]);
                break;
            case SOFT_AP:
                wlanMode.setText(modeArray[3]);
                break;
        }

        // WLAN Security
        AuthMode authMode = wlanInfo.getAuthMode();
        switch (authMode) {
            case OPEN:
                wlanAuthMode.setText(authModeArray[0]);
                break;
            case SHARED:
                wlanAuthMode.setText(authModeArray[1]);
                break;
            case WPA1PSK:
                wlanAuthMode.setText(authModeArray[2]);
                break;
            case WPA2PSD:
                wlanAuthMode.setText(authModeArray[3]);
                break;
            case WPA1EAP:
                wlanAuthMode.setText(authModeArray[4]);
                break;
            case WPA2EAP:
                wlanAuthMode.setText(authModeArray[5]);
                break;
        }
        CryptoMode cryptoMode = wlanInfo.getCryptoMode();
        switch (cryptoMode) {
            case NONE:
                wlanCryptoMode.setText(cryptoModeArray[0]);
                break;
            case WEP64_128:
                wlanCryptoMode.setText(cryptoModeArray[1]);
                break;
            case TKIP:
                wlanCryptoMode.setText(cryptoModeArray[2]);
                break;
            case AES:
                wlanCryptoMode.setText(cryptoModeArray[3]);
                break;
            case AES_TKIP:
                wlanCryptoMode.setText(cryptoModeArray[4]);
                break;
        }

        // IP Address Setting
        IpConfigMode ipConfigMode = wlanInfo.getIpConfigMode();
        switch (ipConfigMode) {
            case STATIC:
                wlanIpConfigMode.setText(ipConfigModeArray[0]);
                break;
            case DHCP:
                wlanIpConfigMode.setText(ipConfigModeArray[1]);
                break;
        }
        wlanIpAddress.setText(wlanInfo.getIpAddress());
        wlanSubnetMask.setText(wlanInfo.getSubnetMask());
        wlanGateway.setText(wlanInfo.getGateway());
        wlanPortNumber.setText(String.valueOf(wlanInfo.getPortNumber()));

        // System Setting
        wlanSystemName.setText(wlanInfo.getSystemName());
        wlanUserName.setText(wlanInfo.getUserName());
        wlanInactivityTime.setText(String.valueOf(wlanInfo.getInactivityTime()));

        // Protocol
        setProtocolSwitch(wlanInfo.getIsWebSSL(), wlanProtocolHTTPS);
        setProtocolSwitch(wlanInfo.getIsTelnet(), wlanProtocolTELNET);
        setProtocolSwitch(wlanInfo.getIsFTP(), wlanProtocolFTP);
        setProtocolSwitch(wlanInfo.getIsSNMP(), wlanProtocolSNMP);
    }

    private void setUserPassword() {
        EditText passwordField = new EditText(this);
        passwordField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        AlertDialog.Builder pskDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.wlan_user_password)
                .setView(passwordField)
                .setPositiveButton("OK", (dialogInterface, i) -> {
                    String password = passwordField.getText().toString();
                    if (password.isEmpty()) {
                        wlanInfo.setUserPassword(password);
                        wlanUserPassword.setText("*******");
                        Toast.makeText(this, getString(R.string.psk_empty), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", (dialogInterface, i) -> {
                    dialogInterface.cancel();
                });
        pskDialog.show();
    }
    private void setPskPassword() {
        EditText passwordField = new EditText(this);
        passwordField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        AlertDialog.Builder pskDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.wlan_psk_password)
                .setView(passwordField)
                .setPositiveButton("OK", (dialogInterface, i) -> {
                    String password = passwordField.getText().toString();
                    wlanPsk.setText("*******");
                    wlanInfo.setPreSharedKey(password);
                })
                .setNegativeButton("Cancel", (dialogInterface, i) -> {
                    dialogInterface.cancel();
                });
        pskDialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK) {
            Log.e("ActivityResult", "fail: " + requestCode);
            return ;
        }
        if (data == null) return;
        Uri fileUri = data.getData();
        if (fileUri == null) return;
        switch (requestCode) {
            case REQUEST_NCT_FILE_UPLOAD:
                if (!checkFileExtension(fileUri, "nct")) {
                    Log.e("WlanSettingActivity", "Invalid file extension");
                    Toast.makeText(this, "Invalid File", Toast.LENGTH_SHORT).show();
                    return;
                }
                String filePath = getFileTempPath(data.getData(), "wlan", ".nct");
                if (filePath == null || filePath.isEmpty()) return;
                wlanInfo = MainActivity.mBixolonLabelPrinter.getWlanInfo(filePath);
                initWlanInfoView();
                break;
            case REQUEST_CERTIFICATE_FILE_UPLOAD:
                if (!checkFileExtension(fileUri, "pem")) {
                    Log.e("WlanSettingActivity", "Invalid file extension");
                    Toast.makeText(this, "Invalid File", Toast.LENGTH_SHORT).show();
                    return;
                }
                byte[] certificateFile = getFileDataFromUri(fileUri);
                if (certificateFile == null) return;
                PemFileType pemType = null;
                switch (wlanPemMode.getText().toString()) {
                    case "CA":
                        pemType = PemFileType.CA;
                        break;
                    case "Client Key":
                        pemType = PemFileType.CLIENT_KEY;
                        break;
                    case "Client Cert":
                        pemType = PemFileType.CLIENT_CERT;
                        break;
                }
                if (pemType != null) {
                    String name = queryFileName(fileUri);
                    wlanCertificateFile.setText(name);
                    sendCertificateToPrinter(pemType, certificateFile);
                }
                break;
            case REQUEST_BIN_CERTIFICATE_FILE_UPLOAD:
                if (!checkFileExtension(fileUri, "bin")) {
                    Log.e("WlanSettingActivity", "Invalid file extension");
                    Toast.makeText(this, "Invalid File", Toast.LENGTH_SHORT).show();
                    return;
                }
                byte[] certificateBinFile = getFileDataFromUri(fileUri);
                wlanInfo.setCertificateBinFile(certificateBinFile);
                if (MainActivity.mBixolonLabelPrinter.setBinaryCertificateFile(wlanInfo) != BXLCommonConst._BXL_RC_SUCCESS) {
                    Toast.makeText(this, "setBinCertificate fail: " + MainActivity.mBixolonLabelPrinter.getLastError(), Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                break;
        }
    }

    private byte[] getFileDataFromUri(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            if (is == null) throw new IOException("URI Open fail");
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = is.read(buffer)) != -1) {
                bos.write(buffer, 0, bytesRead);
            }
            byte[] result = bos.toByteArray();
            bos.close();
            is.close();
            return result;
        } catch (Exception exception) {
            Log.e("Testing", "read error: " + exception);
            return null;
        }
    }

    private boolean checkFileExtension(Uri uri, String extension) {
        String name = queryFileName(uri);
        if (name == null) return false;
        return name.endsWith("." + extension);
    }

    private String queryFileName(Uri uri) {
        try {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor == null) return null;
            int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            cursor.moveToFirst();
            String name = cursor.getString(nameIndex);
            cursor.close();
            return name;
        } catch (Exception exception) {
            Log.e(WlanSettingActivity.class.getSimpleName(), "error: " + exception);
            return null;
        }
    }

    private void setProtocolSwitch(UseSupported supported, Switch view) {
        if (supported == UseSupported.NOT_SUPPORTED) view.setEnabled(false);
        else {
            view.setEnabled(true);
            view.setChecked(supported == UseSupported.ENABLE);
        }
    }

    private UseSupported setWlanProtocol(Switch view) {
        if (view.isEnabled()) {
            if (view.isChecked()) {
                return UseSupported.ENABLE;
            } else {
                return UseSupported.DISABLE;
            }
        } else {
            return UseSupported.NOT_SUPPORTED;
        }
    }

    private void sendCertificateToPrinter(PemFileType pemFileType, byte[] file) {
        updateWlanInfo();
        wlanInfo.setCertificateFile(file, pemFileType);
        MainActivity.mBixolonLabelPrinter.setPemCertificateFile(wlanInfo, pemFileType);
        int error = MainActivity.mBixolonLabelPrinter.getLastError();
        if (error != BXLCommonConst._BXL_RC_SUCCESS) {
            Toast.makeText(this, "sendCertificate fail: " + error, Toast.LENGTH_SHORT).show();
        }
    }

    private boolean updateWlanInfo() {
        // Button으로 처리되는 데이터는 선택 시 적용됨. EditText 입력한 것들만 변경 적용하면 됨

        wlanInfo.setSSID(wlanSSID.getText().toString());

        try {
            wlanInfo.setIpAddress(wlanIpAddress.getText().toString());
            wlanInfo.setSubnetMask(wlanSubnetMask.getText().toString());
            wlanInfo.setGateway(wlanGateway.getText().toString());
            wlanInfo.setPortNumber(Short.parseShort(wlanPortNumber.getText().toString()));
        } catch (Exception exception) {
            Toast.makeText(this, getString(R.string.ip_address_check_fail), Toast.LENGTH_SHORT).show();
            return false;
        }

        wlanInfo.setUserName(wlanUserName.getText().toString());
        wlanInfo.setInactivityTime(Short.parseShort(wlanInactivityTime.getText().toString()));
        wlanInfo.setSystemName(wlanSystemName.getText().toString());

        wlanInfo.setIsWebSSL(setWlanProtocol(wlanProtocolHTTPS));
        wlanInfo.setIsTelnet(setWlanProtocol(wlanProtocolTELNET));
        wlanInfo.setIsFTP(setWlanProtocol(wlanProtocolFTP));
        wlanInfo.setIsSNMP(setWlanProtocol(wlanProtocolSNMP));
        return true;
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
}