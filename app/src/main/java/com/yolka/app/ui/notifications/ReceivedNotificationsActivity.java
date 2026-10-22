package com.yolka.app.ui.notifications;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.yolka.app.databinding.ActivityReceivedNotificationsBinding;
import com.yolka.app.models.ReceivedNotification;
import com.yolka.app.ui.adapters.ReceivedNotificationAdapter;
import com.yolka.app.utils.Store;

import java.util.ArrayList;
import java.util.List;

public class ReceivedNotificationsActivity extends AppCompatActivity {

    private ActivityReceivedNotificationsBinding binding;
    private ReceivedNotificationAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityReceivedNotificationsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());

        adapter = new ReceivedNotificationAdapter(item -> {
            Store.getInstance().markNotificationAsRead(item.getId());
            loadNotifications();
        });

        binding.rvReceivedNotifications.setLayoutManager(new LinearLayoutManager(this));
        binding.rvReceivedNotifications.setAdapter(adapter);

        binding.btnMarkAllRead.setOnClickListener(v -> {
            Store.getInstance().markAllNotificationsAsRead();
            loadNotifications();
            Toast.makeText(this, "Todas las notificaciones marcadas como leídas", Toast.LENGTH_SHORT).show();
        });

        binding.btnClearHistory.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Limpiar historial")
                    .setMessage("¿Deseas vaciar el registro de notificaciones recibidas?")
                    .setPositiveButton("Vaciar", (d, w) -> {
                        Store.getInstance().clearReceivedNotifications();
                        loadNotifications();
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });

        binding.btnGoToPushSettings.setOnClickListener(v -> {
            startActivity(new Intent(ReceivedNotificationsActivity.this, NotificationsActivity.class));
        });

        loadNotifications();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNotifications();
    }

    private void loadNotifications() {
        List<ReceivedNotification> items = Store.getInstance().getReceivedNotifications();

        if (items.isEmpty()) {
            binding.layoutEmptyNotifications.setVisibility(View.VISIBLE);
            binding.rvReceivedNotifications.setVisibility(View.GONE);
            binding.btnMarkAllRead.setVisibility(View.GONE);
            binding.btnClearHistory.setVisibility(View.GONE);
            binding.tvNotificationCountSubtitle.setText("0 notificaciones");
        } else {
            binding.layoutEmptyNotifications.setVisibility(View.GONE);
            binding.rvReceivedNotifications.setVisibility(View.VISIBLE);
            binding.btnMarkAllRead.setVisibility(View.VISIBLE);
            binding.btnClearHistory.setVisibility(View.VISIBLE);

            int unread = 0;
            for (ReceivedNotification n : items) {
                if (!n.isRead()) unread++;
            }
            if (unread > 0) {
                binding.tvNotificationCountSubtitle.setText(items.size() + " notificaciones (" + unread + " sin leer)");
            } else {
                binding.tvNotificationCountSubtitle.setText(items.size() + " notificaciones recibidas");
            }

            adapter.submitList(new ArrayList<>(items));
        }
    }
}
