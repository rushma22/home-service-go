package lk.rush.homeservicego.activities;

import com.google.firebase.firestore.FirebaseFirestore;
import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;

import lk.rush.homeservicego.models.TestMessage;

public class TestFirestoreActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("test")
                .document("firstDoc")
                .set(new TestMessage("Hello Firebase"))
                .addOnSuccessListener(aVoid ->
                        Log.d("FIRESTORE_TEST", "Data written successfully"))
                .addOnFailureListener(e ->
                        Log.e("FIRESTORE_TEST", "Error", e));
    }
}