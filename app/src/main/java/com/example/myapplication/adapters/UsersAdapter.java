package com.example.myapplication.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.databinding.ItemContainerUserBinding;
import com.example.myapplication.listeners.UserListener;
import com.example.myapplication.models.Usuario;

import java.util.List;

public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.UserViewHolder>  {

    private List<Usuario> usuarioList;

    private final UserListener userListener;
    private Context context;
    private View.OnClickListener listener;

    public UsersAdapter(List<Usuario> usuarioList, UserListener userListener) {
        this.usuarioList = usuarioList;
        this.userListener = userListener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemContainerUserBinding itemContainerUserBinding = ItemContainerUserBinding.inflate(LayoutInflater.from(parent.getContext()),parent,false);
        return new UserViewHolder(itemContainerUserBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        holder.setUserData(usuarioList.get(position));
    }

    @Override
    public int getItemCount() {
        return usuarioList.size();
    }

    class UserViewHolder extends RecyclerView.ViewHolder {
        ItemContainerUserBinding binding;
        UserViewHolder(ItemContainerUserBinding itemContainerUserBinding) {
            super(itemContainerUserBinding.getRoot());
            binding = itemContainerUserBinding;
        }
        void setUserData(Usuario user){
            binding.TextName.setText(user.getNombre());
            binding.imageprofile.setImageBitmap(getUserImage(user.getFotodeperfil()));
            binding.mostrarBTN.setOnClickListener(v -> userListener.onUserClicked(user));
        }
    }

    private Bitmap getUserImage(String image){
        byte[] decodedString = android.util.Base64.decode(image, android.util.Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
    }
}
