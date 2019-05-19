package edu.duke.compsci290.friender;

import android.Manifest;
import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,BottomNavigationView.OnNavigationItemSelectedListener, GoogleMap.OnMarkerClickListener {

    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationClient;
    private BottomNavigationView navigationView;
    private String mEmail;

    static final int REQUEST_STATUS = 2;
    private LinearLayout ggwp;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        Log.d("tokensetup", "Refreshed token: " +        refreshedToken);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        navigationView = (BottomNavigationView) findViewById(R.id.navigation);
        navigationView.setOnNavigationItemSelectedListener(this);
        ggwp = (LinearLayout) findViewById(R.id.ggwp);
        final String notification = getIntent().getStringExtra("notification");
        if(notification != null){
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    showBanner(notification);
                }
            }, 1000);
        }
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
        return R.id.navigation_home;
    }


    /*
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                0);
        mMap = googleMap;
        Location l = null;
        SharedPreferences sharedPref = getBaseContext().getSharedPreferences("user", Context.MODE_PRIVATE);
        final String email = sharedPref.getString("email", "");
        GrabLocationTask task  = new MapsActivity.GrabLocationTask(email);
        task.execute((Void) null);
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        UpdateTokenTask task1 = new UpdateTokenTask(email, refreshedToken);
        task1.execute((Void) null);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED){
            mMap.setMyLocationEnabled(true);
            mFusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            // Got last known location. In some rare situations this can be null.
                            SharedPreferences sharedPref = getBaseContext().getSharedPreferences("user", Context.MODE_PRIVATE);
                            String email = sharedPref.getString("email", "");
                            mEmail = sharedPref.getString("email", "");
                            LatLng current = new LatLng(location.getLatitude(), location.getLongitude());
                            UploadLocationTask uploadLocationTask  = new MapsActivity.UploadLocationTask(
                                    email,
                                    current.latitude,
                                    current.longitude
                                    );
                            uploadLocationTask.execute((Void) null);
                            CameraPosition cameraPosition = new CameraPosition.Builder()
                                    .target(current)      // Sets the center of the map to Mountain View
                                    .zoom(16)                   // Sets the zoom
                                    .bearing(0)                // Sets the orientation of the camera to east
                                    .build();                   // Creates a CameraPosition from the builder
                            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                        }
                    });
            mMap.setOnMarkerClickListener(this);
        }



    }

    protected void showBanner(String noti){
        LayoutInflater layoutInflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View customView = layoutInflater.inflate(R.layout.popup,null);
        TextView v = customView.findViewById(R.id.popuptext);
        Log.d("asdgadsdsaasd", noti);
        v.setText(noti);
        Button closePopupBtn = (Button) customView.findViewById(R.id.closePopupBtn);

        //instantiate popup window
        final PopupWindow popupWindow = new PopupWindow(customView, ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT);

        //display the popup window
        popupWindow.showAtLocation(ggwp, Gravity.CENTER, 0, 0);

        //close the popup window on button click
        closePopupBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popupWindow.dismiss();
            }
        });
    }
    @Override
    public boolean onMarkerClick(final Marker marker) {

        //check null
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference().child("media");
        FirebaseHelp help = new FirebaseHelp(marker.getTitle());
        final String ID = help.getUserID(marker.getTitle());
        rootRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                String email = marker.getTitle();
                if (snapshot.hasChild(ID)) {
                    // Retrieve the data from the marker.
                    Intent intent = new Intent(getApplicationContext(),StatusActivity.class);
                    intent.putExtra("email", email);
                    intent.putExtra("myemail", mEmail);
                    intent.putExtra("CLASS_FROM", MapsActivity.class.toString());
                    startActivity(intent);
                }
                else{
                    Toast.makeText(MapsActivity.this, "No Status From User Yet", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Getting Post failed, log a message
                Log.d("fire", "loadPost:onCancelled", databaseError.toException());
                // ...
            }
        });

        // Return false to indicate that we have not consumed the event and that we wish
        // for the default behavior to occur (which is for the camera to move such that the
        // marker is centered and for the marker's info window to open, if it has one).
        return false;
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.navigation_home:
                Intent intent1 = new Intent(getApplicationContext(),MapsActivity.class);
                intent1.putExtra("CLASS_FROM", MapsActivity.class.toString());
                startActivity(intent1);
                break;
            case R.id.navigation_dashboard:
                Intent intent2 = new Intent(getApplicationContext(),AddStatusActivity.class);
                intent2.putExtra("email", mEmail);
                intent2.putExtra("CLASS_FROM", MapsActivity.class.toString());
                startActivityForResult(intent2, REQUEST_STATUS);
                break;
            case R.id.navigation_notifications:
                Intent intent3 = new Intent(getApplicationContext(),ProfileActivity.class);
                intent3.putExtra("email", mEmail);
                intent3.putExtra("CLASS_FROM", MapsActivity.class.toString());
                startActivity(intent3);
                break;
        }
        return true;
    }

    public class UploadLocationTask extends AsyncTask<Void, Void, Boolean> {

        private final String mEmail;
        private final double mLatitude;
        private final double mLongitude;

        UploadLocationTask(String email, double latitude, double longitude) {
            mEmail = email;
            mLatitude = latitude;
            mLongitude = longitude;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            try{
                URL url = new URL(getString(R.string.vm_address) + "update_location");
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setDoInput(true);
                urlConnection.setDoOutput(true);
                urlConnection.setRequestMethod("POST");
                Uri.Builder builder = new Uri.Builder()
                        .appendQueryParameter("email", this.mEmail)
                        .appendQueryParameter("latitude", Double.toString(this.mLatitude))
                        .appendQueryParameter("longitude", Double.toString(this.mLongitude));
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

    public class UpdateTokenTask extends AsyncTask<Void, Void, Boolean> {

        private final String mEmail;
        private final String mToken;

        UpdateTokenTask(String email, String token) {
            mEmail = email;
            mToken = token;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            try{
                URL url = new URL(getString(R.string.vm_address) + "update_token");
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setDoInput(true);
                urlConnection.setDoOutput(true);
                urlConnection.setRequestMethod("POST");
                Uri.Builder builder = new Uri.Builder()
                        .appendQueryParameter("email", this.mEmail)
                        .appendQueryParameter("token", this.mToken);
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

    public class GrabLocationTask extends AsyncTask<Void, Void, Boolean> {
        private final String mEmail;
        private ArrayList<Loc> mLocations = new ArrayList<>();

        GrabLocationTask(String em){
            mEmail = em;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            try{
                URL url = new URL(getString(R.string.vm_address) + "grab_location");
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setDoInput(true);
                urlConnection.setDoOutput(true);
                urlConnection.setRequestMethod("POST");
                Uri.Builder builder = new Uri.Builder().appendQueryParameter(" ", " ");
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
                    InputStream inputStream = new BufferedInputStream(urlConnection.getInputStream());
                    BufferedReader r = new BufferedReader(new InputStreamReader(inputStream));
                    StringBuilder total = new StringBuilder();
                    String line;
                    while ((line = r.readLine()) != null) {
                        total.append(line).append('\n');
                    }
                    JSONObject json = new JSONObject(total.toString());
                    Log.d("anc", total.toString());
                    JSONArray locations =  json.getJSONArray("data");
                    for(int i = 0; i < locations.length(); i++){
                        JSONObject location = locations.getJSONObject(i);
                        double lat = location.getDouble("Latitude");
                        double lon = location.getDouble("Longitude");
                        String email = location.getString("Email");
                        mLocations.add(new Loc(new LatLng(lat, lon), email));
                    }
                }
                else{
                    Log.d("we fucked up somewhere", "RIP");
                }


            }catch (Exception e){
                e.printStackTrace();
            }

            return true;
        }
        @Override
        protected void onPostExecute(final Boolean success) {
            for(Loc l : mLocations){
                if(!l.getEmail().equals(mEmail)){
                    Marker m = mMap.addMarker(new MarkerOptions()
                            .position(l.getLocation())
                            .title(l.getEmail()));
                }
            }

        }

    }
}
