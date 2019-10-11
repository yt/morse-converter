package com.example.morseconverter;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.EditText;
import android.hardware.camera2.CameraManager;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    SurfaceView mCameraView;
    EditText mTextView;
    CameraSource mCameraSource;

    private static final String TAG = "MainActivity";
    private static final int requestPermissionID = 101;
    private boolean isCameraActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mCameraView = findViewById(R.id.surfaceView);
        mTextView = findViewById(R.id.text_view);
        mTextView.setEnabled(false);

        startCameraSource();
    }

    private void flashLightSwitch(boolean onOff, int milliseconds) {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            String cameraId = cameraManager.getCameraIdList()[0];
            cameraManager.setTorchMode(cameraId, onOff);
            Thread.sleep(milliseconds);
        } catch (CameraAccessException e) {
            Log.w(TAG, "Flash light access can not be established");
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode != requestPermissionID) {
            Log.w(TAG, "Got unexpected permission result: " + requestCode);
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (grantResults[0] != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
               return; // TODO implement permission not set page or popup.
        }

        if (!getApplicationContext().getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            Log.w(TAG, "Flash is not available");
        }

        try {
            mCameraSource.start(mCameraView.getHolder());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startCameraSource() {

        final TextRecognizer textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();

        if (!textRecognizer.isOperational()) {
            Log.w(TAG, "Detector dependencies not loaded yet");
            return;
        }

        mCameraSource = new CameraSource.Builder(getApplicationContext(), textRecognizer)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedPreviewSize(1280, 1024)
                .setAutoFocusEnabled(true)
                .setRequestedFps(2.0f)
                .build();


        mCameraView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {

                    if (ActivityCompat.checkSelfPermission(getApplicationContext(),
                            Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.CAMERA},
                                requestPermissionID);
                        return;
                    }
                    mCameraSource.start(mCameraView.getHolder());
                    isCameraActive = true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                mCameraSource.stop();
            }
        });

        textRecognizer.setProcessor(new Detector.Processor<TextBlock>() {
            @Override
            public void release() {
            }

            @Override
            public void receiveDetections(Detector.Detections<TextBlock> detections) {
                final SparseArray<TextBlock> words = detections.getDetectedItems();
                final int wordCount = words.size();
                if (wordCount == 0 )
                    return;

                mTextView.post(new Runnable() {
                    @Override
                    public void run() {
                        StringBuilder stringBuilder = new StringBuilder();

                        for(int i=0; i<wordCount; i++){
                            TextBlock item = words.valueAt(i);
                            stringBuilder.append(item.getValue());
                            stringBuilder.append("\n");
                        }
                        mTextView.setText(stringBuilder.toString());
                    }
                });
            }
        });

    }

    public void cameraSwitchButton(View view) {
        if (isCameraActive){
            mCameraSource.stop();
            isCameraActive = false;
            mTextView.setEnabled(true);
            return;
        }
        try {
            mCameraSource.start(mCameraView.getHolder());
            isCameraActive = true;
            mTextView.setEnabled(false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void flashButton(View view) {
        String morseString = stringToMorse(mTextView.getText().toString());
        executeMorseCode(morseString);
    }

    private String stringToMorse(String text) {

        // TODO map these letters
        char[] letters = { ' ', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0' };
        String[] morseLetters = { " ", "._", "_...", "_._.", "_..", ".", ".._.", "__.", "....", "..", ".___", "_._", "._..",  "__", "_.", "___", ".__.", "__._", "._.", "...", "_", ".._", "..._", ".__", "_.._", "_.__", "__..", ".____", "..___", "...__", "...._", ".... .", "_....", "__...", "___..", "____.", "____ _"};
        String lowerCaseText = text.toLowerCase();
        String morseText = "";
        for (int i = 0; i < lowerCaseText.length(); i++) {
            for (short j = 0; j < 37; j++) {
                if (lowerCaseText.charAt(i) == letters[j]) {
                    morseText += morseLetters[j];
                    morseText += " ";
                    break;
                }
            }
        }
        return morseText;
    }

    private void executeMorseCode(String text) {
        final int milliSecMultiplier = 100;
        final int dot = milliSecMultiplier;
        final int dash = 3 * milliSecMultiplier;
        final int betweenDotDash = milliSecMultiplier;
        final int betweenCharacters = 3 * milliSecMultiplier;
        final int betweenWords = 7 * milliSecMultiplier;


        int i;
        for (i = 0; i < text.length()-1; i++) { // TODO clean this
            char c = text.charAt(i);
            if (c == '.') {
                flashLightSwitch(true, dot);
                if (text.charAt(i+1) == '_'){
                    flashLightSwitch(false, betweenDotDash);
                } else if (text.charAt(i+1) == '.'){
                    flashLightSwitch(false, betweenCharacters);
                }
            } else if (c == '_') {
                flashLightSwitch(true, dash);
                if (text.charAt(i+1) == '.'){
                    flashLightSwitch(false, betweenDotDash);
                } else if (text.charAt(i+1) == '_'){
                    flashLightSwitch(false, betweenCharacters);
                }
            } else if (c == ' ') {
                if (text.charAt(i+1) == ' '){
                    flashLightSwitch(false, betweenWords);
                }
            }
        }
        if (text.charAt(i) == '.') {
            flashLightSwitch(true, dot);
        } else if (text.charAt(i) == '_') {
            flashLightSwitch(true, dash);
        }
        flashLightSwitch(false, 0);
    }
}
