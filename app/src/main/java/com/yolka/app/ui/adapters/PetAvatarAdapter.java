package com.yolka.app.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.yolka.app.R;
import com.yolka.app.databinding.ItemPetAvatarBinding;
import com.yolka.app.models.Pet;

import java.util.ArrayList;
import java.util.List;

public class PetAvatarAdapter extends RecyclerView.Adapter<PetAvatarAdapter.PetAvatarViewHolder> {

    public interface OnPetSelectedListener {
        void onPetSelected(Pet pet);
        void onPetLongClick(Pet pet);
    }

    private final List<Pet> pets = new ArrayList<>();
    private String selectedPetId;
    private final OnPetSelectedListener listener;

    public PetAvatarAdapter(OnPetSelectedListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Pet> newPets, String selectedId) {
        pets.clear();
        if (newPets != null) {
            pets.addAll(newPets);
        }
        this.selectedPetId = selectedId;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PetAvatarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPetAvatarBinding binding = ItemPetAvatarBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new PetAvatarViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull PetAvatarViewHolder holder, int position) {
        Pet pet = pets.get(position);
        boolean isSelected = pet.getId() != null && pet.getId().equals(selectedPetId);
        holder.bind(pet, isSelected, listener);
    }

    @Override
    public int getItemCount() {
        return pets.size();
    }

    public static class PetAvatarViewHolder extends RecyclerView.ViewHolder {
        private final ItemPetAvatarBinding binding;

        public PetAvatarViewHolder(ItemPetAvatarBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Pet pet, boolean isSelected, OnPetSelectedListener listener) {
            binding.tvPetAvatarName.setText(pet.getName());
            String subtitle = !pet.getBreed().isBlank()
                    ? pet.getBreed()
                    : ("CAT".equalsIgnoreCase(pet.getSpecies()) ? "Felino" : "Canino");
            binding.tvPetAvatarBreed.setText(subtitle);
            binding.tvPetSelectedBadge.setVisibility(isSelected ? View.VISIBLE : View.GONE);
            binding.cardPetAvatarCircle.setStrokeWidth(isSelected ? 5 : 2);

            if (pet.getPhotoUrl() != null && !pet.getPhotoUrl().isBlank()) {
                Glide.with(binding.ivPetAvatarPhoto.getContext())
                        .load(pet.getPhotoUrl())
                        .placeholder(R.drawable.ic_yolka_paw)
                        .error(R.drawable.ic_yolka_paw)
                        .circleCrop()
                        .into(binding.ivPetAvatarPhoto);
            } else {
                binding.ivPetAvatarPhoto.setImageResource(R.drawable.ic_yolka_paw);
            }

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) listener.onPetSelected(pet);
            });
            binding.getRoot().setOnLongClickListener(v -> {
                if (listener != null) listener.onPetLongClick(pet);
                return true;
            });
        }
    }
}
