package com.example.geophotoapp;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.util.Log;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int CAMERA_ACTIVITY_REQUEST = 200;

    private RecyclerView recyclerView;
    private PhotoAdapter adapter;
    private List<Photo> photoList;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recyclerViewPhotos);
        FloatingActionButton btnTakePhoto = findViewById(R.id.btnTakePhoto);

        //configurar recyclerview con gridlayout de 3 colm
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        photoList = new ArrayList<>();
        adapter = new PhotoAdapter(photoList);
        recyclerView.setAdapter(adapter);

        //btn para tomar la foto
        btnTakePhoto.setOnClickListener(v -> openCameraActivity());

        //solicitar perimisos
        checkAndRequestPermissions();

    }

    private void checkAndRequestPermissions(){
        List<String> permissionsNeeded = new ArrayList<>();

        //permisos necesarios para la app segun la version
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            //adroid 13+
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED){
                permissionsNeeded.add(Manifest.permission.READ_MEDIA_IMAGES);
            }
        }else {
            //android 12 y anteriores
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            permissionsNeeded.add(Manifest.permission.CAMERA);
        }

        if(!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionsNeeded.toArray(new String[0]),
                    PERMISSION_REQUEST_CODE);
        }else{
            loadPhotos();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == PERMISSION_REQUEST_CODE){
            boolean allGranted = true;
            for(int result : grantResults){
                if(result != PackageManager.PERMISSION_GRANTED){
                    allGranted = false;
                    break;
                }
            }
            if(allGranted){
                loadPhotos();
            }else{
                Toast.makeText(this, "Permisos necesarios no concedidos", Toast.LENGTH_LONG).show();
            }
        }
    }
    private void loadPhotos() {
        photoList.clear();

        Log.d("MainActivity", "Cargando fotos...");


        //query al media store para obtener las fotos
        Uri collection;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            collection= MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
        }else{
            collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        }

        String[] projection = new String[]{
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.DATE_ADDED
        };

        String sortOrder = MediaStore.Images.Media.DATE_ADDED + " DESC";

        try(Cursor cursor = getContentResolver().query(collection, projection, null, null, sortOrder)){
            if(cursor != null){
                Log.d("MainActivity", "Fotos encontradas: " + cursor.getCount());
                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
                int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);

                while(cursor.moveToNext()) {
                    long id = cursor.getLong(idColumn);
                    String data = cursor.getString(dataColumn);
                    photoList.add(new Photo(data, id));
                }
            }

            TextView tvNoPhotos = findViewById(R.id.tvNoPhotos);
            if (photoList.isEmpty()) {
                tvNoPhotos.setVisibility(View.VISIBLE);
            } else {
                tvNoPhotos.setVisibility(View.GONE);
            }
        }catch(Exception e){
            e.printStackTrace();
            Toast.makeText(this, "Error al cargar las fotos" + e.getMessage(), Toast.LENGTH_SHORT).show();;
        }

        adapter.updatePhotos(photoList);
    }

    private void openCameraActivity(){
        Intent intent = new Intent(this, CameraActivity.class);
        startActivityForResult(intent, CAMERA_ACTIVITY_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == CAMERA_ACTIVITY_REQUEST && resultCode == RESULT_OK){
            //recargar las fotos despues de tomar una nueva
            loadPhotos();
            Toast.makeText(this, "Foto tomada con éxito", Toast.LENGTH_SHORT).show();

        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPhotos();
    }


}