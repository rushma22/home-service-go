package lk.rush.homeservicego.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

import lk.rush.homeservicego.R;
import lk.rush.homeservicego.utils.SessionManager;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {

            // Check if a user is currently signed in with Firebase
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {

                // User is logged in — check their role from local session
                SessionManager sessionManager = new SessionManager(this);
                String role = sessionManager.getUserRole();

                // Route to the correct home screen based on role
                if ("admin".equals(role)) {
                    startActivity(new Intent(this, AdminActivity.class));
                } else {
                    startActivity(new Intent(this, HomeActivity.class));
                }

            } else {
                // Not logged in — go to Login screen
                startActivity(new Intent(this, LoginActivity.class));
            }

            finish(); // Close splash so back button won't return here

        }, 2000); // 2 second delay
    }
}
