package com.yolka.app.ui.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.yolka.app.databinding.ItemNotificationRowBinding;
import com.yolka.app.models.NotificationItem;

import java.util.ArrayList;
import java.util.List;

public class NotificationItemAdapter extends RecyclerView.Adapter<NotificationItemAdapter.NotificationViewHolder> {

    private final List<NotificationItem> items = new ArrayList<>();

    public void submitList(List<NotificationItem> list) {
        items.clear();
        if (list != null) {
            items.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemNotificationRowBinding binding = ItemNotificationRowBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new NotificationViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class NotificationViewHolder extends RecyclerView.ViewHolder {
        private final ItemNotificationRowBinding binding;

        public NotificationViewHolder(ItemNotificationRowBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(NotificationItem item) {
            binding.tvNotifTitle.setText(item.getTitle());
            binding.tvNotifBody.setText(item.getBody());
            String meta = item.getCategory()
                    + (item.getScheduledTime().isEmpty() ? "" : " • " + item.getScheduledTime())
                    + (item.getPetName().isEmpty() ? "" : " • " + item.getPetName());
            binding.tvNotifMeta.setText(meta);
        }
    }
}
