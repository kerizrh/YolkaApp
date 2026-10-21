package com.yolka.app.ui.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.yolka.app.R;
import com.yolka.app.databinding.ItemVaccineCardBinding;
import com.yolka.app.models.Pet.VaccineRecord;
import com.yolka.app.utils.DateUtils;

import java.util.ArrayList;
import java.util.List;

public class VaccineAdapter extends RecyclerView.Adapter<VaccineAdapter.VaccineViewHolder> {

    private final List<VaccineRecord> vaccines = new ArrayList<>();

    public void submitList(List<VaccineRecord> list) {
        vaccines.clear();
        if (list != null) {
            for (VaccineRecord v : list) {
                if (!v.isDeleted()) vaccines.add(v);
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VaccineViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemVaccineCardBinding binding = ItemVaccineCardBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new VaccineViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull VaccineViewHolder holder, int position) {
        holder.bind(vaccines.get(position));
    }

    @Override
    public int getItemCount() {
        return vaccines.size();
    }

    public static class VaccineViewHolder extends RecyclerView.ViewHolder {
        private final ItemVaccineCardBinding binding;

        public VaccineViewHolder(ItemVaccineCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(VaccineRecord vaccine) {
            binding.tvVaccineName.setText(vaccine.getVaccineName());
            String notes = !vaccine.getNotes().isBlank() ? vaccine.getNotes() : "Esquema preventivo de vacunación";
            binding.tvVaccineNotes.setText(notes);

            boolean isApplied = !vaccine.getApplicationDate().isBlank();
            binding.tvVaccineAppliedDate.setText(
                    isApplied ? DateUtils.formatIsoDate(vaccine.getApplicationDate()) : "Por aplicar"
            );
            binding.tvVaccineNextDueDate.setText(
                    !vaccine.getNextDueDate().isBlank() ? DateUtils.formatIsoDate(vaccine.getNextDueDate()) : "Por agendar"
            );

            binding.tvVaccineStatusChip.setText(isApplied ? "Aplicada" : "Sugerida");
            binding.tvVaccineStatusChip.setTextColor(ContextCompat.getColor(
                    binding.getRoot().getContext(),
                    isApplied ? R.color.badge_health_text : R.color.badge_nutrition_text
            ));
        }
    }
}
