package com.bixolon.labelprintersample;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bixolon.commonlib.BXLCommonConst;
import com.bixolon.commonlib.common.BXLHelper;
import com.bixolon.commonlib.log.LogService;
import com.bixolon.labelprinter.BixolonLabelPrinter;
import com.bixolon.labelprinter.ResponseType;
import com.bixolon.labelprintersample.R;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public class DirectIoActivity extends Activity implements View.OnClickListener {
    private static final String[] RESPONSE_TYPE = {
            "NONE",
            "STATUS_CHECK",
            "RAW_DATA",
            "NULL",
            "CRLF"
    };

    private static final String[] EXAMPLE_ITEMS = {
            "T_resident",
            "T_Rotate4",
            "V_resident",
            "V_Rotate4",
            "Code39",
            "BD1",
            "BD3",
            "BD4",
            "BD5",
            "Slope",
            "Set Media Type",
            "indo",
            "Free"
    };

    private static final String[] EXAMPLES = {

            // T_resident
            "SS3\n" +    // Set Speed to 5 ips
                    "SD20\n" +    // Set Density level to 20
                    "SW800\n" +    // Set Label Width 800
                    "SOT\n" +    // Set Printing Orientation from Top to Bottom
                    "T26,20,0,1,1,0,0,N,N,'Font - 6 pt'\n" +
                    "T26,49,1,1,1,0,0,N,N,'Font - 8 pt'\n" +
                    "T26,81,2,1,1,0,0,N,N,'Font - 10 pt'\n" +
                    "T26,117,3,1,1,0,0,N,N,'Font - 12 pt'\n" +
                    "T26,156,4,1,1,0,0,R,N,'Font - 15 pt'\n" +
                    "T26,200,5,1,1,0,0,N,N,'Font - 20 pt'\n" +
                    "T26,252,6,1,1,0,0,N,N,'Font - 30 pt'\n" +
                    "P1",

            // T_rotate4
            "SS3\n" +
                    "SW832\n" +
                    "T300,500,4,1,1,0,0,N,N,'ABCDEFG'\n" +
                    "T300,500,4,1,1,0,1,N,N,'ABCDEFG'\n" +
                    "T300,500,4,1,1,0,2,N,N,'ABCDEFG'\n" +
                    "T300,500,4,1,1,0,3,N,N,'ABCDEFG'\n" +
                    "P1",

            // V_resident
            "SS3\n" +    // Set speed to 5 ips
                    "SD20\n" +    // Set density to 20
                    "SW800\n" +    // Set label width to 800
                    "SOT\n" +    // Set printing direction to from top to bottom
                    "V50,100,U,25,25,+1,N,N,N,0,L,0,'Vector Font Test'\n" +
                    "V50,200,U,35,35,-1,N,N,N,0,L,0,'Vector Font Test'\n" +
                    "V50,300,U,35,35,+1,B,R,I,0,L,0,'Vector Font Test '\n" +
                    "V50,400,U,45,25,+1,N,N,N,0,L,0,'Vector Font Test'\n" +
                    "V50,500,U,25,45,+1,N,N,N,0,L,0,'Vector Font Test'\n" +
                    "V50,700,U,65,65,+1,N,N,N,0,L,0,'ABCDEFGHIJKLMNO'\n" +
                    "V50,900,U,65,65,+1,N,N,N,0,L,0,'abcdefghijklmno'\n" +
                    "P1",

            // V_Rotate4
            "SS3\n" +
                    "SW832\n" +
                    "V400,500,U,45,40,+1,N,N,N,0,L,0,'VECTOR FONT'\n" +
                    "V400,500,U,45,40,+1,N,N,N,1,L,0,'VECTOR FONT'\n" +
                    "V400,500,U,45,40,+1,N,N,N,2,L,0,'VECTOR FONT'\n" +
                    "V400,500,U,45,40,+1,N,N,N,3,L,0,'VECTOR FONT'\n" +
                    "P1",

            // Code39
            "SM10,0\n" +
                    "B178,196,0,2,6,100,0,0,'1234567890'\n" +    // Caution : The position is not (178,196)
                    "B150,468,0,4,10,200,0,0,'1234567890'\n" +
                    "P1",

            // BD1
            "SS3\n" +    // Set Speed to 5 ips
                    "SD20\n" +    // Set Density level to 20
                    "SW800\n" +    // Set Label Width to 800
                    "BD50,50,750,500,B,20\n" +
                    "T100,150,5,1,1,0,0,N,N,'Normal Mode'\n" +
                    "T100,300,5,1,1,0,0,R,N,'Reverse Mode'\n" +
                    "SOT\n" +
                    "P1",

            // BD3
            "SS3\n" +    // Set Printing Speed to 5 ips
                    "SD20\n" +    // Set Printing Density level to 20
                    "SW800\n" +    // Set Label Width to 800
                    "BD50,100,400,150,O\n" +    // Draw a block in Overwriting Mode
                    "BD50,200,400,250,O\n" +
                    "BD50,300,400,350,O\n" +
                    "BD100,50,150,400,E\n" +    // Draw a block in Exclusive OR mode
                    "BD200,50,250,400,E\n" +
                    "BD300,50,350,400,E\n" +
                    "BD500,200,700,400,O\n" +
                    "BD510,210,670,370,D\n" +    // Draw a block in Delete mode, namely Erase block area
                    "BD100,600,350,1000,O\n" +
                    "T50,700,5,1,1,0,0,N,N,'NORMAL'\n" +    // Write Text data on image buffer
                    "T50,800,5,1,1,0,0,N,N,'NORMAL'\n" +
                    "BD110,780,340,900,E\n" +
                    "T500,700,5,1,1,0,0,n,N,'TEST'\n" +
                    "BD480,680,700,800,E\n" +
                    "SOT\n" +    // Set Printing Orientation from Top to Bottom
                    "P1",    // Start Printing

            // BD4
            "SW800\n" +
                    "SM10,0\n" +
                    "BD100,300,550,330,O\n" +    // Overwrite mode
                    "BD200,200,250,430,O\n" +    // Overwrite mode
                    "BD400,200,450,430,E\n" +    // Exclusive OR mode
                    "P1",

            // BD5
            "CB\n" +
                    "SW800\n" +
                    "SM10,0\n" +
                    "BD100,300,300,500,O\n" +
                    "BD400,300,700,500,B,30\n" +    // Box mode, additional parameter follows
                    "P1",

            // Slope
            "CB\n" +
                    "SS3\n" +
                    "SD20\n" +
                    "SW8000\n" +
                    "BD100,300,300,800,S,100\n" +    // Slope mode, additional parameter follows
                    "BD600,300,400,800,S,100\n" +
                    "P1",

            // Set Media Type
            "^XA^LRN^MNY^MFN,N^LH15,12^MCY^POI^PW840^CI27" +
                    "^FT20,630^CVY^BD3^FH_^FD38738036071 [)>_1E01_1D961Z07001024_1DUPSN_1DWX9031_1D123_1D_1D1/1_1D15.9_1DN_1D_1DROME_1D_1E_04^FS" +
                    "^FT15,23^A0N,20,24^FVTEST FOR SHIP FROM, DC-SR, PAK^FS" +
                    "^FT15,42^A0N,20,24^FV(201) 828-1241 1241^FS" +
                    "^FT15,61^A0N,20,24^FVTEST FOR SIGNATURE  REQUIRED^FS" +
                    "^FT15,81^A0N,20,24^FVTEST PREPAID BILLING OPTION,^FS" +
                    "^FT15,100^A0N,20,24^FVPARAMUS  NJ 07652^FS" +
                    "^FT15,119^A0N,20,24^FVUNITED STATES^FS" +
                    "^FT60,181^A0N,26,30^FVDOMEXC206.BAG^FS" +
                    "^FT60,208^A0N,26,30^FV(201)825-5515^FS" +
                    "^FT60,236^A0N,26,30^FVTEST FOR KEY, PAK WT NO US DEST^FS" +
                    "^FT60,263^A0N,26,30^FVADDRESS LINE 3 HERE^FS" +
                    "^FT60,290^A0N,26,30^FVADDRESS LINE 2 HERE^FS" +
                    "^FT60,318^A0N,26,30^FVTEST FOR NO ISC IN UIS^FS" +
                    "^FT60,362^A0N,45,44^FVROME   36071^FS" +
                    "^FT60,404^A0N,45,44^FVITALY^FS" +
                    "^FT380,30^A0N,30,34^FV15.9 LBS ^FS" +
                    "^FT523,30^A0N,30,34^FVPAK ^FS" +
                    "^FT673,34^A0N,28,32^FV 1 OF 1^FS" +
                    "^FT500,69^A0N,22,26^FVSHP#: WX90 314L VNB^FS" +
                    "^FT500,91^A0N,22,26^FVSHP WT: 5.5 LBS^FS" +
                    "^FT500,113^A0N,22,26^FVDATE: 25 JUN 06^FS" +
                    "^FT500,136^A0N,22,26^FVAH ^FS" +
                    "^FT685,249^A0N,56,58^FVRFG^FS" +
                    "^FT620,736^A0N,100,76^FV1  ^FS" +
                    "^FT290,618^BY3^BCN,103,N,N,,A^FV42138036071^FS" +
                    "^FT255,493^A0N,80,70^FVITA 360 5-00^FS" +
                    "^FT10,704^A0N,56,58^FVUPS EXPRESS^FS" +
                    "^FT10,737^A0N,26,30^FVTRACKING #: 1Z WX9 031 D3 0700 1024^FS" +
                    "^FO0,762^GB800,4,4^FS" +
                    "^FT790,1039^A0N,22,26^FV ^FS" +
                    "^FT10,1035^A0N,22,26^FVBILLING: ^FS" +
                    "^FT126,1035^A0N,22,26^FVP/P ^FS" +
                    "^FT10,1059^A0N,22,26^FVSIGNATURE REQUIRED^FS" +
                    "^FT10,1083^A0N,22,26^FVEXCHANGE-LIKE ITEM ONLY^FS" +
                    "^FT10,1107^A0N,22,26^FVDESC: Test Merchandise Description^FS" +
                    "^FT10,1131^A0N,22,26^FVIMPORT CONTROL - PAYMENT GUARANTEED^FS" +
                    "^FT10,1151^A0N,22,26^FVRef 4: Ref 4 goes here^FS" +
                    "^FT15,153^A0N,28,32^FVSHIP TO: ^FS" +
                    "^FO0,637^GB798,14,14^FS" +

                    "^FO0,997^GB800,14,14^FS" +

                    "^FO0,416^GB800,4,4^FS" +

                    "^FO240,416^GB3,221,3^FS" +

                    "^FT190,1188^A0N,14,20^FVUOF 12.67.89 Eltron LP2442 12.34 07/2007^FS" +
                    "^FT105,982^BY3^BCN,202,N,N,,A^FV1ZWX9031D307001024^FS" +

                    "^XZ^XZ",

            // indo
            "! 0 200 200 2400 1\n" +
                    "BAR-SENSE\n" +
                    "PAGE-WIDTH 832\n" +
                    "T 7 0 135 55 AGUAS DE BELO HORIZONTE - MG\n" +
                    "T 7 0 30 100 RICARDO FERREIRA LEITE\n" +
                    "T 7 0 30 157 RUA POUSO ALEGRA - 265 - Cep:31000000\n" +
                    "FORM\n" +
                    "PRINT\n",
            ""
    };

    private EditText mCommandEdit;
    private CheckBox mCheckBox;
    private CheckBox mUsingResponseType;
    private Spinner mResponseTypeSelector;
    private EditText mResponseLengthEdit;

    private TextView mResultText;

    private AlertDialog mDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_direct_io);

        mCommandEdit = (EditText) findViewById(R.id.editText1);
        mResponseLengthEdit = (EditText) findViewById(R.id.editText2);
        mResponseTypeSelector = (Spinner) findViewById(R.id.spinner1);

        mResultText = (TextView) findViewById(R.id.textView5);

        mCheckBox = (CheckBox) findViewById(R.id.checkBox1);
        mCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mResponseLengthEdit.setEnabled(isChecked);
            }
        });
        mUsingResponseType = (CheckBox) findViewById(R.id.checkBox2);
        mUsingResponseType.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mResponseTypeSelector.setEnabled(isChecked);
            }
        });

        mResponseTypeSelector.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, RESPONSE_TYPE));

        Button button = (Button) findViewById(R.id.button1);
        button.setOnClickListener(this);
        button = (Button) findViewById(R.id.button2);
        button.setOnClickListener(this);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_direct_io, menu);
        return true;
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button1:
                showExampleDialog();
                break;

            case R.id.button2:
                executeDirectIo();
                break;
        }
    }

    private void showExampleDialog() {
        if (mDialog == null) {
            mDialog = new AlertDialog.Builder(DirectIoActivity.this).setTitle("Examples").setItems(EXAMPLE_ITEMS, new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int which) {
                    mCommandEdit.setText(EXAMPLES[which]);
                }
            }).create();
        }
        mDialog.show();
    }

    private void executeDirectIo() {
        String command = mCommandEdit.getText().toString();
        if (command.length() == 0) {
            Toast.makeText(getApplicationContext(), "Please input command", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean hasResponse = mCheckBox.isChecked();
        boolean usingResponseType = mUsingResponseType.isChecked();

        String string = mResponseLengthEdit.getText().toString();
        if (string.length() == 0) {
            Toast.makeText(getApplicationContext(), "Please input response length", Toast.LENGTH_SHORT).show();
            return;
        }
        int responseLength = Integer.parseInt(string);

        if (hasResponse) {
            if (usingResponseType) {
                switch ((String) mResponseTypeSelector.getSelectedItem()) {
                    case "RAW_DATA":
                        MainActivity.mBixolonLabelPrinter.executeDirectIo(command.getBytes(StandardCharsets.UTF_8), ResponseType.RAW_DATA, bytes -> {
                            Log.d("Testing", "read bytes: " + bytes.length);
                            StringBuilder hexString = new StringBuilder();
                            for (byte b : bytes) {
                                hexString.append(String.format("%02x", b));
                            }
                            String result = hexString.toString();
                            if (!result.isEmpty()) {
                                mResultText.setText(result + "\n" + new String(bytes));
                            }
                            Log.d("Testing", "directIO result: " + result);
                            return null;
                        });
                        break;
                    case "NULL":
                        MainActivity.mBixolonLabelPrinter.executeDirectIo(command.getBytes(StandardCharsets.UTF_8), ResponseType.NULL, bytes -> {
                            Log.d("Testing", "read bytes: " + bytes.length);
                            StringBuilder hexString = new StringBuilder();
                            for (byte b : bytes) {
                                hexString.append(String.format("%02x", b));
                            }
                            String result = hexString.toString();
                            Log.d("Testing", "directIO result: " + result);
                            if (!result.isEmpty()) {
                                mResultText.setText(result + "\n" + new String(bytes));
                            }
                            return null;
                        });
                        break;
                    case "CRLF":
                        MainActivity.mBixolonLabelPrinter.executeDirectIo(command.getBytes(StandardCharsets.UTF_8), ResponseType.CRLF, bytes -> {
                            Log.d("Testing", "read bytes: " + bytes.length);
                            StringBuilder hexString = new StringBuilder();
                            for (byte b : bytes) {
                                hexString.append(String.format("%02x", b));
                            }
                            String result = hexString.toString();
                            Log.d("Testing", "directIO result: " + result);
                            if (!result.isEmpty()) {
                                mResultText.setText(result + "\n" + new String(bytes));
                            }
                            return null;
                        });
                        break;
                    case "NONE":
                        MainActivity.mBixolonLabelPrinter.executeDirectIo(command.getBytes(StandardCharsets.UTF_8), ResponseType.NONE, bytes -> {

                            return null;
                        });
                        break;
                    case "STATUS_CHECK":
                        MainActivity.mBixolonLabelPrinter.executeDirectIo(command.getBytes(StandardCharsets.UTF_8), ResponseType.STATUS_CHECK, bytes -> {
                            Log.d("Testing", "read bytes: " + bytes.length);
                            StringBuilder hexString = new StringBuilder();
                            for (byte b : bytes) {
                                hexString.append(String.format("%02x", b));
                            }
                            String result = hexString.toString();
                            Log.d("Testing", "directIO result: " + result);
                            if (!result.isEmpty()) {
                                mResultText.setText(result);
                            }
                            return null;
                        });
                        break;
                    default:
                        mResultText.setText("");
                        break;
                }
            } else {
                MainActivity.mBixolonLabelPrinter.executeDirectIo(command.getBytes(StandardCharsets.UTF_8), true, 0);
            }

        } else {
            try {
                MainActivity.mBixolonLabelPrinter.executeDirectIo(new String(command.getBytes(StandardCharsets.UTF_8)), false, 0);
            } catch(Exception exception) {
                MainActivity.mBixolonLabelPrinter.executeDirectIo(command.getBytes(StandardCharsets.UTF_8), false, 0);
            }

//            MainActivity.mBixolonLabelPrinter.executeDirectIo(command, ResponseType.NONE, responseLength);
        }
        MainActivity.mBixolonLabelPrinter.executeDirectIo("P1", false, 0);
    }
}
