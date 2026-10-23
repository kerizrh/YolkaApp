package com.yolka.app.utils;

import com.google.gson.Gson;
import com.yolka.app.BuildConfig;
import com.yolka.app.models.ApiPayloads.AuthResponse;
import com.yolka.app.models.ApiPayloads.GenericResponse;
import com.yolka.app.models.ApiPayloads.RefreshTokenRequest;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Api Singleton:
 * - Inyecta en cada petición los headers:
 *   x-token: JWT leído desde Store.getUserToken()
 *   x-client-type: mobile
 * - Refresca automáticamente el token en caso de expiración (401).
 */
public class Api {

    public interface ApiCallback<T> {
        void onSuccess(T result);
        void onError(String message, String errorCode);
    }

    private static volatile Api instance;
    private final YolkaApiService service;
    private final Gson gson = new Gson();

    private Api() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .addInterceptor(chain -> {
                    Request original = chain.request();
                    Request.Builder builder = original.newBuilder()
                            .header("x-client-type", "mobile")
                            .header("Accept", "application/json");

                    String token = Store.getInstance().getUserToken();
                    if (token != null && !token.isBlank()) {
                        builder.header("x-token", token);
                    }
                    return chain.proceed(builder.build());
                })
                .authenticator((route, response) -> {
                    if (response.request().url().encodedPath().contains("/auth/")) {
                        return null;
                    }
                    String refreshToken = Store.getInstance().getRefreshToken();
                    if (refreshToken == null || refreshToken.isBlank()) {
                        return null;
                    }
                    try {
                        RefreshTokenRequest req = new RefreshTokenRequest(
                                refreshToken,
                                Store.getInstance().getDeviceId()
                        );
                        Response<AuthResponse> refreshResp = getService().refreshToken(req).execute();
                        if (refreshResp.isSuccessful() && refreshResp.body() != null && refreshResp.body().token != null) {
                            String newToken = refreshResp.body().token;
                            Store.getInstance().storeUserToken(newToken);
                            if (refreshResp.body().refreshToken != null) {
                                Store.getInstance().storeRefreshToken(refreshResp.body().refreshToken);
                            }
                            return response.request().newBuilder()
                                    .header("x-token", newToken)
                                    .header("x-client-type", "mobile")
                                    .build();
                        }
                    } catch (Exception ignored) {
                    }
                    return null;
                })
                .addInterceptor(logging)
                .build();

        String baseUrl = BuildConfig.BASE_URL;
        if (!baseUrl.endsWith("/")) {
            baseUrl = baseUrl + "/";
        }

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        this.service = retrofit.create(YolkaApiService.class);
    }

    public static Api getInstance() {
        if (instance == null) {
            synchronized (Api.class) {
                if (instance == null) {
                    instance = new Api();
                }
            }
        }
        return instance;
    }

    public YolkaApiService getService() {
        return service;
    }

    public <T> void enqueue(Call<T> call, ApiCallback<T> callback) {
        call.enqueue(new Callback<T>() {
            @Override
            public void onResponse(Call<T> call, Response<T> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    String errMsg = "Error del servidor (" + response.code() + ")";
                    String errCode = "HTTP_" + response.code();
                    try {
                        if (response.errorBody() != null) {
                            String errJson = response.errorBody().string();
                            GenericResponse parsed = gson.fromJson(errJson, GenericResponse.class);
                            if (parsed != null) {
                                if (parsed.message != null && !parsed.message.isBlank()) {
                                    errMsg = parsed.message;
                                } else if (parsed.error != null && !parsed.error.isBlank()) {
                                    errMsg = parsed.error;
                                }
                                if (parsed.code != null && !parsed.code.isBlank()) {
                                    errCode = parsed.code;
                                }
                            }
                        }
                    } catch (Exception ignored) {
                    }
                    callback.onError(errMsg, errCode);
                }
            }

            @Override
            public void onFailure(Call<T> call, Throwable t) {
                callback.onError(
                        "Sin conexión con el servidor Yolka. Verifica tu red.",
                        "NETWORK_ERROR"
                );
            }
        });
    }
}
