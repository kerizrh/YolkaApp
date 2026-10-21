package com.yolka.app.ui.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.yolka.app.databinding.ItemVetClinicCardBinding;
import com.yolka.app.models.VetClinic;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class VetClinicAdapter extends RecyclerView.Adapter<VetClinicAdapter.VetViewHolder> {

    public interface OnVetActionListener {
        void onFocusOnMap(VetClinic clinic);
        void onOpenDirections(VetClinic clinic);
    }

    private final List<VetClinic> clinics = new ArrayList<>();
    private final OnVetActionListener listener;

    public VetClinicAdapter(OnVetActionListener listener) {
        this.listener = listener;
    }

    public void submitList(List<VetClinic> list) {
        clinics.clear();
        if (list != null) {
            clinics.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemVetClinicCardBinding binding = ItemVetClinicCardBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new VetViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull VetViewHolder holder, int position) {
        holder.bind(clinics.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return clinics.size();
    }

    public static class VetViewHolder extends RecyclerView.ViewHolder {
        private final ItemVetClinicCardBinding binding;

        public VetViewHolder(ItemVetClinicCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(VetClinic clinic, OnVetActionListener listener) {
            binding.tvVetClinicName.setText(clinic.getName());

            double dist = clinic.getDistanceMeters();
            if (dist >= 0) {
                if (dist < 1000) {
                    binding.tvVetClinicDistance.setText(String.format(Locale.US, "• A %.0f m", dist));
                } else {
                    binding.tvVetClinicDistance.setText(String.format(Locale.US, "• A %.1f km", dist / 1000.0));
                }
            } else {
                binding.tvVetClinicDistance.setText("• En tu zona");
            }

            String info = clinic.getAddress();
            if (!clinic.getOpeningHours().isBlank()) {
                info += " • " + clinic.getOpeningHours();
            }
            if (!clinic.getPhone().isBlank()) {
                info += " • Tel: " + clinic.getPhone();
            }
            binding.tvVetClinicAddress.setText(info);

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) listener.onFocusOnMap(clinic);
            });
            binding.btnVetDirections.setOnClickListener(v -> {
                if (listener != null) listener.onOpenDirections(clinic);
            });
        }
    }
}
