package com.yolka.app.ui.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.yolka.app.databinding.ItemAppointmentCardBinding;
import com.yolka.app.models.Pet.AppointmentRecord;
import com.yolka.app.utils.DateUtils;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AppointmentAdapter extends RecyclerView.Adapter<AppointmentAdapter.AppointmentViewHolder> {

    private final List<AppointmentRecord> appointments = new ArrayList<>();

    public void submitList(List<AppointmentRecord> list) {
        appointments.clear();
        if (list != null) {
            for (AppointmentRecord a : list) {
                if (!a.isDeleted()) appointments.add(a);
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AppointmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAppointmentCardBinding binding = ItemAppointmentCardBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new AppointmentViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull AppointmentViewHolder holder, int position) {
        holder.bind(appointments.get(position));
    }

    @Override
    public int getItemCount() {
        return appointments.size();
    }

    public static class AppointmentViewHolder extends RecyclerView.ViewHolder {
        private final ItemAppointmentCardBinding binding;

        public AppointmentViewHolder(ItemAppointmentCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(AppointmentRecord record) {
            binding.tvAppointmentReason.setText(record.getReason());
            String clinicLine = record.getClinicName();
            if (!record.getVeterinarianName().isBlank()) {
                clinicLine += " • " + record.getVeterinarianName();
            }
            binding.tvAppointmentClinic.setText(clinicLine);

            String dt = record.getDateTime();
            String monthStr = "OCT";
            String dayStr = "15";
            String timeStr = "10:00 AM";

            if (dt != null && dt.length() >= 10) {
                try {
                    LocalDate date = LocalDate.parse(dt.substring(0, 10));
                    monthStr = date.getMonth().getDisplayName(TextStyle.SHORT, new Locale("es", "MX")).toUpperCase(Locale.ROOT).replace(".", "");
                    dayStr = String.format(Locale.US, "%02d", date.getDayOfMonth());
                } catch (Exception ignored) {
                }
                if (dt.contains("T") && dt.length() >= 16) {
                    timeStr = DateUtils.formatTime12h(dt.substring(11, 16));
                }
            }

            binding.tvAppointmentMonth.setText(monthStr);
            binding.tvAppointmentDay.setText(dayStr);
            binding.tvAppointmentTimePill.setText("🕒  " + timeStr);
        }
    }
}
