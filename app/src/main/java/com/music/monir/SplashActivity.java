package com.music.monir;

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
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import com.music.monir.payment.util.Global;
import com.music.monir.payment.util.IabBroadcastReceiver;
import com.music.monir.payment.util.IabHelper;
import com.music.monir.payment.util.IabResult;
import com.music.monir.payment.util.Inventory;
import com.music.monir.payment.util.Purchase;

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

    public static SplashActivity self;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        /* New Handler to start the Menu-Activity
         * and close this Splash-Screen after some seconds.*/
        mTrialy = new Trialy(this, appKEY);

        new Handler().postDelayed(new Runnable(){
            @Override
            public void run() {
                /* Create an Intent that will start the Menu-Activity. */
                try {
                    switch (verify()){
                        case 2:
                            _start();
                            break;
                        case 1:
                            mTrialy.checkTrial(SKU, mTrialyCallback);
                            break;
                        case 0:
                            Intent intent = new Intent(SplashActivity.this, PurchaseActivity.class);
                            intent.putExtra("BASE","CHECK");
                            startActivity(intent);
                    }
                }
                catch (Exception E){}

            }
        }, 2000);

    }



    private Integer verify() {
        try {
            SharedPreferences prefs = getSharedPreferences(IS_PREMIUM, MODE_PRIVATE);
            String is_premium = prefs.getString(MEMBERSHIP, "FALSE");//"No name defined" is the default value.
            if (is_premium.equals("TRUE"))
                return 2;
            else if (is_premium.equals("FALSE"))
                return 1;
        }
        catch (Exception E){
            return 0;
        }
        return null;
    }

    private void _start() {
        Intent mainIntent = new Intent(SplashActivity.this, MusicChooseActivity.class);
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
                    showDialog("Trial started", String.format(Locale.ENGLISH, "You can now try the premium features for %d days",  daysRemaining), OK);
                    break;
                case STATUS_TRIAL_RUNNING:
                    //The trial is currently running - enable the premium features for the user
                    daysRemaining = Math.round(timeRemaining / (60 * 60 * 24));
                    //Toast.makeText(SplashActivity.this,"Running" + String.valueOf(daysRemaining), Toast.LENGTH_SHORT).show();
                    String notification_message = "Your Trial remaining "+String.valueOf(daysRemaining)+" days";
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        notificationDialog("Trial Running", notification_message);
                    }
                    break;
                case STATUS_TRIAL_JUST_ENDED:
                    //The trial has just ended - block access to the premium features
                    daysRemaining = Math.round(timeRemaining / (60 * 60 * 24));
                    //Toast.makeText(SplashActivity.this,"Ended" + String.valueOf(daysRemaining), Toast.LENGTH_SHORT).show();
                    showDialog("Trial Ended", String.format(Locale.ENGLISH, "Your Trial is just ended. please buy to continue premium features"), BUY_NOW);
                    break;
                case STATUS_TRIAL_NOT_YET_STARTED:
                    daysRemaining = Math.round(timeRemaining / (60 * 60 * 24));
                    //Toast.makeText(SplashActivity.this,"Not Started" + String.valueOf(daysRemaining), Toast.LENGTH_SHORT).show();
                    //The user hasn't requested a trial yet - no need to do anything
                    showDialog("Trial Mode", String.format(Locale.ENGLISH, "Do you want trial?"), START_TRIAL);
                    break;
                case STATUS_TRIAL_OVER:
//                    showDialog("Trial Ended", String.format(Locale.ENGLISH, "Your Trial is ended. please buy to continue premium features"), BUY_NOW);
                    Intent intent = new Intent(SplashActivity.this, PurchaseActivity.class);
                    intent.putExtra("BASE","ENDTRIAL");
                    startActivity(intent);
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
                        _start();
                    }
                });
                break;
            case BUY_NOW:
                ok.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

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
        _start();
    }
    @Override
    public void onResume() {

        super.onResume();
        if (Global.mdefined)
            mTrialy.checkTrial(SKU, mTrialyCallback);
    }



    //    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
//        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
//            Log.d(TAG, "Purchase finished: " + result + ", purchase: "
//                    + purchase);
//            if (result.isFailure()) {
//                complain("Error purchasing: " + result);
//                // setWaitScreen(false);
//                return;
//            }
//            if (!verifyDeveloperPayload(purchase)) {
//                complain("Error purchasing. Authenticity verification failed.");
//                // setWaitScreen(false);
//                return;
//            }
//
//            Log.d(TAG, "Purchase successful.");
//
//
//            if (purchase.getSku().equals(SKU_PREMIUM)) {
//                // bought the premium upgrade!
//                Log.d(TAG, "Purchase is premium upgrade. Congratulating user.");
//                mIsPremium = true;
//            }
//        }
//
//        };



}