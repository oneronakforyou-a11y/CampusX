package com.example.campusx.ui.feed;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campusx.R;
import com.example.campusx.data.FirebaseRepository;
import com.example.campusx.data.MockDataRepository;
import com.example.campusx.model.Item;
import com.example.campusx.model.ItemCategory;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class FeedFragment extends Fragment {
    private static final String TAG = "FeedFragment";
    
    private RecyclerView recyclerView;
    private ItemAdapter adapter;
    private ChipGroup categoryChipGroup;
    private View searchPill;
    private ProgressBar progressBar;
    private TextView emptyText;
    private FirebaseRepository firebaseRepo;
    private MockDataRepository mockRepo;
    private ItemCategory selectedCategory = ItemCategory.ALL_ITEMS;
    private boolean useFirebase = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_feed, container, false);
        
        firebaseRepo = FirebaseRepository.getInstance();
        mockRepo = MockDataRepository.getInstance();
        
        initViews(view);
        setupRecyclerView();
        setupSearchShortcut();
        setupCategoryFilter();
        loadItems();
        
        return view;
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.items_recycler_view);
        categoryChipGroup = view.findViewById(R.id.category_chip_group);
        searchPill = view.findViewById(R.id.search_pill);
        progressBar = view.findViewById(R.id.progress_bar);
        emptyText = view.findViewById(R.id.empty_text);
    }

    private void setupRecyclerView() {
        adapter = new ItemAdapter();
        adapter.setOnItemClickListener(item -> {
            // Navigate to item detail
            Intent intent = new Intent(getContext(), com.example.campusx.ui.detail.ItemDetailActivity.class);
            intent.putExtra(com.example.campusx.ui.detail.ItemDetailActivity.EXTRA_ITEM_ID, item.getId());
            startActivity(intent);
        });
        
        int spanCount = getResources().getConfiguration().screenWidthDp >= 600 ? 3 : 2;
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), spanCount));
        recyclerView.setAdapter(adapter);
    }

    private void setupSearchShortcut() {
        searchPill.setOnClickListener(v -> {
            if (getActivity() == null) {
                return;
            }

            BottomNavigationView bottomNavigation = getActivity().findViewById(R.id.bottom_navigation);
            if (bottomNavigation != null) {
                bottomNavigation.setSelectedItemId(R.id.nav_search);
            }
        });
    }

    private void setupCategoryFilter() {
        categoryChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                int checkedId = checkedIds.get(0);
                
                if (checkedId == R.id.chip_all) {
                    selectedCategory = ItemCategory.ALL_ITEMS;
                } else if (checkedId == R.id.chip_electronics) {
                    selectedCategory = ItemCategory.ELECTRONICS;
                } else if (checkedId == R.id.chip_study_gear) {
                    selectedCategory = ItemCategory.STUDY_GEAR;
                } else if (checkedId == R.id.chip_lifestyle) {
                    selectedCategory = ItemCategory.LIFESTYLE;
                }
                
                loadItems();
            }
        });
    }

    private void loadItems() {
        showLoading(true);
        
        // Try Firebase first, fallback to mock data if Firebase fails
        if (useFirebase && firebaseRepo.getCurrentFirebaseUser() != null) {
            loadItemsFromFirebase();
        } else {
            loadItemsFromMock();
        }
    }
    
    private void loadItemsFromFirebase() {
        if (selectedCategory == ItemCategory.ALL_ITEMS) {
            firebaseRepo.getAllItems()
                    .addOnSuccessListener(querySnapshot -> {
                        List<Item> items = new ArrayList<>();
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            Item item = documentToItem(doc);
                            if (item != null) {
                                items.add(item);
                            }
                        }
                        
                        if (items.isEmpty()) {
                            // No items in Firebase, use mock data
                            Log.d(TAG, "No items in Firebase, using mock data");
                            loadItemsFromMock();
                        } else {
                            showLoading(false);
                            adapter.setItems(items);
                            showEmptyState(items.isEmpty());
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error loading items from Firebase", e);
                        Toast.makeText(getContext(), "Using offline data", Toast.LENGTH_SHORT).show();
                        loadItemsFromMock();
                    });
        } else {
            firebaseRepo.getItemsByCategory(selectedCategory.name())
                    .addOnSuccessListener(querySnapshot -> {
                        List<Item> items = new ArrayList<>();
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            Item item = documentToItem(doc);
                            if (item != null) {
                                items.add(item);
                            }
                        }
                        
                        if (items.isEmpty()) {
                            // No items in this category, use mock data
                            loadItemsFromMock();
                        } else {
                            showLoading(false);
                            adapter.setItems(items);
                            showEmptyState(items.isEmpty());
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error loading items by category from Firebase", e);
                        loadItemsFromMock();
                    });
        }
    }
    
    private void loadItemsFromMock() {
        List<Item> items = mockRepo.getItemsByCategory(selectedCategory);
        showLoading(false);
        adapter.setItems(items);
        showEmptyState(items.isEmpty());
    }
    
    private Item documentToItem(DocumentSnapshot doc) {
        try {
            String id = doc.getString("id");
            String ownerId = doc.getString("ownerId");
            String ownerName = doc.getString("ownerName");
            Double ownerRating = doc.getDouble("ownerRating");
            String title = doc.getString("title");
            String description = doc.getString("description");
            String categoryStr = doc.getString("category");
            Double pricePerDay = doc.getDouble("pricePerDay");
            List<String> images = (List<String>) doc.get("images");
            String pickupLocation = doc.getString("pickupLocation");
            Boolean isAvailable = doc.getBoolean("isAvailable");
            Long createdAt = doc.getLong("createdAt");
            Long updatedAt = doc.getLong("updatedAt");
            
            ItemCategory category = ItemCategory.valueOf(categoryStr);
            
            return new Item(
                    id, ownerId, ownerName, ownerRating != null ? ownerRating : 0.0,
                    title, description, category, pricePerDay != null ? pricePerDay : 0.0,
                    images, pickupLocation, isAvailable != null ? isAvailable : true,
                    createdAt != null ? createdAt : System.currentTimeMillis(),
                    updatedAt != null ? updatedAt : System.currentTimeMillis()
            );
        } catch (Exception e) {
            Log.e(TAG, "Error converting document to Item", e);
            return null;
        }
    }
    
    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
    }
    
    private void showEmptyState(boolean empty) {
        if (emptyText != null) {
            emptyText.setVisibility(empty ? View.VISIBLE : View.GONE);
        }
        recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
    }
}
