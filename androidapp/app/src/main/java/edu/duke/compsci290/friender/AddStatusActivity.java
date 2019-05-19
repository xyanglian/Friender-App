package edu.duke.compsci290.friender;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

import java.util.UUID;
import android.Manifest;
import android.content.ContentValues;
import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

import static android.os.Environment.getExternalStoragePublicDirectory;

public class AddStatusActivity extends AppCompatActivity {

    // create recorder
    MediaRecorder recorder;
    Button startAudio, stopAudio;

    // UI references.
    private EditText mStatus;
    private ImageButton mCamera;
    private ImageButton mAudio;
    private ImageView mImageView;
    private ImageButton mGallery;
    private Button mSubmitButton;
    private Button mReturnButton;

    //images and audios
    static final int REQUEST_TAKE_PHOTO = 1;
    private final int PICK_IMAGE_REQUEST = 71;
    private String mCurrentPhotoPath;
    private  Uri mFilePath;
    static final int REQUEST_AUDIO = 100;
    private boolean image_taken;
    private boolean audio_recorded;
    private boolean audio_uploaded;
    private boolean image_uploaded;
    private Uri ImageDownloadUri;
    private Uri AudioDownloadUri;


    //firebase
    private StorageReference mStorageRef;
    private DatabaseReference mDatabaseRef;

    //others
    private static String mEmail;
    private String mAudioFilePath;
    private Bitmap bitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_status);

        SharedPreferences sharedPref = getBaseContext().getSharedPreferences("user", Context.MODE_PRIVATE);
        mEmail = sharedPref.getString("email", "");
        //set up GV
        mStorageRef = FirebaseStorage.getInstance().getReference();
        mDatabaseRef = FirebaseDatabase.getInstance().getReference().child("media");
        mStatus = findViewById(R.id.status);
        mImageView = findViewById(R.id.imageView);
        mGallery = findViewById(R.id.gallaryButton);
        mCamera = findViewById(R.id.imageButton);
        mAudio = findViewById(R.id.record_button);
        mSubmitButton = findViewById(R.id.submit_button);
        mReturnButton = findViewById(R.id.return_button);
        image_taken = false;
        audio_recorded = false;

        //set up buttons
        mCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                takePics();
            }
        });
        mGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                chooseImage();
            }
        });

        mAudio.setOnClickListener(new View.OnClickListener() {
              @Override
              public void onClick(View view) {
                  Intent intent1 = new Intent(getApplicationContext(),RecordAudioActivity.class);
                  startActivityForResult(intent1, REQUEST_AUDIO);
              }
          });

        //set up submit button
        mSubmitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (!image_taken || !audio_recorded){
                    Toast.makeText(AddStatusActivity.this, "Upload a photo and an audio", Toast.LENGTH_SHORT).show();
                }
                else{
                    if (mFilePath != null) {
                        upload(mFilePath, "Image");
                        image_uploaded=true;
                    }
                    if (mAudioFilePath != null) {
                        Uri file = Uri.fromFile(new File(mAudioFilePath));
                        upload(file, "Audio");
                        audio_uploaded=true;
                    }
                    SharedPreferences sharedPref = getBaseContext().getSharedPreferences("user", Context.MODE_PRIVATE);
                    String email = sharedPref.getString("email", "");
                    String status = mStatus.getText().toString();
                    UpdateStatusTask task  = new UpdateStatusTask(email, status);
                    task.execute((Void) null);
                }

            }
        });
        mReturnButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (image_uploaded && audio_uploaded){
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        public void run() {
                            finish();
                        }
                    }, 700);
                }
                else{
                    Toast.makeText(AddStatusActivity.this, "Upload a photo and an audio", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    public void upload(Uri f, final String type){

        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Uploading...");
        progressDialog.show();

        //StorageReference ref = mStorageRef.child("images/"+ UUID.randomUUID().toString());
        StorageReference ref = mStorageRef.child(mEmail).child(type).child(f.getLastPathSegment());
        ref.putFile(f)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        //prepare for uploading to database
                        FirebaseHelp a = new FirebaseHelp(mEmail);
                        final String ID = a.getUserID(mEmail);
                        final DatabaseReference newPost = mDatabaseRef.child(ID);
                        newPost.child("email").setValue(mEmail);
                        newPost.child(type).setValue(taskSnapshot.getDownloadUrl().toString());
                        //newPost.child(type).setValue("testing");

                        progressDialog.dismiss();
                        Toast.makeText(AddStatusActivity.this, type + " Upload Success", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        Toast.makeText(AddStatusActivity.this, type + " Upload Failed "+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                        double progress = (100.0*taskSnapshot.getBytesTransferred()/taskSnapshot
                                .getTotalByteCount());
                        progressDialog.setMessage(type + " Uploaded "+(int)progress+"%");
                    }
                });

    }

    public void upload_database(String type){
        FirebaseHelp a = new FirebaseHelp(mEmail);
        final String ID = a.getUserID(mEmail);
        final DatabaseReference newPost = mDatabaseRef.child(ID);
        newPost.child("email").setValue(mEmail);
        if (type.equals("Image")) newPost.child(type).setValue(ImageDownloadUri.toString());
        if (type.equals("Audio")) newPost.child(type).setValue(AudioDownloadUri.toString());

    }


    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {

        if (savedInstanceState != null) bitmap = savedInstanceState.getParcelable("image");
        // put values from savedinstancestate into textview
        super.onRestoreInstanceState(savedInstanceState);
        mImageView.setImageBitmap(bitmap);
        mCurrentPhotoPath = savedInstanceState.getString("PhotoPath");
        mAudioFilePath = savedInstanceState.getString("AudioFile");
        mEmail = savedInstanceState.getString("Email");
        mFilePath = Uri.parse(savedInstanceState.getString("FilePath"));

        image_taken = savedInstanceState.getBoolean("it");
        image_uploaded = savedInstanceState.getBoolean("iu");
        audio_recorded = savedInstanceState.getBoolean("ar");
        audio_uploaded = savedInstanceState.getBoolean("au");


    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {

        outState.putParcelable("image", bitmap);
        outState.putString("PhotoPath",mCurrentPhotoPath);
        outState.putString("AudioFile",mAudioFilePath);
        outState.putString("Email",mEmail);
        outState.putString("FilePath", String.valueOf(mFilePath));

        outState.putBoolean("it", image_taken);
        outState.putBoolean("ar", audio_recorded);
        outState.putBoolean("iu", image_uploaded);
        outState.putBoolean("au", audio_uploaded);

        // save values from savedinstancestate into outstate
        super.onSaveInstanceState(outState);

    }

    public class UpdateStatusTask extends AsyncTask<Void, Void, Boolean> {
        private final String mEmail;
        private final String mStatus;

        UpdateStatusTask(String email, String status) {
            mEmail = email;
            mStatus = status;
        }
        @Override
        protected Boolean doInBackground(Void... params) {
            try{
                URL url = new URL(getString(R.string.vm_address) + "update_status");
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setDoInput(true);
                urlConnection.setDoOutput(true);
                urlConnection.setRequestMethod("POST");
                Uri.Builder builder = new Uri.Builder()
                        .appendQueryParameter("email", this.mEmail)
                        .appendQueryParameter("status", this.mStatus);
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
                    Log.d("asdfas", "OK");
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

   ///////////////// camera and image related methods
    public void  takePics(){
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                System.out.println("Error occurred while creating the File: ");
                ex.printStackTrace();
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                mFilePath = FileProvider.getUriForFile(this,
                        "com.example.android.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mFilePath);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = "file:" + image.getAbsolutePath();
        return image;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //returned back from camera
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            image_taken = true;
            try {
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), mFilePath);
                bitmap = crupAndScale(bitmap, 300); // if you mind scaling
                mImageView.setImageBitmap(bitmap);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //returned back from gallery
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            image_taken = true;
            mFilePath = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), mFilePath);
                mImageView.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //returned back from audio activity
        if (requestCode == REQUEST_AUDIO) {
            if (resultCode == Activity.RESULT_OK) {
                audio_recorded = true;
                mAudioFilePath = data.getStringExtra("path");
                Toast.makeText(AddStatusActivity.this, "Audio Saved", Toast.LENGTH_SHORT).show();
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                //Write your code if there's no result
                Toast.makeText(AddStatusActivity.this, "No Audio Returned", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /* Scale down the image to the thumbnail */
    public static  Bitmap crupAndScale (Bitmap source,int scale){
        int factor = source.getHeight() <= source.getWidth() ? source.getHeight(): source.getWidth();
        int longer = source.getHeight() >= source.getWidth() ? source.getHeight(): source.getWidth();
        int x = source.getHeight() >= source.getWidth() ?0:(longer-factor)/2;
        int y = source.getHeight() <= source.getWidth() ?0:(longer-factor)/2;
        source = Bitmap.createBitmap(source, x, y, factor, factor);
        source = Bitmap.createScaledBitmap(source, scale, scale, false);
        return source;
    }


    private void chooseImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

}
