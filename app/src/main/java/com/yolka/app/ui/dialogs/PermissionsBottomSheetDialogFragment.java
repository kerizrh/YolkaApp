package com.yolka.app.ui.dialogs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.yolka.app.databinding.BottomSheetPermissionsBinding;
import com.yolka.app.utils.PermissionUtils;
import com.yolka.app.utils.Store;

import java.util.Map;

public class PermissionsBottomSheetDialogFragment extends BottomSheetDialogFragment {

    public interface OnPermissionsResolvedListener {
        void onPermissionsResolved();
    }

    private BottomSheetPermissionsBinding binding;
    private OnPermissionsResolvedListener listener;

    private final ActivityResultLauncher<String[]> permissionsLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            (Map<String, Boolean> result) -> {
                Store.getInstance().setPermissionsPrompted(true);
                boolean anyGranted = false;
                for (Boolean granted : result.values()) {
                    if (Boolean.TRUE.equals(granted)) {
                        anyGranted = true;
                        break;
                    }
                }
                if (anyGranted && getContext() != null) {
                    Toast.makeText(requireContext(), "Permisos configurados para Yolka", Toast.LENGTH_SHORT).show();
                }
                if (listener != null) {
                    listener.onPermissionsResolved();
                }
                dismissAllowingStateLoss();
            }
    );

    public static PermissionsBottomSheetDialogFragment newInstance() {
        return new PermissionsBottomSheetDialogFragment();
    }

    public void setOnPermissionsResolvedListener(OnPermissionsResolvedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = BottomSheetPermissionsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnGrantPermissions.setOnClickListener(v -> {
            String[] missing = PermissionUtils.getMissingPermissions(requireContext());
            if (missing.length > 0) {
                permissionsLauncher.launch(missing);
            } else {
                Store.getInstance().setPermissionsPrompted(true);
                dismissAllowingStateLoss();
            }
        });

        binding.btnSkipPermissions.setOnClickListener(v -> {
            Store.getInstance().setPermissionsPrompted(true);
            dismissAllowingStateLoss();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
