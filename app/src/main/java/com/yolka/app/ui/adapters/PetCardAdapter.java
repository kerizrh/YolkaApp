package com.yolka.app.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.yolka.app.R;
import com.yolka.app.databinding.ItemPetCardBinding;
import com.yolka.app.models.Pet;
import com.yolka.app.utils.DateUtils;

import java.util.ArrayList;
import java.util.List;

public class PetCardAdapter extends RecyclerView.Adapter<PetCardAdapter.PetCardViewHolder> {

    public interface OnPetCardActionListener {
        void onOpenClinicalRecord(Pet pet);
        void onEditPet(Pet pet);
    }

    private final List<Pet> pets = new ArrayList<>();
    private final OnPetCardActionListener listener;

    public PetCardAdapter(OnPetCardActionListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Pet> newPets) {
        pets.clear();
        if (newPets != null) {
            pets.addAll(newPets);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PetCardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPetCardBinding binding = ItemPetCardBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new PetCardViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull PetCardViewHolder holder, int position) {
        holder.bind(pets.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return pets.size();
    }

    public static class PetCardViewHolder extends RecyclerView.ViewHolder {
        private final ItemPetCardBinding binding;

        public PetCardViewHolder(ItemPetCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Pet pet, OnPetCardActionListener listener) {
            binding.tvPetCardName.setText(pet.getName());
            boolean isFemale = "FEMALE".equalsIgnoreCase(pet.getGender());
            binding.tvPetCardGenderChip.setText(isFemale ? "♀ Hembra" : "♂ Macho");

            String breed = !pet.getBreed().isBlank()
                    ? pet.getBreed()
                    : ("CAT".equalsIgnoreCase(pet.getSpecies()) ? "Gato" : "Perro");
            String age = DateUtils.calculateReadableAge(pet.getBirthDate(), pet.getAgeYears());
            binding.tvPetCardDetails.setText(breed + " • " + age + " • " + pet.getWeight() + " kg");
            binding.tvPetCardSyncBadge.setVisibility(pet.isPendingSync() ? View.VISIBLE : View.GONE);

            if (pet.getPhotoUrl() != null && !pet.getPhotoUrl().isBlank()) {
                Glide.with(binding.ivPetCardPhoto.getContext())
                        .load(pet.getPhotoUrl())
                        .placeholder(R.drawable.ic_yolka_paw)
                        .error(R.drawable.ic_yolka_paw)
                        .centerCrop()
                        .into(binding.ivPetCardPhoto);
            } else {
                binding.ivPetCardPhoto.setImageResource(R.drawable.ic_yolka_paw);
            }

            View.OnClickListener openRecordListener = v -> {
                if (listener != null) listener.onOpenClinicalRecord(pet);
            };
            binding.getRoot().setOnClickListener(openRecordListener);
            binding.ivPetCardPhoto.setOnClickListener(openRecordListener);
            binding.btnEditPetCard.setOnClickListener(v -> {
                if (listener != null) listener.onEditPet(pet);
            });
        }
    }
}
