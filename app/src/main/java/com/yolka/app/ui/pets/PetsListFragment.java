package com.yolka.app.ui.pets;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.yolka.app.context.UserManager;
import com.yolka.app.databinding.FragmentPetsListBinding;
import com.yolka.app.models.ApiPayloads.GenericResponse;
import com.yolka.app.models.Pet;
import com.yolka.app.models.User;
import com.yolka.app.ui.adapters.PetCardAdapter;
import com.yolka.app.utils.Api;
import com.yolka.app.utils.NetworkMonitor;
import com.yolka.app.utils.Store;

import java.util.List;

public class PetsListFragment extends Fragment {

    private FragmentPetsListBinding binding;
    private PetCardAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentPetsListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new PetCardAdapter(new PetCardAdapter.OnPetCardActionListener() {
            @Override
            public void onOpenClinicalRecord(Pet pet) {
                Store.getInstance().setSelectedPetId(pet.getId());
                Intent intent = new Intent(requireContext(), PetDetailActivity.class);
                intent.putExtra(PetDetailActivity.EXTRA_PET, pet);
                startActivity(intent);
            }

            @Override
            public void onEditPet(Pet pet) {
                Intent intent = new Intent(requireContext(), PetFormActivity.class);
                intent.putExtra(PetFormActivity.EXTRA_PET, pet);
                startActivity(intent);
            }
        });

        binding.rvPetsFullList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvPetsFullList.setAdapter(adapter);

        binding.btnAddNewPetFromList.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), PetFormActivity.class))
        );
    }

    @Override
    public void onResume() {
        super.onResume();
        loadPets();
    }

    private void loadPets() {
        List<Pet> cached = Store.getInstance().getCachedPets();
        adapter.submitList(cached);

        if (!NetworkMonitor.getInstance().isOnline()) {
            return;
        }

        User user = UserManager.getInstance().getCurrentUser();
        String userId = user != null ? user.getId() : null;
        Api.getInstance().enqueue(Api.getInstance().getService().getPets(userId), new Api.ApiCallback<GenericResponse>() {
            @Override
            public void onSuccess(GenericResponse result) {
                if (!isAdded() || binding == null) return;
                if (result.pets != null) {
                    Store.getInstance().storeCachedPets(result.pets);
                    adapter.submitList(result.pets);
                }
            }

            @Override
            public void onError(String message, String errorCode) {
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
