package com.example.campusx.ui.main;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.campusx.R;
import com.example.campusx.data.DatabasePopulator;
import com.example.campusx.data.FirebaseRepository;
import com.example.campusx.ui.SystemBarsHelper;
import com.example.campusx.ui.feed.FeedFragment;
import com.example.campusx.ui.profile.ProfileFragment;
import com.example.campusx.ui.search.SearchFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private BottomNavigationView bottomNavigation;
    private FirebaseRepository firebaseRepo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            setContentView(R.layout.activity_main);
            SystemBarsHelper.applySystemBarPadding(findViewById(R.id.main_root));

            // Hide action bar
            if (getSupportActionBar() != null) {
                getSupportActionBar().hide();
            }

            // Initialize Firebase
            firebaseRepo = FirebaseRepository.getInstance();
            
            // Auto-populate database on first launch
            populateDatabaseIfNeeded();

            bottomNavigation = findViewById(R.id.bottom_navigation);
            
            if (bottomNavigation == null) {
                Log.e(TAG, "Bottom navigation is null!");
                Toast.makeText(this, "Error loading navigation", Toast.LENGTH_SHORT).show();
                return;
            }
            
            setupBottomNavigation();

            // Load default fragment
            if (savedInstanceState == null) {
                handleInitialDestination();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * Automatically populate Firestore database with sample items on first launch
     */
    private void populateDatabaseIfNeeded() {
        if (firebaseRepo.getCurrentFirebaseUser() != null) {
            Log.d(TAG, "User authenticated, checking database population...");
            DatabasePopulator populator = new DatabasePopulator(firebaseRepo);
            populator.populateIfNeeded(this);
        } else {
            Log.d(TAG, "User not authenticated, skipping database population");
        }
    }

    private void setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            try {
                Fragment fragment = null;
                int itemId = item.getItemId();

                if (itemId == R.id.nav_feed) {
                    fragment = new FeedFragment();
                } else if (itemId == R.id.nav_search) {
                    fragment = new SearchFragment();
                } else if (itemId == R.id.nav_rentals) {
                    fragment = new com.example.campusx.ui.rentals.MyRentalsFragment();
                } else if (itemId == R.id.nav_profile) {
                    fragment = new ProfileFragment();
                }

                return loadFragment(fragment);
            } catch (Exception e) {
                Log.e(TAG, "Error in navigation: " + e.getMessage(), e);
                Toast.makeText(MainActivity.this, "Navigation error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                return false;
            }
        });
    }

    private void handleInitialDestination() {
        String destination = getIntent().getStringExtra("navigate_to");
        if ("rentals".equals(destination)) {
            bottomNavigation.setSelectedItemId(R.id.nav_rentals);
        } else {
            loadFragment(new FeedFragment());
        }
    }

    @Override
    protected void onNewIntent(android.content.Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (bottomNavigation != null) {
            handleInitialDestination();
        }
    }

    private boolean loadFragment(Fragment fragment) {
        try {
            if (fragment != null) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .commit();
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading fragment: " + e.getMessage(), e);
            Toast.makeText(this, "Error loading screen: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        return false;
    }
}
