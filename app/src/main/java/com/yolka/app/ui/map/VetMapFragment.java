package com.yolka.app.ui.map;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.yolka.app.databinding.FragmentVetMapBinding;
import com.yolka.app.models.ApiPayloads.GenericResponse;
import com.yolka.app.models.VetClinic;
import com.yolka.app.ui.adapters.VetClinicAdapter;
import com.yolka.app.utils.Api;
import com.yolka.app.utils.NetworkMonitor;
import com.yolka.app.utils.PermissionUtils;
import com.yolka.app.utils.Store;

import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Marker;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class VetMapFragment extends Fragment {

    // Coordenadas base por defecto (Colima / Villa de Álvarez — donde DataSeeder tiene las 6 clínicas)
    private static final double DEFAULT_LAT = 19.2499;
    private static final double DEFAULT_LNG = -103.7271;

    private FragmentVetMapBinding binding;
    private VetClinicAdapter adapter;
    private FusedLocationProviderClient fusedLocationClient;
    private double userLat = DEFAULT_LAT;
    private double userLng = DEFAULT_LNG;
    private List<VetClinic> clinics = new ArrayList<>();

    private final ActivityResultLauncher<String[]> locationPermLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> {
                boolean fineGranted = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION));
                boolean coarseGranted = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_COARSE_LOCATION));
                if (fineGranted || coarseGranted) {
                    fetchDeviceLocation();
                } else if (isAdded() && getContext() != null) {
                    Toast.makeText(requireContext(), "Permiso de ubicación denegado. Se usará la ubicación base.", Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentVetMapBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        setupOsmMap();
        setupRecycler();

        binding.btnCenterMyLocation.setOnClickListener(v -> {
            requestOrFetchLocation();
            binding.osmMapView.getController().animateTo(new GeoPoint(userLat, userLng));
            binding.osmMapView.getController().setZoom(14.5);
        });

        binding.btnSortVets.setOnClickListener(v -> {
            sortClinicsByDistance();
            adapter.submitList(clinics);
            Toast.makeText(requireContext(), "Ordenadas por cercanía a tu ubicación", Toast.LENGTH_SHORT).show();
        });

        requestOrFetchLocation();
        loadVetClinics();
    }

    private void setupOsmMap() {
        binding.osmMapView.setTileSource(TileSourceFactory.MAPNIK);
        binding.osmMapView.setMultiTouchControls(true);
        binding.osmMapView.getController().setZoom(13.8);
        binding.osmMapView.getController().setCenter(new GeoPoint(userLat, userLng));
    }

    private void setupRecycler() {
        adapter = new VetClinicAdapter(new VetClinicAdapter.OnVetActionListener() {
            @Override
            public void onFocusOnMap(VetClinic clinic) {
                GeoPoint point = new GeoPoint(clinic.getLatitude(), clinic.getLongitude());
                binding.osmMapView.getController().animateTo(point);
                binding.osmMapView.getController().setZoom(16.5);
            }

            @Override
            public void onOpenDirections(VetClinic clinic) {
                String uri = "geo:" + clinic.getLatitude() + "," + clinic.getLongitude()
                        + "?q=" + clinic.getLatitude() + "," + clinic.getLongitude()
                        + "(" + Uri.encode(clinic.getName()) + ")";
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                try {
                    startActivity(mapIntent);
                } catch (Exception e) {
                    String webUrl = "https://www.openstreetmap.org/?mlat="
                            + clinic.getLatitude() + "&mlon=" + clinic.getLongitude() + "#map=17/"
                            + clinic.getLatitude() + "/" + clinic.getLongitude();
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)));
                }
            }
        });
        binding.rvVetClinics.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvVetClinics.setAdapter(adapter);
    }

    private void requestOrFetchLocation() {
        if (PermissionUtils.hasLocationPermission(requireContext())) {
            fetchDeviceLocation();
        } else {
            locationPermLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private void fetchDeviceLocation() {
        try {
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (!isAdded() || binding == null || location == null) return;
                // Si el emulador está en Mountain View (37.42) lejos de las clínicas semilla de Colima,
                // calculamos distancias desde la ubicación real o desde el centro de la ciudad si supera 100 km
                float[] results = new float[1];
                Location.distanceBetween(location.getLatitude(), location.getLongitude(), DEFAULT_LAT, DEFAULT_LNG, results);
                if (results[0] < 100_000) {
                    userLat = location.getLatitude();
                    userLng = location.getLongitude();
                }
                sortClinicsByDistance();
                renderMarkersAndList();
            });
        } catch (SecurityException ignored) {
        }
    }

    private void loadVetClinics() {
        List<VetClinic> cached = Store.getInstance().getCachedVets();
        if (!cached.isEmpty()) {
            this.clinics = cached;
            sortClinicsByDistance();
            renderMarkersAndList();
        }

        if (!NetworkMonitor.getInstance().isOnline()) return;

        Api.getInstance().enqueue(Api.getInstance().getService().getVetClinics(), new Api.ApiCallback<GenericResponse>() {
            @Override
            public void onSuccess(GenericResponse result) {
                if (!isAdded() || binding == null) return;
                if (result.vets != null) {
                    clinics = result.vets;
                    Store.getInstance().storeCachedVets(clinics);
                    sortClinicsByDistance();
                    renderMarkersAndList();
                }
            }

            @Override
            public void onError(String message, String errorCode) {
            }
        });
    }

    private void sortClinicsByDistance() {
        for (VetClinic c : clinics) {
            float[] dist = new float[1];
            Location.distanceBetween(userLat, userLng, c.getLatitude(), c.getLongitude(), dist);
            c.setDistanceMeters(dist[0]);
        }
        clinics.sort(Comparator.comparingDouble(VetClinic::getDistanceMeters));
    }

    private void renderMarkersAndList() {
        if (binding == null) return;
        binding.tvMapRadiusBadge.setText("● Radio: 5 km • " + clinics.size() + " Clínicas");
        binding.tvNearbyVetsCount.setText(clinics.size() + " cercanas");
        adapter.submitList(clinics);

        binding.osmMapView.getOverlays().clear();

        Marker userMarker = new Marker(binding.osmMapView);
        userMarker.setPosition(new GeoPoint(userLat, userLng));
        userMarker.setTitle("Tu ubicación");
        userMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        binding.osmMapView.getOverlays().add(userMarker);

        for (VetClinic c : clinics) {
            Marker m = new Marker(binding.osmMapView);
            m.setPosition(new GeoPoint(c.getLatitude(), c.getLongitude()));
            m.setTitle(c.getName());
            m.setSnippet(c.getAddress() + "\n" + c.getOpeningHours() + " • Tel: " + c.getPhone());
            m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            binding.osmMapView.getOverlays().add(m);
        }

        binding.osmMapView.invalidate();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null) binding.osmMapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (binding != null) binding.osmMapView.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
