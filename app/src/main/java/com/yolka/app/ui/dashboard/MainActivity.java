package com.yolka.app.ui.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;

import com.yolka.app.R;
import com.yolka.app.context.AuthManager;
import com.yolka.app.context.ThemeManager;
import com.yolka.app.context.UserManager;
import com.yolka.app.databinding.ActivityMainBinding;
import com.yolka.app.databinding.NavHeaderMainBinding;
import com.yolka.app.models.Pet;
import com.yolka.app.ui.auth.LoginActivity;
import com.yolka.app.ui.map.VetMapFragment;
import com.yolka.app.ui.notifications.NotificationsActivity;
import com.yolka.app.ui.pets.PetDetailActivity;
import com.yolka.app.ui.pets.PetsListFragment;
import com.yolka.app.ui.routines.RoutinesFragment;
import com.yolka.app.ui.settings.AppSettingsActivity;
import com.yolka.app.ui.settings.ProfileSettingsActivity;
import com.yolka.app.ui.tips.TipsFragment;
import com.yolka.app.ui.dialogs.PermissionsBottomSheetDialogFragment;
import com.yolka.app.utils.NetworkMonitor;
import com.yolka.app.utils.PermissionUtils;
import com.yolka.app.utils.Store;
import com.yolka.app.utils.SyncQueueManager;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupTopBarAndDrawer();
        setupBottomNavigation();
        observeOfflineAndSyncStatus();

        if (savedInstanceState == null) {
            loadFragment(new DashboardFragment(), "Inicio");
            checkPermissionsOnStartup();
        }
    }

    private void checkPermissionsOnStartup() {
        if (!PermissionUtils.hasAllEssentialPermissions(this) && !Store.getInstance().arePermissionsPrompted()) {
            PermissionsBottomSheetDialogFragment dialog = PermissionsBottomSheetDialogFragment.newInstance();
            dialog.show(getSupportFragmentManager(), "PermissionsBottomSheet");
        }
    }

    private void setupTopBarAndDrawer() {
        binding.ivTopBarMenuLogo.setOnClickListener(v ->
                binding.drawerLayout.openDrawer(GravityCompat.END)
        );

        View headerView = binding.navigationView.getHeaderView(0);
        NavHeaderMainBinding headerBinding = NavHeaderMainBinding.bind(headerView);

        UserManager.getInstance().getUserLiveData().observe(this, user -> {
            if (user != null) {
                headerBinding.tvDrawerUserName.setText(user.getFullName() != null ? user.getFullName() : user.getFirstName());
                headerBinding.tvDrawerUserEmail.setText(user.getEmail() != null ? user.getEmail() : "");
                String first = user.getFirstName();
                String initials = first != null && !first.isEmpty()
                        ? first.substring(0, 1).toUpperCase()
                        : "U";
                binding.tvTopUserInitials.setText(initials);
                headerBinding.tvDrawerUserInitials.setText(initials);
            }
        });

        View.OnClickListener openProfileListener = v -> {
            binding.drawerLayout.closeDrawer(GravityCompat.END);
            startActivity(new Intent(MainActivity.this, ProfileSettingsActivity.class));
        };
        headerBinding.cardDrawerProfile.setOnClickListener(openProfileListener);
        headerBinding.btnDrawerClose.setOnClickListener(v -> binding.drawerLayout.closeDrawer(GravityCompat.END));

        // Al presionar el avatar superior derecho se despliega el sidebar lateral derecho
        binding.cardTopUserAvatar.setOnClickListener(v ->
                binding.drawerLayout.openDrawer(GravityCompat.END)
        );

        binding.btnTopNotifications.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, NotificationsActivity.class))
        );

        binding.navigationView.setNavigationItemSelectedListener(item -> {
            binding.drawerLayout.closeDrawer(GravityCompat.END);
            int id = item.getItemId();
            if (id == R.id.drawer_notifications) {
                startActivity(new Intent(MainActivity.this, NotificationsActivity.class));
            } else if (id == R.id.drawer_app_settings) {
                startActivity(new Intent(MainActivity.this, AppSettingsActivity.class));
            }
            return true;
        });

        binding.btnDrawerLogout.setOnClickListener(v -> {
            binding.drawerLayout.closeDrawer(GravityCompat.END);
            AuthManager.getInstance().logout(() -> {
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (binding.drawerLayout.isDrawerOpen(GravityCompat.END)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.END);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    private void setupBottomNavigation() {
        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_dashboard) {
                loadFragment(new DashboardFragment(), "Inicio");
                return true;
            } else if (id == R.id.nav_routines) {
                loadFragment(new RoutinesFragment(), "Rutinas y Hábitos");
                return true;
            } else if (id == R.id.nav_vets_map) {
                loadFragment(new VetMapFragment(), "Veterinarias Cercanas");
                return true;
            }
            return false;
        });
    }

    private void observeOfflineAndSyncStatus() {
        NetworkMonitor.getInstance().getOnlineState().observe(this, online -> updateOfflineBanner());
        SyncQueueManager.getInstance().getPendingCountLiveData().observe(this, count -> updateOfflineBanner());
    }

    private void updateOfflineBanner() {
        boolean online = NetworkMonitor.getInstance().isOnline();
        int pending = Store.getInstance().getPendingSyncQueue().size();
        if (!online) {
            binding.tvOfflineSyncBanner.setVisibility(View.VISIBLE);
            binding.tvOfflineSyncBanner.setText(pending > 0
                    ? "Modo sin conexión • " + pending + " cambio(s) en cola para sincronizar"
                    : getString(R.string.offline_banner));
        } else if (pending > 0) {
            binding.tvOfflineSyncBanner.setVisibility(View.VISIBLE);
            binding.tvOfflineSyncBanner.setText("Sincronizando " + pending + " cambio(s) con Yolka Cloud...");
            SyncQueueManager.getInstance().flushPendingQueue(syncedCount -> runOnUiThread(() -> {
                binding.tvOfflineSyncBanner.setVisibility(View.GONE);
                if (syncedCount > 0) {
                    Toast.makeText(MainActivity.this, R.string.sync_completed, Toast.LENGTH_SHORT).show();
                }
            }));
        } else {
            binding.tvOfflineSyncBanner.setVisibility(View.GONE);
        }
    }

    public void openFirstOrSelectedPetMedical() {
        List<Pet> pets = Store.getInstance().getCachedPets();
        if (pets.isEmpty()) {
            loadFragment(new PetsListFragment(), "Mis Mascotas");
            return;
        }
        String selectedId = Store.getInstance().getSelectedPetId();
        Pet chosen = pets.get(0);
        for (Pet p : pets) {
            if (p.getId() != null && p.getId().equals(selectedId)) {
                chosen = p;
                break;
            }
        }
        Intent intent = new Intent(this, PetDetailActivity.class);
        intent.putExtra(PetDetailActivity.EXTRA_PET, chosen);
        startActivity(intent);
    }

    public void openRoutinesSection() {
        binding.bottomNav.setSelectedItemId(R.id.nav_routines);
    }

    public void openTipsSection() {
        loadFragment(new TipsFragment(), "Consejos y Guías");
    }

    public void loadFragment(Fragment fragment, String subtitle) {
        binding.tvTopBarSubtitle.setText(subtitle);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }
}
