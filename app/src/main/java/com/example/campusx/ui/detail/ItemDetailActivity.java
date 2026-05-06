package com.example.campusx.ui.detail;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.example.campusx.R;
import com.example.campusx.data.FirebaseRepository;
import com.example.campusx.data.MockDataRepository;
import com.example.campusx.model.Booking;
import com.example.campusx.model.BookingStatus;
import com.example.campusx.model.Item;
import com.example.campusx.model.ItemCategory;
import com.example.campusx.ui.SystemBarsHelper;
import com.example.campusx.ui.booking.BookingConfirmationActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;

public class ItemDetailActivity extends AppCompatActivity {
    private static final String TAG = "ItemDetailActivity";
    public static final String EXTRA_ITEM_ID = "item_id";
    
    private ViewPager2 imageViewPager;
    private TextView itemTitle, itemPrice, itemDescription;
    private TextView ownerName, ownerRating, pickupLocation;
    private TextInputEditText startDateInput, endDateInput;
    private TextView totalPriceText;
    private MaterialButton bookButton;
    private MaterialToolbar toolbar;
    private ProgressBar progressBar;
    
    private Item item;
    private FirebaseRepository firebaseRepo;
    private MockDataRepository mockRepo;
    private Calendar startDate, endDate;
    private SimpleDateFormat dateFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_detail);
        SystemBarsHelper.applySystemBarPadding(findViewById(R.id.item_detail_root));

        firebaseRepo = FirebaseRepository.getInstance();
        mockRepo = MockDataRepository.getInstance();
        dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

        String itemId = getIntent().getStringExtra(EXTRA_ITEM_ID);
        if (itemId == null) {
            finish();
            return;
        }

        initViews();
        setupToolbar();
        loadItem(itemId);
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        imageViewPager = findViewById(R.id.image_viewpager);
        itemTitle = findViewById(R.id.item_title);
        itemPrice = findViewById(R.id.item_price);
        itemDescription = findViewById(R.id.item_description);
        ownerName = findViewById(R.id.owner_name);
        ownerRating = findViewById(R.id.owner_rating);
        pickupLocation = findViewById(R.id.pickup_location);
        startDateInput = findViewById(R.id.start_date_input);
        endDateInput = findViewById(R.id.end_date_input);
        totalPriceText = findViewById(R.id.total_price_text);
        bookButton = findViewById(R.id.book_button);
        progressBar = findViewById(R.id.progress_bar);
    }
    
    private void loadItem(String itemId) {
        showLoading(true);
        
        // Try Firebase first
        if (firebaseRepo.getCurrentFirebaseUser() != null) {
            firebaseRepo.getItem(itemId)
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            item = documentToItem(documentSnapshot);
                            if (item != null) {
                                showLoading(false);
                                populateData();
                                setupDatePickers();
                                setupBookButton();
                            } else {
                                loadItemFromMock(itemId);
                            }
                        } else {
                            loadItemFromMock(itemId);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error loading item from Firebase", e);
                        loadItemFromMock(itemId);
                    });
        } else {
            loadItemFromMock(itemId);
        }
    }
    
    private void loadItemFromMock(String itemId) {
        item = mockRepo.getItemById(itemId);
        showLoading(false);
        
        if (item == null) {
            Toast.makeText(this, "Item not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        populateData();
        setupDatePickers();
        setupBookButton();
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
        if (show) {
            itemTitle.setVisibility(View.GONE);
            itemPrice.setVisibility(View.GONE);
            itemDescription.setVisibility(View.GONE);
        } else {
            itemTitle.setVisibility(View.VISIBLE);
            itemPrice.setVisibility(View.VISIBLE);
            itemDescription.setVisibility(View.VISIBLE);
        }
    }

    private void setupToolbar() {
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void populateData() {
        itemTitle.setText(item.getTitle());
        itemPrice.setText(getString(R.string.per_day, item.getPricePerDay()));
        itemDescription.setText(item.getDescription());
        ownerName.setText(item.getOwnerName());
        ownerRating.setText(String.format(Locale.getDefault(), "%.1f (%d)", item.getOwnerRating(),
                (int)(item.getOwnerRating() * 2)));
        pickupLocation.setText(item.getPickupLocation());

        List<String> images = item.getImages() != null && !item.getImages().isEmpty()
                ? item.getImages()
                : Collections.singletonList("");
        imageViewPager.setAdapter(new ImagePagerAdapter(images));
    }

    private void setupDatePickers() {
        startDateInput.setOnClickListener(v -> showStartDatePicker());
        endDateInput.setOnClickListener(v -> showEndDatePicker());
    }

    private void showStartDatePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    startDate = Calendar.getInstance();
                    startDate.set(year, month, dayOfMonth);
                    startDateInput.setText(dateFormat.format(startDate.getTime()));
                    updateTotalPrice();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void showEndDatePicker() {
        if (startDate == null) {
            Toast.makeText(this, "Please select start date first", Toast.LENGTH_SHORT).show();
            return;
        }

        Calendar calendar = (Calendar) startDate.clone();
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    endDate = Calendar.getInstance();
                    endDate.set(year, month, dayOfMonth);
                    endDateInput.setText(dateFormat.format(endDate.getTime()));
                    updateTotalPrice();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.getDatePicker().setMinDate(calendar.getTimeInMillis());
        datePickerDialog.show();
    }

    private void updateTotalPrice() {
        if (startDate != null && endDate != null) {
            long diffInMillis = endDate.getTimeInMillis() - startDate.getTimeInMillis();
            int days = (int) (diffInMillis / (1000 * 60 * 60 * 24)) + 1;
            double totalPrice = days * item.getPricePerDay();
            
            totalPriceText.setText(getString(R.string.total_price, totalPrice));
            totalPriceText.setVisibility(View.VISIBLE);
            bookButton.setText(getString(R.string.book_now, totalPrice));
        }
    }

    private void setupBookButton() {
        bookButton.setOnClickListener(v -> {
            if (startDate == null || endDate == null) {
                Toast.makeText(this, "Please select dates", Toast.LENGTH_SHORT).show();
                return;
            }
            createBooking();
        });
    }

    private void createBooking() {
        // Generate OTP
        String otp = String.format(Locale.getDefault(), "%06d", new Random().nextInt(999999));
        
        // Calculate total price
        long diffInMillis = endDate.getTimeInMillis() - startDate.getTimeInMillis();
        int days = (int) (diffInMillis / (1000 * 60 * 60 * 24)) + 1;
        double totalPrice = days * item.getPricePerDay();

        // Get current user ID
        String currentUserId = firebaseRepo.getCurrentUserId();
        if (currentUserId == null) {
            currentUserId = mockRepo.getCurrentUser().getId();
        }
        
        String currentUserName = mockRepo.getCurrentUser().getName();
        if (firebaseRepo.getCurrentFirebaseUser() != null && firebaseRepo.getCurrentFirebaseUser().getEmail() != null) {
            String email = firebaseRepo.getCurrentFirebaseUser().getEmail();
            currentUserName = email.contains("@") ? email.substring(0, email.indexOf("@")) : email;
        }

        // Create booking
        Booking booking = new Booking(
                UUID.randomUUID().toString(),
                item.getId(),
                item.getTitle(),
                getFirstImageUrl(),
                currentUserId,
                currentUserName,
                item.getOwnerId(),
                item.getOwnerName(),
                startDate.getTimeInMillis(),
                endDate.getTimeInMillis(),
                totalPrice,
                BookingStatus.PENDING,
                otp,
                item.getPickupLocation(),
                System.currentTimeMillis(),
                System.currentTimeMillis()
        );

        // Save to Firebase first, fallback to mock
        if (firebaseRepo.getCurrentFirebaseUser() != null) {
            showLoading(true);
            firebaseRepo.createBooking(booking)
                    .addOnSuccessListener(aVoid -> {
                        showLoading(false);
                        Log.d(TAG, "Booking created in Firebase: " + booking.getId());
                        navigateToConfirmation(booking.getId());
                    })
                    .addOnFailureListener(e -> {
                        showLoading(false);
                        Log.e(TAG, "Error creating booking in Firebase", e);
                        // Fallback to mock
                        mockRepo.addBooking(booking);
                        navigateToConfirmation(booking.getId());
                    });
        } else {
            mockRepo.addBooking(booking);
            navigateToConfirmation(booking.getId());
        }
    }
    
    private void navigateToConfirmation(String bookingId) {
        Intent intent = new Intent(this, BookingConfirmationActivity.class);
        intent.putExtra(BookingConfirmationActivity.EXTRA_BOOKING_ID, bookingId);
        startActivity(intent);
        finish();
    }

    private String getFirstImageUrl() {
        if (item.getImages() != null && !item.getImages().isEmpty()) {
            return item.getImages().get(0);
        }
        return "";
    }

    private static class ImagePagerAdapter extends RecyclerView.Adapter<ImagePagerAdapter.ImageViewHolder> {
        private final List<String> imageUrls;

        ImagePagerAdapter(List<String> imageUrls) {
            this.imageUrls = imageUrls;
        }

        @NonNull
        @Override
        public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ImageView imageView = new ImageView(parent.getContext());
            imageView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            ));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            return new ImageViewHolder(imageView);
        }

        @Override
        public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
            String url = imageUrls.get(position);
            if (url == null || url.isEmpty()) {
                holder.imageView.setImageResource(R.drawable.ic_launcher_background);
            } else {
                Glide.with(holder.imageView.getContext())
                        .load(url)
                        .placeholder(R.drawable.ic_launcher_background)
                        .into(holder.imageView);
            }
        }

        @Override
        public int getItemCount() {
            return imageUrls.size();
        }

        static class ImageViewHolder extends RecyclerView.ViewHolder {
            private final ImageView imageView;

            ImageViewHolder(@NonNull ImageView imageView) {
                super(imageView);
                this.imageView = imageView;
            }
        }
    }
}

