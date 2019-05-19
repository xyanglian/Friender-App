package edu.duke.compsci290.friender;


import android.content.Intent;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Message;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageException;
import com.google.firebase.storage.StorageReference;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Scanner;

import static android.content.ContentValues.TAG;

public class StatusActivity extends AppCompatActivity {
    //UI
    private Button mPoke;
    private TextView mUsername;
    private TextView mStatus;
    private TextView mStatusCreatedTime;
    private TextView mWelcome;
    private static final String AUTH_KEY = "key=YOUR_SERVER_KEY";
    private String email0; //my email.
    private String email1; //receipient's email.
    private ImageButton mAudio;
    private ImageView mImageView;
    private String mEmailAdd;
    private Button mReturnButton;

    private StorageReference mStorageRef;
    private DatabaseReference mDatabaseRef;

    private Bitmap mBitmap;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //bug of jumping from addstatus to here
        Intent intent = this.getIntent();
        Bundle temp = intent.getExtras();
        if (intent == null || temp == null || temp.getString("CLASS_FROM") == null ||
                (temp.getString("CLASS_FROM") != null && !temp.getString("CLASS_FROM").equals(MapsActivity.class.toString()))
                ){
            finish();
            Log.d("shit", "intent not from map. Finish");
        }
        else{
            Log.d("shit", temp.getString("CLASS_FROM"));
        }
        super.onCreate(savedInstanceState);




        setContentView(R.layout.activity_status);
        //set up GV
        mPoke = findViewById(R.id.poke_button);
        mUsername = findViewById(R.id.username);
        mStatus = findViewById(R.id.current_status);
        mStatusCreatedTime = findViewById(R.id.status_created_at);
        mWelcome = findViewById(R.id.welcome_text);
        email0 = getIntent().getStringExtra("myemail");
        email1 = getIntent().getStringExtra("email");
        Log.d("asddsafadsfdas", email0);
        Log.d("asddsafadsfdas", email1);
        mAudio = findViewById(R.id.play_voice);
        mImageView = findViewById(R.id.imageView);
        mEmailAdd = getIntent().getStringExtra("email");
        mStorageRef = FirebaseStorage.getInstance().getReference();
        mDatabaseRef = FirebaseDatabase.getInstance().getReference();
        mReturnButton = findViewById(R.id.return_button);

        //download strings from Wilson database
        GetStatusTask task  = new GetStatusTask(mEmailAdd);
        task.execute((Void) null);
        //download image from firebase storage
        download("Image");

        mPoke.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendNotification();
            }
        });
        mAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                download("Audio");
            }
        });
        mReturnButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent1 = new Intent(getApplicationContext(),MapsActivity.class);
                intent1.putExtra("CLASS_FROM", StatusActivity.class.toString());
                startActivity(intent1);
                finish();
            }
        });
    }


    ////////firebase get image and audio

    //download the picture or play the audio
    public void download(final String type){
        final FirebaseHelp help = new FirebaseHelp(mEmailAdd);
        String ID = help.getUserID(mEmailAdd);

        mDatabaseRef.child("media").child(ID).child(type).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Log.d(TAG, snapshot.getValue().toString());
                String imageUri = (String) snapshot.getValue();

                //create background task to download the Url
                if (type.equals("Image")) new DownloadFilesTask().execute(imageUri);
                else if (type.equals("Audio")){
                    MediaPlayer mediaPlayer = new MediaPlayer();
                    try {
                        mediaPlayer.setDataSource(imageUri);
                        mediaPlayer.prepare();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    mediaPlayer.start();
                    Toast.makeText(StatusActivity.this, "Recording Playing",
                            Toast.LENGTH_LONG).show();
                }

            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

    }

    private void sendNotification() {
        PokeTask task  = new PokeTask(this.email0, this.email1);
        task.execute((Void) null);
        Toast.makeText(getBaseContext(),
                "Poked",
                Toast.LENGTH_LONG).show();

    }

    public class PokeTask extends AsyncTask<Void, Void, Boolean> {

        private final String mEmail0;
        private final String mEmail1;

        PokeTask(String e0, String e1) {
            mEmail0 = e0;
            mEmail1 = e1;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            try{
                URL url = new URL(getString(R.string.vm_address) + "send_poke");
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setDoInput(true);
                urlConnection.setDoOutput(true);
                urlConnection.setRequestMethod("POST");
                Uri.Builder builder = new Uri.Builder()
                        .appendQueryParameter("email0", this.mEmail0)
                        .appendQueryParameter("email1", this.mEmail1);
                String query = builder.build().getEncodedQuery();
                OutputStream os = urlConnection.getOutputStream();
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(os, "UTF-8"));
                writer.write(query);
                writer.flush();
                writer.close();
                os.close();

                int statusCode = urlConnection.getResponseCode();
                if (statusCode ==  200) {
                }
                else{
                    Log.d("we fucked up somewhere", "RIP");
                }


            }catch (Exception e){
                e.printStackTrace();
            }

            return true;
        }
    }

    private String convertStreamToString(InputStream is) {
        Scanner s = new Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next().replace(",", ",\n") : "";
    }


    /////download image from firebase and set bitmap to image view
    private class DownloadFilesTask extends AsyncTask<String, Integer, Bitmap> {
        protected Bitmap getBitmapFromURL(String src) {
            try {
                URL url = new URL(src);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();
                Bitmap myBitmap = BitmapFactory.decodeStream(input);
                return myBitmap;
            } catch (IOException e) {
                // Log exception
                return null;
            }
        }
        @Override
        protected Bitmap doInBackground(String... src) {
            try {
                URL url = new URL(src[0]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();
                Bitmap myBitmap = BitmapFactory.decodeStream(input);
                return myBitmap;
            } catch (IOException e) {
                // Log exception
                return null;
            }
        }
        @Override
        protected void onPostExecute(Bitmap result) {
            mImageView.setImageBitmap(result);
        }
    }


    ///////////////Wilson database///////
    public class GetStatusTask extends AsyncTask<Void, Void, Boolean> {
        private  String mEmail;
        private  String username;
        private  String time;
        private  String status;
        GetStatusTask(String em) {
            mEmail = em;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            try {
                URL url = new URL(getString(R.string.vm_address) + "get_status");
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setDoInput(true);
                urlConnection.setDoOutput(true);
                urlConnection.setRequestMethod("POST");
                Uri.Builder builder = new Uri.Builder()
                        .appendQueryParameter("email", this.mEmail);
                String query = builder.build().getEncodedQuery();
                OutputStream os = urlConnection.getOutputStream();
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(os, "UTF-8"));
                writer.write(query);
                writer.flush();
                writer.close();
                os.close();

                int statusCode = urlConnection.getResponseCode();
                if (statusCode == 200) {
                    InputStream inputStream = new BufferedInputStream(urlConnection.getInputStream());
                    BufferedReader r = new BufferedReader(new InputStreamReader(inputStream));
                    StringBuilder total = new StringBuilder();
                    String line;
                    while ((line = r.readLine()) != null) {
                        total.append(line).append('\n');
                    }
                    JSONObject json = new JSONObject(total.toString());
                    username = json.getString("username");
                    status = json.getString("status");
                    time = json.getString("time");
                } else {
                    Log.d("we fucked up somewhere", "RIP");
                }


            } catch (Exception e) {
                e.printStackTrace();
            }

            return true;
        }
        @Override
        protected void onPostExecute(final Boolean success) {
            mUsername.setText("Username: "+username);
            mStatus.setText("Status: "+status);
            mStatusCreatedTime.setText("Status Created At: "+time);
            mWelcome.setText(username + "'s Status");
        }
    }




}
