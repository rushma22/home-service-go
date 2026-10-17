package lk.rush.homeservicego.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;

import lk.rush.homeservicego.R;
import lk.rush.homeservicego.utils.SessionManager;

public class AdminActivity extends AppCompatActivity {

    private TextView tvAdminWelcome;
    private MaterialCardView cardViewBookings, cardManageServices, cardAddService;
    private MaterialButton btnAdminLogout;

    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        sessionManager       = new SessionManager(this);

        tvAdminWelcome       = findViewById(R.id.tvAdminWelcome);
        cardViewBookings     = findViewById(R.id.cardViewBookings);
        cardManageServices   = findViewById(R.id.cardManageServices);
        cardAddService       = findViewById(R.id.cardAddService);
        btnAdminLogout       = findViewById(R.id.btnAdminLogout);

        String adminName = sessionManager.getUserName();
        if (adminName != null && !adminName.isEmpty()) {
            tvAdminWelcome.setText("Welcome, " + adminName.split(" ")[0] + "!");
        } else {
            tvAdminWelcome.setText("Welcome, Admin!");
        }

        cardViewBookings.setOnClickListener(v ->
                startActivity(new Intent(this, AdminBookingsActivity.class)));

        cardManageServices.setOnClickListener(v ->
                startActivity(new Intent(this, AdminManageServicesActivity.class)));

        cardAddService.setOnClickListener(v ->
                startActivity(new Intent(this, AdminAddServiceActivity.class)));

        btnAdminLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            sessionManager.logoutUser();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }
}
