package lk.rush.homeservicego.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

import lk.rush.homeservicego.R;
import lk.rush.homeservicego.adapters.ServiceAdapter;
import lk.rush.homeservicego.models.Service;

public class ServiceListActivity extends AppCompatActivity {

    private TextView tvCategoryTitle, tvEmptySubtitle;
    private RecyclerView rvServices;
    private LinearLayout layoutEmpty;
    private ProgressBar progressBar;
    private ServiceAdapter adapter;
    private ArrayList<Service> serviceList;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_list);

        tvCategoryTitle  = findViewById(R.id.tvCategoryTitle);
        rvServices       = findViewById(R.id.rvServices);
        layoutEmpty      = findViewById(R.id.layoutEmpty);
        tvEmptySubtitle  = findViewById(R.id.tvEmptySubtitle);
        progressBar      = findViewById(R.id.progressBar);

        db = FirebaseFirestore.getInstance();

        serviceList = new ArrayList<>();
        adapter = new ServiceAdapter(serviceList, service -> {
            Intent intent = new Intent(ServiceListActivity.this, ServiceDetailActivity.class);
            intent.putExtra("service", service);
            startActivity(intent);
        });
        rvServices.setLayoutManager(new LinearLayoutManager(this));
        rvServices.setAdapter(adapter);

        String category = getIntent().getStringExtra("category");

        if (category != null) {
            tvCategoryTitle.setText(category + " Services");
            loadServices(category);
        } else {
            Toast.makeText(this, "Category not found", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadServices(String category) {
        progressBar.setVisibility(View.VISIBLE);
        rvServices.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.GONE);

        db.collection("services")
                .whereEqualTo("category", category)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    progressBar.setVisibility(View.GONE);
                    serviceList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Service service = document.toObject(Service.class);
                        serviceList.add(service);
                    }
                    adapter.notifyDataSetChanged();

                    if (serviceList.isEmpty()) {
                        tvEmptySubtitle.setText("There are no " + category.toLowerCase()
                                + " services available at the moment.\nPlease check back later.");
                        layoutEmpty.setVisibility(View.VISIBLE);
                        rvServices.setVisibility(View.GONE);
                    } else {
                        rvServices.setVisibility(View.VISIBLE);
                        layoutEmpty.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error loading services: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
