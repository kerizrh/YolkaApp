package com.yolka.app.ui.adapters;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.yolka.app.databinding.ItemReceivedNotificationBinding;
import com.yolka.app.models.ReceivedNotification;

public class ReceivedNotificationAdapter extends ListAdapter<ReceivedNotification, ReceivedNotificationAdapter.ViewHolder> {

    public interface OnNotificationClickListener {
        void onNotificationClick(ReceivedNotification item);
    }

    private final OnNotificationClickListener clickListener;

    public ReceivedNotificationAdapter(OnNotificationClickListener clickListener) {
        super(new DiffUtil.ItemCallback<ReceivedNotification>() {
            @Override
            public boolean areItemsTheSame(@NonNull ReceivedNotification oldItem, @NonNull ReceivedNotification newItem) {
                return oldItem.getId().equals(newItem.getId());
            }

            @Override
            public boolean areContentsTheSame(@NonNull ReceivedNotification oldItem, @NonNull ReceivedNotification newItem) {
                return oldItem.isRead() == newItem.isRead()
                        && oldItem.getTitle().equals(newItem.getTitle())
                        && oldItem.getBody().equals(newItem.getBody());
            }
        });
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemReceivedNotificationBinding binding = ItemReceivedNotificationBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemReceivedNotificationBinding binding;

        public ViewHolder(@NonNull ItemReceivedNotificationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(ReceivedNotification item) {
            binding.tvNotificationTitle.setText(item.getTitle());
            binding.tvNotificationBody.setText(item.getBody());

            String category = item.getCategory() != null ? item.getCategory().toUpperCase() : "GENERAL";
            String emoji = "🔔";
            String catName = "Alerta";

            switch (category) {
                case "VACCINE":
                    emoji = "💉";
                    catName = "Vacunación";
                    break;
                case "DEWORMING":
                    emoji = "💊";
                    catName = "Desparasitación";
                    break;
                case "APPOINTMENT":
                    emoji = "📅";
                    catName = "Cita Médica";
                    break;
                case "GROOMING":
                    emoji = "🛁";
                    catName = "Higiene";
                    break;
                case "FEEDING":
                    emoji = "🍖";
                    catName = "Alimentación";
                    break;
                case "WALK":
                    emoji = "🦮";
                    catName = "Paseo";
                    break;
                case "REMINDER":
                    emoji = "⏰";
                    catName = "Recordatorio";
                    break;
                default:
                    emoji = "🐾";
                    catName = "Yolka";
                    break;
            }

            binding.tvNotificationEmoji.setText(emoji);

            if (item.getPetName() != null && !item.getPetName().isBlank()) {
                binding.tvNotificationCategoryBadge.setText(catName + " • " + item.getPetName());
            } else {
                binding.tvNotificationCategoryBadge.setText(catName);
            }

            long now = System.currentTimeMillis();
            CharSequence timeSpan = DateUtils.getRelativeTimeSpanString(
                    item.getTimestamp(),
                    now,
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE
            );
            binding.tvNotificationTime.setText(timeSpan);

            binding.viewUnreadDot.setVisibility(item.isRead() ? View.GONE : View.VISIBLE);
            binding.getRoot().setAlpha(item.isRead() ? 0.78f : 1.0f);

            binding.getRoot().setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onNotificationClick(item);
                }
            });
        }
    }
}
