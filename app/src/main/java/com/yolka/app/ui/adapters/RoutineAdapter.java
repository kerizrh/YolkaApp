package com.yolka.app.ui.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.yolka.app.databinding.ItemRoutineCardBinding;
import com.yolka.app.models.Pet.RoutineRecord;
import com.yolka.app.utils.DateUtils;

import java.util.ArrayList;
import java.util.List;

public class RoutineAdapter extends RecyclerView.Adapter<RoutineAdapter.RoutineViewHolder> {

    private final List<RoutineRecord> routines = new ArrayList<>();

    public void submitList(List<RoutineRecord> list, String categoryFilter) {
        routines.clear();
        if (list != null) {
            for (RoutineRecord r : list) {
                if (r.isDeleted()) continue;
                if (categoryFilter == null || categoryFilter.isEmpty() || "ALL".equalsIgnoreCase(categoryFilter)) {
                    routines.add(r);
                } else if ("GROOMING".equalsIgnoreCase(categoryFilter)) {
                    if (r.getCategory().startsWith("GROOMING")) {
                        routines.add(r);
                    }
                } else if (categoryFilter.equalsIgnoreCase(r.getCategory())) {
                    routines.add(r);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RoutineViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemRoutineCardBinding binding = ItemRoutineCardBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new RoutineViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull RoutineViewHolder holder, int position) {
        holder.bind(routines.get(position));
    }

    @Override
    public int getItemCount() {
        return routines.size();
    }

    public static class RoutineViewHolder extends RecyclerView.ViewHolder {
        private final ItemRoutineCardBinding binding;

        public RoutineViewHolder(ItemRoutineCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(RoutineRecord routine) {
            binding.tvRoutineCardTitle.setText(routine.getTitle());
            binding.tvRoutineCardTime.setText(DateUtils.formatTime12h(routine.getTime()));

            String cat = routine.getCategory();
            if ("WALK".equalsIgnoreCase(cat)) {
                binding.tvRoutineCategoryIcon.setText("🐾");
            } else if (cat != null && cat.startsWith("GROOMING")) {
                binding.tvRoutineCategoryIcon.setText("🛁");
            } else {
                binding.tvRoutineCategoryIcon.setText("🍴");
            }

            String notes = !routine.getNotes().isBlank() ? routine.getNotes() : "Rutina activa";
            binding.tvRoutineCardNotes.setText(notes);

            List<String> days = routine.getDaysOfWeek();
            String freqLabel = "Diario";
            if (days != null && !days.isEmpty()) {
                if (days.size() == 5 && !days.contains("SAT") && !days.contains("SUN")) {
                    freqLabel = "Lunes a Viernes";
                } else if (days.size() == 2 && days.contains("SAT") && days.contains("SUN")) {
                    freqLabel = "Fines de semana";
                } else if (days.size() < 7) {
                    freqLabel = String.join(", ", days);
                }
            }
            binding.tvRoutineCardDays.setText("Frecuencia: " + freqLabel);
        }
    }
}
