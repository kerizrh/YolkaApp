package com.yolka.app.ui.tips;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.yolka.app.databinding.BottomSheetTipDetailBinding;
import com.yolka.app.models.Tip;

public class TipDetailBottomSheet extends BottomSheetDialogFragment {

    private BottomSheetTipDetailBinding binding;
    private Tip tip;

    public static TipDetailBottomSheet newInstance(Tip tip) {
        TipDetailBottomSheet sheet = new TipDetailBottomSheet();
        sheet.tip = tip;
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = BottomSheetTipDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (tip != null) {
            binding.tvSheetTipCategory.setText(tip.getCategory() + " • " + tip.getSpecies());
            binding.tvSheetTipTitle.setText(tip.getTitle());
            binding.tvSheetTipContent.setText(
                    !tip.getContent().isBlank() ? tip.getContent() : tip.getSummary()
            );
        }
        binding.btnCloseTipSheet.setOnClickListener(v -> dismissAllowingStateLoss());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
