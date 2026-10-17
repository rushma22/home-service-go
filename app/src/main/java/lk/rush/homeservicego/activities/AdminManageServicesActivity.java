package lk.rush.homeservicego.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
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
import lk.rush.homeservicego.adapters.AdminServiceAdapter;
import lk.rush.homeservicego.models.Service;

public class AdminManageServicesActivity extends AppCompatActivity {

    private RecyclerView rvServices;
    private AdminServiceAdapter adapter;
    private ArrayList<Service> serviceList;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private Button btnAddNewService;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_manage_services);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        db = FirebaseFirestore.getInstance();

        rvServices       = findViewById(R.id.rvServices);
        progressBar      = findViewById(R.id.progressBar);
        tvEmpty          = findViewById(R.id.tvEmpty);
        btnAddNewService = findViewById(R.id.btnAddNewService);

        serviceList = new ArrayList<>();

        // Adapter now handles both Edit and Delete
        adapter = new AdminServiceAdapter(serviceList, new AdminServiceAdapter.OnActionListener() {
            @Override
            public void onEdit(Service service, int position) {
                // Open Edit screen with the selected service
                Intent intent = new Intent(AdminManageServicesActivity.this, AdminEditServiceActivity.class);
                intent.putExtra("service", service);
                startActivity(intent);
            }

            @Override
            public void onDelete(Service service, int position) {
                showDeleteDialog(service, position);
            }
        });

        rvServices.setLayoutManager(new LinearLayoutManager(this));
        rvServices.setAdapter(adapter);

        btnAddNewService.setOnClickListener(v ->
                startActivity(new Intent(this, AdminAddServiceActivity.class)));

        loadServices();
    }

    // Reload list every time we come back (after Add or Edit)
    @Override
    protected void onResume() {
        super.onResume();
        loadServices();
    }

    private void loadServices() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        db.collection("services")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    progressBar.setVisibility(View.GONE);
                    serviceList.clear();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Service service = doc.toObject(Service.class);
                        serviceList.add(service);
                    }

                    adapter.notifyDataSetChanged();

                    if (serviceList.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to load services", Toast.LENGTH_SHORT).show();
                });
    }

    private void showDeleteDialog(Service service, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Service")
                .setMessage("Are you sure you want to delete \"" + service.getName() + "\"?")
                .setPositiveButton("Delete", (dialog, which) -> deleteService(service, position))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteService(Service service, int position) {
        db.collection("services").document(service.getServiceId())
                .delete()
                .addOnSuccessListener(unused -> {
                    adapter.removeItem(position);
                    Toast.makeText(this, "Service deleted", Toast.LENGTH_SHORT).show();
                    if (serviceList.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to delete: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
