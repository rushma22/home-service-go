package lk.rush.homeservicego.activities;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import lk.rush.homeservicego.R;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    // These values come from the Service object (passed via Intent)
    private double latitude;
    private double longitude;
    private String serviceName;

    // Default location = Colombo, Sri Lanka (used if no location stored)
    private static final double DEFAULT_LAT = 6.9271;
    private static final double DEFAULT_LNG = 79.8612;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Service Location");
        }

        // Get location data passed from ServiceDetailActivity
        latitude    = getIntent().getDoubleExtra("latitude", DEFAULT_LAT);
        longitude   = getIntent().getDoubleExtra("longitude", DEFAULT_LNG);
        serviceName = getIntent().getStringExtra("serviceName");

        // If no coordinates stored (0,0), use default Colombo location
        if (latitude == 0.0 && longitude == 0.0) {
            latitude  = DEFAULT_LAT;
            longitude = DEFAULT_LNG;
            Toast.makeText(this, "Showing default location", Toast.LENGTH_SHORT).show();
        }

        // Load the Google Map asynchronously
        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.map);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    // Called when the map is fully loaded and ready to use
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Create a point on the map with the service coordinates
        LatLng location = new LatLng(latitude, longitude);

        // Add a marker (pin) with the service name as label
        mMap.addMarker(new MarkerOptions()
                .position(location)
                .title(serviceName));

        // Move camera to the marker and zoom in (zoom 15 = street level)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15));
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
