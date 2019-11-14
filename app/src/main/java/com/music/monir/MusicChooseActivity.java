package com.music.monir;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class MusicChooseActivity extends AppCompatActivity {

    private ListView mListMusic;
    private CustomAdapter mAdapter;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private ArrayList<MusicItem> arrMusicItems;
    private ArrayList<String> arrMusicTitle = new ArrayList<String>();
    private ArrayList<String> arrFilteredTitle = new ArrayList<String>();
    private ArrayList<String> arrFilteredsecibdTitle = new ArrayList<String>();
    private ArrayList<String> arrFilteredSecondName = new ArrayList<String>();
    private ArrayList<String> arrFirstName = new ArrayList<String>();

    private ImageView ivPurchase;

    private Spinner mFirstName;
    CustomSpinnerAdapter spinnerArrayAdapter;
    CustomSpinnerAdapter spinnerArraySecondAdapter;
    private Spinner mSecondName;
    public boolean set_filter;
    protected final int STORAGE_PERMISSIONS_REQUEST_CODE = 100;

    // Progress Dialog
    private ProgressDialog pDialog;
    public static final int progress_bar_type = 0;
    private String mCurrentMusicName = "";
    public static MusicChooseActivity self;
    String MEMBERSHIP = "membership";

    class SortbyName implements Comparator<MusicItem>
    {
        // Used for sorting in ascending order of
        // roll number
        public int compare(MusicItem a, MusicItem b)
        {
            return a.toString().compareTo(b.toString());
        }
    }


    class SortbyString implements Comparator<String>
    {
        // Used for sorting in ascending order of
        // roll number
        public int compare(String a, String b)
        {
            return a.compareTo(b);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_choose);
        self = this;
        mListMusic = findViewById(R.id.list_music);
        mFirstName = findViewById(R.id.spin_firstname);
        mSecondName = findViewById(R.id.spin_secondname);
        arrMusicTitle = new ArrayList<String>();
//        mAdapter = new CustomAdapter(arrMusicTitle, getApplicationContext());
//        mListMusic.setAdapter(mAdapter);
        ivPurchase = findViewById(R.id.ivPurchase);
        Intent intent = getIntent();
        if (intent.getStringExtra(MEMBERSHIP).equals("Trial"))
            ivPurchase.setVisibility(View.VISIBLE);
        else
            ivPurchase.setVisibility(View.INVISIBLE);

        ivPurchase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent  = new Intent(MusicChooseActivity.this, PurchaseActivity.class);
                intent.putExtra("BASE","BUY");
                startActivity(intent);
//                SplashActivity.getInstance().onUpgradeAppButtonClicked();
            }
        });
        if(!isPermissionGranted()){
            requestPermissions();
        }


        arrMusicItems = new ArrayList<MusicItem>();
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mDatabase.child("convalidaciones").addChildEventListener(new ChildEventListener(){
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                MusicItem item = dataSnapshot.getValue(MusicItem.class);
                arrMusicItems.add(item);
                arrMusicTitle.add(item.toString());
                //arrMusicItems = sortByName(arrMusicItems);
                Collections.sort(arrMusicTitle, new SortbyString());
                Collections.sort(arrMusicItems, new SortbyName());

                if (is_new(item.toString().split("_")[0],arrFirstName))
                {
                    arrFirstName.add(item.toString().split("_")[0]);
                    Log.e("Check if it is running",item.toString().split("_")[0]);
                    setadapter(arrFirstName,mFirstName);
                }
//                mAdapter = new CustomAdapter(arrMusicTitle, getApplicationContext());
//                mListMusic.setAdapter(mAdapter);

            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        mListMusic.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                mCurrentMusicName = arrMusicItems.get(position).toString();
                String selected_name = arrFilteredsecibdTitle.get(position);
                for (int i = 0; i < arrMusicItems.size(); i ++){
                    MusicItem item = arrMusicItems.get(i);
                    if ((item.getInstrument().toString()
                            + "_"
                            +item.getBeats().toString())
                            .equals(selected_name.split("_")[0]
                            + "_"
                            +selected_name.split("_")[1]))
                    {
                        new DownloadFileFromURL().execute(arrMusicItems.get(i).getUrl());
                        break;
                    }
                }

            }
        });

        mFirstName.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filter_first(arrFirstName.get(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        mSecondName.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filter_second(mFirstName.getSelectedItem().toString(),mSecondName.getSelectedItem().toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }


    public ArrayList<MusicItem> sortByName ( ArrayList<MusicItem> contacts)
    {
        String contactName1, contactName2;

        for (int i = 0; i < contacts.size ( ); i++ )
        {
            contactName1 = contacts.get (i).toString();
            for (int n = i + 1; n < contacts.size ( )-1; n++ )
            {
                contactName2 = contacts.get (n).toString ( );

                if (contactName1.compareToIgnoreCase (contactName2) > 0)
                {
                    MusicItem temp = contacts.get (i);
                    contacts.set (i, contacts.get (n));
                    contacts.set (n, temp);
                }
            }
        }
        return contacts;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 0: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }
            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }


    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case progress_bar_type: // we set this to 0
                pDialog = new ProgressDialog(this);
                pDialog.setMessage("Downloading file. Please wait...");
                pDialog.setIndeterminate(false);
                pDialog.setMax(100);
                pDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                pDialog.setCancelable(true);
                pDialog.show();
                return pDialog;
            default:
                return null;
        }
    }

    /**
     * Background Async Task to download file
     * */
    class DownloadFileFromURL extends AsyncTask<String, String, String> {

        /**
         * Before starting background thread Show Progress Bar Dialog
         * */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showDialog(progress_bar_type);
        }

        /**
         * Downloading file in background thread
         * */
        @Override
        protected String doInBackground(String... f_url) {
            int count;
            try {
                URL url = new URL(f_url[0]);
                URLConnection conection = url.openConnection();
                conection.connect();

                // this will be useful so that you can show a tipical 0-100%
                // progress bar
                int lenghtOfFile = conection.getContentLength();

                // download the file
                InputStream input = new BufferedInputStream(url.openStream(),
                        8192);

                // Output stream
                OutputStream output = new FileOutputStream(getExternalFilesDir(null).getAbsoluteFile() + "/temp.mp3");

                byte data[] = new byte[1024];

                long total = 0;

                while ((count = input.read(data)) != -1) {
                    total += count;
                    // publishing the progress....
                    // After this onProgressUpdate will be called
                    publishProgress("" + (int) ((total * 100) / lenghtOfFile));

                    // writing data to file
                    output.write(data, 0, count);
                }

                // flushing output
                output.flush();

                // closing streams
                output.close();
                input.close();

            } catch (Exception e) {
                Log.e("Error: ", e.getMessage());
            }

            return null;
        }

        /**
         * Updating progress bar
         * */
        protected void onProgressUpdate(String... progress) {
            // setting progress percentage
            pDialog.setProgress(Integer.parseInt(progress[0]));
        }

        /**
         * After completing background task Dismiss the progress dialog
         * **/
        @Override
        protected void onPostExecute(String file_url) {
            // dismiss the dialog after the file was downloaded
            dismissDialog(progress_bar_type);
            Intent intent = new Intent(MusicChooseActivity.this, TanpuraChooseActivity.class);
            intent.putExtra("filename", mCurrentMusicName);
            startActivity(intent);

        }
    }


    protected boolean isPermissionGranted() {
        int read;
        read = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return read == PackageManager.PERMISSION_GRANTED;
    }


    protected void requestPermissions() {

        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.READ_EXTERNAL_STORAGE);
        ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);

        // Request permission. It's possible this can be auto answered if device policy
        // sets the permission in a given state or the user denied the permission
        // previously and checked "Never ask again".
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        }, STORAGE_PERMISSIONS_REQUEST_CODE);

    }
    // Read from the database

    public static MusicChooseActivity getInstance(){
        return self;
    }
    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();

    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setMessage("Are you sure you want to exit?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        MusicChooseActivity.super.onBackPressed();
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

    /*
     * check if new item
     */
    private boolean is_new(String item_name, ArrayList<String> array_list){
        boolean is_new = true;
        for ( int i = 0; i < array_list.size(); i ++){
            if (item_name.equals(array_list.get(i)))
            {
                is_new = false;
                break;
            }
        }
        return is_new;

    }

    private void setadapter(ArrayList<String> arrayname, Spinner spinner){
        Collections.sort(arrayname, new SortbyString());
        spinnerArrayAdapter = new CustomSpinnerAdapter(this,R.layout.spinner_text_item,arrayname);
        spinner.setAdapter(spinnerArrayAdapter);
    }

    private void filter_first(String item){
        arrFilteredTitle.clear();
        arrFilteredSecondName.clear();
        for (int i = 0; i < arrMusicTitle.size(); i ++){
            if (arrMusicTitle.get(i).split("_")[0].equals(item))
            {
                arrFilteredTitle.add(arrMusicTitle.get(i));
                if(is_new(arrMusicTitle.get(i).split("_")[1],arrFilteredSecondName))
                    arrFilteredSecondName.add(arrMusicTitle.get(i).split("_")[1]);
            }
        }

//        mAdapter = new CustomAdapter(arrFilteredTitle, getApplicationContext());
//        mListMusic.setAdapter(mAdapter);
        setadapter(arrFilteredSecondName,mSecondName);
    }

    private void filter_second(String firstitem, String seconditem){
        arrFilteredsecibdTitle.clear();
        for (int i = 0; i < arrMusicTitle.size(); i ++){
            if (arrMusicTitle.get(i).split("_")[0].equals(firstitem)
                    && arrMusicTitle.get(i).split("_")[1].equals(seconditem) )
//                !arrMusicTitle.get(i).split("_")[2].equals("")
            {
                arrFilteredsecibdTitle.add(arrMusicTitle.get(i));
            }
        }

        mAdapter = new CustomAdapter(arrFilteredsecibdTitle, getApplicationContext());
        mListMusic.setAdapter(mAdapter);
    }
}
