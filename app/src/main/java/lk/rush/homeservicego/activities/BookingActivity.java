package lk.rush.homeservicego.activities;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import lk.rush.homeservicego.R;
import lk.rush.homeservicego.models.Service;
import lk.rush.homeservicego.utils.SessionManager;

public class BookingActivity extends AppCompatActivity {

    private TextView tvBookingServiceName, tvSelectedAddress, btnPickAddress;
    private Button btnSelectDate, btnSelectTime, btnConfirmBooking;
    private View cardAddress, cardServiceImage;
    private ImageView imgServicePreview;
    private ProgressBar progressBar;

    private FirebaseFirestore db;
    private SessionManager sessionManager;

    private String selectedDate    = "";
    private String selectedTime    = "";
    private String selectedAddress = "";
    private double addressLat      = 0;
    private double addressLng      = 0;

    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    // Holds current address search results
    private final List<Address> addressList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Book Service");
        }

        Service service = (Service) getIntent().getSerializableExtra("service");
        if (service == null) {
            Toast.makeText(this, "Service not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db             = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(this);

        tvBookingServiceName = findViewById(R.id.tvBookingServiceName);
        btnSelectDate        = findViewById(R.id.btnSelectDate);
        btnSelectTime        = findViewById(R.id.btnSelectTime);
        btnConfirmBooking    = findViewById(R.id.btnConfirmBooking);
        btnPickAddress       = (TextView) findViewById(R.id.btnPickAddress);
        tvSelectedAddress    = findViewById(R.id.tvSelectedAddress);
        cardAddress          = findViewById(R.id.cardAddress);
        cardServiceImage     = findViewById(R.id.cardServiceImage);
        imgServicePreview    = findViewById(R.id.imgServicePreview);
        progressBar          = findViewById(R.id.progressBar);

        // Show service name in header
        tvBookingServiceName.setText(service.getName());

        // Show service image if available
        if (service.getImageUrl() != null && !service.getImageUrl().isEmpty()) {
            cardServiceImage.setVisibility(View.VISIBLE);
            Glide.with(this)
                    .load(service.getImageUrl())
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(imgServicePreview);
        }

        // Address search button
        btnPickAddress.setOnClickListener(v -> openAddressSearch());

        // Date picker
        btnSelectDate.setOnClickListener(v -> {
            Calendar cal   = Calendar.getInstance();
            DatePickerDialog datePicker = new DatePickerDialog(this,
                    (view, y, m, d) -> {
                        selectedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", y, m + 1, d);
                        btnSelectDate.setText("Date: " + selectedDate);
                    },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH));
            datePicker.getDatePicker().setMinDate(System.currentTimeMillis());
            datePicker.show();
        });

        // Time picker
        btnSelectTime.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new TimePickerDialog(this,
                    (view, h, min) -> {
                        String amPm = h >= 12 ? "PM" : "AM";
                        int h12 = h > 12 ? h - 12 : (h == 0 ? 12 : h);
                        selectedTime = String.format(Locale.getDefault(), "%02d:%02d %s", h12, min, amPm);
                        btnSelectTime.setText("Time: " + selectedTime);
                    },
                    cal.get(Calendar.HOUR_OF_DAY),
                    cal.get(Calendar.MINUTE),
                    false).show();
        });

        btnConfirmBooking.setOnClickListener(v -> confirmBooking(service));
    }

    // ── Address search dialog (same as admin panel — uses Geocoder, no billing needed) ──
    private void openAddressSearch() {
        int dp = (int) getResources().getDisplayMetrics().density;

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(16 * dp, 8 * dp, 16 * dp, 8 * dp);

        EditText searchInput = new EditText(this);
        searchInput.setHint("Type your address...");
        searchInput.setSingleLine(true);
        layout.addView(searchInput);

        ListView resultsList = new ListView(this);
        resultsList.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 300 * dp));
        layout.addView(resultsList);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Search Address")
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

                // Capture as String right away so the lambda gets the correct value
                final String query = s.toString().trim();

                if (query.length() < 2) {
                    addressList.clear();
                    adapter.clear();
                    return;
                }

                searchRunnable = () -> searchAddresses(query, adapter);
                searchHandler.postDelayed(searchRunnable, 400);
            }
        });

        resultsList.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= addressList.size()) return;
            Address address = addressList.get(position);
            addressLat      = address.getLatitude();
            addressLng      = address.getLongitude();
            selectedAddress = address.getAddressLine(0);
            tvSelectedAddress.setText(selectedAddress);
            cardAddress.setVisibility(View.VISIBLE);
            dialog.dismiss();
        });
    }

    // Runs Geocoder in background thread, updates list on UI thread
    private void searchAddresses(String query, ArrayAdapter<String> adapter) {
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
                    // Sync API for Android 12 and below
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

    // ── Confirm and save booking to Firestore ─────────────────────────────
    private void confirmBooking(Service service) {
        if (selectedDate.isEmpty()) {
            Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedTime.isEmpty()) {
            Toast.makeText(this, "Please select a time", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedAddress.isEmpty()) {
            Toast.makeText(this, "Please select your service address", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnConfirmBooking.setEnabled(false);

        String userId   = sessionManager.getUserId();
        String userName = sessionManager.getUserName();

        HashMap<String, Object> bookingData = new HashMap<>();
        bookingData.put("userId",       userId);
        bookingData.put("userName",     userName);
        bookingData.put("serviceId",    service.getServiceId());
        bookingData.put("serviceName",  service.getName());
        bookingData.put("bookingDate",  selectedDate);
        bookingData.put("bookingTime",  selectedTime);
        bookingData.put("status",       "pending");
        bookingData.put("locationName", selectedAddress);
        bookingData.put("locationLat",  addressLat);
        bookingData.put("locationLng",  addressLng);
        bookingData.put("createdAt",    System.currentTimeMillis());

        db.collection("bookings")
                .add(bookingData)
                .addOnSuccessListener(ref -> {
                    ref.update("bookingId", ref.getId());
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Booking confirmed!", Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    btnConfirmBooking.setEnabled(true);
                    Toast.makeText(this, "Booking failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
