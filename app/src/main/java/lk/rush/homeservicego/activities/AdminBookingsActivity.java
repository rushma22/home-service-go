package lk.rush.homeservicego.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

import lk.rush.homeservicego.R;
import lk.rush.homeservicego.adapters.AdminBookingAdapter;
import lk.rush.homeservicego.models.Booking;

public class AdminBookingsActivity extends AppCompatActivity {

    private RecyclerView rvAdminBookings;
    private AdminBookingAdapter adapter;
    private ArrayList<Booking> bookingList;
    private ProgressBar progressBar;
    private TextView tvEmpty;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_bookings);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        db = FirebaseFirestore.getInstance();

        rvAdminBookings = findViewById(R.id.rvAdminBookings);
        progressBar     = findViewById(R.id.progressBar);
        tvEmpty         = findViewById(R.id.tvEmpty);

        bookingList = new ArrayList<>();

        // Pass a lambda that shows an AlertDialog to pick new status
        adapter = new AdminBookingAdapter(bookingList, (booking, position) -> {
            showStatusDialog(booking, position);
        });

        rvAdminBookings.setLayoutManager(new LinearLayoutManager(this));
        rvAdminBookings.setAdapter(adapter);

        loadAllBookings();
    }

    private void loadAllBookings() {
        progressBar.setVisibility(View.VISIBLE);

        // Admin loads ALL bookings from Firestore
        db.collection("bookings")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    progressBar.setVisibility(View.GONE);
                    bookingList.clear();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Booking booking = doc.toObject(Booking.class);
                        bookingList.add(booking);
                    }

                    // Sort bookings from newest to oldest (newest on top)
                    bookingList.sort((b1, b2) -> {
                        if (b1.getCreatedAt() > 0 && b2.getCreatedAt() > 0) {
                            return Long.compare(b2.getCreatedAt(), b1.getCreatedAt());
                        }

                        String d1 = b1.getBookingDate() != null ? b1.getBookingDate() : "";
                        String d2 = b2.getBookingDate() != null ? b2.getBookingDate() : "";
                        int dateComp = d2.compareTo(d1);
                        if (dateComp != 0) return dateComp;

                        String t1 = b1.getBookingTime() != null ? b1.getBookingTime() : "";
                        String t2 = b2.getBookingTime() != null ? b2.getBookingTime() : "";
                        int timeComp = t2.compareTo(t1);
                        if (timeComp != 0) return timeComp;

                        return Long.compare(b2.getCreatedAt(), b1.getCreatedAt());
                    });

                    adapter.notifyDataSetChanged();

                    if (bookingList.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                    } else {
                        tvEmpty.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to load bookings", Toast.LENGTH_SHORT).show();
                });
    }

    // Shows a dialog with status options: pending, confirmed, completed
    private void showStatusDialog(Booking booking, int position) {
        String[] statusOptions = {"pending", "confirmed", "completed"};

        new AlertDialog.Builder(this)
                .setTitle("Update Booking Status")
                .setItems(statusOptions, (dialog, which) -> {
                    String newStatus = statusOptions[which];

                    // Update status in Firestore using the bookingId
                    db.collection("bookings").document(booking.getBookingId())
                            .update("status", newStatus)
                            .addOnSuccessListener(unused -> {
                                // Update the local object and refresh that card
                                booking.setStatus(newStatus);
                                adapter.updateItem(position);
                                Toast.makeText(this, "Status updated to: " + newStatus, Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to update status", Toast.LENGTH_SHORT).show();
                            });
                })
                .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
