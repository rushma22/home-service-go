package lk.rush.homeservicego.activities;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import lk.rush.homeservicego.R;

public class AdminAddServiceActivity extends AppCompatActivity {

    // ── Cloudinary Config ──────────────────────────────────────────────────
    private static final String CLOUDINARY_CLOUD_NAME    = "dvvrgpo5q";
    private static final String CLOUDINARY_UPLOAD_PRESET = "HomeServiceGo";
    // ──────────────────────────────────────────────────────────────────────

    private EditText etServiceName, etDescription, etPrice, etProviderPhone;
    private Spinner spinnerCategory;
    private View btnSaveService, btnPickLocation, btnPickImage;
    private TextView tvSelectedLocation;
    private View cardLocation;
    private ProgressBar progressBar;
    private ImageView imgServicePreview;

    private FirebaseFirestore db;
    private Uri selectedImageUri = null;

    private double selectedLat = 6.9271;
    private double selectedLng = 79.8612;
    private boolean locationPicked = false;

    private ActivityResultLauncher<String> imagePickerLauncher;

    private final String[] categories = {"Cleaning", "Plumbing", "Electrical", "Appliance Repair"};
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    private final List<Address> addressList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_add_service);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Add New Service");
        }

        db = FirebaseFirestore.getInstance();

        etServiceName      = findViewById(R.id.etServiceName);
        etDescription      = findViewById(R.id.etDescription);
        etPrice            = findViewById(R.id.etPrice);
        etProviderPhone    = findViewById(R.id.etProviderPhone);
        spinnerCategory    = findViewById(R.id.spinnerCategory);
        btnSaveService     = findViewById(R.id.btnSaveService);
        btnPickLocation    = findViewById(R.id.btnPickLocation);
        tvSelectedLocation = findViewById(R.id.tvSelectedLocation);
        cardLocation       = findViewById(R.id.cardLocation);
        progressBar        = findViewById(R.id.progressBar);
        btnPickImage       = findViewById(R.id.btnPickImage);
        imgServicePreview  = findViewById(R.id.imgServicePreview);

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, categories);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(spinnerAdapter);

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedImageUri = uri;
                        imgServicePreview.setVisibility(View.VISIBLE);
                        imgServicePreview.setImageURI(uri);
                    }
                });

        btnPickImage.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        btnPickLocation.setOnClickListener(v -> openLocationSearch());
        btnSaveService.setOnClickListener(v -> saveService());
    }

    private void openLocationSearch() {
        int dp = (int) getResources().getDisplayMetrics().density;

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(16 * dp, 8 * dp, 16 * dp, 8 * dp);

        EditText searchInput = new EditText(this);
        searchInput.setHint("Type a place name...");
        searchInput.setSingleLine(true);
        layout.addView(searchInput);

        ListView resultsList = new ListView(this);
        resultsList.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 300 * dp));
        layout.addView(resultsList);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Search Location")
                .setView(layout)
                .setNegativeButton("Cancel", null)
                .create();
        dialog.show();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_list_item_1, new ArrayList<>());
        resultsList.setAdapter(adapter);

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);

                final String query = s.toString().trim();

                if (query.length() < 2) {
                    addressList.clear();
                    adapter.clear();
                    return;
                }

                searchRunnable = () -> searchLocations(query, adapter);
                searchHandler.postDelayed(searchRunnable, 400);
            }
        });

        resultsList.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= addressList.size()) return;
            Address address = addressList.get(position);
            selectedLat    = address.getLatitude();
            selectedLng    = address.getLongitude();
            locationPicked = true;
            tvSelectedLocation.setText(address.getAddressLine(0));
            cardLocation.setVisibility(View.VISIBLE);
            dialog.dismiss();
        });
    }

    private void searchLocations(String query, ArrayAdapter<String> adapter) {
        new Thread(() -> {
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> results;

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    // New async API for Android 13+
                    final List<Address> temp = new ArrayList<>();
                    geocoder.getFromLocationName(query, 6, temp::addAll);
                    results = temp;
                } else {
                    // Sync API for older Android versions
                    results = geocoder.getFromLocationName(query, 6);
                }

                final List<Address> finalResults = results;
                runOnUiThread(() -> {
                    addressList.clear();
                    if (finalResults != null) addressList.addAll(finalResults);
                    adapter.clear();
                    for (Address a : addressList) adapter.add(a.getAddressLine(0));
                    adapter.notifyDataSetChanged();
                });
            } catch (Exception ignored) {}
        }).start();
    }

    // ── Save ──────────────────────────────────────────────────────────────
    private void saveService() {
        String name     = etServiceName.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem().toString();
        String desc     = etDescription.getText().toString().trim();
        String priceStr = etPrice.getText().toString().trim();
        String phone    = etProviderPhone.getText().toString().trim();

        if (TextUtils.isEmpty(name))     { etServiceName.setError("Enter service name");     etServiceName.requestFocus();   return; }
        if (TextUtils.isEmpty(desc))     { etDescription.setError("Enter description");      etDescription.requestFocus();   return; }
        if (TextUtils.isEmpty(priceStr)) { etPrice.setError("Enter price");                  etPrice.requestFocus();         return; }
        if (TextUtils.isEmpty(phone))    { etProviderPhone.setError("Enter provider phone"); etProviderPhone.requestFocus(); return; }

        double price;
        try { price = Double.parseDouble(priceStr); }
        catch (NumberFormatException e) { etPrice.setError("Enter a valid number"); etPrice.requestFocus(); return; }

        String locationLabel = locationPicked
                ? tvSelectedLocation.getText().toString()
                : "Colombo, Sri Lanka (default)";

        progressBar.setVisibility(View.VISIBLE);
        btnSaveService.setEnabled(false);

        if (selectedImageUri != null) {
            uploadImageToCloudinary(selectedImageUri,
                    imageUrl -> saveToFirestore(name, category, desc, price, phone, locationLabel, imageUrl),
                    error -> {
                        progressBar.setVisibility(View.GONE);
                        btnSaveService.setEnabled(true);
                        Toast.makeText(this, "Image upload failed: " + error, Toast.LENGTH_LONG).show();
                    });
        } else {
            saveToFirestore(name, category, desc, price, phone, locationLabel, "");
        }
    }

    // ── Upload to Cloudinary ──────────────────────────────────────────────
    private void uploadImageToCloudinary(Uri imageUri, OnSuccess onSuccess, OnFailure onFailure) {
        new Thread(() -> {
            try {
                // Read and resize image
                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                if (bitmap == null) { runOnUiThread(() -> onFailure.run("Could not read image")); return; }

                int maxSize = 800;
                float scale = Math.min((float) maxSize / bitmap.getWidth(), (float) maxSize / bitmap.getHeight());
                if (scale < 1f) {
                    bitmap = Bitmap.createScaledBitmap(bitmap,
                            (int)(bitmap.getWidth() * scale), (int)(bitmap.getHeight() * scale), true);
                }

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 75, baos);
                byte[] imageData = baos.toByteArray();

                // Multipart POST to Cloudinary
                String boundary = "Boundary" + UUID.randomUUID().toString().replace("-", "");
                URL url = new URL("https://api.cloudinary.com/v1_1/" + CLOUDINARY_CLOUD_NAME + "/image/upload");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(30000);
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
                // upload_preset field
                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"upload_preset\"\r\n\r\n");
                dos.writeBytes(CLOUDINARY_UPLOAD_PRESET + "\r\n");
                // image file
                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"image.jpg\"\r\n");
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

                // Parse secure_url from response JSON
                int urlStart = response.indexOf("\"secure_url\":\"") + 14;
                int urlEnd   = response.indexOf("\"", urlStart);
                if (urlStart > 13 && urlEnd > urlStart) {
                    String imageUrl = response.substring(urlStart, urlEnd).replace("\\/", "/");
                    runOnUiThread(() -> onSuccess.run(imageUrl));
                } else {
                    runOnUiThread(() -> onFailure.run("Upload failed. Check your Cloudinary preset is set to Unsigned."));
                }
            } catch (Exception e) {
                runOnUiThread(() -> onFailure.run(e.getMessage()));
            }
        }).start();
    }

    // ── Save to Firestore ─────────────────────────────────────────────────
    private void saveToFirestore(String name, String category, String desc, double price,
                                  String phone, String locationLabel, String imageUrl) {
        HashMap<String, Object> data = new HashMap<>();
        data.put("name",          name);
        data.put("category",      category);
        data.put("description",   desc);
        data.put("price",         price);
        data.put("providerPhone", phone);
        data.put("latitude",      selectedLat);
        data.put("longitude",     selectedLng);
        data.put("locationName",  locationLabel);
        data.put("imageUrl",      imageUrl);

        db.collection("services").add(data)
                .addOnSuccessListener(ref -> {
                    ref.update("serviceId", ref.getId());
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Service added successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    btnSaveService.setEnabled(true);
                    Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    interface OnSuccess { void run(String result); }
    interface OnFailure { void run(String error); }

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }
}
