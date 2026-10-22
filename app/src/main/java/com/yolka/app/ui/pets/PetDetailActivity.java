package com.yolka.app.ui.pets;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.yolka.app.R;
import com.yolka.app.databinding.ActivityPetDetailBinding;
import com.yolka.app.models.ApiPayloads.GenericResponse;
import com.yolka.app.models.Pet;
import com.yolka.app.ui.adapters.AppointmentAdapter;
import com.yolka.app.ui.adapters.DewormingAdapter;
import com.yolka.app.ui.adapters.VaccineAdapter;
import com.yolka.app.ui.medical.AppointmentFormBottomSheet;
import com.yolka.app.ui.medical.DewormingFormBottomSheet;
import com.yolka.app.ui.medical.VaccineFormBottomSheet;
import com.yolka.app.utils.Api;
import com.yolka.app.utils.DateUtils;
import com.yolka.app.utils.NetworkMonitor;
import com.yolka.app.utils.Store;

import java.util.Locale;

public class PetDetailActivity extends AppCompatActivity {

    public static final String EXTRA_PET = "extra_pet";
    public static final String EXTRA_INITIAL_TAB = "extra_initial_tab";

    private ActivityPetDetailBinding binding;
    private Pet currentPet;
    private int activeTabIndex = 0; // 0: Vacunas, 1: Desparasitación, 2: Citas

    private final VaccineAdapter vaccineAdapter = new VaccineAdapter();
    private final DewormingAdapter dewormingAdapter = new DewormingAdapter();
    private final AppointmentAdapter appointmentAdapter = new AppointmentAdapter();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPetDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getIntent() != null) {
            currentPet = (Pet) getIntent().getSerializableExtra(EXTRA_PET);
            activeTabIndex = getIntent().getIntExtra(EXTRA_INITIAL_TAB, 0);
        }
        if (currentPet == null) {
            finish();
            return;
        }

        binding.rvClinicalRecords.setLayoutManager(new LinearLayoutManager(this));

        vaccineAdapter.setOnVaccineClickListener(vaccine -> {
            VaccineFormBottomSheet.newInstanceForEdit(currentPet, vaccine, updatedPet -> {
                currentPet = updatedPet;
                renderPetHeader();
                switchClinicalTab(activeTabIndex);
            }).show(getSupportFragmentManager(), "EditVaccineSheet");
        });

        dewormingAdapter.setOnDewormingClickListener(deworming -> {
            DewormingFormBottomSheet.newInstanceForEdit(currentPet, deworming, updatedPet -> {
                currentPet = updatedPet;
                renderPetHeader();
                switchClinicalTab(activeTabIndex);
            }).show(getSupportFragmentManager(), "EditDewormingSheet");
        });

        appointmentAdapter.setOnAppointmentClickListener(appointment -> {
            AppointmentFormBottomSheet.newInstanceForEdit(currentPet, appointment, updatedPet -> {
                currentPet = updatedPet;
                renderPetHeader();
                switchClinicalTab(activeTabIndex);
            }).show(getSupportFragmentManager(), "EditAppointmentSheet");
        });

        binding.btnBackPetDetail.setOnClickListener(v -> finish());
        binding.btnEditPetFromDetail.setOnClickListener(v -> {
            Intent intent = new Intent(this, PetFormActivity.class);
            intent.putExtra(PetFormActivity.EXTRA_PET, currentPet);
            startActivity(intent);
        });

        binding.tabVaccines.setOnClickListener(v -> switchClinicalTab(0));
        binding.tabDewormings.setOnClickListener(v -> switchClinicalTab(1));
        binding.tabAppointments.setOnClickListener(v -> switchClinicalTab(2));

        binding.btnAddClinicalRecord.setOnClickListener(v -> openActiveTabForm());

        renderPetHeader();
        switchClinicalTab(activeTabIndex);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshPetDetails();
    }

    private void refreshPetDetails() {
        if (currentPet == null || currentPet.getId() == null) return;

        for (Pet cached : Store.getInstance().getCachedPets()) {
            if (currentPet.getId().equals(cached.getId())) {
                currentPet = cached;
                renderPetHeader();
                switchClinicalTab(activeTabIndex);
                break;
            }
        }

        if (!NetworkMonitor.getInstance().isOnline() || currentPet.getId().startsWith("offline-")) {
            return;
        }

        Api.getInstance().enqueue(Api.getInstance().getService().getPetById(currentPet.getId()), new Api.ApiCallback<GenericResponse>() {
            @Override
            public void onSuccess(GenericResponse result) {
                if (isFinishing() || result.pet == null) return;
                currentPet = result.pet;
                Store.getInstance().upsertCachedPet(currentPet);
                renderPetHeader();
                switchClinicalTab(activeTabIndex);
            }

            @Override
            public void onError(String message, String errorCode) {
            }
        });
    }

    private void renderPetHeader() {
        binding.tvPetHeaderName.setText(currentPet.getName());
        binding.tvPetCardTitleName.setText(currentPet.getName());

        boolean isFemale = "FEMALE".equalsIgnoreCase(currentPet.getGender());
        binding.tvPetGenderBadge.setText(isFemale ? "♀ Hembra" : "♂ Macho");

        String breed = !currentPet.getBreed().isBlank()
                ? currentPet.getBreed()
                : ("CAT".equalsIgnoreCase(currentPet.getSpecies()) ? "Gato" : "Perro");
        String age = DateUtils.calculateReadableAge(currentPet.getBirthDate(), currentPet.getAgeYears());
        binding.tvPetBreedAndAge.setText(breed + " • " + age);

        binding.tvPetWeightValue.setText(String.format(Locale.US, "%.1f kg ✓", currentPet.getWeight()));

        if (currentPet.getPhotoUrl() != null && !currentPet.getPhotoUrl().isBlank()) {
            Glide.with(this)
                    .load(currentPet.getPhotoUrl())
                    .placeholder(R.drawable.ic_yolka_paw)
                    .error(R.drawable.ic_yolka_paw)
                    .centerCrop()
                    .into(binding.ivPetDetailPhoto);
        } else {
            binding.ivPetDetailPhoto.setImageResource(R.drawable.ic_yolka_paw);
        }
    }

    private void switchClinicalTab(int tabIndex) {
        this.activeTabIndex = tabIndex;

        binding.tabVaccines.setBackgroundResource(tabIndex == 0 ? R.drawable.bg_rounded_card : 0);
        binding.tabVaccines.setTextColor(ContextCompat.getColor(this,
                tabIndex == 0 ? R.color.yolka_text_primary : R.color.yolka_text_secondary));

        binding.tabDewormings.setBackgroundResource(tabIndex == 1 ? R.drawable.bg_rounded_card : 0);
        binding.tabDewormings.setTextColor(ContextCompat.getColor(this,
                tabIndex == 1 ? R.color.yolka_text_primary : R.color.yolka_text_secondary));

        binding.tabAppointments.setBackgroundResource(tabIndex == 2 ? R.drawable.bg_rounded_card : 0);
        binding.tabAppointments.setTextColor(ContextCompat.getColor(this,
                tabIndex == 2 ? R.color.yolka_text_primary : R.color.yolka_text_secondary));

        if (tabIndex == 0) {
            binding.tvClinicalSectionTitle.setText("Historial de Vacunación");
            binding.btnAddClinicalRecord.setText("⊕  Registrar Vacuna");
            binding.rvClinicalRecords.setAdapter(vaccineAdapter);
            vaccineAdapter.submitList(currentPet.getVaccines());
            binding.tvClinicalEmpty.setVisibility(vaccineAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
        } else if (tabIndex == 1) {
            binding.tvClinicalSectionTitle.setText("Desparasitación Preventiva");
            binding.btnAddClinicalRecord.setText("⊕  Registrar Desparasitación");
            binding.rvClinicalRecords.setAdapter(dewormingAdapter);
            dewormingAdapter.submitList(currentPet.getDewormings());
            binding.tvClinicalEmpty.setVisibility(dewormingAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
        } else {
            binding.tvClinicalSectionTitle.setText("Próxima Cita Veterinaria");
            binding.btnAddClinicalRecord.setText("⊕  Registrar Cita Médica");
            binding.rvClinicalRecords.setAdapter(appointmentAdapter);
            appointmentAdapter.submitList(currentPet.getAppointments());
            binding.tvClinicalEmpty.setVisibility(appointmentAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
        }
    }

    private void openActiveTabForm() {
        VaccineFormBottomSheet.OnPetUpdatedListener callback = updatedPet -> {
            currentPet = updatedPet;
            renderPetHeader();
            switchClinicalTab(activeTabIndex);
        };

        if (activeTabIndex == 0) {
            VaccineFormBottomSheet.newInstance(currentPet, callback)
                    .show(getSupportFragmentManager(), "VaccineSheet");
        } else if (activeTabIndex == 1) {
            DewormingFormBottomSheet.newInstance(currentPet, callback)
                    .show(getSupportFragmentManager(), "DewormingSheet");
        } else {
            AppointmentFormBottomSheet.newInstance(currentPet, callback)
                    .show(getSupportFragmentManager(), "AppointmentSheet");
        }
    }
}
