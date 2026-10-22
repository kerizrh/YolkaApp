package com.yolka.app.ui.medical;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.yolka.app.databinding.BottomSheetAppointmentFormBinding;
import com.yolka.app.models.ApiPayloads.AppointmentRequest;
import com.yolka.app.models.ApiPayloads.GenericResponse;
import com.yolka.app.models.Pet;
import com.yolka.app.utils.Api;
import com.yolka.app.utils.DateUtils;
import com.yolka.app.utils.NetworkMonitor;
import com.yolka.app.utils.Store;
import com.yolka.app.utils.SyncQueueManager;

import java.time.LocalDate;
import java.util.Calendar;
import java.util.Locale;
import java.util.UUID;

public class AppointmentFormBottomSheet extends BottomSheetDialogFragment {

    private BottomSheetAppointmentFormBinding binding;
    private Pet pet;
    private Pet.AppointmentRecord editingRecord;
    private VaccineFormBottomSheet.OnPetUpdatedListener listener;
    private String selectedTime = "10:00";

    public static AppointmentFormBottomSheet newInstance(Pet pet, VaccineFormBottomSheet.OnPetUpdatedListener listener) {
        return newInstanceForEdit(pet, null, listener);
    }

    public static AppointmentFormBottomSheet newInstanceForEdit(Pet pet, Pet.AppointmentRecord record, VaccineFormBottomSheet.OnPetUpdatedListener listener) {
        AppointmentFormBottomSheet sheet = new AppointmentFormBottomSheet();
        sheet.pet = pet;
        sheet.editingRecord = record;
        sheet.listener = listener;
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = BottomSheetAppointmentFormBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (editingRecord != null) {
            binding.tvAppointmentFormTitle.setText("Editar Cita Médica 📅");
            binding.btnSaveAppointment.setText("✓  Actualizar Cita Médica");
            binding.etAppointmentReason.setText(editingRecord.getReason());
            binding.etAppointmentClinic.setText(editingRecord.getClinicName());
            binding.etAppointmentVetName.setText(editingRecord.getVeterinarianName());

            String dt = editingRecord.getDateTime();
            if (dt != null && dt.length() >= 10) {
                binding.etAppointmentDate.setText(dt.substring(0, 10));
                if (dt.contains("T") && dt.length() >= 16) {
                    selectedTime = dt.substring(11, 16);
                } else {
                    selectedTime = "10:00";
                }
            } else {
                binding.etAppointmentDate.setText(LocalDate.now().plusDays(3).toString());
                selectedTime = "10:00";
            }
            binding.etAppointmentTime.setText(DateUtils.formatTime12h(selectedTime));
        } else {
            binding.tvAppointmentFormTitle.setText("Registrar Cita Médica 📅");
            binding.btnSaveAppointment.setText("✓  Guardar Cita Médica");
            binding.etAppointmentDate.setText(LocalDate.now().plusDays(3).toString());
            selectedTime = "10:00";
            binding.etAppointmentTime.setText(DateUtils.formatTime12h(selectedTime));
        }

        binding.etAppointmentDate.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(
                    requireContext(),
                    (dp, year, month, dayOfMonth) -> binding.etAppointmentDate.setText(
                            String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                    ),
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
            ).show();
        });

        binding.etAppointmentTime.setOnClickListener(v -> {
            int initialHour = 10;
            int initialMinute = 0;
            if (selectedTime != null && selectedTime.contains(":")) {
                try {
                    String[] parts = selectedTime.split(":");
                    initialHour = Integer.parseInt(parts[0].trim());
                    initialMinute = Integer.parseInt(parts[1].trim());
                } catch (Exception ignored) {
                }
            }

            new TimePickerDialog(
                    requireContext(),
                    (tp, hourOfDay, minute) -> {
                        selectedTime = String.format(Locale.US, "%02d:%02d", hourOfDay, minute);
                        binding.etAppointmentTime.setText(DateUtils.formatTime12h(selectedTime));
                    },
                    initialHour,
                    initialMinute,
                    false
            ).show();
        });

        binding.btnSaveAppointment.setOnClickListener(v -> saveAppointment());
    }

    private void saveAppointment() {
        String reason = binding.etAppointmentReason.getText().toString().trim();
        String clinic = binding.etAppointmentClinic.getText().toString().trim();
        String vetName = binding.etAppointmentVetName.getText().toString().trim();
        String date = binding.etAppointmentDate.getText().toString().trim();

        if (reason.isEmpty()) {
            binding.etAppointmentReason.setError("Ingresa el motivo de la cita");
            return;
        }
        if (clinic.isEmpty()) {
            binding.etAppointmentClinic.setError("Ingresa el nombre de la clínica");
            return;
        }

        String isoDateTime = date + "T" + selectedTime + ":00";
        boolean completed = editingRecord != null && editingRecord.isCompleted();
        AppointmentRequest req = new AppointmentRequest(clinic, vetName, reason, isoDateTime, "", completed);
        binding.btnSaveAppointment.setEnabled(false);

        if (!NetworkMonitor.getInstance().isOnline()) {
            saveOffline(req);
            return;
        }

        boolean isEditing = editingRecord != null && editingRecord.getId() != null;
        retrofit2.Call<GenericResponse> call = isEditing
                ? Api.getInstance().getService().updateAppointment(pet.getId(), editingRecord.getId(), req)
                : Api.getInstance().getService().addAppointment(pet.getId(), req);

        Api.getInstance().enqueue(call, new Api.ApiCallback<GenericResponse>() {
            @Override
            public void onSuccess(GenericResponse result) {
                if (!isAdded()) return;
                Pet updated = result.pet != null ? result.pet : pet;
                if (result.pet == null && isEditing) {
                    editingRecord.setClinicName(req.clinicName);
                    editingRecord.setVeterinarianName(req.veterinarianName);
                    editingRecord.setReason(req.reason);
                    editingRecord.setDateTime(req.dateTime);
                    editingRecord.setCompleted(req.completed != null && req.completed);
                    updated = pet;
                }
                Store.getInstance().upsertCachedPet(updated);
                if (listener != null) listener.onPetUpdated(updated);
                dismissAllowingStateLoss();
            }

            @Override
            public void onError(String message, String errorCode) {
                if (!isAdded()) return;
                binding.btnSaveAppointment.setEnabled(true);
                if ("NETWORK_ERROR".equals(errorCode)) {
                    saveOffline(req);
                } else {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void saveOffline(AppointmentRequest req) {
        if (editingRecord != null) {
            editingRecord.setClinicName(req.clinicName);
            editingRecord.setVeterinarianName(req.veterinarianName);
            editingRecord.setReason(req.reason);
            editingRecord.setDateTime(req.dateTime);
            editingRecord.setCompleted(req.completed != null && req.completed);
            pet.setPendingSync(true);

            Store.getInstance().upsertCachedPet(pet);
            SyncQueueManager.getInstance().enqueue(SyncQueueManager.OP_UPDATE_APPOINTMENT, pet.getId(), editingRecord.getId(), req);
            Toast.makeText(requireContext(), "Cita actualizada offline.", Toast.LENGTH_SHORT).show();
        } else {
            Pet.AppointmentRecord rec = new Pet.AppointmentRecord();
            rec.setId(UUID.randomUUID().toString());
            rec.setClinicName(req.clinicName);
            rec.setVeterinarianName(req.veterinarianName);
            rec.setReason(req.reason);
            rec.setDateTime(req.dateTime);
            rec.setCompleted(false);
            pet.getAppointments().add(0, rec);
            pet.setPendingSync(true);

            Store.getInstance().upsertCachedPet(pet);
            SyncQueueManager.getInstance().enqueue(SyncQueueManager.OP_ADD_APPOINTMENT, pet.getId(), null, req);
            Toast.makeText(requireContext(), "Cita guardada offline.", Toast.LENGTH_SHORT).show();
        }
        if (listener != null) listener.onPetUpdated(pet);
        dismissAllowingStateLoss();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
