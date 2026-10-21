package com.yolka.app.utils;

import com.yolka.app.models.ApiPayloads.*;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface YolkaApiService {

    // Módulo 1: Autenticación
    @POST("auth/register")
    Call<AuthResponse> register(@Body RegisterRequest request);

    @POST("auth/login")
    Call<AuthResponse> login(@Body LoginRequest request);

    @POST("auth/google")
    Call<AuthResponse> googleSignIn(@Body GoogleLoginRequest request);

    @POST("auth/confirm-account")
    Call<GenericResponse> confirmAccount(@Body ConfirmAccountRequest request);

    @POST("auth/request-confirmation")
    Call<GenericResponse> requestConfirmation(@Body EmailRequest request);

    @POST("auth/recover-password")
    Call<GenericResponse> recoverPassword(@Body EmailRequest request);

    @POST("auth/reset-password")
    Call<GenericResponse> resetPassword(@Body ResetPasswordRequest request);

    @POST("auth/refresh-token")
    Call<AuthResponse> refreshToken(@Body RefreshTokenRequest request);

    @POST("auth/logout")
    Call<GenericResponse> logout(@Body LogoutRequest request);

    // Módulo 2: Mascotas
    @GET("pets")
    Call<GenericResponse> getPets(@Query("userId") String userId);

    @POST("pets")
    Call<GenericResponse> createPet(@Body CreatePetRequest request);

    @GET("pets/{id}")
    Call<GenericResponse> getPetById(@Path("id") String id);

    @PUT("pets/{id}")
    Call<GenericResponse> updatePet(@Path("id") String id, @Body CreatePetRequest request);

    @DELETE("pets/{id}")
    Call<GenericResponse> deletePet(@Path("id") String id);

    // Módulo 3, 4 y 5: Salud y Rutinas
    @POST("pets/{id}/vaccines")
    Call<GenericResponse> addVaccine(@Path("id") String petId, @Body VaccineRequest request);

    @POST("pets/{id}/dewormings")
    Call<GenericResponse> addDeworming(@Path("id") String petId, @Body DewormingRequest request);

    @POST("pets/{id}/appointments")
    Call<GenericResponse> addAppointment(@Path("id") String petId, @Body AppointmentRequest request);

    @POST("pets/{id}/routines")
    Call<GenericResponse> addRoutine(@Path("id") String petId, @Body RoutineRequest request);

    @POST("pets/{id}/reminders")
    Call<GenericResponse> addReminder(@Path("id") String petId, @Body ReminderRequest request);

    @PATCH("pets/{id}/reminders/{reminderId}")
    Call<GenericResponse> toggleReminder(
            @Path("id") String petId,
            @Path("reminderId") String reminderId,
            @Query("completed") boolean completed
    );

    // Módulo 6: Veterinarias
    @GET("vets")
    Call<GenericResponse> getVetClinics();

    // Módulo 7: Consejos
    @GET("tips")
    Call<GenericResponse> getTips(@Query("category") String category, @Query("species") String species);

    // Módulo 8: Notificaciones Push
    @GET("notifications")
    Call<GenericResponse> getNotifications(@Query("userId") String userId);

    @POST("notifications")
    Call<GenericResponse> createNotification(@Body CreateNotificationRequest request);

    // Módulo 9 y 10: Usuario, Perfil y Respaldos
    @GET("users/{id}")
    Call<GenericResponse> getUserProfile(@Path("id") String userId);

    @PUT("users/{id}")
    Call<GenericResponse> updateUserProfile(@Path("id") String userId, @Body UpdateProfileRequest request);

    @PUT("users/{id}/password")
    Call<GenericResponse> changePassword(@Path("id") String userId, @Body ChangePasswordRequest request);

    @PUT("users/{id}/fcm-token")
    Call<GenericResponse> updateFcmToken(@Path("id") String userId, @Body FcmTokenRequest request);

    @DELETE("users/{id}")
    Call<GenericResponse> deleteAccount(@Path("id") String userId);

    @POST("backups")
    Call<GenericResponse> createCloudBackup(@Query("userId") String userId);

    @GET("backups/restore")
    Call<GenericResponse> restoreCloudBackup(@Query("userId") String userId);
}
