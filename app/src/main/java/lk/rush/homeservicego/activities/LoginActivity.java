package lk.rush.homeservicego.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;

import lk.rush.homeservicego.R;
import lk.rush.homeservicego.utils.SessionManager;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout tilEmail, tilPassword;
    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvRegister;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore fs;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Bind views
        tilEmail    = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        etEmail     = findViewById(R.id.etEmail);
        etPassword  = findViewById(R.id.etPassword);
        btnLogin    = findViewById(R.id.btnLogin);
        tvRegister  = findViewById(R.id.tvRegister);
        progressBar = findViewById(R.id.progressBar);

        mAuth          = FirebaseAuth.getInstance();
        fs             = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(this);

        // Clear error as user types in each field
        clearErrorOnType(etEmail,    tilEmail);
        clearErrorOnType(etPassword, tilPassword);

        btnLogin.setOnClickListener(v -> loginUser());

        tvRegister.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));
    }

    // ── Validation ────────────────────────────────────────────────────────
    private boolean validateInputs(String email, String password) {
        // Reset all errors first
        tilEmail.setError(null);
        tilPassword.setError(null);

        boolean valid = true;

        // Email checks
        if (email.isEmpty()) {
            tilEmail.setError("Email is required");
            if (valid) etEmail.requestFocus();
            valid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Enter a valid email  (e.g. user@gmail.com)");
            if (valid) etEmail.requestFocus();
            valid = false;
        }

        // Password checks
        if (password.isEmpty()) {
            tilPassword.setError("Password is required");
            if (valid) etPassword.requestFocus();
            valid = false;
        } else if (password.length() < 6) {
            tilPassword.setError("Password must be at least 6 characters");
            if (valid) etPassword.requestFocus();
            valid = false;
        }

        return valid;
    }

    // ── Login ──────────────────────────────────────────────────────────────
    private void loginUser() {
        String email    = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Stop if validation fails
        if (!validateInputs(email, password)) return;

        progressBar.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);

        // Step 1: Sign in with Firebase Authentication
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    String userId = authResult.getUser().getUid();

                    // Step 2: Fetch user profile from Firestore
                    fs.collection("users").document(userId).get()
                            .addOnSuccessListener((DocumentSnapshot doc) -> {
                                progressBar.setVisibility(View.GONE);

                                if (doc.exists()) {
                                    String name = doc.getString("name");
                                    String role = doc.getString("role");
                                    if (role == null) role = "customer";

                                    sessionManager.createLoginSession(userId, name, email, role);
                                    Log.d("LOGIN", "Logged in: " + name + " | Role: " + role);

                                    Intent intent;
                                    if (role.equalsIgnoreCase("admin")) {
                                        Toast.makeText(this, "Welcome Admin, " + name + "!", Toast.LENGTH_SHORT).show();
                                        intent = new Intent(LoginActivity.this, AdminActivity.class);
                                    } else {
                                        Toast.makeText(this, "Welcome, " + name + "!", Toast.LENGTH_SHORT).show();
                                        intent = new Intent(LoginActivity.this, HomeActivity.class);
                                    }
                                    startActivity(intent);
                                    finish();
                                } else {
                                    // Auto-create user profile document if missing from Firestore
                                    HashMap<String, Object> userData = new HashMap<>();
                                    userData.put("userId", userId);
                                    userData.put("name", email.split("@")[0]);
                                    userData.put("email", email);
                                    userData.put("phone", "");
                                    userData.put("role", "customer");

                                    fs.collection("users").document(userId).set(userData)
                                            .addOnSuccessListener(unused -> {
                                                progressBar.setVisibility(View.GONE);
                                                sessionManager.createLoginSession(userId, email.split("@")[0], email, "customer");
                                                Toast.makeText(this, "Welcome, " + email.split("@")[0] + "!", Toast.LENGTH_SHORT).show();
                                                startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                                                finish();
                                            })
                                            .addOnFailureListener(e -> {
                                                progressBar.setVisibility(View.GONE);
                                                btnLogin.setEnabled(true);
                                                Toast.makeText(this, "Profile creation error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                            });
                                }
                            })
                            .addOnFailureListener(e -> {
                                progressBar.setVisibility(View.GONE);
                                btnLogin.setEnabled(true);
                                Log.e("LOGIN", "Firestore PERMISSION_DENIED or Error fetching profile for userId: " + userId, e);
                                Toast.makeText(this, "Failed to load profile (Permission Denied). Check Firebase Firestore Rules.", Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    btnLogin.setEnabled(true);
                    // Show error under the email field for wrong credentials
                    tilPassword.setError("Invalid email or password");
                    Log.e("LOGIN", "Auth error", e);
                });
    }

    // ── Helper: clear error when user starts typing ───────────────────────
    private void clearErrorOnType(EditText editText, TextInputLayout layout) {
        editText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                layout.setError(null);
            }
        });
    }
}
