package com.ezlol.vkmusicplayer;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.material.snackbar.Snackbar;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {
    EditText usernameEditText, passwordEditText;
    Button signInBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        usernameEditText = findViewById(R.id.usernameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        signInBtn = findViewById(R.id.signIn);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String accessToken = preferences.getString("access_token", "");
        String secret = preferences.getString("secret", "");

        if(accessToken.length() > 0 && secret.length() > 0){
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
            return; // ХЗ, надо ли раз есть finish(), но на всякий случай
        }
        signInBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if(view.getId() == R.id.signIn){
            AuthTask authTask = new AuthTask();
            authTask.execute();
        }
    }

    class AuthTask extends AsyncTask<Void, Void, AppAPI.Auth> {
        @Override
        protected AppAPI.Auth doInBackground(Void... voids) {
            try {
                return new AppAPI.Auth(usernameEditText.getText().toString(), passwordEditText.getText().toString());
            } catch (AppAPI.Exception ignored) {
                Snackbar.make(signInBtn, R.string.invalid_credentials, Snackbar.LENGTH_LONG).show();
                passwordEditText.getText().clear();
            }
            return null;
        }

        @Override
        protected void onPostExecute(AppAPI.Auth authLink) {
            super.onPostExecute(authLink);
            if(authLink != null) {
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit()
                        .putString("access_token", authLink.getToken())
                        .putString("secret", authLink.getSecret())
                        .apply();
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
                finish();
            }
        }
    }
}