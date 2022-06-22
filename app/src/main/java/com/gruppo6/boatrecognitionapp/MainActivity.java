package com.gruppo6.boatrecognitionapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private static final String[] CAMERA_PERMISSION = new String[]{Manifest.permission.CAMERA};
    private static final int CAMERA_REQUEST_CODE = 10;
    static final int REQUEST_IMAGE_CAPTURE = 1;
    private static int boatCounter = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnStartCamera = findViewById(R.id.btnStartCamera);
        TextView boatCounterView = findViewById(R.id.boatCounter);

        boatCounterView.setText("Boat counter: " + String.valueOf(boatCounter));

        btnStartCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (hasCameraPermission()) {
                    startCamera();
                } else {
                    requestPermission();
                }
            }
        });

        Bundle extras = getIntent().getExtras();
        if(extras != null){
            int value = extras.getInt("boatCounterSignal");
            boatCounter += value;
            boatCounterView.setText("Boat counter: " + String.valueOf(boatCounter));
        }

    }

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(
                this,
                CAMERA_PERMISSION,
                CAMERA_REQUEST_CODE
        );
    }

    private void startCamera() {
        Intent intent = new Intent(this, CameraActivity.class);
        startActivity(intent);
    }
}