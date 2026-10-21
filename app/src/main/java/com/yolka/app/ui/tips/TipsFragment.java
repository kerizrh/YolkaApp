package com.yolka.app.ui.tips;

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
import com.yolka.app.databinding.FragmentTipsBinding;
import com.yolka.app.models.ApiPayloads.GenericResponse;
import com.yolka.app.models.Tip;
import com.yolka.app.ui.adapters.TipAdapter;
import com.yolka.app.utils.Api;
import com.yolka.app.utils.NetworkMonitor;
import com.yolka.app.utils.Store;

import java.util.ArrayList;
import java.util.List;

public class TipsFragment extends Fragment {

    private FragmentTipsBinding binding;
    private TipAdapter adapter;
    private List<Tip> allTips = new ArrayList<>();
    private String selectedSpecies = "ALL";
    private String selectedCategory = "ALL";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentTipsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new TipAdapter(tip ->
                TipDetailBottomSheet.newInstance(tip).show(getChildFragmentManager(), "TipSheet")
        );
        binding.rvTipsCatalog.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvTipsCatalog.setAdapter(adapter);

        setupFilterListeners();
        loadTips();
    }

    private void setupFilterListeners() {
        binding.btnTipsSpeciesAll.setOnClickListener(v -> selectSpecies("ALL"));
        binding.btnTipsSpeciesDog.setOnClickListener(v -> selectSpecies("DOG"));
        binding.btnTipsSpeciesCat.setOnClickListener(v -> selectSpecies("CAT"));

        binding.chipTipCatAll.setOnClickListener(v -> selectCategory("ALL"));
        binding.chipTipCatFeeding.setOnClickListener(v -> selectCategory("FEEDING"));
        binding.chipTipCatHygiene.setOnClickListener(v -> selectCategory("HYGIENE"));
        binding.chipTipCatExercise.setOnClickListener(v -> selectCategory("EXERCISE"));
        binding.chipTipCatVaccines.setOnClickListener(v -> selectCategory("VACCINATION"));
        binding.chipTipCatFun.setOnClickListener(v -> selectCategory("FUN_FACTS"));
    }

    private void selectSpecies(String species) {
        this.selectedSpecies = species;
        binding.btnTipsSpeciesAll.setBackgroundResource("ALL".equals(species) ? R.drawable.bg_rounded_card : 0);
        binding.btnTipsSpeciesDog.setBackgroundResource("DOG".equals(species) ? R.drawable.bg_rounded_card : 0);
        binding.btnTipsSpeciesCat.setBackgroundResource("CAT".equals(species) ? R.drawable.bg_rounded_card : 0);
        applyLocalFilter();
    }

    private void selectCategory(String category) {
        this.selectedCategory = category;
        binding.chipTipCatAll.setBackgroundResource("ALL".equals(category) ? R.drawable.bg_pill_input : R.drawable.bg_rounded_card);
        binding.chipTipCatFeeding.setBackgroundResource("FEEDING".equals(category) ? R.drawable.bg_pill_input : R.drawable.bg_rounded_card);
        binding.chipTipCatHygiene.setBackgroundResource("HYGIENE".equals(category) ? R.drawable.bg_pill_input : R.drawable.bg_rounded_card);
        binding.chipTipCatExercise.setBackgroundResource("EXERCISE".equals(category) ? R.drawable.bg_pill_input : R.drawable.bg_rounded_card);
        binding.chipTipCatVaccines.setBackgroundResource("VACCINATION".equals(category) ? R.drawable.bg_pill_input : R.drawable.bg_rounded_card);
        binding.chipTipCatFun.setBackgroundResource("FUN_FACTS".equals(category) ? R.drawable.bg_pill_input : R.drawable.bg_rounded_card);
        applyLocalFilter();
    }

    private void loadTips() {
        List<Tip> cached = Store.getInstance().getCachedTips();
        if (!cached.isEmpty()) {
            this.allTips = cached;
            applyLocalFilter();
        }

        if (!NetworkMonitor.getInstance().isOnline()) return;

        Api.getInstance().enqueue(Api.getInstance().getService().getTips(null, null), new Api.ApiCallback<GenericResponse>() {
            @Override
            public void onSuccess(GenericResponse result) {
                if (!isAdded() || binding == null) return;
                if (result.tips != null) {
                    allTips = result.tips;
                    Store.getInstance().storeCachedTips(allTips);
                    applyLocalFilter();
                }
            }

            @Override
            public void onError(String message, String errorCode) {
            }
        });
    }

    private void applyLocalFilter() {
        List<Tip> filtered = new ArrayList<>();
        for (Tip t : allTips) {
            boolean speciesMatch = "ALL".equalsIgnoreCase(selectedSpecies)
                    || "ALL".equalsIgnoreCase(t.getSpecies())
                    || selectedSpecies.equalsIgnoreCase(t.getSpecies());
            boolean catMatch = "ALL".equalsIgnoreCase(selectedCategory)
                    || selectedCategory.equalsIgnoreCase(t.getCategory())
                    || ("VACCINATION".equalsIgnoreCase(selectedCategory) && "DEWORMING".equalsIgnoreCase(t.getCategory()));
            if (speciesMatch && catMatch) {
                filtered.add(t);
            }
        }
        adapter.submitList(filtered);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
