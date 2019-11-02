package com.music.moniro;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import com.music.moniro.payment.payment_activity;

import java.util.Locale;

import io.trialy.library.Trialy;
import io.trialy.library.TrialyCallback;

import static io.trialy.library.Constants.STATUS_TRIAL_JUST_ENDED;
import static io.trialy.library.Constants.STATUS_TRIAL_JUST_STARTED;
import static io.trialy.library.Constants.STATUS_TRIAL_NOT_YET_STARTED;
import static io.trialy.library.Constants.STATUS_TRIAL_OVER;
import static io.trialy.library.Constants.STATUS_TRIAL_RUNNING;

public class SplashActivity extends AppCompatActivity {



    private String appKEY = "2KUNOKEJX1FUXLUYNQ4";
    private String SKU = "_test";
    final String OK = "OK";
    final String BUY_NOW = "BUY NOW";
    final String START_TRIAL = "START TRIAL";
    Trialy mTrialy;

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
                    mTrialy.checkTrial(SKU, mTrialyCallback);
                }
                catch (Exception E){}

            }
        }, 2000);

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
                    showDialog("Trial Mode", String.format(Locale.ENGLISH, "You can try premium features for 14 days."), START_TRIAL);
                    break;
                case STATUS_TRIAL_OVER:
                    showDialog("Trial Ended", String.format(Locale.ENGLISH, "Your Trial is ended. please buy to continue premium features"), BUY_NOW);
                    break;
            }
            Log.i("TRIALY", "Returned status: " + Trialy.getStatusMessage(status));
        }

    };

    private void showDialog(String title, String message, String buttonLabel){

        final Dialog dialog = new Dialog(SplashActivity.this);
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
                        Intent intent = new Intent(SplashActivity.this, payment_activity.class);
                        startActivity(intent);
                        finish();
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


}