package edu.duke.compsci290.friender;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

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

import butterknife.BindView;
import butterknife.ButterKnife;

public class SignupActivity extends AppCompatActivity {
    private static final String TAG = "SignUpActivity";
    private View mProgressView;
    private UserSignupTask mAuthTask = null;
    @BindView(R.id.user_name) EditText mUserName;
    @BindView(R.id.email) EditText mEmail;
    @BindView(R.id.password) EditText mPassword;
    @BindView(R.id.confirm_password) EditText mConfirmPassword;
    @BindView(R.id.sign_up_button) Button mSignUpButton;
    @BindView(R.id.link_login) TextView mLogInLink;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        ButterKnife.bind(this);

        mSignUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signup();
            }
        });

        mLogInLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Finish the registration screen and return to the Login activity
                goToLoginIn();
                finish();
            }
        });
        mProgressView = findViewById(R.id.sign_up_progress);
    }

    public void goToLoginIn(){
        Intent intent = new Intent(getApplicationContext(),LoginActivity.class);
        startActivity(intent);
    }

    public void signup() {
        if (!validate() || mAuthTask != null) {
            onSignupFailed();
            return;
        }

        mSignUpButton.setEnabled(false);

        mProgressView.setVisibility(View.VISIBLE);

        String name = mUserName.getText().toString();
        String email = mEmail.getText().toString();
        String password = mPassword.getText().toString();
        mAuthTask = new UserSignupTask(email, password, name);
        mAuthTask.execute((Void) null);


    }
    public class UserSignupTask extends AsyncTask<Void, Void, Boolean> {

        private final String mEmail;
        private final String mPassword;
        private final String mName;
        int errorcode;

        UserSignupTask(String email, String password, String name) {
            mEmail = email;
            mPassword = password;
            mName = name;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            try{
                URL url = new URL(getString(R.string.vm_address) + "signup");
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setDoInput(true);
                urlConnection.setDoOutput(true);
                urlConnection.setRequestMethod("POST");
                Uri.Builder builder = new Uri.Builder()
                        .appendQueryParameter("name", this.mName)
                        .appendQueryParameter("username", this.mEmail)
                        .appendQueryParameter("password", this.mPassword);
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

                    JSONObject jsonTotal = new JSONObject(total.toString());
                    errorcode = jsonTotal.getInt("error");
                    Log.d("Total output", total.toString());
                    Log.d("errorcode output", errorcode+"");
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
            mProgressView.setVisibility(View.GONE);

            if (success) {
                mAuthTask = null;
                if(errorcode == 0){ ;
                    onSignupSuccess();
                } else if(errorcode == 1){
                    Toast.makeText(getBaseContext(),
                            "Sign Up failed. User already exists",
                            Toast.LENGTH_LONG).show();
                    mSignUpButton.setEnabled(true);
                } else{
                    Toast.makeText(getBaseContext(),
                            "Server down.",
                            Toast.LENGTH_LONG).show();
                }
                //finish();
            }
            else{
                onSignupFailed();
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            mProgressView.setVisibility(View.GONE);
        }

    }


    public void onSignupSuccess() {
        mSignUpButton.setEnabled(true);
        setResult(RESULT_OK, null);
        Toast.makeText(getBaseContext(),
                  "Sign Up successful. Please check your email to activate",
                       Toast.LENGTH_LONG).show();
        goToLoginIn();
        finish();
    }

    public void onSignupFailed() {
        Toast.makeText(getBaseContext(), "Sign Up failed", Toast.LENGTH_LONG).show();

        mSignUpButton.setEnabled(true);
    }

    public boolean validate() {
        boolean valid = true;

        String name = mUserName.getText().toString();
        String email = mEmail.getText().toString();
        String password = mPassword.getText().toString();
        String reEnterPassword = mConfirmPassword.getText().toString();

        if (name.isEmpty() || name.length() < 3) {
            mUserName.setError("at least 3 characters");
            valid = false;
        } else {
            mUserName.setError(null);
        }

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() || !email.contains("duke.edu")) {
            mEmail.setError("enter a valid email address (only Duke emails accepted)");
            valid = false;
        } else {
            mEmail.setError(null);
        }

        if (password.isEmpty() || password.length() < 6 || password.length() > 10) {
            mPassword.setError("between 4 and 10 alphanumeric characters");
            valid = false;
        } else {
            mPassword.setError(null);
        }

        if (reEnterPassword.isEmpty() || reEnterPassword.length() < 4 || reEnterPassword.length() > 10 || !(reEnterPassword.equals(password))) {
            mConfirmPassword.setError("Password Do not match");
            valid = false;
        } else {
            mConfirmPassword.setError(null);
        }

        return valid;
    }


}

