package com.yolka.app.ui.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.yolka.app.databinding.ItemDashboardActivityBinding;
import com.yolka.app.models.Pet.RoutineRecord;
import com.yolka.app.utils.DateUtils;

import java.util.ArrayList;
import java.util.List;

public class DashboardActivityAdapter extends RecyclerView.Adapter<DashboardActivityAdapter.ActivityViewHolder> {

    public interface OnActivityClickListener {
        void onRoutineClick(RoutineRecord routine);
    }

    private final List<RoutineRecord> items = new ArrayList<>();
    private final OnActivityClickListener listener;

    public DashboardActivityAdapter(OnActivityClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<RoutineRecord> routines) {
        items.clear();
        if (routines != null) {
            for (RoutineRecord r : routines) {
                if (!r.isDeleted() && r.isEnabled()) {
                    items.add(r);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ActivityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemDashboardActivityBinding binding = ItemDashboardActivityBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new ActivityViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ActivityViewHolder holder, int position) {
        holder.bind(items.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ActivityViewHolder extends RecyclerView.ViewHolder {
        private final ItemDashboardActivityBinding binding;

        public ActivityViewHolder(ItemDashboardActivityBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(RoutineRecord routine, OnActivityClickListener listener) {
            binding.tvActivityTitle.setText(routine.getTitle());
            binding.tvActivityTimeChip.setText(DateUtils.formatTime12h(routine.getTime()));

            String notes = !routine.getNotes().isBlank()
                    ? routine.getNotes()
                    : getCategoryReadableLabel(routine.getCategory());
            binding.tvActivitySubtitle.setText(notes);

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) listener.onRoutineClick(routine);
            });
        }

        private String getCategoryReadableLabel(String category) {
            if ("WALK".equalsIgnoreCase(category)) return "Paseo diario programado";
            if ("GROOMING_BATH".equalsIgnoreCase(category)) return "Higiene • Baño programado";
            if ("GROOMING_NAILS".equalsIgnoreCase(category)) return "Higiene • Corte de uñas";
            return "Alimentación programada";
        }
    }
}
