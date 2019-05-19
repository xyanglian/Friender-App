package edu.duke.compsci290.friender;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class MyFirebaseInstanceIDService extends FirebaseInstanceIdService{
    private static final String TAG = "MyFirebaseIIDService";
    private String mEmail;

    @Override
    public void onTokenRefresh() {
        // Get updated InstanceID token.
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        Log.d(TAG, "Refreshed token: " +        refreshedToken);

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // Instance ID token to your app server.
        sendRegistrationToServer(refreshedToken);
    }

    private void sendRegistrationToServer(String refreshedToken) {
        SharedPreferences sharedPref = getBaseContext().getSharedPreferences("user", Context.MODE_PRIVATE);
        mEmail = sharedPref.getString("email", "");
        try {
            URL url = new URL("http://vcm-3269.vm.duke.edu/" + "update_token");
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(true);
            urlConnection.setRequestMethod("POST");
            Uri.Builder builder = new Uri.Builder()
                    .appendQueryParameter("username", this.mEmail)
                    .appendQueryParameter("token", refreshedToken);
            String query = builder.build().getEncodedQuery();
            OutputStream os = urlConnection.getOutputStream();
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(os, "UTF-8"));
            writer.write(query);
            writer.flush();
            writer.close();
            os.close();
        } catch (Exception e){
            e.printStackTrace();
        }
       // if(refreshedToken != null){
//            OkHttpClient client = new OkHttpClient();
//            FormBody.Builder formBuilder = new FormBody.Builder().add("token", refreshedToken);
//            RequestBody formBody = formBuilder.build();
//            Request request = new Request.Builder()
//                    .url("http://vcm-3269.vm.duke.edu/")
//                    .post(formBody)
//                    .build();

//        } else{
//            Toast.makeText(getBaseContext(),
//                    "Token is null.",
//                    Toast.LENGTH_LONG).show();
//        }
    }
}
