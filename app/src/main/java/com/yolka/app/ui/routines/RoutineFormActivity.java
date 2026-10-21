package com.yolka.app.ui.routines;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.yolka.app.R;
import com.yolka.app.databinding.ActivityRoutineFormBinding;
import com.yolka.app.models.ApiPayloads.GenericResponse;
import com.yolka.app.models.ApiPayloads.RoutineRequest;
import com.yolka.app.models.Pet;
import com.yolka.app.ui.adapters.PetAvatarAdapter;
import com.yolka.app.utils.Api;
import com.yolka.app.utils.DateUtils;
import com.yolka.app.utils.NetworkMonitor;
import com.yolka.app.utils.Store;
import com.yolka.app.utils.SyncQueueManager;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class RoutineFormActivity extends AppCompatActivity {

    private ActivityRoutineFormBinding binding;
    private PetAvatarAdapter petAvatarAdapter;
    private List<Pet> pets;
    private Pet selectedPet;

    private String selectedCategory = "FEEDING"; // FEEDING | WALK | GROOMING_BATH
    private String selectedTime = "08:00";
    private List<String> selectedDays = Arrays.asList("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRoutineFormBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        pets = Store.getInstance().getCachedPets();
        String savedId = Store.getInstance().getSelectedPetId();
        for (Pet p : pets) {
            if (p.getId() != null && p.getId().equals(savedId)) {
                selectedPet = p;
                break;
            }
        }
        if (selectedPet == null && !pets.isEmpty()) {
            selectedPet = pets.get(0);
        }

        setupPetSelector();
        setupCategoryAndFrequencySelectors();
        setupDateAndTimePickers();

        binding.btnBackRoutineForm.setOnClickListener(v -> finish());
        binding.btnSaveRoutine.setOnClickListener(v -> saveRoutine());
    }

    private void setupPetSelector() {
        petAvatarAdapter = new PetAvatarAdapter(new PetAvatarAdapter.OnPetSelectedListener() {
            @Override
            public void onPetSelected(Pet pet) {
                selectedPet = pet;
                Store.getInstance().setSelectedPetId(pet.getId());
                petAvatarAdapter.submitList(pets, pet.getId());
            }

            @Override
            public void onPetLongClick(Pet pet) {
            }
        });
        binding.rvRoutineAssignPets.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        );
        binding.rvRoutineAssignPets.setAdapter(petAvatarAdapter);
        petAvatarAdapter.submitList(pets, selectedPet != null ? selectedPet.getId() : null);
    }

    private void setupCategoryAndFrequencySelectors() {
        binding.cardRoutineTypeFeeding.setOnClickListener(v -> selectCategory("FEEDING"));
        binding.cardRoutineTypeWalk.setOnClickListener(v -> selectCategory("WALK"));
        binding.cardRoutineTypeHygiene.setOnClickListener(v -> selectCategory("GROOMING_BATH"));

        binding.chipFreqDaily.setOnClickListener(v -> {
            selectedDays = Arrays.asList("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN");
            highlightFrequencyChip(0);
        });
        binding.chipFreqWeekdays.setOnClickListener(v -> {
            selectedDays = Arrays.asList("MON", "TUE", "WED", "THU", "FRI");
            highlightFrequencyChip(1);
        });
        binding.chipFreqWeekends.setOnClickListener(v -> {
            selectedDays = Arrays.asList("SAT", "SUN");
            highlightFrequencyChip(2);
        });
    }

    private void selectCategory(String category) {
        this.selectedCategory = category;
        binding.cardRoutineTypeFeeding.setCardBackgroundColor(ContextCompat.getColor(this,
                "FEEDING".equals(category) ? R.color.yolka_primary : R.color.yolka_surface));
        binding.cardRoutineTypeWalk.setCardBackgroundColor(ContextCompat.getColor(this,
                "WALK".equals(category) ? R.color.yolka_primary : R.color.yolka_surface));
        binding.cardRoutineTypeHygiene.setCardBackgroundColor(ContextCompat.getColor(this,
                category.startsWith("GROOMING") ? R.color.yolka_primary : R.color.yolka_surface));

        if (binding.etRoutineTitle.getText().toString().trim().isEmpty()) {
            if ("WALK".equals(category)) {
                binding.etRoutineTitle.setHint("Paseo vespertino en el parque");
            } else if (category.startsWith("GROOMING")) {
                binding.etRoutineTitle.setHint("Baño y corte de uñas");
            } else {
                binding.etRoutineTitle.setHint("Desayuno nutritivo matutino");
            }
        }
    }

    private void highlightFrequencyChip(int index) {
        binding.chipFreqDaily.setBackgroundResource(index == 0 ? R.drawable.bg_pill_input : R.drawable.bg_rounded_card);
        binding.chipFreqWeekdays.setBackgroundResource(index == 1 ? R.drawable.bg_pill_input : R.drawable.bg_rounded_card);
        binding.chipFreqWeekends.setBackgroundResource(index == 2 ? R.drawable.bg_pill_input : R.drawable.bg_rounded_card);
    }

    private void setupDateAndTimePickers() {
        binding.tvRoutineStartDate.setText("📅  " + DateUtils.formatIsoDate(LocalDate.now().toString()));
        binding.tvRoutineTimeDisplay.setText("🕒  " + DateUtils.formatTime12h(selectedTime));

        binding.cardPickStartDate.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(
                    this,
                    (dp, year, month, dayOfMonth) -> {
                        String dateStr = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                        binding.tvRoutineStartDate.setText("📅  " + DateUtils.formatIsoDate(dateStr));
                    },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
            ).show();
        });

        binding.cardPickRoutineTime.setOnClickListener(v -> {
            new TimePickerDialog(
                    this,
                    (tp, hourOfDay, minute) -> {
                        selectedTime = String.format(Locale.US, "%02d:%02d", hourOfDay, minute);
                        binding.tvRoutineTimeDisplay.setText("🕒  " + DateUtils.formatTime12h(selectedTime));
                    },
                    8,
                    0,
                    false
            ).show();
        });
    }

    private void saveRoutine() {
        if (selectedPet == null) {
            Toast.makeText(this, "Primero registra o selecciona una mascota", Toast.LENGTH_SHORT).show();
            return;
        }

        String title = binding.etRoutineTitle.getText().toString().trim();
        String description = binding.etRoutineDescription.getText().toString().trim();
        if (title.isEmpty()) {
            binding.etRoutineTitle.setError("Ingresa el título de la actividad");
            return;
        }

        RoutineRequest req = new RoutineRequest(
                selectedCategory,
                title,
                selectedTime,
                selectedDays,
                true,
                description
        );
        binding.btnSaveRoutine.setEnabled(false);

        if (!NetworkMonitor.getInstance().isOnline()) {
            saveRoutineOffline(req);
            return;
        }

        Api.getInstance().enqueue(Api.getInstance().getService().addRoutine(selectedPet.getId(), req), new Api.ApiCallback<GenericResponse>() {
            @Override
            public void onSuccess(GenericResponse result) {
                binding.btnSaveRoutine.setEnabled(true);
                if (result.pet != null) {
                    Store.getInstance().upsertCachedPet(result.pet);
                }
                Toast.makeText(RoutineFormActivity.this, "Rutina guardada exitosamente", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(String message, String errorCode) {
                binding.btnSaveRoutine.setEnabled(true);
                if ("NETWORK_ERROR".equals(errorCode)) {
                    saveRoutineOffline(req);
                } else {
                    Toast.makeText(RoutineFormActivity.this, message, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void saveRoutineOffline(RoutineRequest req) {
        Pet.RoutineRecord rec = new Pet.RoutineRecord();
        rec.setId(UUID.randomUUID().toString());
        rec.setCategory(req.category);
        rec.setTitle(req.title);
        rec.setTime(req.time);
        rec.setDaysOfWeek(req.daysOfWeek);
        rec.setEnabled(true);
        rec.setNotes(req.notes);

        selectedPet.getRoutines().add(0, rec);
        selectedPet.setPendingSync(true);
        Store.getInstance().upsertCachedPet(selectedPet);

        SyncQueueManager.getInstance().enqueue(
                SyncQueueManager.OP_ADD_ROUTINE,
                selectedPet.getId(),
                null,
                req
        );

        Toast.makeText(this, "Rutina guardada sin conexión. Se sincronizará al reconectar.", Toast.LENGTH_LONG).show();
        finish();
    }
}
