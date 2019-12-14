package com.music.monir;

import android.Manifest;
import android.accounts.Account;
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
import android.util.Patterns;
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
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
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
import java.util.regex.Pattern;

import io.trialy.library.Trialy;

public class SplashActivity extends AppCompatActivity {


    final String BUY_NOW = "BUY NOW";
    final String CLOSE = "CLOSE";

    String MEMBERSHIP = "membership";
    Integer MY_PERMISSIONS_REQUEST_READ_STATE = 0;

    private String TAG = "AccountsActivityTAG";
    private String wantPermission = Manifest.permission.GET_ACCOUNTS;
    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final int REQUEST_CODE = 2;
    private static final int RC_SIGN_IN = 3;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        self = this;
        check_permission();
    }
    public void check_username(){
        SharedPreferences pref = getSharedPreferences(MODE_STATUS,MODE_PRIVATE);
        userEmail = pref.getString("user_email",null);
//        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
//                .requestEmail()
//                .build();
//
//        GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
//        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
//        startActivityForResult(signInIntent, RC_SIGN_IN);
//        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
////        Log.e("account",account.getEmail());

        if (userEmail==null){
            Intent googlePicker = AccountPicker.newChooseAccountIntent(null, null, new String[]{GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE},
                        true, null, null, null, null);
            startActivityForResult(googlePicker, REQUEST_CODE);
        }
        else
            get_payment_info();




    }

    public void check_permission(){
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
                check_username();
            }
        }
    }

    private void get_payment_info() {
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
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG,"Failed to rad value ", databaseError.toException());
                Toast.makeText(SplashActivity.this, "Failed to read Data", Toast.LENGTH_LONG).show();
            }

        });
        check_payment();
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
                        break;
                    }
                    else
                    {
                        editor.putString("status","Trial");
                        editor.apply();
                        main();
                        break;
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
        if( userEmail != null) {
            FirebaseApp.initializeApp(this);
            String id = "payment/" + userEmail.replace(".", "");
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference myRef = database.getReference(id + "/email");
            myRef.setValue(userEmail);
            myRef = database.getReference(id + "/date");
            myRef.setValue(getToday());
            myRef = database.getReference(id + "/status");
            myRef.setValue(mode);
            myRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    String value = dataSnapshot.getValue(String.class);
                    Log.d(TAG, "Value is:" + value);

                }


                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.w(TAG, "Failed to rad value ", databaseError.toException());
                }
            });
            main();
        }
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
        finish();
        startActivity(mainIntent);


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
                        dialog.dismiss();
                        finish();
                        startActivity(intent);


                    }
                });
                break;
            case CLOSE:
                ok.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //start trial
                        dialog.dismiss();
                        finish();

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
                    check_username();
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
                editor.apply();
            }
            Log.d(TAG, userEmail);
        }
        if (requestCode == RC_SIGN_IN) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
        if (userEmail == null){
            Toast.makeText(SplashActivity.this,"Waning. Your data will not saved to server", Toast.LENGTH_LONG).show();
        }
        get_payment_info();

    }
    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);

            // Signed in successfully, show authenticated UI.
            Log.e("account",account.getEmail());
        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w(TAG, "signInResult:failed code=" + e.getStatusCode());
//            updateUI(null);
        }
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

    public static SplashActivity getInstance(){
        return self;
    }

}