package com.example.campusx.ui.booking;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.example.campusx.R;
import com.example.campusx.data.FirebaseRepository;
import com.example.campusx.data.MockDataRepository;
import com.example.campusx.model.Booking;
import com.example.campusx.model.BookingStatus;
import com.example.campusx.ui.SystemBarsHelper;
import com.example.campusx.ui.main.MainActivity;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BookingConfirmationActivity extends AppCompatActivity {
    private static final String TAG = "BookingConfirmation";
    public static final String EXTRA_BOOKING_ID = "booking_id";

    private TextView otpText, itemTitle, bookingDates, totalPrice, pickupLocation;
    private MaterialButton viewRentalsButton, doneButton;
    private ProgressBar progressBar;
    private Booking booking;
    private FirebaseRepository firebaseRepo;
    private MockDataRepository mockRepo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_confirmation);
        SystemBarsHelper.applySystemBarPadding(findViewById(R.id.booking_confirmation_root));

        firebaseRepo = FirebaseRepository.getInstance();
        mockRepo = MockDataRepository.getInstance();
        setupBackNavigation();

        String bookingId = getIntent().getStringExtra(EXTRA_BOOKING_ID);
        if (bookingId == null) {
            finish();
            return;
        }

        initViews();
        loadBooking(bookingId);
    }

    private void initViews() {
        otpText = findViewById(R.id.otp_text);
        itemTitle = findViewById(R.id.item_title);
        bookingDates = findViewById(R.id.booking_dates);
        totalPrice = findViewById(R.id.total_price);
        pickupLocation = findViewById(R.id.pickup_location);
        viewRentalsButton = findViewById(R.id.view_rentals_button);
        doneButton = findViewById(R.id.done_button);
        progressBar = findViewById(R.id.progress_bar);
    }
    
    private void loadBooking(String bookingId) {
        showLoading(true);
        
        // Try Firebase first
        if (firebaseRepo.getCurrentFirebaseUser() != null) {
            firebaseRepo.getBooking(bookingId)
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            booking = documentToBooking(documentSnapshot);
                            if (booking != null) {
                                showLoading(false);
                                populateData();
                                setupButtons();
                            } else {
                                loadBookingFromMock(bookingId);
                            }
                        } else {
                            loadBookingFromMock(bookingId);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error loading booking from Firebase", e);
                        loadBookingFromMock(bookingId);
                    });
        } else {
            loadBookingFromMock(bookingId);
        }
    }
    
    private void loadBookingFromMock(String bookingId) {
        // Find booking in mock data
        for (Booking b : mockRepo.getBookings()) {
            if (b.getId().equals(bookingId)) {
                booking = b;
                break;
            }
        }
        
        showLoading(false);
        
        if (booking == null) {
            Toast.makeText(this, "Booking not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        populateData();
        setupButtons();
    }
    
    private Booking documentToBooking(DocumentSnapshot doc) {
        try {
            String id = doc.getString("id");
            String itemId = doc.getString("itemId");
            String itemTitle = doc.getString("itemTitle");
            String itemImage = doc.getString("itemImage");
            String renterId = doc.getString("renterId");
            String renterName = doc.getString("renterName");
            String ownerId = doc.getString("ownerId");
            String ownerName = doc.getString("ownerName");
            Long startDate = doc.getLong("startDate");
            Long endDate = doc.getLong("endDate");
            Double totalPrice = doc.getDouble("totalPrice");
            String statusStr = doc.getString("status");
            String otp = doc.getString("otp");
            String pickupLocation = doc.getString("pickupLocation");
            Long createdAt = doc.getLong("createdAt");
            Long updatedAt = doc.getLong("updatedAt");
            
            BookingStatus status = BookingStatus.valueOf(statusStr);
            
            return new Booking(
                    id, itemId, itemTitle, itemImage,
                    renterId, renterName, ownerId, ownerName,
                    startDate != null ? startDate : System.currentTimeMillis(),
                    endDate != null ? endDate : System.currentTimeMillis(),
                    totalPrice != null ? totalPrice : 0.0,
                    status, otp, pickupLocation,
                    createdAt != null ? createdAt : System.currentTimeMillis(),
                    updatedAt != null ? updatedAt : System.currentTimeMillis()
            );
        } catch (Exception e) {
            Log.e(TAG, "Error converting document to Booking", e);
            return null;
        }
    }
    
    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? android.view.View.VISIBLE : android.view.View.GONE);
        }
    }

    private void populateData() {
        otpText.setText(booking.getOtp());
        itemTitle.setText(booking.getItemTitle());
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        String startDateStr = dateFormat.format(new Date(booking.getStartDate()));
        String endDateStr = dateFormat.format(new Date(booking.getEndDate()));
        bookingDates.setText(startDateStr + " - " + endDateStr);
        
        totalPrice.setText(getString(R.string.total_price, booking.getTotalPrice()));
        pickupLocation.setText(booking.getPickupLocation());
    }

    private void setupButtons() {
        viewRentalsButton.setOnClickListener(v -> {
            navigateToMain("rentals");
        });

        doneButton.setOnClickListener(v -> navigateToMain(null));
    }

    private void setupBackNavigation() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                navigateToMain(null);
            }
        });
    }

    private void navigateToMain(String targetTab) {
        Intent intent = new Intent(this, MainActivity.class);
        if (targetTab != null) {
            intent.putExtra("navigate_to", targetTab);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}
