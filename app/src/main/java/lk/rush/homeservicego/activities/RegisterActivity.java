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
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;

import lk.rush.homeservicego.R;

public class RegisterActivity extends AppCompatActivity {

    private TextInputLayout tilName, tilEmail, tilPhone, tilPassword;
    private EditText etName, etEmail, etPhone, etPassword;
    private Button btnRegister;
    private TextView tvLogin;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore fs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Bind views
        tilName     = findViewById(R.id.tilName);
        tilEmail    = findViewById(R.id.tilEmail);
        tilPhone    = findViewById(R.id.tilPhone);
        tilPassword = findViewById(R.id.tilPassword);
        etName      = findViewById(R.id.etName);
        etEmail     = findViewById(R.id.etEmail);
        etPhone     = findViewById(R.id.etPhone);
        etPassword  = findViewById(R.id.etPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvLogin     = findViewById(R.id.tvLogin);
        progressBar = findViewById(R.id.progressBar);

        mAuth = FirebaseAuth.getInstance();
        fs    = FirebaseFirestore.getInstance();

        // Clear errors as user types
        clearErrorOnType(etName,     tilName);
        clearErrorOnType(etEmail,    tilEmail);
        clearErrorOnType(etPhone,    tilPhone);
        clearErrorOnType(etPassword, tilPassword);

        btnRegister.setOnClickListener(v -> registerUser());
        tvLogin.setOnClickListener(v -> finish()); // Back to Login
    }

    // ── Validation ─────────────────────────────────────────────────────────
    private boolean validateInputs(String name, String email, String phone, String password) {
        // Reset all errors first
        tilName.setError(null);
        tilEmail.setError(null);
        tilPhone.setError(null);
        tilPassword.setError(null);

        boolean valid = true;

        // Full Name
        if (name.isEmpty()) {
            tilName.setError("Full name is required");
            if (valid) etName.requestFocus();
            valid = false;
        } else if (name.length() < 2) {
            tilName.setError("Name must be at least 2 characters");
            if (valid) etName.requestFocus();
            valid = false;
        } else if (!name.matches(".*[a-zA-Z].*")) {
            tilName.setError("Name must contain letters");
            if (valid) etName.requestFocus();
            valid = false;
        }

        // Email
        if (email.isEmpty()) {
            tilEmail.setError("Email is required");
            if (valid) etEmail.requestFocus();
            valid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Enter a valid email  (e.g. user@gmail.com)");
            if (valid) etEmail.requestFocus();
            valid = false;
        }

        // Phone — must be 9–15 digits, optional leading +
        if (phone.isEmpty()) {
            tilPhone.setError("Phone number is required");
            if (valid) etPhone.requestFocus();
            valid = false;
        } else if (!phone.matches("^[+]?[0-9]{9,15}$")) {
            tilPhone.setError("Enter a valid phone number (9–15 digits)");
            if (valid) etPhone.requestFocus();
            valid = false;
        }

        // Password
        if (password.isEmpty()) {
            tilPassword.setError("Password is required");
            if (valid) etPassword.requestFocus();
            valid = false;
        } else if (password.length() < 6) {
            tilPassword.setError("Password must be at least 6 characters");
            if (valid) etPassword.requestFocus();
            valid = false;
        } else if (password.contains(" ")) {
            tilPassword.setError("Password cannot contain spaces");
            if (valid) etPassword.requestFocus();
            valid = false;
        }

        return valid;
    }

    // ── Register ───────────────────────────────────────────────────────────
    private void registerUser() {
        String name     = etName.getText().toString().trim();
        String email    = etEmail.getText().toString().trim();
        String phone    = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Stop if validation fails
        if (!validateInputs(name, email, phone, password)) return;

        progressBar.setVisibility(View.VISIBLE);
        btnRegister.setEnabled(false);

        // Step 1: Create account in Firebase Authentication
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    String userId = authResult.getUser().getUid();

                    // Step 2: Save user profile in Firestore (NO password stored)
                    HashMap<String, Object> userData = new HashMap<>();
                    userData.put("userId", userId);
                    userData.put("name",   name);
                    userData.put("email",  email);
                    userData.put("phone",  phone);
                    userData.put("role",   "customer");

                    fs.collection("users").document(userId).set(userData)
                            .addOnSuccessListener(unused -> {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(this, "Account created! Please sign in.", Toast.LENGTH_SHORT).show();
                                Log.d("REGISTER", "User saved: " + userId);
                                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                progressBar.setVisibility(View.GONE);
                                btnRegister.setEnabled(true);
                                Toast.makeText(this, "Failed to save profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                Log.e("REGISTER", "Firestore error", e);
                            });
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    btnRegister.setEnabled(true);
                    // Show Firebase error under email (e.g. "email already in use")
                    String msg = e.getMessage();
                    if (msg != null && msg.contains("email address is already in use")) {
                        tilEmail.setError("This email is already registered");
                        etEmail.requestFocus();
                    } else if (msg != null && msg.contains("badly formatted")) {
                        tilEmail.setError("Enter a valid email address");
                        etEmail.requestFocus();
                    } else {
                        Toast.makeText(this, "Registration failed: " + msg, Toast.LENGTH_LONG).show();
                    }
                    Log.e("REGISTER", "Auth error", e);
                });
    }

    // ── Helper: clear error when user starts typing ────────────────────────
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
