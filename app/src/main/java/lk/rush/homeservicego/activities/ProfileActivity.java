package lk.rush.homeservicego.activities;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.UUID;

import lk.rush.homeservicego.R;
import lk.rush.homeservicego.utils.SessionManager;

public class ProfileActivity extends AppCompatActivity {

    @SuppressWarnings("SpellCheckingInspection")
    private static final String CLOUDINARY_CLOUD_NAME    = "dvvrgpo5q";
    private static final String CLOUDINARY_UPLOAD_PRESET = "HomeServiceGo";

    private EditText etName, etEmail, etPhone;
    private TextView tvRole, tvAvatarInitial;
    private ImageView imgProfile;
    private Button btnSave;
    private ProgressBar progressBar;

    private FirebaseFirestore db;
    private SessionManager sessionManager;

    private Uri selectedImageUri = null;
    private ActivityResultLauncher<String> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("My Profile");
        }

        db             = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(this);

        etName          = findViewById(R.id.etName);
        etEmail         = findViewById(R.id.etEmail);
        etPhone         = findViewById(R.id.etPhone);
        tvRole          = findViewById(R.id.tvRole);
        tvAvatarInitial = findViewById(R.id.tvAvatarInitial);
        imgProfile      = findViewById(R.id.imgProfile);
        btnSave         = findViewById(R.id.btnSave);
        progressBar     = findViewById(R.id.progressBar);
        FrameLayout frameAvatar = findViewById(R.id.frameAvatar);

        // Image picker
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedImageUri = uri;
                        tvAvatarInitial.setVisibility(View.GONE);
                        imgProfile.setVisibility(View.VISIBLE);
                        imgProfile.setImageURI(uri);
                    }
                });

        frameAvatar.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        loadProfile();
        btnSave.setOnClickListener(v -> saveProfile());
    }

    private void loadProfile() {
        progressBar.setVisibility(View.VISIBLE);
        String userId = sessionManager.getUserId();

        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(document -> {
                    progressBar.setVisibility(View.GONE);
                    if (document.exists()) {
                        etName.setText(document.getString("name"));
                        etEmail.setText(document.getString("email"));
                        etPhone.setText(document.getString("phone"));
                        tvRole.setText(document.getString("role"));

                        // Load saved profile image if exists
                        String imageUrl = document.getString("profileImageUrl");
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            tvAvatarInitial.setVisibility(View.GONE);
                            imgProfile.setVisibility(View.VISIBLE);
                            loadImageFromUrl(imageUrl);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Log.e("PROFILE", "Failed to load profile for userId: " + userId, e);
                    Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadImageFromUrl(String imageUrl) {
        new Thread(() -> {
            try {
                URL url = new URL(imageUrl);
                Bitmap bitmap = BitmapFactory.decodeStream(url.openStream());
                runOnUiThread(() -> imgProfile.setImageBitmap(bitmap));
            } catch (Exception ignored) {}
        }).start();
    }

    private void saveProfile() {
        String name  = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            etName.setError("Enter your name");
            etName.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(phone)) {
            etPhone.setError("Enter your phone number");
            etPhone.requestFocus();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnSave.setEnabled(false);

        if (selectedImageUri != null) {
            // Upload image first, then save all data
            uploadImageToCloudinary(selectedImageUri,
                    imageUrl -> saveToFirestore(name, phone, imageUrl),
                    error -> {
                        progressBar.setVisibility(View.GONE);
                        btnSave.setEnabled(true);
                        Toast.makeText(this, "Image upload failed: " + error, Toast.LENGTH_LONG).show();
                    });
        } else {
            saveToFirestore(name, phone, null);
        }
    }

    private void saveToFirestore(String name, String phone, String imageUrl) {
        String userId = sessionManager.getUserId();

        HashMap<String, Object> updates = new HashMap<>();
        updates.put("name",  name);
        updates.put("phone", phone);
        if (imageUrl != null) {
            updates.put("profileImageUrl", imageUrl);
        }

        db.collection("users").document(userId)
                .update(updates)
                .addOnSuccessListener(unused -> {
                    progressBar.setVisibility(View.GONE);
                    btnSave.setEnabled(true);
                    sessionManager.updateUserName(name);
                    Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    btnSave.setEnabled(true);
                    Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @SuppressWarnings("SpellCheckingInspection")
    private void uploadImageToCloudinary(Uri imageUri, OnSuccess onSuccess, OnFailure onFailure) {
        new Thread(() -> {
            try {
                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                if (bitmap == null) { runOnUiThread(() -> onFailure.run("Could not read image")); return; }

                // Resize to max 400px for profile photo
                int maxSize = 400;
                float scale = Math.min((float) maxSize / bitmap.getWidth(), (float) maxSize / bitmap.getHeight());
                if (scale < 1f) {
                    bitmap = Bitmap.createScaledBitmap(bitmap,
                            (int)(bitmap.getWidth() * scale), (int)(bitmap.getHeight() * scale), true);
                }

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                byte[] imageData = baos.toByteArray();

                String boundary = "Boundary" + UUID.randomUUID().toString().replace("-", "");
                URL url = new URL("https://api.cloudinary.com/v1_1/" + CLOUDINARY_CLOUD_NAME + "/image/upload");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(30000);
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"upload_preset\"\r\n\r\n");
                dos.writeBytes(CLOUDINARY_UPLOAD_PRESET + "\r\n");
                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"profile.jpg\"\r\n");
                dos.writeBytes("Content-Type: image/jpeg\r\n\r\n");
                dos.write(imageData);
                dos.writeBytes("\r\n--" + boundary + "--\r\n");
                dos.flush();
                dos.close();

                int code = conn.getResponseCode();
                InputStream responseStream = (code == 200) ? conn.getInputStream() : conn.getErrorStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(responseStream));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                String response = sb.toString();

                int urlStart = response.indexOf("\"secure_url\":\"") + 14;
                int urlEnd   = response.indexOf("\"", urlStart);
                if (urlStart > 13 && urlEnd > urlStart) {
                    String uploadedUrl = response.substring(urlStart, urlEnd).replace("\\/", "/");
                    runOnUiThread(() -> onSuccess.run(uploadedUrl));
                } else {
                    runOnUiThread(() -> onFailure.run("Upload failed"));
                }
            } catch (Exception e) {
                runOnUiThread(() -> onFailure.run(e.getMessage()));
            }
        }).start();
    }

    interface OnSuccess { void run(String result); }
    interface OnFailure { void run(String error); }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
