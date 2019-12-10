package com.music.monir;

import android.Manifest;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.AccountPicker;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import io.trialy.library.Trialy;

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

    private String TAG = "AccountsActivityTAG";
    private String wantPermission = Manifest.permission.GET_ACCOUNTS;
    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final int REQUEST_CODE = 2;

    final Integer PAID = 2;
    final Integer TRIAL = 1;
    final Integer END_TRIAL = 0;
    final String MODE_STATUS = "trial_mode_status";
    private static Calendar calendar;
    private static SimpleDateFormat dateFormat;
    private static String date;
    String daysRemaining = "";
    public static SplashActivity self;
    public static String userEmail = null;
    public ArrayList<user> array_user = new ArrayList<>();
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        check_username();
    }
    private void check_username(){
        SharedPreferences pref = getSharedPreferences(MODE_STATUS,MODE_PRIVATE);
        String useremail = pref.getString("user_email","None");
        if (useremail.equals("None")){
            Intent googlePicker = AccountPicker.newChooseAccountIntent(null, null, new String[]{GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE},
                        true, null, null, null, null);
            startActivityForResult(googlePicker, REQUEST_CODE);
        }
        else
            check_permission(useremail);
    }

    private void check_permission(String useremail){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    Activity#requestPermissions
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.GET_ACCOUNTS},
                        MY_PERMISSIONS_REQUEST_READ_STATE);
            }
            else
            {
                get_payment_info(useremail);
            }
        }
    }

    private void get_payment_info(String useremail) {
        FirebaseApp.initializeApp(this);
        try {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("payment");
        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    HashMap<String, Object> dataMap = (HashMap<String, Object>) dataSnapshot.getValue();
                    for (String key : dataMap.keySet()) {

                        Object data = dataMap.get(key);
                        try {
                            HashMap<String, Object> userData = (HashMap<String, Object>) data;
                            String email = userData.get("email").toString();
                            String date = userData.get("date").toString();
                            String status = userData.get("status").toString();
                            user user = new user(email, date, status);
                            array_user.add(user);
                        }
                        catch (Exception e){
                            Log.e(TAG,e.toString());
                        }
                    }
                    Log.e("Length",String.valueOf(array_user.size()));
                    check_payment();

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG,"Failed to rad value ", databaseError.toException());
                Toast.makeText(SplashActivity.this, "Failed to read Data", Toast.LENGTH_LONG).show();
                progressDialog.dismiss();
            }

        });
    }

    private void check_payment(){
        if(userEmail!=null) {
            boolean is_new = true;
            for (int i = 0; i < array_user.size(); i++) {
                if (array_user.get(i).getEmail().equals(userEmail)) {
                    SharedPreferences.Editor editor = getSharedPreferences(MODE_STATUS, MODE_PRIVATE).edit();
                    editor.putString("start_date",array_user.get(i).getDate());
                    is_new = false;
                    if (array_user.get(i).getStatus().equals("paid"))
                    {
                        editor.putString("status","Paid");
                        editor.apply();
                        main();
                    }
                    else
                    {
                        editor.putString("status","Trial");
                        editor.apply();
                        main();
                    }
                }
            }
            if (is_new)
                upload_trial_data(userEmail, "trial");
        }
        else{
            main();
        }

    }

    private void upload_trial_data(String userEmail,String mode) {
        FirebaseApp.initializeApp(this);
        String id = "payment/" + userEmail.split("@")[0].replace(".","") + getToday().replace("/","_");
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference(id+"/email");
        myRef.setValue(userEmail);
        myRef = database.getReference(id+"/date");
        myRef.setValue(getToday());
        myRef = database.getReference(id+"/status");
        myRef.setValue(mode);
        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String value = dataSnapshot.getValue(String.class);
                Log.d(TAG, "Value is:" + value);
                Toast.makeText(SplashActivity.this,"Your data saved to server successfully",Toast.LENGTH_LONG).show();
                main();
            }


            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG,"Failed to rad value ", databaseError.toException());
                Toast.makeText(SplashActivity.this,"Data saving failed"+databaseError.toString(),Toast.LENGTH_LONG).show();
                main();
            }
        });
    }

    private void main(){

        File file = new File(getExternalFilesDir(null).getAbsoluteFile() + "/tanpura.mp3");
        if(file.exists())
            file.delete();
        new Handler().postDelayed(new Runnable(){
            @Override
            public void run() {
                /* Create an Intent that will start the Menu-Activity. */
                try {
                    Integer in_trial = is_in_trial();
                    switch (in_trial){
                        case 2:
                            _start("Premium");
                            break;
                        case 1:
                            //Toast.makeText(SplashActivity.this,"Running" + String.valueOf(daysRemaining), Toast.LENGTH_SHORT).show();
                            String notification_message = "Your Trial remaining "+daysRemaining+" days";
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                notificationDialog("Trial Running", notification_message);
                            }
                            else{

                                Toast.makeText(SplashActivity.this, "Your Trial remaining "+String.valueOf(daysRemaining)+" days", Toast.LENGTH_LONG).show();
                                _start("Trial");
                            }
                            break;
                        case 0:
                            showDialog("Trial Ended", String.format(Locale.ENGLISH, "Your Trial is just ended. please buy to continue"), BUY_NOW);
                            break;

                    }
                }
                catch (Exception E){
                    Log.e("Error",E.toString());
                }
            }
        }, 2000);
    }

    private Integer is_in_trial(){
        SharedPreferences pref = getSharedPreferences(MODE_STATUS,MODE_PRIVATE);
        String status = pref.getString("status","None");
        String start_date = pref.getString("start_date",getToday());
        switch (status){
            case "None":
                SharedPreferences.Editor editor = getSharedPreferences(MODE_STATUS, MODE_PRIVATE).edit();
                editor.putString("status","Trial");
                editor.putString("start_date",getToday());
                editor.apply();
                daysRemaining = "5";
                return TRIAL;
            case "Trial":

                Integer remain = 5-Integer.parseInt(getCountOfDays(start_date,getToday()));
                if (remain > 0){
                    daysRemaining = String.valueOf(remain);
                    return  TRIAL;
                }
                else
                    return END_TRIAL;
            case "Paid":
                return PAID;
            default:
                return TRIAL;

        }
    }

    private void _start(String membership) {
        Intent mainIntent = new Intent(SplashActivity.this, MusicChooseActivity.class);
        mainIntent.putExtra(MEMBERSHIP,membership);
        startActivity(mainIntent);
        finish();
    }

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
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 0: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    get_payment_info(userEmail);
                } else {
                    Toast.makeText(this, "Required permission",Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode,
                                    final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            userEmail = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
            if (!userEmail.equals("")){
                SharedPreferences.Editor editor = getSharedPreferences(MODE_STATUS, MODE_PRIVATE).edit();
                editor.putString("user_email",userEmail);
            }
            Log.d(TAG, userEmail);
        }
        if (userEmail == null){
            Toast.makeText(SplashActivity.this,"Waning. Your data will not save to server", Toast.LENGTH_LONG).show();
        }
        check_permission(userEmail);

    }

    public static String getToday(){
        calendar = Calendar.getInstance();
        dateFormat = new SimpleDateFormat("MM/dd/yyyy");
        date = dateFormat.format(calendar.getTime());
        return date.toString();
    }

    public static String getCountOfDays(String start_date, String end_date) {
        if((!(start_date.equals("")))&&(!end_date.equals(""))) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());

            Date createdConvertedDate = null, expireCovertedDate = null, todayWithZeroTime = null;
            try {
                createdConvertedDate = dateFormat.parse(start_date);
                expireCovertedDate = dateFormat.parse(end_date);

                Date today = new Date();

                todayWithZeroTime = dateFormat.parse(dateFormat.format(today));
            } catch (ParseException e) {
                e.printStackTrace();
            }

            int cYear = 0, cMonth = 0, cDay = 0;

            if (createdConvertedDate.before(todayWithZeroTime)) {
                Calendar cCal = Calendar.getInstance();
                cCal.setTime(createdConvertedDate);
                cYear = cCal.get(Calendar.YEAR);
                cMonth = cCal.get(Calendar.MONTH);
                cDay = cCal.get(Calendar.DAY_OF_MONTH);

            } else {
                Calendar cCal = Calendar.getInstance();
                cCal.setTime(todayWithZeroTime);
                cYear = cCal.get(Calendar.YEAR);
                cMonth = cCal.get(Calendar.MONTH);
                cDay = cCal.get(Calendar.DAY_OF_MONTH);
            }
    /*Calendar todayCal = Calendar.getInstance();
    int todayYear = todayCal.get(Calendar.YEAR);
    int today = todayCal.get(Calendar.MONTH);
    int todayDay = todayCal.get(Calendar.DAY_OF_MONTH);
    */
            Calendar eCal = Calendar.getInstance();
            eCal.setTime(expireCovertedDate);

            int eYear = eCal.get(Calendar.YEAR);
            int eMonth = eCal.get(Calendar.MONTH);
            int eDay = eCal.get(Calendar.DAY_OF_MONTH);

            Calendar date1 = Calendar.getInstance();
            Calendar date2 = Calendar.getInstance();

            date1.clear();
            date1.set(cYear, cMonth, cDay);
            date2.clear();
            date2.set(eYear, eMonth, eDay);

            long diff = date2.getTimeInMillis() - date1.getTimeInMillis();

            float dayCount = (float) diff / (24 * 60 * 60 * 1000);
            if(dayCount > 0) {
                String duration = ("" + (int) dayCount);
                return duration;
            }
            else {

            }
        }
        return "0";
    }

}