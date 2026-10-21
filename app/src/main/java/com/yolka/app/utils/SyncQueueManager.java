package com.yolka.app.utils;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.gson.Gson;
import com.yolka.app.models.ApiPayloads.*;
import com.yolka.app.models.Pet;
import com.yolka.app.models.User;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import retrofit2.Response;

/**
 * SyncQueueManager (Opción A — inspirado en SyncQueueRepository.ts de Kafoo, sin SQLite):
 * Encola operaciones realizadas cuando no hay red y las ejecuta en orden FIFO
 * tan pronto como NetworkMonitor detecta que se restableció la conexión.
 */
public class SyncQueueManager {

    public static final String OP_CREATE_PET = "CREATE_PET";
    public static final String OP_UPDATE_PET = "UPDATE_PET";
    public static final String OP_DELETE_PET = "DELETE_PET";
    public static final String OP_ADD_VACCINE = "ADD_VACCINE";
    public static final String OP_ADD_DEWORMING = "ADD_DEWORMING";
    public static final String OP_ADD_APPOINTMENT = "ADD_APPOINTMENT";
    public static final String OP_ADD_ROUTINE = "ADD_ROUTINE";
    public static final String OP_ADD_REMINDER = "ADD_REMINDER";
    public static final String OP_TOGGLE_REMINDER = "TOGGLE_REMINDER";
    public static final String OP_UPDATE_PROFILE = "UPDATE_PROFILE";

    public static class SyncQueueItem implements Serializable {
        public String id;
        public String operation;
        public String targetId;
        public String subTargetId;
        public String payloadJson;
        public int retries;
        public String createdAt;

        public SyncQueueItem() {}

        public SyncQueueItem(String operation, String targetId, String subTargetId, String payloadJson) {
            this.id = UUID.randomUUID().toString();
            this.operation = operation;
            this.targetId = targetId;
            this.subTargetId = subTargetId;
            this.payloadJson = payloadJson;
            this.retries = 0;
            this.createdAt = Instant.now().toString();
        }
    }

    public interface SyncCompleteListener {
        void onSyncFinished(int syncedCount);
    }

    private static volatile SyncQueueManager instance;
    private final Gson gson = new Gson();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean isFlushing = new AtomicBoolean(false);
    private final MutableLiveData<Integer> pendingCountLiveData = new MutableLiveData<>(0);

    private SyncQueueManager() {
        updatePendingCount();
    }

    public static SyncQueueManager getInstance() {
        if (instance == null) {
            synchronized (SyncQueueManager.class) {
                if (instance == null) {
                    instance = new SyncQueueManager();
                }
            }
        }
        return instance;
    }

    public LiveData<Integer> getPendingCountLiveData() {
        return pendingCountLiveData;
    }

    public synchronized void enqueue(String operation, String targetId, String subTargetId, Object payload) {
        List<SyncQueueItem> queue = Store.getInstance().getPendingSyncQueue();
        String json = payload != null ? gson.toJson(payload) : "{}";
        queue.add(new SyncQueueItem(operation, targetId, subTargetId, json));
        Store.getInstance().storePendingSyncQueue(queue);
        updatePendingCount();
    }

    private void updatePendingCount() {
        try {
            pendingCountLiveData.postValue(Store.getInstance().getPendingSyncQueue().size());
        } catch (Exception ignored) {
        }
    }

    public void flushPendingQueue(SyncCompleteListener listener) {
        if (!isFlushing.compareAndSet(false, true)) {
            return;
        }
        executor.execute(() -> {
            int syncedCount = 0;
            try {
                List<SyncQueueItem> queue = Store.getInstance().getPendingSyncQueue();
                if (queue.isEmpty()) {
                    if (listener != null) listener.onSyncFinished(0);
                    return;
                }

                // Mapa de IDs temporales creados offline -> IDs definitivos de YolkaApi
                Map<String, String> tempToServerPetIds = new HashMap<>();
                YolkaApiService api = Api.getInstance().getService();

                Iterator<SyncQueueItem> iterator = queue.iterator();
                while (iterator.hasNext()) {
                    SyncQueueItem item = iterator.next();
                    String resolvedTargetId = tempToServerPetIds.getOrDefault(item.targetId, item.targetId);
                    boolean success = false;

                    try {
                        switch (item.operation) {
                            case OP_CREATE_PET: {
                                CreatePetRequest req = gson.fromJson(item.payloadJson, CreatePetRequest.class);
                                Response<GenericResponse> resp = api.createPet(req).execute();
                                if (resp.isSuccessful() && resp.body() != null && resp.body().pet != null) {
                                    Pet serverPet = resp.body().pet;
                                    if (item.targetId != null) {
                                        tempToServerPetIds.put(item.targetId, serverPet.getId());
                                        Store.getInstance().removeCachedPet(item.targetId);
                                    }
                                    Store.getInstance().upsertCachedPet(serverPet);
                                    success = true;
                                }
                                break;
                            }
                            case OP_UPDATE_PET: {
                                CreatePetRequest req = gson.fromJson(item.payloadJson, CreatePetRequest.class);
                                Response<GenericResponse> resp = api.updatePet(resolvedTargetId, req).execute();
                                if (resp.isSuccessful() && resp.body() != null && resp.body().pet != null) {
                                    Store.getInstance().upsertCachedPet(resp.body().pet);
                                    success = true;
                                }
                                break;
                            }
                            case OP_DELETE_PET: {
                                Response<GenericResponse> resp = api.deletePet(resolvedTargetId).execute();
                                if (resp.isSuccessful() || resp.code() == 404) {
                                    Store.getInstance().removeCachedPet(resolvedTargetId);
                                    success = true;
                                }
                                break;
                            }
                            case OP_ADD_VACCINE: {
                                VaccineRequest req = gson.fromJson(item.payloadJson, VaccineRequest.class);
                                Response<GenericResponse> resp = api.addVaccine(resolvedTargetId, req).execute();
                                if (resp.isSuccessful() && resp.body() != null && resp.body().pet != null) {
                                    Store.getInstance().upsertCachedPet(resp.body().pet);
                                    success = true;
                                }
                                break;
                            }
                            case OP_ADD_DEWORMING: {
                                DewormingRequest req = gson.fromJson(item.payloadJson, DewormingRequest.class);
                                Response<GenericResponse> resp = api.addDeworming(resolvedTargetId, req).execute();
                                if (resp.isSuccessful() && resp.body() != null && resp.body().pet != null) {
                                    Store.getInstance().upsertCachedPet(resp.body().pet);
                                    success = true;
                                }
                                break;
                            }
                            case OP_ADD_APPOINTMENT: {
                                AppointmentRequest req = gson.fromJson(item.payloadJson, AppointmentRequest.class);
                                Response<GenericResponse> resp = api.addAppointment(resolvedTargetId, req).execute();
                                if (resp.isSuccessful() && resp.body() != null && resp.body().pet != null) {
                                    Store.getInstance().upsertCachedPet(resp.body().pet);
                                    success = true;
                                }
                                break;
                            }
                            case OP_ADD_ROUTINE: {
                                RoutineRequest req = gson.fromJson(item.payloadJson, RoutineRequest.class);
                                Response<GenericResponse> resp = api.addRoutine(resolvedTargetId, req).execute();
                                if (resp.isSuccessful() && resp.body() != null && resp.body().pet != null) {
                                    Store.getInstance().upsertCachedPet(resp.body().pet);
                                    success = true;
                                }
                                break;
                            }
                            case OP_ADD_REMINDER: {
                                ReminderRequest req = gson.fromJson(item.payloadJson, ReminderRequest.class);
                                Response<GenericResponse> resp = api.addReminder(resolvedTargetId, req).execute();
                                if (resp.isSuccessful() && resp.body() != null && resp.body().pet != null) {
                                    Store.getInstance().upsertCachedPet(resp.body().pet);
                                    success = true;
                                }
                                break;
                            }
                            case OP_TOGGLE_REMINDER: {
                                boolean completed = Boolean.parseBoolean(item.payloadJson);
                                Response<GenericResponse> resp = api.toggleReminder(resolvedTargetId, item.subTargetId, completed).execute();
                                if (resp.isSuccessful() && resp.body() != null && resp.body().pet != null) {
                                    Store.getInstance().upsertCachedPet(resp.body().pet);
                                    success = true;
                                }
                                break;
                            }
                            case OP_UPDATE_PROFILE: {
                                UpdateProfileRequest req = gson.fromJson(item.payloadJson, UpdateProfileRequest.class);
                                Response<GenericResponse> resp = api.updateUserProfile(resolvedTargetId, req).execute();
                                if (resp.isSuccessful() && resp.body() != null && resp.body().user != null) {
                                    User updated = resp.body().user;
                                    Store.getInstance().storeUser(updated);
                                    success = true;
                                }
                                break;
                            }
                        }
                    } catch (Exception ignored) {
                    }

                    if (success) {
                        iterator.remove();
                        syncedCount++;
                    } else {
                        item.retries += 1;
                        if (item.retries >= 5) {
                            iterator.remove();
                        }
                    }
                }

                Store.getInstance().storePendingSyncQueue(queue);
                updatePendingCount();
            } finally {
                isFlushing.set(false);
                if (listener != null) {
                    listener.onSyncFinished(syncedCount);
                }
            }
        });
    }
}
