package icsd.corpa;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

public class RegisterActivity extends AppCompatActivity {

    private Button register;
    private EditText username, name, password, city;
    private TextView error;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        username = findViewById(R.id.username);
        password = findViewById(R.id.password);
        name = findViewById(R.id.name);
        city = findViewById(R.id.city);
        error = findViewById(R.id.error);

        register = findViewById(R.id.register);
        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                register();
            }
        });
    }

    private void register() {
        if(isLogInEmpty()) return;

        new Thread(new Runnable() {
            @Override
            public void run() {
                final StringBuilder str = new StringBuilder();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        register.setEnabled(false);
                        findViewById(R.id.loading).setVisibility(View.VISIBLE);
                        error.setText("");
                        error.setVisibility(View.INVISIBLE);
                    }
                });

                try{
                    JSONObject content = new JSONObject()
                            .put("username", username.getText().toString())
                            .put("password", password.getText().toString())
                            .put("userType", "User");

                    Connection.Response loginForm = Jsoup
                            .connect("https://kostas109.pythonanywhere.com/createUser")
                            .method(Connection.Method.POST)
                            .ignoreContentType(true)
                            .header("Content-Type", "application/json")
                            .requestBody(content.toString())
                            .execute();

                    if(!loginForm.body().equals("200")) str.append(loginForm.body());

                }catch (Exception e){
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
                        if(str.length()>0){
                            error.setText(str.toString());
                            register.setEnabled(true);
                        }
                        else{
                            error.setText("success");
                            error.setTextColor(Color.GREEN);
                            finish();
                        }
                    }
                });
            }
        }).start();
    }

    private boolean isLogInEmpty(){
        boolean empty=false;
        if(TextUtils.isEmpty(username.getText().toString())){
            username.setError("username cannot be empty");
            empty=true;
        }
        if(TextUtils.isEmpty(name.getText().toString())){
            name.setError("name cannot be empty");
            empty=true;
        }
        if(TextUtils.isEmpty(password.getText().toString())){
            password.setError("password cannot be empty");
            empty=true;
        }
        return empty;

    }
}
