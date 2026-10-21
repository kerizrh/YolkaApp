package com.yolka.app.ui.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.yolka.app.context.UserManager;
import com.yolka.app.databinding.FragmentDashboardBinding;
import com.yolka.app.models.ApiPayloads.GenericResponse;
import com.yolka.app.models.Pet;
import com.yolka.app.models.Tip;
import com.yolka.app.models.User;
import com.yolka.app.ui.adapters.DashboardActivityAdapter;
import com.yolka.app.ui.adapters.PetAvatarAdapter;
import com.yolka.app.ui.adapters.TipAdapter;
import com.yolka.app.ui.pets.PetDetailActivity;
import com.yolka.app.ui.pets.PetFormActivity;
import com.yolka.app.utils.Api;
import com.yolka.app.utils.DateUtils;
import com.yolka.app.utils.NetworkMonitor;
import com.yolka.app.utils.Store;

import java.util.ArrayList;
import java.util.List;

public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;
    private PetAvatarAdapter petAvatarAdapter;
    private DashboardActivityAdapter activityAdapter;
    private TipAdapter tipAdapter;
    private List<Pet> currentPets = new ArrayList<>();
    private Pet selectedPet;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        User user = UserManager.getInstance().getCurrentUser();
        if (user != null) {
            binding.tvDashboardGreeting.setText("¡Hola, " + user.getFirstName() + "! 👋");
        }

        setupRecyclers();
        setupActions();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshDashboardData();
    }

    private void setupRecyclers() {
        petAvatarAdapter = new PetAvatarAdapter(new PetAvatarAdapter.OnPetSelectedListener() {
            @Override
            public void onPetSelected(Pet pet) {
                selectPet(pet);
            }

            @Override
            public void onPetLongClick(Pet pet) {
                Intent intent = new Intent(requireContext(), PetFormActivity.class);
                intent.putExtra(PetFormActivity.EXTRA_PET, pet);
                startActivity(intent);
            }
        });
        binding.rvPetAvatars.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        );
        binding.rvPetAvatars.setAdapter(petAvatarAdapter);

        activityAdapter = new DashboardActivityAdapter(routine -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).openRoutinesSection();
            }
        });
        binding.rvTodayActivities.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvTodayActivities.setAdapter(activityAdapter);

        tipAdapter = new TipAdapter(this::showTipDialog);
        binding.rvDashboardTips.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvDashboardTips.setAdapter(tipAdapter);
    }

    private void setupActions() {
        binding.swipeDashboard.setOnRefreshListener(this::refreshDashboardData);

        binding.btnAddPetCircle.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), PetFormActivity.class))
        );

        binding.cardSelectedPetBanner.setOnClickListener(v -> openSelectedPetClinicalRecord(0));
        binding.btnOpenClinicalRecord.setOnClickListener(v -> openSelectedPetClinicalRecord(0));

        binding.cardQuickVaccine.setOnClickListener(v -> openSelectedPetClinicalRecord(0));
        binding.cardQuickAppointment.setOnClickListener(v -> openSelectedPetClinicalRecord(2));

        binding.tvSeeAllActivities.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).openRoutinesSection();
            }
        });

        binding.tvSeeAllTips.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).openTipsSection();
            }
        });
    }

    private void openSelectedPetClinicalRecord(int initialTab) {
        if (selectedPet == null) {
            Toast.makeText(requireContext(), "Primero registra tu primera mascota", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(requireContext(), PetFormActivity.class));
            return;
        }
        Intent intent = new Intent(requireContext(), PetDetailActivity.class);
        intent.putExtra(PetDetailActivity.EXTRA_PET, selectedPet);
        intent.putExtra(PetDetailActivity.EXTRA_INITIAL_TAB, initialTab);
        startActivity(intent);
    }

    public void refreshDashboardData() {
        if (binding == null) return;

        // 1. Cargar inmediatamente desde caché cifrada en Store (funciona aún sin conexión)
        List<Pet> cachedPets = Store.getInstance().getCachedPets();
        if (!cachedPets.isEmpty()) {
            applyPetsList(cachedPets);
        }
        List<Tip> cachedTips = Store.getInstance().getCachedTips();
        if (!cachedTips.isEmpty()) {
            tipAdapter.submitList(cachedTips);
        }

        if (!NetworkMonitor.getInstance().isOnline()) {
            binding.swipeDashboard.setRefreshing(false);
            return;
        }

        // 2. Sincronizar con YolkaApi cuando hay internet
        User user = UserManager.getInstance().getCurrentUser();
        String userId = user != null ? user.getId() : null;

        Api.getInstance().enqueue(Api.getInstance().getService().getPets(userId), new Api.ApiCallback<GenericResponse>() {
            @Override
            public void onSuccess(GenericResponse result) {
                if (!isAdded() || binding == null) return;
                binding.swipeDashboard.setRefreshing(false);
                if (result.pets != null) {
                    Store.getInstance().storeCachedPets(result.pets);
                    applyPetsList(result.pets);
                }
            }

            @Override
            public void onError(String message, String errorCode) {
                if (!isAdded() || binding == null) return;
                binding.swipeDashboard.setRefreshing(false);
            }
        });

        Api.getInstance().enqueue(Api.getInstance().getService().getTips(null, null), new Api.ApiCallback<GenericResponse>() {
            @Override
            public void onSuccess(GenericResponse result) {
                if (!isAdded() || binding == null) return;
                if (result.tips != null) {
                    Store.getInstance().storeCachedTips(result.tips);
                    tipAdapter.submitList(result.tips);
                }
            }

            @Override
            public void onError(String message, String errorCode) {
            }
        });
    }

    private void applyPetsList(List<Pet> pets) {
        this.currentPets = pets != null ? pets : new ArrayList<>();
        if (currentPets.isEmpty()) {
            selectedPet = null;
            petAvatarAdapter.submitList(currentPets, null);
            binding.tvSelectedPetSummaryTitle.setText("Aún no tienes mascotas registradas");
            binding.tvSelectedPetSummarySub.setText("Toca el botón '+ Agregar' para registrar a tu perro o gato");
            activityAdapter.submitList(new ArrayList<>());
            binding.tvEmptyActivities.setVisibility(View.VISIBLE);
            return;
        }

        String savedSelectedId = Store.getInstance().getSelectedPetId();
        Pet match = null;
        for (Pet p : currentPets) {
            if (p.getId() != null && p.getId().equals(savedSelectedId)) {
                match = p;
                break;
            }
        }
        if (match == null) {
            match = currentPets.get(0);
        }
        selectPet(match);
    }

    private void selectPet(Pet pet) {
        this.selectedPet = pet;
        Store.getInstance().setSelectedPetId(pet.getId());
        petAvatarAdapter.submitList(currentPets, pet.getId());

        String age = DateUtils.calculateReadableAge(pet.getBirthDate(), pet.getAgeYears());
        String breed = !pet.getBreed().isBlank() ? pet.getBreed() : ("CAT".equalsIgnoreCase(pet.getSpecies()) ? "Felino" : "Canino");
        binding.tvSelectedPetSummaryTitle.setText(pet.getName() + " • " + breed);
        binding.tvSelectedPetSummarySub.setText(age + " • " + pet.getWeight() + " kg • Vacunas: " + pet.getVaccines().size());

        List<Pet.RoutineRecord> routines = pet.getRoutines();
        activityAdapter.submitList(routines);
        binding.tvEmptyActivities.setVisibility(
                (routines == null || routines.isEmpty()) ? View.VISIBLE : View.GONE
        );
    }

    private void showTipDialog(Tip tip) {
        new AlertDialog.Builder(requireContext())
                .setTitle(tip.getTitle())
                .setMessage(tip.getContent() != null && !tip.getContent().isBlank() ? tip.getContent() : tip.getSummary())
                .setPositiveButton("Cerrar", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
