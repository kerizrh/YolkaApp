package com.yolka.app.ui.pets;

import android.app.DatePickerDialog;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.yolka.app.R;
import com.yolka.app.context.UserManager;
import com.yolka.app.databinding.ActivityPetFormBinding;
import com.yolka.app.models.ApiPayloads.CreatePetRequest;
import com.yolka.app.models.ApiPayloads.GenericResponse;
import com.yolka.app.models.Pet;
import com.yolka.app.models.User;
import com.yolka.app.utils.Api;
import com.yolka.app.utils.DateUtils;
import com.yolka.app.utils.NetworkMonitor;
import com.yolka.app.utils.Store;
import com.yolka.app.utils.SyncQueueManager;

import java.time.LocalDate;
import java.time.Period;
import java.util.Calendar;
import java.util.Locale;
import java.util.UUID;

public class PetFormActivity extends AppCompatActivity {

    public static final String EXTRA_PET = "extra_pet";

    private ActivityPetFormBinding binding;
    private Pet editingPet;
    private String selectedSpecies = "DOG";
    private String selectedGender = "MALE";
    private String selectedSize = "SMALL";
    private String selectedBirthDate = "2024-03-15";
    private String selectedPhotoUriOrUrl = "";

    private final ActivityResultLauncher<String> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            (Uri uri) -> {
                if (uri != null) {
                    selectedPhotoUriOrUrl = uri.toString();
                    Glide.with(this)
                            .load(uri)
                            .circleCrop()
                            .into(binding.ivPetFormPreview);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPetFormBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getIntent() != null && getIntent().hasExtra(EXTRA_PET)) {
            editingPet = (Pet) getIntent().getSerializableExtra(EXTRA_PET);
        }

        setupInteractions();
        if (editingPet != null) {
            populateEditingPet(editingPet);
        } else {
            binding.etPetBirthDate.setText(selectedBirthDate);
            updateSpeciesSelection("DOG");
            updateGenderSelection("MALE");
            updateSizeSelection("SMALL");
        }
    }

    private void setupInteractions() {
        binding.btnBackPetForm.setOnClickListener(v -> finish());
        View.OnClickListener photoPickerListener = v -> imagePickerLauncher.launch("image/*");
        binding.cardPetPhotoPicker.setOnClickListener(photoPickerListener);
        binding.ivPetFormPreview.setOnClickListener(photoPickerListener);

        binding.cardSpeciesDog.setOnClickListener(v -> updateSpeciesSelection("DOG"));
        binding.cardSpeciesCat.setOnClickListener(v -> updateSpeciesSelection("CAT"));

        binding.btnGenderMale.setOnClickListener(v -> updateGenderSelection("MALE"));
        binding.btnGenderFemale.setOnClickListener(v -> updateGenderSelection("FEMALE"));

        binding.cardSizeSmall.setOnClickListener(v -> updateSizeSelection("SMALL"));
        binding.cardSizeMedium.setOnClickListener(v -> updateSizeSelection("MEDIUM"));
        binding.cardSizeLarge.setOnClickListener(v -> updateSizeSelection("LARGE"));

        binding.etPetBirthDate.setOnClickListener(v -> showBirthDatePicker());

        binding.btnWeightMinus.setOnClickListener(v -> adjustWeight(-0.5));
        binding.btnWeightPlus.setOnClickListener(v -> adjustWeight(0.5));

        binding.btnSavePet.setOnClickListener(v -> savePet());
        binding.btnDeletePet.setOnClickListener(v -> confirmDeletePet());
    }

    private void populateEditingPet(Pet pet) {
        binding.tvPetFormHeaderTitle.setText("Editar Mascota");
        binding.btnSavePet.setText("✓  Guardar cambios");
        binding.btnDeletePet.setVisibility(View.VISIBLE);

        binding.etPetName.setText(pet.getName());
        binding.etPetBreed.setText(pet.getBreed());
        binding.etPetColor.setText(pet.getColor());
        binding.etPetWeight.setText(String.format(Locale.US, "%.1f", pet.getWeight()));

        if (pet.getPhotoUrl() != null && !pet.getPhotoUrl().isBlank()) {
            selectedPhotoUriOrUrl = pet.getPhotoUrl();
            Glide.with(this)
                    .load(pet.getPhotoUrl())
                    .placeholder(R.drawable.ic_camera_add)
                    .circleCrop()
                    .into(binding.ivPetFormPreview);
        }

        if (!pet.getBirthDate().isBlank()) {
            selectedBirthDate = pet.getBirthDate();
        }
        binding.etPetBirthDate.setText(selectedBirthDate);
        binding.tvCalculatedAgeBadge.setText("🎂 " + DateUtils.calculateReadableAge(selectedBirthDate, pet.getAgeYears()));

        updateSpeciesSelection(pet.getSpecies());
        updateGenderSelection(pet.getGender());
        updateSizeSelection(pet.getSize());
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void updateSpeciesSelection(String species) {
        selectedSpecies = "CAT".equalsIgnoreCase(species) ? "CAT" : "DOG";
        boolean isDog = "DOG".equals(selectedSpecies);

        // Perro
        binding.cardSpeciesDog.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this,
                isDog ? R.color.yolka_primary_deep : R.color.yolka_card_stroke)));
        binding.cardSpeciesDog.setStrokeWidth(dpToPx(isDog ? 2 : 1));
        binding.cardSpeciesDog.setCardBackgroundColor(ContextCompat.getColor(this,
                isDog ? R.color.badge_lavender_bg : R.color.yolka_surface));
        binding.cardSpeciesDog.setCardElevation(dpToPx(isDog ? 2 : 0));
        binding.tvSpeciesDogTitle.setTextColor(ContextCompat.getColor(this,
                isDog ? R.color.yolka_primary_deep : R.color.yolka_text_primary));
        binding.tvSpeciesDogSub.setTextColor(ContextCompat.getColor(this,
                isDog ? R.color.yolka_primary_deep : R.color.yolka_text_secondary));

        // Gato
        binding.cardSpeciesCat.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this,
                !isDog ? R.color.yolka_primary_deep : R.color.yolka_card_stroke)));
        binding.cardSpeciesCat.setStrokeWidth(dpToPx(!isDog ? 2 : 1));
        binding.cardSpeciesCat.setCardBackgroundColor(ContextCompat.getColor(this,
                !isDog ? R.color.badge_lavender_bg : R.color.yolka_surface));
        binding.cardSpeciesCat.setCardElevation(dpToPx(!isDog ? 2 : 0));
        binding.tvSpeciesCatTitle.setTextColor(ContextCompat.getColor(this,
                !isDog ? R.color.yolka_primary_deep : R.color.yolka_text_primary));
        binding.tvSpeciesCatSub.setTextColor(ContextCompat.getColor(this,
                !isDog ? R.color.yolka_primary_deep : R.color.yolka_text_secondary));
    }

    private void updateGenderSelection(String gender) {
        selectedGender = "FEMALE".equalsIgnoreCase(gender) ? "FEMALE" : "MALE";
        boolean isMale = "MALE".equals(selectedGender);

        if (isMale) {
            binding.btnGenderMale.setBackgroundResource(R.drawable.bg_pill_selected);
            binding.btnGenderMale.setTextColor(ContextCompat.getColor(this, R.color.white));
            binding.btnGenderFemale.setBackground(null);
            binding.btnGenderFemale.setTextColor(ContextCompat.getColor(this, R.color.yolka_text_secondary));
        } else {
            binding.btnGenderMale.setBackground(null);
            binding.btnGenderMale.setTextColor(ContextCompat.getColor(this, R.color.yolka_text_secondary));
            binding.btnGenderFemale.setBackgroundResource(R.drawable.bg_pill_selected);
            binding.btnGenderFemale.setTextColor(ContextCompat.getColor(this, R.color.white));
        }
    }

    private void updateSizeSelection(String size) {
        if ("LARGE".equalsIgnoreCase(size)) selectedSize = "LARGE";
        else if ("MEDIUM".equalsIgnoreCase(size)) selectedSize = "MEDIUM";
        else selectedSize = "SMALL";

        boolean isSmall = "SMALL".equals(selectedSize);
        boolean isMedium = "MEDIUM".equals(selectedSize);
        boolean isLarge = "LARGE".equals(selectedSize);

        // Pequeño
        binding.cardSizeSmall.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this,
                isSmall ? R.color.yolka_primary_deep : R.color.yolka_card_stroke)));
        binding.cardSizeSmall.setStrokeWidth(dpToPx(isSmall ? 2 : 1));
        binding.cardSizeSmall.setCardBackgroundColor(ContextCompat.getColor(this,
                isSmall ? R.color.badge_lavender_bg : R.color.yolka_surface));
        binding.cardSizeSmall.setCardElevation(dpToPx(isSmall ? 2 : 0));
        binding.tvSizeSmallTitle.setTextColor(ContextCompat.getColor(this,
                isSmall ? R.color.yolka_primary_deep : R.color.yolka_text_primary));
        binding.tvSizeSmallSub.setTextColor(ContextCompat.getColor(this,
                isSmall ? R.color.yolka_primary_deep : R.color.yolka_text_secondary));

        // Mediano
        binding.cardSizeMedium.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this,
                isMedium ? R.color.yolka_primary_deep : R.color.yolka_card_stroke)));
        binding.cardSizeMedium.setStrokeWidth(dpToPx(isMedium ? 2 : 1));
        binding.cardSizeMedium.setCardBackgroundColor(ContextCompat.getColor(this,
                isMedium ? R.color.badge_lavender_bg : R.color.yolka_surface));
        binding.cardSizeMedium.setCardElevation(dpToPx(isMedium ? 2 : 0));
        binding.tvSizeMediumTitle.setTextColor(ContextCompat.getColor(this,
                isMedium ? R.color.yolka_primary_deep : R.color.yolka_text_primary));
        binding.tvSizeMediumSub.setTextColor(ContextCompat.getColor(this,
                isMedium ? R.color.yolka_primary_deep : R.color.yolka_text_secondary));

        // Grande
        binding.cardSizeLarge.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this,
                isLarge ? R.color.yolka_primary_deep : R.color.yolka_card_stroke)));
        binding.cardSizeLarge.setStrokeWidth(dpToPx(isLarge ? 2 : 1));
        binding.cardSizeLarge.setCardBackgroundColor(ContextCompat.getColor(this,
                isLarge ? R.color.badge_lavender_bg : R.color.yolka_surface));
        binding.cardSizeLarge.setCardElevation(dpToPx(isLarge ? 2 : 0));
        binding.tvSizeLargeTitle.setTextColor(ContextCompat.getColor(this,
                isLarge ? R.color.yolka_primary_deep : R.color.yolka_text_primary));
        binding.tvSizeLargeSub.setTextColor(ContextCompat.getColor(this,
                isLarge ? R.color.yolka_primary_deep : R.color.yolka_text_secondary));
    }

    private void showBirthDatePicker() {
        Calendar cal = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedBirthDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                    binding.etPetBirthDate.setText(selectedBirthDate);
                    binding.tvCalculatedAgeBadge.setText("🎂 " + DateUtils.calculateReadableAge(selectedBirthDate, 0));
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        );
        dialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        dialog.show();
    }

    private void adjustWeight(double delta) {
        double current = 4.5;
        try {
            current = Double.parseDouble(binding.etPetWeight.getText().toString().trim());
        } catch (Exception ignored) {
        }
        double updated = Math.max(0.2, current + delta);
        binding.etPetWeight.setText(String.format(Locale.US, "%.1f", updated));
    }

    private void savePet() {
        String name = binding.etPetName.getText().toString().trim();
        if (name.isEmpty()) {
            binding.etPetName.setError("El nombre de la mascota es obligatorio");
            return;
        }

        double weight = 4.5;
        try {
            weight = Double.parseDouble(binding.etPetWeight.getText().toString().trim());
            if (weight <= 0) weight = 1.0;
        } catch (Exception ignored) {
        }

        int ageYears = 0;
        try {
            LocalDate birth = LocalDate.parse(selectedBirthDate);
            ageYears = Math.max(0, Period.between(birth, LocalDate.now()).getYears());
        } catch (Exception ignored) {
        }

        User user = UserManager.getInstance().getCurrentUser();
        CreatePetRequest req = new CreatePetRequest();
        req.userId = user != null ? user.getId() : null;
        req.name = name;
        req.species = selectedSpecies;
        req.breed = binding.etPetBreed.getText().toString().trim();
        req.color = binding.etPetColor.getText().toString().trim();
        req.gender = selectedGender;
        req.birthDate = selectedBirthDate;
        req.ageYears = ageYears;
        req.weight = weight;
        req.size = selectedSize;
        req.photoUrl = selectedPhotoUriOrUrl != null ? selectedPhotoUriOrUrl.trim() : "";

        binding.btnSavePet.setEnabled(false);

        if (editingPet == null) {
            if (!NetworkMonitor.getInstance().isOnline()) {
                savePetOffline(req, null);
                return;
            }
            Api.getInstance().enqueue(Api.getInstance().getService().createPet(req), new Api.ApiCallback<GenericResponse>() {
                @Override
                public void onSuccess(GenericResponse result) {
                    binding.btnSavePet.setEnabled(true);
                    if (result.pet != null) {
                        Store.getInstance().upsertCachedPet(result.pet);
                        Store.getInstance().setSelectedPetId(result.pet.getId());
                    }
                    Toast.makeText(PetFormActivity.this, "Mascota registrada exitosamente", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onError(String message, String errorCode) {
                    binding.btnSavePet.setEnabled(true);
                    if ("NETWORK_ERROR".equals(errorCode)) {
                        savePetOffline(req, null);
                    } else {
                        Toast.makeText(PetFormActivity.this, message, Toast.LENGTH_LONG).show();
                    }
                }
            });
        } else {
            if (!NetworkMonitor.getInstance().isOnline()) {
                savePetOffline(req, editingPet);
                return;
            }
            Api.getInstance().enqueue(Api.getInstance().getService().updatePet(editingPet.getId(), req), new Api.ApiCallback<GenericResponse>() {
                @Override
                public void onSuccess(GenericResponse result) {
                    binding.btnSavePet.setEnabled(true);
                    if (result.pet != null) {
                        Store.getInstance().upsertCachedPet(result.pet);
                    }
                    Toast.makeText(PetFormActivity.this, "Información actualizada", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onError(String message, String errorCode) {
                    binding.btnSavePet.setEnabled(true);
                    if ("NETWORK_ERROR".equals(errorCode)) {
                        savePetOffline(req, editingPet);
                    } else {
                        Toast.makeText(PetFormActivity.this, message, Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
    }

    private void savePetOffline(CreatePetRequest req, Pet existing) {
        Pet localPet = existing != null ? existing : new Pet();
        if (existing == null) {
            String tempId = "offline-" + UUID.randomUUID().toString();
            localPet.setId(tempId);
            localPet.setUuid(tempId);
        }
        localPet.setName(req.name);
        localPet.setSpecies(req.species);
        localPet.setBreed(req.breed);
        localPet.setColor(req.color);
        localPet.setGender(req.gender);
        localPet.setBirthDate(req.birthDate);
        localPet.setAgeYears(req.ageYears);
        localPet.setWeight(req.weight);
        localPet.setSize(req.size);
        localPet.setPhotoUrl(req.photoUrl);
        localPet.setPendingSync(true);

        Store.getInstance().upsertCachedPet(localPet);
        Store.getInstance().setSelectedPetId(localPet.getId());

        SyncQueueManager.getInstance().enqueue(
                existing == null ? SyncQueueManager.OP_CREATE_PET : SyncQueueManager.OP_UPDATE_PET,
                localPet.getId(),
                null,
                req
        );

        Toast.makeText(this, "Guardado sin conexión. Se sincronizará automáticamente al reconectar.", Toast.LENGTH_LONG).show();
        finish();
    }

    private void confirmDeletePet() {
        if (editingPet == null) return;
        new AlertDialog.Builder(this)
                .setTitle("Eliminar a " + editingPet.getName())
                .setMessage("¿Estás seguro de que deseas eliminar el perfil de esta mascota?")
                .setPositiveButton("Eliminar", (d, w) -> {
                    String petId = editingPet.getId();
                    if (!NetworkMonitor.getInstance().isOnline()) {
                        Store.getInstance().removeCachedPet(petId);
                        SyncQueueManager.getInstance().enqueue(SyncQueueManager.OP_DELETE_PET, petId, null, null);
                        Toast.makeText(this, "Mascota eliminada localmente (pendiente de sincronizar)", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    Api.getInstance().enqueue(Api.getInstance().getService().deletePet(petId), new Api.ApiCallback<GenericResponse>() {
                        @Override
                        public void onSuccess(GenericResponse result) {
                            Store.getInstance().removeCachedPet(petId);
                            Toast.makeText(PetFormActivity.this, "Mascota eliminada", Toast.LENGTH_SHORT).show();
                            finish();
                        }

                        @Override
                        public void onError(String message, String errorCode) {
                            Toast.makeText(PetFormActivity.this, message, Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
