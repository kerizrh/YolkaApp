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
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.yolka.app.R;
import com.yolka.app.databinding.BottomSheetDewormingFormBinding;
import com.yolka.app.models.ApiPayloads.DewormingRequest;
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

public class DewormingFormBottomSheet extends BottomSheetDialogFragment {

    private BottomSheetDewormingFormBinding binding;
    private Pet pet;
    private VaccineFormBottomSheet.OnPetUpdatedListener listener;
    private String selectedType = "INTERNAL";

    public static DewormingFormBottomSheet newInstance(Pet pet, VaccineFormBottomSheet.OnPetUpdatedListener listener) {
        DewormingFormBottomSheet sheet = new DewormingFormBottomSheet();
        sheet.pet = pet;
        sheet.listener = listener;
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = BottomSheetDewormingFormBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.etDewormAppliedDate.setText(LocalDate.now().toString());
        binding.etDewormNextDate.setText(LocalDate.now().plusMonths(1).toString());

        binding.btnDewormInternal.setOnClickListener(v -> selectType("INTERNAL"));
        binding.btnDewormExternal.setOnClickListener(v -> selectType("EXTERNAL"));

        binding.etDewormAppliedDate.setOnClickListener(v -> pickDate(binding.etDewormAppliedDate));
        binding.etDewormNextDate.setOnClickListener(v -> pickDate(binding.etDewormNextDate));

        binding.btnSaveDeworming.setOnClickListener(v -> saveDeworming());
    }

    private void selectType(String type) {
        selectedType = type;
        boolean internal = "INTERNAL".equals(type);
        binding.btnDewormInternal.setBackgroundResource(internal ? R.drawable.bg_rounded_card : 0);
        binding.btnDewormInternal.setTextColor(ContextCompat.getColor(requireContext(),
                internal ? R.color.yolka_text_primary : R.color.yolka_text_secondary));

        binding.btnDewormExternal.setBackgroundResource(!internal ? R.drawable.bg_rounded_card : 0);
        binding.btnDewormExternal.setTextColor(ContextCompat.getColor(requireContext(),
                !internal ? R.color.yolka_text_primary : R.color.yolka_text_secondary));
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

    private void saveDeworming() {
        String product = binding.etDewormProduct.getText().toString().trim();
        String applied = binding.etDewormAppliedDate.getText().toString().trim();
        String nextDate = binding.etDewormNextDate.getText().toString().trim();
        String notes = binding.etDewormNotes.getText().toString().trim();

        if (product.isEmpty()) {
            binding.etDewormProduct.setError("Ingresa el producto administrado");
            return;
        }

        DewormingRequest req = new DewormingRequest(selectedType, product, applied, nextDate, notes);
        binding.btnSaveDeworming.setEnabled(false);

        if (!NetworkMonitor.getInstance().isOnline()) {
            saveOffline(req);
            return;
        }

        Api.getInstance().enqueue(Api.getInstance().getService().addDeworming(pet.getId(), req), new Api.ApiCallback<GenericResponse>() {
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
                binding.btnSaveDeworming.setEnabled(true);
                if ("NETWORK_ERROR".equals(errorCode)) {
                    saveOffline(req);
                } else {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void saveOffline(DewormingRequest req) {
        Pet.DewormingRecord rec = new Pet.DewormingRecord();
        rec.setId(UUID.randomUUID().toString());
        rec.setType(req.type);
        rec.setProductName(req.productName);
        rec.setApplicationDate(req.applicationDate);
        rec.setNextDueDate(req.nextDueDate);
        rec.setNotes(req.notes);
        pet.getDewormings().add(0, rec);
        pet.setPendingSync(true);

        Store.getInstance().upsertCachedPet(pet);
        SyncQueueManager.getInstance().enqueue(SyncQueueManager.OP_ADD_DEWORMING, pet.getId(), null, req);
        Toast.makeText(requireContext(), "Desparasitación guardada offline.", Toast.LENGTH_SHORT).show();
        if (listener != null) listener.onPetUpdated(pet);
        dismissAllowingStateLoss();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
