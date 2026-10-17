package lk.rush.homeservicego.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;

import lk.rush.homeservicego.R;

public class EmergencyActivity extends AppCompatActivity {

    private MaterialCardView cardEmergencyPlumbing;
    private MaterialCardView cardEmergencyElectrical;
    private MaterialCardView cardEmergencyCleaning;
    private MaterialCardView cardEmergencyAppliance;
    private Button btnEmergencyCall;
    private Button btnCancelEmergency;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("🚨 Emergency Assistance");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        cardEmergencyPlumbing   = findViewById(R.id.cardEmergencyPlumbing);
        cardEmergencyElectrical = findViewById(R.id.cardEmergencyElectrical);
        cardEmergencyCleaning   = findViewById(R.id.cardEmergencyCleaning);
        cardEmergencyAppliance  = findViewById(R.id.cardEmergencyAppliance);
        btnEmergencyCall        = findViewById(R.id.btnEmergencyCall);
        btnCancelEmergency      = findViewById(R.id.btnCancelEmergency);

        cardEmergencyPlumbing.setOnClickListener(v -> openEmergencyCategory("Plumbing"));
        cardEmergencyElectrical.setOnClickListener(v -> openEmergencyCategory("Electrical"));
        cardEmergencyCleaning.setOnClickListener(v -> openEmergencyCategory("Cleaning"));
        cardEmergencyAppliance.setOnClickListener(v -> openEmergencyCategory("Appliance Repair"));

        btnEmergencyCall.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:18007378423")); // 1-800-SERVICE
            startActivity(intent);
        });

        btnCancelEmergency.setOnClickListener(v -> finish());
    }

    private void openEmergencyCategory(String category) {
        Intent intent = new Intent(EmergencyActivity.this, ServiceListActivity.class);
        intent.putExtra("category", category);
        intent.putExtra("isEmergency", true);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
