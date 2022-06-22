package com.gruppo6.boatrecognitionapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Rect;
import android.media.Image;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.util.Size;
import android.view.Gravity;
import android.view.OrientationEventListener;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.common.model.LocalModel;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions;
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions;
import com.google.mlkit.vision.objects.defaults.PredefinedCategory;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class CameraActivity extends AppCompatActivity {

    private final String TAG = "CameraActivity";

    private Set<Integer> objIndex = new HashSet<>
            (Arrays.asList(629, 781, 815, 872, 626, 915, 485));

    private PreviewView previewView;
    private Button captureImage;

    // AI model init
    private Executor executor = Executors.newSingleThreadExecutor();
    LocalModel localModel =
            new LocalModel.Builder()
                    .setAssetFilePath("mobilenet.tflite")
                    .build();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View decorView = getWindow().getDecorView();
        // Hide the status bar.
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
        setContentView(R.layout.activity_camera);

        previewView = findViewById(R.id.preview_view);
        captureImage = findViewById(R.id.captureImg);
        captureImage.setVisibility(View.VISIBLE);
        findViewById(R.id.loadingPanel).setVisibility(View.GONE);

        final ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {

                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    bindPreview(cameraProvider);

                } catch (ExecutionException | InterruptedException e) {
                    // No errors need to be handled for this Future.
                    // This should never be reached.
                }
            }
        }, ContextCompat.getMainExecutor(this));
    }

    void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {

        Preview preview = new Preview.Builder()
                .build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .build();

        ImageCapture.Builder builder = new ImageCapture.Builder();

        final ImageCapture imageCapture = builder
                .setTargetRotation(this.getWindowManager().getDefaultDisplay().getRotation())
                .build();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        Camera camera = cameraProvider.bindToLifecycle((LifecycleOwner)this, cameraSelector, preview, imageAnalysis, imageCapture);

        // AI model constructor
        CustomObjectDetectorOptions customObjectDetectorOptions =
                new CustomObjectDetectorOptions.Builder(localModel)
                        .setDetectorMode(CustomObjectDetectorOptions.SINGLE_IMAGE_MODE)
                        .enableMultipleObjects()
                        .enableClassification()
                        .setClassificationConfidenceThreshold(0.5f)
                        .setMaxPerObjectLabelCount(3)
                        .build();
        ObjectDetector objectDetector =
                ObjectDetection.getClient(customObjectDetectorOptions);

        Toast toast = Toast.makeText(CameraActivity.this, "No boat recognized", Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);

        captureImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                captureImage.setVisibility(View.GONE);
                findViewById(R.id.loadingPanel).setVisibility(View.VISIBLE);
                captureImage.setEnabled(false);

                imageCapture.takePicture(executor, new ImageCapture.OnImageCapturedCallback() {
                    @Override
                    public void onCaptureSuccess(@NonNull ImageProxy imageProxy) {
                        super.onCaptureSuccess(imageProxy);
                        @SuppressLint("UnsafeOptInUsageError") Image mediaImage = imageProxy.getImage();
                        InputImage image =
                                InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
                        // Object detection on image
                        objectDetector.process(image)
                                .addOnSuccessListener(
                                        new OnSuccessListener<List<DetectedObject>>() {
                                            @Override
                                            public void onSuccess(List<DetectedObject> results) {
                                                captureImage.setVisibility(View.VISIBLE);
                                                findViewById(R.id.loadingPanel).setVisibility(View.GONE);
                                                captureImage.setEnabled(true);
                                                if(results.isEmpty()){
                                                    toast.show();
                                                }
                                                for (DetectedObject detectedObject : results) {
                                                    //Rect boundingBox = detectedObject.getBoundingBox();
                                                    //Integer trackingId = detectedObject.getTrackingId();
                                                    // liner -> 629
                                                    // schooner -> 781
                                                    // speedboat -> 815
                                                    // trimaran -> 872
                                                    // lifeboat -> 626
                                                    // yawl -> 915
                                                    // catamaran -> 485
                                                    for (DetectedObject.Label label : detectedObject.getLabels()) {
                                                        String text = label.getText();
                                                        int index = label.getIndex();
                                                        float confidence = label.getConfidence();
                                                        Log.d(TAG, "Object found: " + text
                                                                + ", Index: " + index
                                                                + ", Confidence: " + confidence);

                                                        if(objIndex.contains(index)){
                                                            toast.cancel();
                                                            Intent intent = new Intent(CameraActivity.this, MainActivity.class);
                                                            intent.putExtra("boatCounterSignal", 1);
                                                            startActivity(intent);
                                                        }else {
                                                            toast.show();
                                                        }
                                                    }
                                                }
                                            }
                                        })
                                .addOnFailureListener(
                                        new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                toast.show();
                                            }
                                        });
                        imageProxy.close();
                    }
                });


            }
        });
    }
}