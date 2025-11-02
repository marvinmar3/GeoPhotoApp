package com.example.geophotoapp;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.exifinterface.media.ExifInterface;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.OnTokenCanceledListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CameraActivity extends AppCompatActivity {
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int PERMISSION_REQUEST_CODE=101;

    private TextView tvStatus;
    private File photoFile;
    private FusedLocationProviderClient fusedLocationClient;
    private Location currentLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        tvStatus = findViewById(R.id.tvStatus);
        Button btnCapture = findViewById(R.id.btnCapture);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        btnCapture.setOnClickListener(v -> {
            if (checkPermissions()) {
                getCurrentLocationAndTakePhoto();
            } else {
                requestPermissions();
            }
        });

        tvStatus.setText("Listo para tomar foto");
    }

    private boolean checkPermissions() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{
                        Manifest.permission.CAMERA,
                        Manifest.permission.ACCESS_FINE_LOCATION
                },
                PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (checkPermissions()) {
                Toast.makeText(this, "Permisos concedidos", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permisos denegados", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void getCurrentLocationAndTakePhoto() {
        tvStatus.setText("Obteniendo ubicación GPS...");

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions();
            return;
        }

        // Obtener ubicación actual con alta prioridad
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, new CancellationToken() {
            @NonNull
            @Override
            public CancellationToken onCanceledRequested(@NonNull OnTokenCanceledListener onTokenCanceledListener) {
                return this;
            }

            @Override
            public boolean isCancellationRequested() {
                return false;
            }
        }).addOnSuccessListener(location -> {
            if (location != null) {
                currentLocation = location;
                tvStatus.setText(String.format(Locale.getDefault(),
                        "Ubicación obtenida: %.6f, %.6f\nAbriendo cámara...",
                        location.getLatitude(), location.getLongitude()));
                launchCamera();
            } else {
                tvStatus.setText("No se pudo obtener ubicación, tomando foto sin GPS...");
                currentLocation = null;
                launchCamera();
            }
        }).addOnFailureListener(e -> {
            tvStatus.setText("Error al obtener ubicación: " + e.getMessage());
            currentLocation = null;
            launchCamera();
        });
    }

    private void launchCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                tvStatus.setText("Error al crear archivo: " + ex.getMessage());
                return;
            }

            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        getPackageName() + ".fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        } else {
            Toast.makeText(this, "No hay aplicación de cámara disponible", Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";

        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            tvStatus.setText("Foto capturada, procesando...");
            processImage();
        } else {
            tvStatus.setText("Captura cancelada");
        }
    }

    private void processImage() {
        if (photoFile == null || !photoFile.exists()) {
            Toast.makeText(this, "Error: archivo de foto no encontrado", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Agregar metadatos EXIF
            ExifInterface exif = new ExifInterface(photoFile.getAbsolutePath());

            // Agregar fecha y hora
            String datetime = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault())
                    .format(new Date());
            exif.setAttribute(ExifInterface.TAG_DATETIME, datetime);
            exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, datetime);
            exif.setAttribute(ExifInterface.TAG_DATETIME_DIGITIZED, datetime);

            // Agregar geolocalización si está disponible
            if (currentLocation != null) {
                exif.setLatLong(currentLocation.getLatitude(), currentLocation.getLongitude());

                // Agregar altitud si está disponible
                if (currentLocation.hasAltitude()) {
                    exif.setAltitude(currentLocation.getAltitude());
                }

                tvStatus.setText(String.format(Locale.getDefault(),
                        "GPS agregado: %.6f, %.6f",
                        currentLocation.getLatitude(), currentLocation.getLongitude()));
            } else {
                tvStatus.setText("Foto guardada sin GPS");
            }

            // Guardar cambios en el archivo
            exif.saveAttributes();

            // Copiar archivo a la galería del dispositivo
            saveToGallery();

        } catch (IOException e) {
            e.printStackTrace();
            tvStatus.setText("Error al procesar imagen: " + e.getMessage());
            Toast.makeText(this, "Error al agregar metadatos", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveToGallery() {
        try {
            // Crear nombre único para la imagen en la galería
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                    .format(new Date());
            String displayName = "GeoPhoto_" + timeStamp + ".jpg";

            // Para Android 10+ usar MediaStore
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, displayName);
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);

                Uri uri = getContentResolver().insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

                if (uri != null) {
                    try (OutputStream out = getContentResolver().openOutputStream(uri);
                         InputStream in = new FileInputStream(photoFile)) {

                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = in.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                        }

                        Toast.makeText(this, "Foto guardada en la galería", Toast.LENGTH_SHORT).show();

                        // Notificar que la operación fue exitosa
                        setResult(RESULT_OK);
                        finish();
                    }
                }
            } else {
                // Para versiones anteriores a Android 10
                File picturesDir = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES);
                File destFile = new File(picturesDir, displayName);

                try (InputStream in = new FileInputStream(photoFile);
                     OutputStream out = new FileOutputStream(destFile)) {

                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                }

                // Notificar al MediaStore
                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                mediaScanIntent.setData(Uri.fromFile(destFile));
                sendBroadcast(mediaScanIntent);

                Toast.makeText(this, "Foto guardada en la galería", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }

        } catch (IOException e) {
            e.printStackTrace();
            tvStatus.setText("Error al guardar en galería: " + e.getMessage());
            Toast.makeText(this, "Error al guardar en galería", Toast.LENGTH_SHORT).show();
        }
    }


}
