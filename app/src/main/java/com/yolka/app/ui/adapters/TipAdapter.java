package com.yolka.app.ui.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.yolka.app.R;
import com.yolka.app.databinding.ItemTipCardBinding;
import com.yolka.app.models.Tip;

import java.util.ArrayList;
import java.util.List;

public class TipAdapter extends RecyclerView.Adapter<TipAdapter.TipViewHolder> {

    public interface OnTipClickListener {
        void onTipClick(Tip tip);
    }

    private final List<Tip> tips = new ArrayList<>();
    private final OnTipClickListener listener;

    public TipAdapter(OnTipClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Tip> newTips) {
        tips.clear();
        if (newTips != null) {
            tips.addAll(newTips);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TipViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemTipCardBinding binding = ItemTipCardBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new TipViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull TipViewHolder holder, int position) {
        holder.bind(tips.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return tips.size();
    }

    public static class TipViewHolder extends RecyclerView.ViewHolder {
        private final ItemTipCardBinding binding;

        public TipViewHolder(ItemTipCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Tip tip, OnTipClickListener listener) {
            binding.tvTipTitle.setText(tip.getTitle());
            binding.tvTipSummary.setText(tip.getSummary());

            String cat = tip.getCategory();
            int colorRes = R.color.badge_lavender_text;
            String label = "Bienestar";
            if ("FEEDING".equalsIgnoreCase(cat)) {
                label = "Nutrición";
                colorRes = R.color.badge_nutrition_text;
            } else if ("HYGIENE".equalsIgnoreCase(cat)) {
                label = "Higiene";
                colorRes = R.color.badge_hygiene_text;
            } else if ("VACCINATION".equalsIgnoreCase(cat) || "DEWORMING".equalsIgnoreCase(cat)) {
                label = "Salud";
                colorRes = R.color.badge_health_text;
            } else if ("EXERCISE".equalsIgnoreCase(cat)) {
                label = "Paseos";
                colorRes = R.color.badge_health_text;
            } else if ("FUN_FACTS".equalsIgnoreCase(cat)) {
                label = "Curiosidades";
                colorRes = R.color.badge_fun_text;
            }

            binding.tvTipCategoryBadge.setText(label);
            binding.tvTipCategoryBadge.setTextColor(
                    ContextCompat.getColor(binding.getRoot().getContext(), colorRes)
            );

            String speciesTag = "DOG".equalsIgnoreCase(tip.getSpecies())
                    ? "• 🐶 Perros • 3 min"
                    : ("CAT".equalsIgnoreCase(tip.getSpecies()) ? "• 🐱 Gatos • 3 min" : "• 🐾 General • 3 min");
            binding.tvTipSpeciesTag.setText(speciesTag);

            if (tip.getImageUrl() != null && !tip.getImageUrl().isBlank()) {
                Glide.with(binding.ivTipThumbnail.getContext())
                        .load(tip.getImageUrl())
                        .placeholder(R.drawable.ic_yolka_paw)
                        .error(R.drawable.ic_yolka_paw)
                        .centerCrop()
                        .into(binding.ivTipThumbnail);
            } else {
                binding.ivTipThumbnail.setImageResource(R.drawable.ic_yolka_paw);
            }

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) listener.onTipClick(tip);
            });
        }
    }
}
