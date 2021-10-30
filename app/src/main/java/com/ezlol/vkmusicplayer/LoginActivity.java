package com.ezlol.vkmusicplayer;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.google.android.material.snackbar.Snackbar;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {
    LinearLayout loginLayout, validationLayout;
    EditText usernameEditText, passwordEditText, validationCodeEditText;
    Button signInBtn, validateBtn;

    AppAPI.Auth authLink;

    private String username;
    private String password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        loginLayout = findViewById(R.id.loginLayout);
        validationLayout = findViewById(R.id.validationLayout);
        usernameEditText = findViewById(R.id.usernameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        validationCodeEditText = findViewById(R.id.validationCodeEditText);
        signInBtn = findViewById(R.id.signIn);
        validateBtn = findViewById(R.id.validateBtn);

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
        validateBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()) {
            case R.id.signIn: {
                this.username = usernameEditText.getText().toString();
                this.password = passwordEditText.getText().toString();
                if(username.length() > 0 && password.length() > 0) {
                    AuthTask authTask = new AuthTask();
                    authTask.execute();
                }
                break;
            }
            case R.id.validateBtn:{
                if(authLink != null){
                    new ValidateTask().execute(validationCodeEditText.getText().toString(), username, password);
                }else{
                    Log.e("FATAL ERROR", "Auth link is null ???");
                }
                break;
            }
        }
    }

    class AuthTask extends AsyncTask<Void, Void, AppAPI.Auth> {
        @Override
        protected AppAPI.Auth doInBackground(Void... voids) {
            try {
                return new AppAPI.Auth(usernameEditText.getText().toString(), passwordEditText.getText().toString());
            } catch (AppAPI.AuthException | AppAPI.NeedValidationException | AppAPI.NeedCaptchaException ignored) {
                Snackbar.make(signInBtn, R.string.invalid_credentials, Snackbar.LENGTH_LONG).show();
                passwordEditText.getText().clear();
            }
            return null;
        }

        @Override
        protected void onPostExecute(AppAPI.Auth link) {
            super.onPostExecute(authLink);
            authLink = link;
            if(authLink != null) {
                if(authLink.isNeedValidation()) {
                    loginLayout.setVisibility(View.GONE);
                    validationLayout.setVisibility(View.VISIBLE);
                }else if(authLink.isNeedCaptcha()){
                    Snackbar.make(signInBtn, "Captcha need, but i not able to show it for u, sry ;/", Snackbar.LENGTH_LONG).show();
                }else{
                    PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit()
                            .putString("access_token", authLink.getToken())
                            .putString("secret", authLink.getSecret())
                            .apply();
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    startActivity(intent);
                    finish();
                    return;
                }
            }
        }
    }

    class ValidateTask extends AsyncTask<String, Void, Boolean>{
        @Override
        protected Boolean doInBackground(String... strings) {
            try {
                return authLink.validate(strings[0], strings[1], strings[2]);
            } catch (AppAPI.AuthException ignored) {}
            return false;
        }

        @Override
        protected void onPostExecute(Boolean b) {
            super.onPostExecute(b);
            if(b != null){
                if(b){
                    PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit()
                            .putString("access_token", authLink.getToken())
                            .putString("secret", authLink.getSecret())
                            .apply();
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    startActivity(intent);
                    finish();
                    return;
                }
            }
            Snackbar.make(signInBtn, R.string.invalid_validation_code, Snackbar.LENGTH_LONG).show();
        }
    }
}