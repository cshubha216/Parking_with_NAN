package net.mobilewebprint.nan;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;


public class park_detection extends AppCompatActivity {
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private CameraDevice cameraDevice;
    private TextureView img_view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.parking_detection_layout);

        // Initialize the TextureView
        img_view = findViewById(R.id.camtextview);

        // Request camera permission and set up the camera
        if (checkCameraPermission()) {
            openCamera();
        }
    }

    private boolean checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                // Handle permission denied
            }
        }
    }

    private void openCamera() {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            String cameraId = cameraManager.getCameraIdList()[0]; // Use the first camera, change as needed

            // Open the camera
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            cameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    cameraDevice = camera;
                    setupCameraPreview();
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    onPause();
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    onPause();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private int captureTextureViewAsJpeg() {
        TextureView textureView = findViewById(R.id.camtextview); // Replace with your TextureView ID
        int parking_bits = 0;

        // Check if the TextureView is available
        if (textureView.isAvailable()) {
            Bitmap bitmap = textureView.getBitmap();

            // Save the bitmap as a JPEG file
            File file = new File(getExternalFilesDir(null), "parking_view.jpg"); // Define the file path and name
            FileOutputStream outputStream = null;

            try {
                outputStream = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream); // 100 is the highest quality
                outputStream.flush();
                outputStream.close();
                // Notify the MediaScanner to add the image to the gallery (optional)
                MediaScannerConnection.scanFile(this, new String[]{file.getAbsolutePath()}, null, null);
                Toast.makeText(this, "TextureView captured and saved as JPEG", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (outputStream != null) {
                        outputStream.close();
                    }

                    if (! Python.isStarted()) {
                        Python.start(new AndroidPlatform(this));
                    }


                    Python py = Python.getInstance();
                    final PyObject pyobj = py.getModule("main");
                    PyObject obj = pyobj.callAttr("main");
                    parking_bits = obj.toInt();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return parking_bits;
    }

    private void setupCameraPreview() {
        SurfaceTexture surfaceTexture = img_view.getSurfaceTexture();
        assert surfaceTexture != null;

        // Set the size of the preview to match the size of the TextureView
        surfaceTexture.setDefaultBufferSize(img_view.getWidth(), img_view.getHeight());

        Surface surface = new Surface(surfaceTexture);

        try {
            final CaptureRequest.Builder captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);

            // Create a CameraCaptureSession for the preview
            cameraDevice.createCaptureSession(Collections.singletonList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    if (cameraDevice == null) {
                        return;
                    }

                    try {
                        // Start the camera preview
                        CaptureRequest captureRequest = captureRequestBuilder.build();
                        session.setRepeatingRequest(captureRequest, null, null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    // Handle configuration failure
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
    }

}
