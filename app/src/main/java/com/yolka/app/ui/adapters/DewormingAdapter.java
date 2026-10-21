package com.yolka.app.ui.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.yolka.app.databinding.ItemDewormingCardBinding;
import com.yolka.app.models.Pet.DewormingRecord;
import com.yolka.app.utils.DateUtils;

import java.util.ArrayList;
import java.util.List;

public class DewormingAdapter extends RecyclerView.Adapter<DewormingAdapter.DewormingViewHolder> {

    private final List<DewormingRecord> dewormings = new ArrayList<>();

    public void submitList(List<DewormingRecord> list) {
        dewormings.clear();
        if (list != null) {
            for (DewormingRecord d : list) {
                if (!d.isDeleted()) dewormings.add(d);
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DewormingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemDewormingCardBinding binding = ItemDewormingCardBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new DewormingViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull DewormingViewHolder holder, int position) {
        holder.bind(dewormings.get(position));
    }

    @Override
    public int getItemCount() {
        return dewormings.size();
    }

    public static class DewormingViewHolder extends RecyclerView.ViewHolder {
        private final ItemDewormingCardBinding binding;

        public DewormingViewHolder(ItemDewormingCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(DewormingRecord record) {
            boolean isExternal = "EXTERNAL".equalsIgnoreCase(record.getType());
            binding.tvDewormingTypeBadge.setText(isExternal ? "Externa" : "Interna");
            binding.tvDewormingProduct.setText(record.getProductName());
            binding.tvDewormingNotes.setText(
                    !record.getNotes().isBlank() ? record.getNotes() : (isExternal ? "Cada 60 días" : "Cada 30 días")
            );
            binding.tvDewormingLastDate.setText("Última: " + DateUtils.formatIsoDate(record.getApplicationDate()));
            binding.tvDewormingNextDate.setText("Siguiente: " + DateUtils.formatIsoDate(record.getNextDueDate()));
        }
    }
}
