package com.example.carebridge;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private EditText weightEditText, heartRateEditText, sugarEditText, bpEditText;
    private DatabaseReference databaseReference;
    private String selectedDateKey;
    private int todayPosition = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        databaseReference = FirebaseDatabase.getInstance().getReference("Vitals");

        weightEditText = findViewById(R.id.weightValueEditText);
        heartRateEditText = findViewById(R.id.heartRateEditText);
        sugarEditText = findViewById(R.id.sugarEditText);
        bpEditText = findViewById(R.id.bpEditText);
        Button saveButton = findViewById(R.id.saveButton);
        ImageView shareIcon = findViewById(R.id.share);

        Calendar today = Calendar.getInstance();
        selectedDateKey = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(today.getTime());

        RecyclerView recyclerView = findViewById(R.id.dateRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        List<CalendarDate> dateList = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -29);
        for (int i = 0; i < 30; i++) {
            String day = new SimpleDateFormat("EEE", Locale.getDefault()).format(calendar.getTime());
            String date = new SimpleDateFormat("dd", Locale.getDefault()).format(calendar.getTime());
            dateList.add(new CalendarDate(day, date));

            Calendar temp = Calendar.getInstance();
            temp.add(Calendar.DAY_OF_YEAR, -29 + i);
            String dateKey = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(temp.getTime());
            if (dateKey.equals(selectedDateKey)) {
                todayPosition = i;
            }

            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        DateAdapter adapter = new DateAdapter(dateList, position -> {
            weightEditText.setText("");
            heartRateEditText.setText("");
            sugarEditText.setText("");
            bpEditText.setText("");

            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, -29 + position);
            selectedDateKey = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());

            databaseReference.child(selectedDateKey).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        weightEditText.setText(snapshot.child("weight").getValue(String.class));
                        heartRateEditText.setText(snapshot.child("heartRate").getValue(String.class));
                        sugarEditText.setText(snapshot.child("sugar").getValue(String.class));
                        bpEditText.setText(snapshot.child("bp").getValue(String.class));
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(MainActivity.this, "Failed to load data", Toast.LENGTH_SHORT).show();
                }
            });
        }, todayPosition);

        recyclerView.setAdapter(adapter);
        if (todayPosition != -1) {
            recyclerView.scrollToPosition(todayPosition);
        }

        saveButton.setOnClickListener(v -> {
            String weight = weightEditText.getText().toString().trim();
            String heartRate = heartRateEditText.getText().toString().trim();
            String sugar = sugarEditText.getText().toString().trim();
            String bp = bpEditText.getText().toString().trim();

            if (weight.isEmpty() && heartRate.isEmpty() && sugar.isEmpty() && bp.isEmpty()) {
                Toast.makeText(this, "Please enter at least one value", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("weight", weight);
            dataMap.put("heartRate", heartRate);
            dataMap.put("sugar", sugar);
            dataMap.put("bp", bp);

            databaseReference.child(selectedDateKey).setValue(dataMap)
                    .addOnSuccessListener(aVoid -> Toast.makeText(this, "Vitals saved", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to save: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });

        shareIcon.setOnClickListener(v -> {
            try {
                View rootView = getWindow().getDecorView().getRootView();
                Bitmap bitmap = Bitmap.createBitmap(rootView.getWidth(), rootView.getHeight(), Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                rootView.draw(canvas);

                File cachePath = new File(getCacheDir(), "images");
                cachePath.mkdirs();
                File file = new File(cachePath, "screenshot.png");
                FileOutputStream stream = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                stream.close();

                Uri contentUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
                if (contentUri != null) {
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("image/*");
                    shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(shareIntent, "Share via"));
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Error sharing screenshot", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
