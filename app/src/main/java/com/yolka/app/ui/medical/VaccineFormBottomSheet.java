package com.yolka.app.ui.medical;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.yolka.app.databinding.BottomSheetVaccineFormBinding;
import com.yolka.app.models.ApiPayloads.GenericResponse;
import com.yolka.app.models.ApiPayloads.VaccineRequest;
import com.yolka.app.models.Pet;
import com.yolka.app.utils.Api;
import com.yolka.app.utils.NetworkMonitor;
import com.yolka.app.utils.Store;
import com.yolka.app.utils.SyncQueueManager;

import java.time.LocalDate;
import java.util.Calendar;
import java.util.Locale;
import java.util.UUID;

public class VaccineFormBottomSheet extends BottomSheetDialogFragment {

    public interface OnPetUpdatedListener {
        void onPetUpdated(Pet updatedPet);
    }

    private BottomSheetVaccineFormBinding binding;
    private Pet pet;
    private OnPetUpdatedListener listener;

    public static VaccineFormBottomSheet newInstance(Pet pet, OnPetUpdatedListener listener) {
        VaccineFormBottomSheet sheet = new VaccineFormBottomSheet();
        sheet.pet = pet;
        sheet.listener = listener;
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = BottomSheetVaccineFormBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String today = LocalDate.now().toString();
        String nextYear = LocalDate.now().plusYears(1).toString();
        binding.etVaccineAppliedDate.setText(today);
        binding.etVaccineNextDueDate.setText(nextYear);

        binding.etVaccineAppliedDate.setOnClickListener(v -> pickDate(binding.etVaccineAppliedDate));
        binding.etVaccineNextDueDate.setOnClickListener(v -> pickDate(binding.etVaccineNextDueDate));

        binding.btnSaveVaccine.setOnClickListener(v -> saveVaccine());
    }

    private void pickDate(EditText target) {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> target.setText(
                        String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                ),
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void saveVaccine() {
        String name = binding.etVaccineName.getText().toString().trim();
        String applied = binding.etVaccineAppliedDate.getText().toString().trim();
        String nextDue = binding.etVaccineNextDueDate.getText().toString().trim();
        String notes = binding.etVaccineNotes.getText().toString().trim();

        if (name.isEmpty()) {
            binding.etVaccineName.setError("Ingresa el nombre de la vacuna");
            return;
        }

        VaccineRequest req = new VaccineRequest(name, applied, nextDue, notes);
        binding.btnSaveVaccine.setEnabled(false);

        if (!NetworkMonitor.getInstance().isOnline()) {
            saveOffline(req);
            return;
        }

        Api.getInstance().enqueue(Api.getInstance().getService().addVaccine(pet.getId(), req), new Api.ApiCallback<GenericResponse>() {
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
                binding.btnSaveVaccine.setEnabled(true);
                if ("NETWORK_ERROR".equals(errorCode)) {
                    saveOffline(req);
                } else {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void saveOffline(VaccineRequest req) {
        Pet.VaccineRecord rec = new Pet.VaccineRecord();
        rec.setId(UUID.randomUUID().toString());
        rec.setVaccineName(req.vaccineName);
        rec.setApplicationDate(req.applicationDate);
        rec.setNextDueDate(req.nextDueDate);
        rec.setNotes(req.notes);
        pet.getVaccines().add(0, rec);
        pet.setPendingSync(true);

        Store.getInstance().upsertCachedPet(pet);
        SyncQueueManager.getInstance().enqueue(SyncQueueManager.OP_ADD_VACCINE, pet.getId(), null, req);
        Toast.makeText(requireContext(), "Vacuna guardada offline. Se sincronizará al reconectar.", Toast.LENGTH_SHORT).show();
        if (listener != null) listener.onPetUpdated(pet);
        dismissAllowingStateLoss();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
