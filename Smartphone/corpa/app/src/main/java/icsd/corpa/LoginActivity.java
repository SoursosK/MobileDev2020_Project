package icsd.corpa;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;


public class LoginActivity extends AppCompatActivity {

    private EditText username, password;
    private TextView error;
    private Button register, login;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        username = findViewById(R.id.username);
        password = findViewById(R.id.password);
        register = findViewById(R.id.register);
        login = findViewById(R.id.login);
        error = findViewById(R.id.error);

        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            }
        });

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logIn();
            }
        });
    }

    private void logIn() {
        if (isLogInEmpty()) return;

        new Thread(new Runnable() {
            @Override
            public void run() {
                final StringBuilder str = new StringBuilder();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        login.setEnabled(false);
                        register.setEnabled(false);
                        findViewById(R.id.loading).setVisibility(View.VISIBLE);
                        error.setText("");
                        error.setVisibility(View.INVISIBLE);
                    }
                });

                try {
                    JSONObject content = new JSONObject()
                            .put("username", username.getText().toString())
                            .put("password", password.getText().toString())
                            .put("userType", "User");

                    Connection.Response loginForm = Jsoup
                            .connect("https://kostas109.pythonanywhere.com/login")
                            .method(Connection.Method.POST)
                            .ignoreContentType(true)
                            .header("Content-Type", "application/json")
                            .requestBody(content.toString())
                            .execute();

                    if (!loginForm.body().equals("User")) str.append("invalid credentials");

                } catch (Exception e) {
                    str.append("network error");
                    str.append("\n");
                    str.append(e.toString());
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        findViewById(R.id.loading).setVisibility(View.INVISIBLE);
                        error.setVisibility(View.VISIBLE);
                        error.setTextColor(Color.RED);
                        if (str.length() > 0) {
                            error.setText(str.toString());
                            login.setEnabled(true);
                            register.setEnabled(true);
                        } else {
                            error.setText("success");
                            error.setTextColor(Color.GREEN);
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                        }
                    }
                });
            }
        }).start();

    }

    private boolean isLogInEmpty() {
        boolean empty = false;
        if (TextUtils.isEmpty(username.getText().toString())) {
            username.setError("username cannot be empty");
            empty = true;
        }
        if (TextUtils.isEmpty(password.getText().toString())) {
            password.setError("password cannot be empty");
            empty = true;
        }
        return empty;

    }
}
