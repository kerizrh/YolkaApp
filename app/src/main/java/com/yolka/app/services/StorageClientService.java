package com.yolka.app.services;

import android.content.Context;
import android.net.Uri;
import android.webkit.MimeTypeMap;

import com.yolka.app.models.ApiPayloads.GenericResponse;
import com.yolka.app.utils.Api;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

public class StorageClientService {

    private static volatile StorageClientService instance;

    private StorageClientService() {}

    public static StorageClientService getInstance() {
        if (instance == null) {
            synchronized (StorageClientService.class) {
                if (instance == null) {
                    instance = new StorageClientService();
                }
            }
        }
        return instance;
    }

    public void uploadImage(Context context, Uri uri, Api.ApiCallback<String> callback) {
        try {
            String mimeType = context.getContentResolver().getType(uri);
            if (mimeType == null) {
                mimeType = "image/jpeg";
            }
            String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
            String filename = "upload_" + System.currentTimeMillis() + (extension != null ? "." + extension : ".jpg");

            InputStream is = context.getContentResolver().openInputStream(uri);
            if (is == null) {
                if (callback != null) callback.onError("No se pudo leer la imagen seleccionada", "URI_READ_FAILED");
                return;
            }

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] data = new byte[8192];
            int nRead;
            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();
            is.close();

            byte[] bytes = buffer.toByteArray();
            RequestBody requestBody = RequestBody.create(MediaType.parse(mimeType), bytes);
            MultipartBody.Part part = MultipartBody.Part.createFormData("file", filename, requestBody);

            Api.getInstance().enqueue(
                    Api.getInstance().getService().uploadFile(part),
                    new Api.ApiCallback<GenericResponse>() {
                        @Override
                        public void onSuccess(GenericResponse result) {
                            if (result != null && result.url != null && !result.url.isBlank()) {
                                if (callback != null) callback.onSuccess(result.url);
                            } else {
                                if (callback != null) callback.onError("Respuesta inválida del servidor", "INVALID_RESPONSE");
                            }
                        }

                        @Override
                        public void onError(String message, String errorCode) {
                            if (callback != null) callback.onError(message, errorCode);
                        }
                    }
            );
        } catch (Exception e) {
            if (callback != null) callback.onError("Error procesando imagen: " + e.getMessage(), "IMAGE_PROCESS_ERROR");
        }
    }
}
