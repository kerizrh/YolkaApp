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
    private VaccineFormBottomSheet.OnPetUpdatedListener listener;

    public static AppointmentFormBottomSheet newInstance(Pet pet, VaccineFormBottomSheet.OnPetUpdatedListener listener) {
        AppointmentFormBottomSheet sheet = new AppointmentFormBottomSheet();
        sheet.pet = pet;
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

        binding.etAppointmentDate.setText(LocalDate.now().plusDays(3).toString());
        binding.etAppointmentTime.setText("10:00");

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
            new TimePickerDialog(
                    requireContext(),
                    (tp, hourOfDay, minute) -> binding.etAppointmentTime.setText(
                            String.format(Locale.US, "%02d:%02d", hourOfDay, minute)
                    ),
                    10,
                    0,
                    true
            ).show();
        });

        binding.btnSaveAppointment.setOnClickListener(v -> saveAppointment());
    }

    private void saveAppointment() {
        String reason = binding.etAppointmentReason.getText().toString().trim();
        String clinic = binding.etAppointmentClinic.getText().toString().trim();
        String vetName = binding.etAppointmentVetName.getText().toString().trim();
        String date = binding.etAppointmentDate.getText().toString().trim();
        String time = binding.etAppointmentTime.getText().toString().trim();

        if (reason.isEmpty()) {
            binding.etAppointmentReason.setError("Ingresa el motivo de la cita");
            return;
        }
        if (clinic.isEmpty()) {
            binding.etAppointmentClinic.setError("Ingresa el nombre de la clínica");
            return;
        }

        String isoDateTime = date + "T" + time + ":00";
        AppointmentRequest req = new AppointmentRequest(clinic, vetName, reason, isoDateTime, "", false);
        binding.btnSaveAppointment.setEnabled(false);

        if (!NetworkMonitor.getInstance().isOnline()) {
            saveOffline(req);
            return;
        }

        Api.getInstance().enqueue(Api.getInstance().getService().addAppointment(pet.getId(), req), new Api.ApiCallback<GenericResponse>() {
            @Override
            public void onSuccess(GenericResponse result) {
                if (!isAdded()) return;
                Pet updated = result.pet != null ? result.pet : pet;
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
        if (listener != null) listener.onPetUpdated(pet);
        dismissAllowingStateLoss();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
