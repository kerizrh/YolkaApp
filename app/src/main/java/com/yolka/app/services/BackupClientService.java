package com.yolka.app.services;

import com.yolka.app.context.UserManager;
import com.yolka.app.models.ApiPayloads.BackupInfo;
import com.yolka.app.models.ApiPayloads.GenericResponse;
import com.yolka.app.models.User;
import com.yolka.app.utils.Api;
import com.yolka.app.utils.Store;

import java.time.Instant;

/**
 * BackupClientService:
 * Orquesta la creación y restauración de copias de seguridad cifradas (.v2.ylk AES-256-GCM)
 * almacenadas en el servidor a través de BackupsController.
 */
public class BackupClientService {

    private static volatile BackupClientService instance;

    private BackupClientService() {}

    public static BackupClientService getInstance() {
        if (instance == null) {
            synchronized (BackupClientService.class) {
                if (instance == null) {
                    instance = new BackupClientService();
                }
            }
        }
        return instance;
    }

    public void createCloudBackup(Api.ApiCallback<BackupInfo> callback) {
        User currentUser = UserManager.getInstance().getCurrentUser();
        String userId = currentUser != null ? currentUser.getId() : null;

        Api.getInstance().enqueue(
                Api.getInstance().getService().createCloudBackup(userId),
                new Api.ApiCallback<GenericResponse>() {
                    @Override
                    public void onSuccess(GenericResponse result) {
                        String now = result.backupInfo != null && result.backupInfo.updatedAt != null
                                ? result.backupInfo.updatedAt
                                : Instant.now().toString();
                        Store.getInstance().setLastBackupAt(now);
                        if (currentUser != null) {
                            currentUser.setHasDeviceBackup(true);
                            currentUser.setLastDeviceBackupAt(now);
                            UserManager.getInstance().setUser(currentUser);
                        }
                        if (callback != null) callback.onSuccess(result.backupInfo);
                    }

                    @Override
                    public void onError(String message, String errorCode) {
                        if (callback != null) callback.onError(message, errorCode);
                    }
                }
        );
    }

    public void restoreCloudBackup(Api.ApiCallback<GenericResponse> callback) {
        User currentUser = UserManager.getInstance().getCurrentUser();
        String userId = currentUser != null ? currentUser.getId() : null;

        Api.getInstance().enqueue(
                Api.getInstance().getService().restoreCloudBackup(userId),
                new Api.ApiCallback<GenericResponse>() {
                    @Override
                    public void onSuccess(GenericResponse result) {
                        if (result.backupInfo != null && result.backupInfo.updatedAt != null) {
                            Store.getInstance().setLastBackupAt(result.backupInfo.updatedAt);
                        }
                        if (callback != null) callback.onSuccess(result);
                    }

                    @Override
                    public void onError(String message, String errorCode) {
                        if (callback != null) callback.onError(message, errorCode);
                    }
                }
        );
    }
}
