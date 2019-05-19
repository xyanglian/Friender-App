package edu.duke.compsci290.friender;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;

import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class ProfileActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener {
    private BottomNavigationView navigationView;
    private String mEmail;
    private TextView mUsername;
    private TextView mStatus;
    private TextView mStatusTime;
    private TextView mEmailView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences sharedPref = getBaseContext().getSharedPreferences("user", Context.MODE_PRIVATE);
        mEmail = sharedPref.getString("email", "");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        mUsername = findViewById(R.id.username);
        mEmailView = findViewById(R.id.email);
        mStatus = findViewById(R.id.current_status);
        mStatusTime = findViewById(R.id.status_created_at);
        navigationView = (BottomNavigationView) findViewById(R.id.navigation);
        navigationView.setOnNavigationItemSelectedListener(this);


        GetStatusTask task  = new GetStatusTask(mEmail);
        task.execute((Void) null);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.navigation_home:
                Intent intent1 = new Intent(getApplicationContext(),MapsActivity.class);
                startActivity(intent1);
                break;
            case R.id.navigation_dashboard:
                Intent intent2 = new Intent(getApplicationContext(),MapsActivity.class);
                startActivity(intent2);
                break;
            case R.id.navigation_notifications:
                Intent intent3 = new Intent(getApplicationContext(),ProfileActivity.class);
                startActivity(intent3);
                break;
        }
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        updateNavigationBarState();
    }

    // Remove inter-activity transition to avoid screen tossing on tapping bottom navigation items
    @Override
    public void onPause() {
        super.onPause();
        overridePendingTransition(0, 0);
    }

    private void updateNavigationBarState(){
        int actionId = getNavigationMenuItemId();
        selectBottomNavigationBarItem(actionId);
    }

    void selectBottomNavigationBarItem(int itemId) {
        MenuItem item = navigationView.getMenu().findItem(itemId);
        item.setChecked(true);
    }

    int getNavigationMenuItemId() {
        return R.id.navigation_notifications;
    }

    public class GetStatusTask extends AsyncTask<Void, Void, Boolean> {
        private  String Email;
        private  String username;
        private  String time;
        private  String status;
        GetStatusTask(String em) {
            Email = em;
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
                        .appendQueryParameter("email", this.Email);
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
            mStatus.setText("Most Recent Status: "+status);
            mStatusTime.setText("Status Created At: "+time);
            mEmailView.setText("Email Address: "+Email);
        }
    }

}
