package lk.rush.homeservicego.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;
import java.util.Map;

import lk.rush.homeservicego.R;
import lk.rush.homeservicego.models.Booking;
import lk.rush.homeservicego.utils.NotificationHelper;
import lk.rush.homeservicego.utils.SessionManager;
import lk.rush.homeservicego.utils.ShakeDetector;

public class HomeActivity extends AppCompatActivity {

    private MaterialCardView cardCleaning, cardPlumbing, cardElectrical, cardAppliance;
    private Button btnLogout, btnViewBookings, btnProfile;
    private TextView tvTitle;

    private SessionManager sessionManager;
    private ListenerRegistration bookingListener;
    private final Map<String, String> previousStatuses = new HashMap<>();
    private boolean isFirstLoad = true;

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private ShakeDetector mShakeDetector;

    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        cardCleaning    = findViewById(R.id.cardCleaning);
        cardPlumbing    = findViewById(R.id.cardPlumbing);
        cardElectrical  = findViewById(R.id.cardElectrical);
        cardAppliance   = findViewById(R.id.cardAppliance);
        btnLogout       = findViewById(R.id.btnLogout);
        btnViewBookings = findViewById(R.id.btnViewBookings);
        btnProfile      = findViewById(R.id.btnProfile);
        tvTitle         = findViewById(R.id.tvTitle);

        sessionManager = new SessionManager(this);

        setupNotificationPermission();
        setupShakeDetector();

        cardCleaning.setOnClickListener(v -> openServiceList("Cleaning"));
        cardPlumbing.setOnClickListener(v -> openServiceList("Plumbing"));
        cardElectrical.setOnClickListener(v -> openServiceList("Electrical"));
        cardAppliance.setOnClickListener(v -> openServiceList("Appliance Repair"));

        btnViewBookings.setOnClickListener(v ->
                startActivity(new Intent(HomeActivity.this, BookingHistoryActivity.class)));

        btnProfile.setOnClickListener(v ->
                startActivity(new Intent(HomeActivity.this, ProfileActivity.class)));

        updateGreeting();
        listenForBookingUpdates();

        btnLogout.setOnClickListener(v -> {
            if (bookingListener != null) {
                bookingListener.remove();
            }
            FirebaseAuth.getInstance().signOut();
            sessionManager.logoutUser();
            Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void setupNotificationPermission() {
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    // Permission result handled
                });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void listenForBookingUpdates() {
        String userId = sessionManager.getUserId();
        if (userId == null || userId.isEmpty()) return;

        bookingListener = FirebaseFirestore.getInstance()
                .collection("bookings")
                .whereEqualTo("userId", userId)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;

                    if (isFirstLoad) {
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            Booking booking = doc.toObject(Booking.class);
                            if (booking != null && booking.getBookingId() != null && booking.getStatus() != null) {
                                previousStatuses.put(booking.getBookingId(), booking.getStatus());
                            }
                        }
                        isFirstLoad = false;
                        return;
                    }

                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        Booking booking = dc.getDocument().toObject(Booking.class);
                        if (booking == null || booking.getBookingId() == null) continue;

                        String oldStatus = previousStatuses.get(booking.getBookingId());
                        String newStatus = booking.getStatus();

                        if (oldStatus != null && newStatus != null && !oldStatus.equalsIgnoreCase(newStatus)) {
                            String title = "Booking Status Updated";
                            String serviceName = booking.getServiceName() != null ? booking.getServiceName() : "Service";
                            String message = "Your booking for '" + serviceName +
                                    "' status is now " + capitalize(newStatus) + ".";

                            NotificationHelper.showNotification(HomeActivity.this, title, message);
                        }

                        if (newStatus != null) {
                            previousStatuses.put(booking.getBookingId(), newStatus);
                        }
                    }
                });
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return "";
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private void setupShakeDetector() {
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (mSensorManager != null) {
            mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            mShakeDetector = new ShakeDetector();
            mShakeDetector.setOnShakeListener(() -> {
                Intent intent = new Intent(HomeActivity.this, EmergencyActivity.class);
                startActivity(intent);
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateGreeting();
        if (mSensorManager != null && mAccelerometer != null) {
            mSensorManager.registerListener(mShakeDetector, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(mShakeDetector);
        }
    }

    private void updateGreeting() {
        String fullName = sessionManager.getUserName();
        if (fullName != null && !fullName.isEmpty()) {
            String firstName = fullName.split(" ")[0];
            tvTitle.setText("Hi, " + firstName + "!");
        }
    }

    private void openServiceList(String category) {
        Intent intent = new Intent(HomeActivity.this, ServiceListActivity.class);
        intent.putExtra("category", category);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bookingListener != null) {
            bookingListener.remove();
        }
    }
}
