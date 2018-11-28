package uk.ac.ed.coinz;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity  implements View.OnClickListener{
    private final String tag = "MainActivity";

    private FirebaseAuth mAuth;
    EditText loginUsername;
    EditText loginPassword;
    ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mAuth = FirebaseAuth.getInstance();
        findViewById(R.id.TextViewRegister).setOnClickListener(this);
        findViewById(R.id.loginButton).setOnClickListener(this);
        loginUsername = findViewById(R.id.LoginUsername);
        loginPassword = findViewById(R.id.LoginPassword);
        progressBar = findViewById(R.id.progressbar0);

    }

    @SuppressLint("LogNotTimber")
    private void login() {
        String email = loginUsername.getText().toString().trim();
        String password = loginPassword.getText().toString().trim();
        if(email.isEmpty()){
            loginUsername.setError("An email Address is required");
            loginUsername.requestFocus();
            return;
        }
        if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            loginUsername.setError(("This is not a valid email"));
            loginUsername.requestFocus();
            return;
        }
        if(password.isEmpty()){
            loginPassword.setError("Password is required");
            loginPassword.requestFocus();
            return;
        }
        if(password.length()<6){
            loginPassword.setError("The length of password should greater than 6");
            loginPassword.requestFocus();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        Log.d(tag,"[On login] logging in");

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    progressBar.setVisibility((View.GONE));
                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
                        FirebaseUser user = mAuth.getCurrentUser();
                        //connectDatabase(user);
                        Intent intent = new Intent(MainActivity.this,GameActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                    } else {
                        // If sign in fails, display a message to the user.
                        Toast.makeText(MainActivity.this, "Authentication failed.",
                                Toast.LENGTH_SHORT).show();
                    }

                    // ...
                });
    }

    @SuppressLint("LogNotTimber")
    public void onClick(View view){
        switch (view.getId()){
            case R.id.TextViewRegister:
                Log.d(tag,"[OnClick sign up] Opening sign up activity");
                startActivity(new Intent(this,RegisterActivity.class));

            case R.id.loginButton:
                login();
                break;
        }
    }
}
