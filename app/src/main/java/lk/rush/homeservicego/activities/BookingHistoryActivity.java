package lk.rush.homeservicego.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

import lk.rush.homeservicego.R;
import lk.rush.homeservicego.adapters.BookingAdapter;
import lk.rush.homeservicego.models.Booking;
import lk.rush.homeservicego.utils.SessionManager;

public class BookingHistoryActivity extends AppCompatActivity {

    private RecyclerView rvBookings;
    private BookingAdapter adapter;
    private ArrayList<Booking> bookingList;
    private ProgressBar progressBar;
    private LinearLayout layoutEmpty;

    private FirebaseFirestore db;
    private SessionManager sessionManager;
    private ListenerRegistration historyListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_history);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        db             = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(this);

        rvBookings  = findViewById(R.id.rvBookings);
        progressBar = findViewById(R.id.progressBar);
        layoutEmpty = findViewById(R.id.layoutEmpty);

        bookingList = new ArrayList<>();
        adapter     = new BookingAdapter(bookingList);
        rvBookings.setLayoutManager(new LinearLayoutManager(this));
        rvBookings.setAdapter(adapter);

        loadMyBookings();
    }

    private void loadMyBookings() {
        progressBar.setVisibility(View.VISIBLE);
        rvBookings.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.GONE);

        String userId = sessionManager.getUserId();

        if (userId == null || userId.isEmpty()) {
            progressBar.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.VISIBLE);
            return;
        }

        historyListener = db.collection("bookings")
                .whereEqualTo("userId", userId)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    progressBar.setVisibility(View.GONE);

                    if (e != null) {
                        Toast.makeText(this, "Failed to load bookings: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (queryDocumentSnapshots != null) {
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
                            layoutEmpty.setVisibility(View.VISIBLE);
                            rvBookings.setVisibility(View.GONE);
                        } else {
                            rvBookings.setVisibility(View.VISIBLE);
                            layoutEmpty.setVisibility(View.GONE);
                        }
                    }
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (historyListener != null) {
            historyListener.remove();
        }
    }
}
