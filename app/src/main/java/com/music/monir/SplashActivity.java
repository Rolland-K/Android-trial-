package com.music.monir;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.music.monir.payment.util.Global;
import com.music.monir.payment.util.IabBroadcastReceiver;
import com.music.monir.payment.util.IabHelper;
import com.music.monir.payment.util.IabResult;
import com.music.monir.payment.util.Inventory;
import com.music.monir.payment.util.Purchase;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.trialy.library.Trialy;
import io.trialy.library.TrialyCallback;

import static io.trialy.library.Constants.STATUS_TRIAL_JUST_ENDED;
import static io.trialy.library.Constants.STATUS_TRIAL_JUST_STARTED;
import static io.trialy.library.Constants.STATUS_TRIAL_NOT_YET_STARTED;
import static io.trialy.library.Constants.STATUS_TRIAL_OVER;
import static io.trialy.library.Constants.STATUS_TRIAL_RUNNING;

public class SplashActivity extends AppCompatActivity {

    private String appKEY = "ODN7F4TU7GB2A0AQ6KV";
//    private String appKEY = "2KUNOKEJX1FUXLUYNQ4";
    private String SKU = "saath";
    final String OK = "OK";
    final String BUY_NOW = "BUY NOW";
    final String START_TRIAL = "START TRIAL";
    final String CLOSE = "CLOSE";
    Trialy mTrialy;
    boolean buying = false;

    String MEMBERSHIP = "membership";
    String IS_PREMIUM = "5RZZJLZVF5";
    Integer MY_PERMISSIONS_REQUEST_READ_STATE = 0;
    Integer MY_PERMISSIONS_REQUEST_READ_EXTERNAL = 1;

    public static SplashActivity self;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    Activity#requestPermissions
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_PHONE_STATE,Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_READ_STATE);
            }
            else
            {
                main();
            }
        }
        main();


    }

    private void main(){
        mTrialy = new Trialy(this, appKEY);
        File file = new File(getExternalFilesDir(null).getAbsoluteFile() + "/tanpura.mp3");
        if(file.exists())
            file.delete();
        checkExternalMedia();
        new Handler().postDelayed(new Runnable(){
            @Override
            public void run() {
                /* Create an Intent that will start the Menu-Activity. */
                try {
                    switch (verify()){
                        case 2:
                            _start("Premium");
                            break;
                        case 1:
                            mTrialy.checkTrial(SKU, mTrialyCallback);
                            break;
                    }
                }
                catch (Exception E){
                    Log.e("Error",E.toString());
                }
            }
        }, 2000);
    }



    private Integer verify() {
        switch (read_license()){
            case "verified":
                return 2;
            case "Invalid value":
                return 1;
            case "No license":
                return 1;
            default:
                return 1;
        }
    }

    private void _start(String membership) {
        Intent mainIntent = new Intent(SplashActivity.this, MusicChooseActivity.class);
        mainIntent.putExtra(MEMBERSHIP,membership);
        startActivity(mainIntent);
        finish();
    }

    private TrialyCallback mTrialyCallback = new TrialyCallback() {
        @Override
        public void onResult(int status, long timeRemaining, String sku) {
            int daysRemaining;
            switch (status){
                case STATUS_TRIAL_JUST_STARTED:
                    //The trial has just started - enable the premium features for the user
                    daysRemaining = Math.round(timeRemaining / (60 * 60 * 24));
                    //Toast.makeText(SplashActivity.this,"Just started" + String.valueOf(daysRemaining), Toast.LENGTH_SHORT).show();
                    showDialog("Trial started", String.format(Locale.ENGLISH, "You can try trial for %d days",  daysRemaining), OK);
                    break;
                case STATUS_TRIAL_RUNNING:
                    //The trial is currently running - enable the premium features for the user
                    daysRemaining = Math.round(timeRemaining / (60 * 60 * 24));
                    //Toast.makeText(SplashActivity.this,"Running" + String.valueOf(daysRemaining), Toast.LENGTH_SHORT).show();
                    String notification_message = "Your Trial remaining "+String.valueOf(daysRemaining)+" days";
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        notificationDialog("Trial Running", notification_message);
                    }
                    else{

                        Toast.makeText(SplashActivity.this, "Your Trial remaining "+String.valueOf(daysRemaining)+" days", Toast.LENGTH_LONG).show();
                        _start("Trial");
                    }
                    break;
                case STATUS_TRIAL_JUST_ENDED:
                    //The trial has just ended - block access to the premium features
                    daysRemaining = Math.round(timeRemaining / (60 * 60 * 24));
                    //Toast.makeText(SplashActivity.this,"Ended" + String.valueOf(daysRemaining), Toast.LENGTH_SHORT).show();
                    showDialog("Trial Ended", String.format(Locale.ENGLISH, "Your Trial is just ended. please buy to continue"), BUY_NOW);
                    break;
                case STATUS_TRIAL_NOT_YET_STARTED:
                    daysRemaining = Math.round(timeRemaining / (60 * 60 * 24));
                    //Toast.makeText(SplashActivity.this,"Not Started" + String.valueOf(daysRemaining), Toast.LENGTH_SHORT).show();
                    //The user hasn't requested a trial yet - no need to do anything
                    showDialog("Trial Mode", String.format(Locale.ENGLISH, "Do you want to start the trial?"), START_TRIAL);
                    break;
                case STATUS_TRIAL_OVER:
                    showDialog("Trial Ended", String.format(Locale.ENGLISH, "Your Trial is ended. please buy to continue features"), BUY_NOW);
//                    Intent intent = new Intent(SplashActivity.this, PurchaseActivity.class);
//                    intent.putExtra("BASE","ENDTRIAL");
//                    startActivity(intent);
                    break;
            }
            Log.i("TRIALY", "Returned status: " + Trialy.getStatusMessage(status));
        }

    };


    private void showDialog(String title, String message, String buttonLabel){

        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.trial_dialog);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.setCanceledOnTouchOutside(false);

        TextView dialog_msg = (TextView) dialog.findViewById(R.id.message);
        TextView dialog_title = (TextView)dialog.findViewById(R.id.dlg_title);
        dialog_title.setText(title);
        dialog_msg.setText(message);
        Button ok = (Button) dialog.findViewById(R.id.btn_ok);
        Button dismiss = (Button)dialog.findViewById(R.id.btn_dismiss);
        dismiss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        ok.setText(buttonLabel);
        switch (buttonLabel){
            case OK:
                ok.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                        _start("Trial");
                    }
                });
                break;
            case BUY_NOW:
                ok.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent  = new Intent(SplashActivity.this, PurchaseActivity.class);
                        intent.putExtra("BASE","BUY");
                        startActivity(intent);
                        dialog.dismiss();
                    }
                });
                break;
            case START_TRIAL:
                ok.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //start trial
                        mTrialy.startTrial(SKU, mTrialyCallback);
                        dialog.dismiss();
                    }
                });
                dismiss.setVisibility(View.VISIBLE);
                break;
            case CLOSE:
                ok.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //start trial
                        finish();
                        dialog.dismiss();
                    }
                });
        }
        dialog.show();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void notificationDialog(String creator, String message) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String NOTIFICATION_CHANNEL_ID = "tutorialspoint_01";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            @SuppressLint("WrongConstant") NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, creator, NotificationManager.IMPORTANCE_MAX);
            // Configure the notification channel.
            notificationChannel.setDescription(message);
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.setVibrationPattern(new long[]{0, 1000, 500, 1000});
            notificationChannel.enableVibration(true);
            notificationManager.createNotificationChannel(notificationChannel);
        }
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        notificationBuilder.setAutoCancel(false)
                .setDefaults(Notification.DEFAULT_ALL)
                .setWhen(System.currentTimeMillis())
                .setTicker("Tutorialspoint")
                //.setPriority(Notification.PRIORITY_MAX)
                .setContentTitle("Task status")
                .setContentText(message)
                .setContentInfo(creator);

        notificationBuilder.setSmallIcon(R.drawable.icon);

        notificationManager.notify(1, notificationBuilder.build());
        _start("Trial");
    }
    @Override
    public void onResume() {

        super.onResume();
        if (Global.mdefined)
            mTrialy.checkTrial(SKU, mTrialyCallback);
    }


    private void checkExternalMedia(){
        boolean mExternalStorageAvailable = false;
        boolean mExternalStorageWriteable = false;
        String state = Environment.getExternalStorageState();

        if (Environment.MEDIA_MOUNTED.equals(state)) {
            // Can read and write the media
            mExternalStorageAvailable = mExternalStorageWriteable = true;
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            // Can only read the media
            mExternalStorageAvailable = true;
            mExternalStorageWriteable = false;
        } else {
            // Can't read or write
            mExternalStorageAvailable = mExternalStorageWriteable = false;
        }

    }
    private String readRaw(){
        File root = android.os.Environment.getExternalStorageDirectory();
        File dir = new File (root.getAbsolutePath() + "/saathsangeet");
        File file = new File(dir, "License.so");

        StringBuilder text = new StringBuilder();

        try {
            BufferedReader brr = new BufferedReader(new FileReader(file));
            String line;

            while ((line = brr.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
            brr.close();
        }
        catch (IOException e) {
            return "No file";
        }
        return String.valueOf(text);
    }

    public String read_license(){

        String device_imei = "";
        Integer info_limit = 15;
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q)
        {
            device_imei = Settings.System.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
            info_limit = 16;


        }
        else {
            TelephonyManager TelephonyMgr = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    Activity#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for Activity#requestPermissions for more details.
                    Toast.makeText(this, "Need to Permission", Toast.LENGTH_LONG);
                }
            }
            device_imei = TelephonyMgr.getDeviceId();
        }
        String secred_string = readRaw();
        if (secred_string.equals("No file")) {
//            Toast.makeText(this, "No license" + device_imei, Toast.LENGTH_SHORT).show();
            return "No license";
        }
        String secred_index = secred_string.split(":")[0];

        ArrayList<String> sec_index = new ArrayList<>();
        StringBuilder s_index = new StringBuilder(secred_index);
        Integer default_i = 0;
        while (sec_index.size()<device_imei.length()) {
            Integer pointer = default_i + 1;
            String id = "";
            Integer length = Integer.parseInt(String.valueOf(s_index.charAt(default_i)));
            for (int j = 0; j < length; j++) {
                id += s_index.charAt(pointer + j);
            }
            sec_index.add(id);
            default_i = pointer + length;
        }
        Log.e("recover",String.valueOf(sec_index));

        String secret_id = "";
        StringBuilder key = new StringBuilder(secred_string);
        for (int i = 0; i < device_imei.length(); i ++){
            secret_id += key.charAt(Integer.parseInt(sec_index.get(i)) + secred_index.length() +2);
        }
        Log.e("secret",secret_id);


        if (secret_id.equals(device_imei)) {
//            Toast.makeText(this, "verified" + device_imei, Toast.LENGTH_SHORT).show();
            return "verified";
        }
        else {
//            Toast.makeText(this, "Invalid value" + device_imei, Toast.LENGTH_SHORT).show();
            return "Invalid value";
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 0: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    main();
                } else {
                    Toast.makeText(this, "Required permission",Toast.LENGTH_LONG).show();
                }
            }
        }
    }


}