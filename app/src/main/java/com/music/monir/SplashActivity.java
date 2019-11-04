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

public class SplashActivity extends AppCompatActivity implements IabBroadcastReceiver.IabBroadcastListener,
        DialogInterface.OnClickListener{

    private String appKEY = "ODN7F4TU7GB2A0AQ6KV";
    private String SKU = "saath";
    final String OK = "OK";
    final String BUY_NOW = "BUY NOW";
    final String START_TRIAL = "START TRIAL";
    final String CLOSE = "CLOSE";
    Trialy mTrialy;
    boolean InvalidDeveice = false;

    String IS_PREMIUM = "5RZZJLZVF5";
    String MEMBERSHIP = "membership";
    String base64EncodedPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAs0p78+rUMwrVUUGzA43bpsGxQoeA2UFVAV9uph6+Z5sjfkkXTkaNJj7igSdVaM+Dj+NNgfV7zkhzD1y2EkvKsX7Zy1yXPTQjcS+ddg8uz+HCy/FZKnwVHgJPRxdXujLl40iyB1NrVJUtI3nbzOAbsS2PJBybJC9JMCnoBro/+1AfI1JMPNwCid8lV8TwAYqWl7KOCQD8uQQbqNTfS1GAOY5TS7bi/vicF/m+YlCjyKCD6XnNfNDm2liJLi07mwd/FlV6dM1/5IE9/kd2RhvRRXg9MVGIdYKtHH5nf/Ru11rZw0SKFaTvXdW5lwP9nwQOzmeBajxWyqGO5KYHLuq1ZwIDAQAB";

    static final String TAG = "TrivialDrive";

    // Does the user have the premium upgrade?
    static boolean mIsPremium = false;


    // Tracks the currently owned infinite gas SKU, and the options in the Manage dialog
    String mInfiniteGasSku = "";
    String mFirstChoiceSku = "";
    String mSecondChoiceSku = "";

    // Used to select between purchasing gas on a monthly or yearly basis
    String mSelectedSubscriptionPeriod = "";

    // SKUs for our products: the premium upgrade (non-consumable) and gas (consumable)
    static final String SKU_PREMIUM = "membership_premium";


    // SKU for our subscription (infinite gas)

    // (arbitrary) request code for the purchase flow
    static final int RC_REQUEST = 10001;

    // Graphics for the gas gauge

    // How many units (1/4 tank is our unit) fill in the tank.

    // Current amount of gas in tank, in units
    int mTank;

    // The helper object
    IabHelper mHelper;

    // Provides purchase notification while this app is running
    IabBroadcastReceiver mBroadcastReceiver;
    public static SplashActivity self;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        /* New Handler to start the Menu-Activity
         * and close this Splash-Screen after some seconds.*/
        mTrialy = new Trialy(this, appKEY);
        loadData();
        self = this;

        init();




        new Handler().postDelayed(new Runnable(){
            @Override
            public void run() {
                /* Create an Intent that will start the Menu-Activity. */
                try {
                    if (verify())
                    {
                        _start();
                    }
                    else {
                        mTrialy.checkTrial(SKU, mTrialyCallback);
                    }
                }
                catch (Exception E){}

            }
        }, 2000);

    }

    private void init() {

        mHelper = new IabHelper(this, base64EncodedPublicKey);

        // enable debug logging (for a production application, you should set this to false).
        mHelper.enableDebugLogging(true);

        // Start setup. This is asynchronous and the specified listener
        // will be called once setup completes.
        Log.d(TAG, "Starting setup.");
        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                Log.d(TAG, "Setup finished.");

                if (!result.isSuccess()) {
                    // Oh noes, there was a problem.
                    complain("Problem setting up in-app billing: " + result);
                    InvalidDeveice = true;
                    return;
                }

                // Have we been disposed of in the meantime? If so, quit.
                if (mHelper == null) return;

                // Important: Dynamically register for broadcast messages about updated purchases.
                // We register the receiver here instead of as a <receiver> in the Manifest
                // because we always call getPurchases() at startup, so therefore we can ignore
                // any broadcasts sent while the app isn't running.
                // Note: registering this listener in an Activity is a bad idea, but is done here
                // because this is a SAMPLE. Regardless, the receiver must be registered after
                // IabHelper is setup, but before first call to getPurchases().
                mBroadcastReceiver = new IabBroadcastReceiver(SplashActivity.this);
                IntentFilter broadcastFilter = new IntentFilter(IabBroadcastReceiver.ACTION);
                registerReceiver(mBroadcastReceiver, broadcastFilter);

                // IAB is fully set up. Now, let's get an inventory of stuff we own.
                Log.d(TAG, "Setup successful. Querying inventory.");
                try {
                    mHelper.queryInventoryAsync(mGotInventoryListener);
                } catch (IabHelper.IabAsyncInProgressException e) {
                    complain("Error querying inventory. Another async operation in progress.");
                }
            }
        });
    }

    private boolean verify() {

        if (mIsPremium)
            return true;
        else
            return false;

        /*
        SharedPreferences prefs = getSharedPreferences(IS_PREMIUM, MODE_PRIVATE);
        String is_premium = prefs.getString(MEMBERSHIP, "FALSE");//"No name defined" is the default value.
        if (is_premium.equals("TRUE"))
            return true;
        else
            return false;
         */
    }

    private void upgrade_membership(){

        SharedPreferences.Editor editor = getSharedPreferences(IS_PREMIUM, MODE_PRIVATE).edit();
        editor.putString(MEMBERSHIP, "TRUE");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            editor.apply();
        }

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
//                        Intent intent = new Intent(SplashActivity.this, payment_activity.class);
//                        startActivity(intent);
                        onUpgradeAppButtonClicked();
//                        finish();
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
    IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            Log.d(TAG, "Query inventory finished.");

            // Have we been disposed of in the meantime? If so, quit.
            if (mHelper == null) return;

            // Is it a failure?
            if (result.isFailure()) {
                complain("Failed to query inventory: " + result);
                return;
            }

            Log.d(TAG, "Query inventory was successful.");

            /*
             * Check for items we own. Notice that for each purchase, we check
             * the developer payload to see if it's correct! See
             * verifyDeveloperPayload().
             */

            // Do we have the premium upgrade?
            Purchase premiumPurchase = inventory.getPurchase(SKU_PREMIUM);
            mIsPremium = (premiumPurchase != null && verifyDeveloperPayload(premiumPurchase));
            Log.d(TAG, "User is " + (mIsPremium ? "PREMIUM" : "NOT PREMIUM"));

            /*
            // First find out which subscription is auto renewing
            Purchase gasMonthly = inventory.getPurchase(SKU_INFINITE_GAS_MONTHLY);
            Purchase gasYearly = inventory.getPurchase(SKU_INFINITE_GAS_YEARLY);
            if (gasMonthly != null && gasMonthly.isAutoRenewing()) {
                mInfiniteGasSku = SKU_INFINITE_GAS_MONTHLY;
                mAutoRenewEnabled = true;
            } else if (gasYearly != null && gasYearly.isAutoRenewing()) {
                mInfiniteGasSku = SKU_INFINITE_GAS_YEARLY;
                mAutoRenewEnabled = true;
            } else {
                mInfiniteGasSku = "";
                mAutoRenewEnabled = false;
            }


            // The user is subscribed if either subscription exists, even if neither is auto
            // renewing
            mSubscribedToInfiniteGas = (gasMonthly != null && verifyDeveloperPayload(gasMonthly))
                    || (gasYearly != null && verifyDeveloperPayload(gasYearly));
            Log.d(TAG, "User " + (mSubscribedToInfiniteGas ? "HAS" : "DOES NOT HAVE")
                    + " infinite gas subscription.");
            if (mSubscribedToInfiniteGas) mTank = TANK_MAX;

             */

            // Check for gas delivery -- if we own gas, we should fill up the tank immediately
//            Purchase gasPurchase = inventory.getPurchase(SKU_GAS);
            /*
            if (gasPurchase != null && verifyDeveloperPayload(gasPurchase)) {
                Log.d(TAG, "We have gas. Consuming it.");
                try {
                    mHelper.consumeAsync(inventory.getPurchase(SKU_GAS), mConsumeFinishedListener);
                } catch (IabHelper.IabAsyncInProgressException e) {
                    complain("Error consuming gas. Another async operation in progress.");
                }
                return;
            }*/
            setWaitScreen(false);
            Log.d(TAG, "Initial inventory query finished; enabling main UI.");
        }
    };




    public void onUpgradeAppButtonClicked() {
        init();
        if (InvalidDeveice){
            try {
                showDialog("Invalid Device", "Your Device isn't supprot google Billing.", CLOSE);
            }catch (Exception E){
                Log.e("Error", E.toString());
            }
            try {
                Toast.makeText(MusicChooseActivity.getInstance(),"Your Device isn't supprot google Billing.", Toast.LENGTH_LONG).show();
            }catch (Exception E){
                Log.e("Error", E.toString());
            }

        }
        else {
            onUpgradeAppButtonClicked();

            Log.d(TAG, "Upgrade button clicked; launching purchase flow for upgrade.");
            setWaitScreen(true);

            /* TODO: for security, generate your payload here for verification. See the comments on
             *        verifyDeveloperPayload() for more info. Since this is a SAMPLE, we just use
             *        an empty string, but on a production app you should carefully generate this. */
            String payload = "";

            try {
                mHelper.launchPurchaseFlow(this, SKU_PREMIUM, RC_REQUEST,
                        mPurchaseFinishedListener, payload);
            } catch (IabHelper.IabAsyncInProgressException e) {
                complain("Error launching purchase flow. Another async operation in progress.");
                setWaitScreen(false);
            }
        }
    }



    void loadData() {
        SharedPreferences sp = getPreferences(MODE_PRIVATE);
        mTank = sp.getInt("tank", 2);
        Log.d(TAG, "Loaded data: tank = " + String.valueOf(mTank));
    }


    @Override
    public void onClick(DialogInterface dialog, int id) {
        if (id == 0 /* First choice item */) {
            mSelectedSubscriptionPeriod = mFirstChoiceSku;
        } else if (id == 1 /* Second choice item */) {
            mSelectedSubscriptionPeriod = mSecondChoiceSku;
        } else if (id == DialogInterface.BUTTON_POSITIVE /* continue button */) {
            /* TODO: for security, generate your payload here for verification. See the comments on
             *        verifyDeveloperPayload() for more info. Since this is a SAMPLE, we just use
             *        an empty string, but on a production app you should carefully generate
             *        this. */
            String payload = "";

            if (TextUtils.isEmpty(mSelectedSubscriptionPeriod)) {
                // The user has not changed from the default selection
                mSelectedSubscriptionPeriod = mFirstChoiceSku;
            }

            List<String> oldSkus = null;
            if (!TextUtils.isEmpty(mInfiniteGasSku)
                    && !mInfiniteGasSku.equals(mSelectedSubscriptionPeriod)) {
                // The user currently has a valid subscription, any purchase action is going to
                // replace that subscription
                oldSkus = new ArrayList<String>();
                oldSkus.add(mInfiniteGasSku);
            }

            setWaitScreen(true);
            Log.d(TAG, "Launching purchase flow for gas subscription.");
            try {
                mHelper.launchPurchaseFlow(this, mSelectedSubscriptionPeriod, IabHelper.ITEM_TYPE_SUBS,
                        oldSkus, RC_REQUEST, mPurchaseFinishedListener, payload);
            } catch (IabHelper.IabAsyncInProgressException e) {
                complain("Error launching purchase flow. Another async operation in progress.");
                setWaitScreen(false);
            }
            // Reset the dialog options
            mSelectedSubscriptionPeriod = "";
            mFirstChoiceSku = "";
            mSecondChoiceSku = "";
        } else if (id != DialogInterface.BUTTON_NEGATIVE) {
            // There are only four buttons, this should not happen
            Log.e(TAG, "Unknown button clicked in subscription dialog: " + id);
        }



    }



    boolean verifyDeveloperPayload(Purchase p) {
        String payload = p.getDeveloperPayload();

        /*
         * TODO: verify that the developer payload of the purchase is correct. It will be
         * the same one that you sent when initiating the purchase.
         *
         * WARNING: Locally generating a random string when starting a purchase and
         * verifying it here might seem like a good approach, but this will fail in the
         * case where the user purchases an item on one device and then uses your app on
         * a different device, because on the other device you will not have access to the
         * random string you originally generated.
         *
         * So a good developer payload has these characteristics:
         *
         * 1. If two different users purchase an item, the payload is different between them,
         *    so that one user's purchase can't be replayed to another user.
         *
         * 2. The payload must be such that you can verify it even when the app wasn't the
         *    one who initiated the purchase flow (so that items purchased by the user on
         *    one device work on other devices owned by the user).
         *
         * Using your own server to store and verify developer payloads across app
         * installations is recommended.
         */

        return true;
    }
    //Here!
    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            Log.d(TAG, "Purchase finished: " + result + ", purchase: " + purchase);

            // if we were disposed of in the meantime, quit.
            if (mHelper == null) return;

            if (result.isFailure()) {
                complain("Error purchasing: " + result);
                setWaitScreen(false);
                return;
            }
            if (!verifyDeveloperPayload(purchase)) {
                complain("Error purchasing. Authenticity verification failed.");
                setWaitScreen(false);
                return;
            }

            Log.d(TAG, "Purchase successful.");

            if (purchase.getSku().equals(SKU_PREMIUM)) {
                // bought the premium upgrade!
                Log.d(TAG, "Purchase is premium upgrade. Congratulating user.");
                alert("Thank you for Buying App!");
                mIsPremium = true;
                upgrade_membership();

                /*
                SharedPreferences.Editor editor = getSharedPreferences(MEMBERSHIP, MODE_PRIVATE).edit();
                editor.putString(IS_PREMIUM, "TRUE");
                editor.apply();
                 */
                setWaitScreen(false);
            }
        }
    };



    @Override
    public void onDestroy() {
        super.onDestroy();

        // very important:
        if (mBroadcastReceiver != null) {
            unregisterReceiver(mBroadcastReceiver);
        }

        // very important:
        Log.d(TAG, "Destroying helper.");
        if (mHelper != null) {
            try {
                mHelper.disposeWhenFinished();
            }catch (Exception E){
                Log.e("Error: ", E.toString());
            }
            mHelper = null;
        }
    }



    // Enables or disables the "please wait" screen.
    void setWaitScreen(boolean set) {
//        findViewById(R.id.screen_main).setVisibility(set ? View.GONE : View.VISIBLE);
//        findViewById(R.id.screen_wait).setVisibility(set ? View.VISIBLE : View.GONE);
    }

    void complain(String message) {
        Log.e(TAG, "**** TrivialDrive Error: " + message);

        //alert("Error: " + message);
    }

    void alert(String message) {
        AlertDialog.Builder bld = new AlertDialog.Builder(this);
        bld.setMessage(message);
        bld.setNeutralButton("OK", null);
        Log.d(TAG, "Showing alert dialog: " + message);
        bld.create().show();
    }

    public static SplashActivity getInstance(){
        return self;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult(" + requestCode + "," + resultCode + "," + data);
        if (mHelper == null) return;

        // Pass on the activity result to the helper for handling
        if (!mHelper.handleActivityResult(requestCode, resultCode, data)) {
            // not handled, so handle it ourselves (here's where you'd
            // perform any handling of activity results not related to in-app
            // billing...
            super.onActivityResult(requestCode, resultCode, data);
        }
        else {
            Log.d(TAG, "onActivityResult handled by IABUtil.");
        }
    }

    @Override
    public void receivedBroadcast() {
        // Received a broadcast notification that the inventory of items has changed
        Log.d(TAG, "Received broadcast notification. Querying inventory.");
        try {
            mHelper.queryInventoryAsync(mGotInventoryListener);
        } catch (IabHelper.IabAsyncInProgressException e) {
            complain("Error querying inventory. Another async operation in progress.");
        }
    }
}