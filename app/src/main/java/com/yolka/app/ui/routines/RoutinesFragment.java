package com.yolka.app.ui.routines;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.yolka.app.R;
import com.yolka.app.context.UserManager;
import com.yolka.app.databinding.FragmentRoutinesBinding;
import com.yolka.app.models.ApiPayloads.GenericResponse;
import com.yolka.app.models.Pet;
import com.yolka.app.models.User;
import com.yolka.app.ui.adapters.PetAvatarAdapter;
import com.yolka.app.ui.adapters.RoutineAdapter;
import com.yolka.app.utils.Api;
import com.yolka.app.utils.NetworkMonitor;
import com.yolka.app.utils.Store;

import java.util.ArrayList;
import java.util.List;

public class RoutinesFragment extends Fragment {

    private FragmentRoutinesBinding binding;
    private PetAvatarAdapter petAvatarAdapter;
    private RoutineAdapter routineAdapter;
    private List<Pet> pets = new ArrayList<>();
    private Pet selectedPet;
    private String activeFilter = "ALL";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentRoutinesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        petAvatarAdapter = new PetAvatarAdapter(new PetAvatarAdapter.OnPetSelectedListener() {
            @Override
            public void onPetSelected(Pet pet) {
                selectedPet = pet;
                Store.getInstance().setSelectedPetId(pet.getId());
                petAvatarAdapter.submitList(pets, pet.getId());
                renderRoutines();
            }

            @Override
            public void onPetLongClick(Pet pet) {
            }
        });
        binding.rvRoutinesPetsBar.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        );
        binding.rvRoutinesPetsBar.setAdapter(petAvatarAdapter);

        routineAdapter = new RoutineAdapter();
        binding.rvRoutinesList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvRoutinesList.setAdapter(routineAdapter);

        binding.btnOpenNewRoutine.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), RoutineFormActivity.class))
        );

        binding.filterAllRoutines.setOnClickListener(v -> applyCategoryFilter("ALL"));
        binding.filterFeedingRoutines.setOnClickListener(v -> applyCategoryFilter("FEEDING"));
        binding.filterWalkRoutines.setOnClickListener(v -> applyCategoryFilter("WALK"));
        binding.filterHygieneRoutines.setOnClickListener(v -> applyCategoryFilter("GROOMING"));
    }

    @Override
    public void onResume() {
        super.onResume();
        loadPetsAndRoutines();
    }

    private void loadPetsAndRoutines() {
        pets = Store.getInstance().getCachedPets();
        syncSelectionAndRender();

        if (!NetworkMonitor.getInstance().isOnline()) return;

        User user = UserManager.getInstance().getCurrentUser();
        String userId = user != null ? user.getId() : null;
        Api.getInstance().enqueue(Api.getInstance().getService().getPets(userId), new Api.ApiCallback<GenericResponse>() {
            @Override
            public void onSuccess(GenericResponse result) {
                if (!isAdded() || binding == null) return;
                if (result.pets != null) {
                    pets = result.pets;
                    Store.getInstance().storeCachedPets(pets);
                    syncSelectionAndRender();
                }
            }

            @Override
            public void onError(String message, String errorCode) {
            }
        });
    }

    private void syncSelectionAndRender() {
        if (pets.isEmpty()) {
            selectedPet = null;
            petAvatarAdapter.submitList(pets, null);
            routineAdapter.submitList(new ArrayList<>(), activeFilter);
            binding.tvEmptyRoutinesState.setVisibility(View.VISIBLE);
            return;
        }
        String savedId = Store.getInstance().getSelectedPetId();
        Pet found = null;
        for (Pet p : pets) {
            if (p.getId() != null && p.getId().equals(savedId)) {
                found = p;
                break;
            }
        }
        selectedPet = found != null ? found : pets.get(0);
        petAvatarAdapter.submitList(pets, selectedPet.getId());
        renderRoutines();
    }

    private void applyCategoryFilter(String filter) {
        this.activeFilter = filter;
        binding.filterAllRoutines.setBackgroundResource("ALL".equals(filter) ? R.drawable.bg_rounded_card : 0);
        binding.filterFeedingRoutines.setBackgroundResource("FEEDING".equals(filter) ? R.drawable.bg_rounded_card : 0);
        binding.filterWalkRoutines.setBackgroundResource("WALK".equals(filter) ? R.drawable.bg_rounded_card : 0);
        binding.filterHygieneRoutines.setBackgroundResource("GROOMING".equals(filter) ? R.drawable.bg_rounded_card : 0);

        binding.filterAllRoutines.setTextColor(ContextCompat.getColor(requireContext(),
                "ALL".equals(filter) ? R.color.yolka_text_primary : R.color.yolka_text_secondary));
        binding.filterFeedingRoutines.setTextColor(ContextCompat.getColor(requireContext(),
                "FEEDING".equals(filter) ? R.color.yolka_text_primary : R.color.yolka_text_secondary));
        binding.filterWalkRoutines.setTextColor(ContextCompat.getColor(requireContext(),
                "WALK".equals(filter) ? R.color.yolka_text_primary : R.color.yolka_text_secondary));
        binding.filterHygieneRoutines.setTextColor(ContextCompat.getColor(requireContext(),
                "GROOMING".equals(filter) ? R.color.yolka_text_primary : R.color.yolka_text_secondary));

        renderRoutines();
    }

    private void renderRoutines() {
        if (selectedPet == null) {
            routineAdapter.submitList(new ArrayList<>(), activeFilter);
            binding.tvEmptyRoutinesState.setVisibility(View.VISIBLE);
            return;
        }
        routineAdapter.submitList(selectedPet.getRoutines(), activeFilter);
        binding.tvEmptyRoutinesState.setVisibility(routineAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
