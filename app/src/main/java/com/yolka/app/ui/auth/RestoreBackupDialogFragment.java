package com.yolka.app.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.yolka.app.databinding.DialogRestoreBackupBinding;
import com.yolka.app.models.ApiPayloads.GenericResponse;
import com.yolka.app.services.BackupClientService;
import com.yolka.app.utils.Api;
import com.yolka.app.utils.DateUtils;

public class RestoreBackupDialogFragment extends DialogFragment {

    public interface OnRestoreDecisionListener {
        void onDecisionFinished(boolean restored);
    }

    private DialogRestoreBackupBinding binding;
    private OnRestoreDecisionListener listener;
    private String lastBackupDate;

    public static RestoreBackupDialogFragment newInstance(String lastBackupDate, OnRestoreDecisionListener listener) {
        RestoreBackupDialogFragment fragment = new RestoreBackupDialogFragment();
        fragment.lastBackupDate = lastBackupDate;
        fragment.listener = listener;
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogRestoreBackupBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (lastBackupDate != null && !lastBackupDate.isBlank()) {
            binding.tvRestoreBackupDescription.setText(
                    "Encontramos una copia cifrada en la nube (" + DateUtils.formatIsoDate(lastBackupDate)
                            + "). ¿Deseas restaurar tus mascotas, expediente clínico y rutinas?"
            );
        }

        binding.btnSkipRestore.setOnClickListener(v -> {
            dismissAllowingStateLoss();
            if (listener != null) listener.onDecisionFinished(false);
        });

        binding.btnConfirmRestore.setOnClickListener(v -> {
            binding.pbRestoreBackup.setVisibility(View.VISIBLE);
            binding.btnConfirmRestore.setEnabled(false);
            binding.btnSkipRestore.setEnabled(false);

            BackupClientService.getInstance().restoreCloudBackup(new Api.ApiCallback<GenericResponse>() {
                @Override
                public void onSuccess(GenericResponse result) {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), "Respaldo restaurado exitosamente", Toast.LENGTH_SHORT).show();
                    dismissAllowingStateLoss();
                    if (listener != null) listener.onDecisionFinished(true);
                }

                @Override
                public void onError(String message, String errorCode) {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                    dismissAllowingStateLoss();
                    if (listener != null) listener.onDecisionFinished(false);
                }
            });
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
