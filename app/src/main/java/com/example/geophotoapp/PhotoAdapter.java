package com.example.geophotoapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.io.File;
import androidx.core.content.FileProvider;
public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder>{
    private List<Photo> photos;

    public PhotoAdapter(List<Photo> photos) {
        this.photos = photos;
    }

    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_photo, parent, false);
        return new PhotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        Photo photo = photos.get(position);

        //cargar imagen de forma eficiente (thumbnail)
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 4;
        Bitmap bitmap = BitmapFactory.decodeFile(photo.getPath(), options);
        holder.imageView.setImageBitmap(bitmap);

        holder.imageView.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);

                // Crear URI usando FileProvider para Android 7.0+
                Uri photoUri;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    // Para Android 7.0+, usar FileProvider
                    java.io.File file = new java.io.File(photo.getPath());
                    photoUri = androidx.core.content.FileProvider.getUriForFile(
                            v.getContext(),
                            v.getContext().getPackageName() + ".fileprovider",
                            file
                    );
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } else {
                    // Para versiones anteriores, usar URI directo
                    photoUri = Uri.parse("file://" + photo.getPath());
                }

                intent.setDataAndType(photoUri, "image/*");
                v.getContext().startActivity(intent);

            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(v.getContext(),
                        "Error al abrir la imagen: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount(){
        return photos.size();
    }

    public void updatePhotos(List<Photo> newPhotos) {
        this.photos = newPhotos;
        notifyDataSetChanged();
    }

    static class PhotoViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        PhotoViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);

        }
    }


}
