package edu.duke.compsci290.friender;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageException;
import com.google.firebase.storage.StorageReference;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import static android.content.ContentValues.TAG;

public class FirebaseHelp {

    private StorageReference mStorageRef;
    private DatabaseReference mDatabaseRef;
    private String mEmail;
    private String mID;

    public FirebaseHelp(String email){
        mStorageRef = FirebaseStorage.getInstance().getReference();
        mDatabaseRef = FirebaseDatabase.getInstance().getReference();
        mEmail = email;
        mID = getUserID(mEmail);
    }

    public String getUserID(String email){
        StringBuilder a = new StringBuilder(email);
        for (int i = 0; i < a.length(); i++){
            if (a.charAt(i) == '.' || a.charAt(i) == '#' || a.charAt(i) == '$' || a.charAt(i) == '[' || a.charAt(i) == ']'){
                a.deleteCharAt(i);
            }
            if (a.charAt(i) == '@'){
                a.delete(i,a.length());
            }
        }

        return a.toString();
    }

}
