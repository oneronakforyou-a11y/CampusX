package com.example.campusx.ui.rentals;

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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campusx.R;
import com.example.campusx.data.FirebaseRepository;
import com.example.campusx.model.Booking;
import com.example.campusx.model.BookingStatus;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MyRentalsFragment extends Fragment {
    private static final String TAG = "MyRentalsFragment";
    private static final int TAB_REQUESTS = 0;
    private static final int TAB_HISTORY = 1;

    private RecyclerView recyclerView;
    private TabLayout tabLayout;
    private TextView emptyText;
    private ProgressBar progressBar;
    private RentalAdapter adapter;
    private FirebaseRepository firebaseRepo;
    private int currentTab = TAB_REQUESTS;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_rentals, container, false);

        firebaseRepo = FirebaseRepository.getInstance();

        initViews(view);
        setupRecyclerView();
        setupTabs();
        loadBookings();

        return view;
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.rentals_recycler_view);
        tabLayout = view.findViewById(R.id.tab_layout);
        emptyText = view.findViewById(R.id.empty_text);
        progressBar = view.findViewById(R.id.progress_bar);
    }

    private void setupRecyclerView() {
        adapter = new RentalAdapter();
        adapter.setActionListener(new RentalAdapter.ActionListener() {
            @Override
            public void onAccept(Booking booking) {
                updateBookingStatus(booking, BookingStatus.CONFIRMED);
            }

            @Override
            public void onDecline(Booking booking) {
                updateBookingStatus(booking, BookingStatus.DECLINED);
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void setupTabs() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                loadBookings();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                loadBookings();
            }
        });
    }

    private void loadBookings() {
        String currentUserId = firebaseRepo.getCurrentUserId();
        adapter.setCurrentUserId(currentUserId);
        adapter.setShowRequestActions(currentTab == TAB_REQUESTS);

        if (currentUserId == null) {
            showLoading(false);
            adapter.setBookings(new ArrayList<>());
            emptyText.setText(R.string.sign_in_for_bookings);
            emptyText.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            return;
        }

        showLoading(true);
        if (currentTab == TAB_REQUESTS) {
            loadIncomingRequests(currentUserId);
        } else {
            loadFullHistory(currentUserId);
        }
    }

    private void loadIncomingRequests(String currentUserId) {
        firebaseRepo.getBookingsByOwner(currentUserId)
                .addOnSuccessListener(querySnapshot -> {
                    List<Booking> requests = new ArrayList<>();
                    addBookingsFromSnapshot(querySnapshot, requests, new HashSet<>());
                    List<Booking> pendingRequests = new ArrayList<>();
                    for (Booking booking : requests) {
                        if (booking.getStatus() == BookingStatus.PENDING) {
                            pendingRequests.add(booking);
                        }
                    }
                    displayBookings(pendingRequests);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading booking requests from Firebase", e);
                    showLoading(false);
                    Toast.makeText(getContext(), "Firebase booking requests failed", Toast.LENGTH_SHORT).show();
                    displayBookings(new ArrayList<>());
                });
    }

    private void loadFullHistory(String currentUserId) {
        List<Booking> allBookings = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();

        firebaseRepo.getBookingsByRenter(currentUserId)
                .addOnCompleteListener(renterTask -> {
                    if (renterTask.isSuccessful() && renterTask.getResult() != null) {
                        addBookingsFromSnapshot(renterTask.getResult(), allBookings, seenIds);
                    } else if (renterTask.getException() != null) {
                        Log.e(TAG, "Error loading buying history", renterTask.getException());
                    }

                    firebaseRepo.getBookingsByOwner(currentUserId)
                            .addOnCompleteListener(ownerTask -> {
                                if (ownerTask.isSuccessful() && ownerTask.getResult() != null) {
                                    addBookingsFromSnapshot(ownerTask.getResult(), allBookings, seenIds);
                                } else if (ownerTask.getException() != null) {
                                    Log.e(TAG, "Error loading selling history", ownerTask.getException());
                                }
                                displayBookings(allBookings);
                            });
                });
    }

    private void addBookingsFromSnapshot(QuerySnapshot snapshot, List<Booking> bookings, Set<String> seenIds) {
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            Booking booking = documentToBooking(doc);
            if (booking != null && seenIds.add(booking.getId())) {
                bookings.add(booking);
            }
        }
    }

    private void displayBookings(List<Booking> bookings) {
        showLoading(false);
        bookings.sort(Comparator.comparingLong(Booking::getCreatedAt).reversed());
        adapter.setBookings(bookings);
        emptyText.setText(currentTab == TAB_REQUESTS ? getString(R.string.no_pending_requests) : getString(R.string.activity_empty));
        boolean isEmpty = bookings.isEmpty();
        emptyText.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void updateBookingStatus(Booking booking, BookingStatus status) {
        showLoading(true);
        firebaseRepo.updateBookingStatus(booking.getId(), status.name())
                .addOnSuccessListener(unused -> {
                    Toast.makeText(getContext(), status == BookingStatus.CONFIRMED ? "Booking accepted" : "Booking declined", Toast.LENGTH_SHORT).show();
                    loadBookings();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating booking status", e);
                    showLoading(false);
                    Toast.makeText(getContext(), "Could not update booking", Toast.LENGTH_SHORT).show();
                });
    }

    private Booking documentToBooking(DocumentSnapshot doc) {
        try {
            String statusStr = doc.getString("status");
            Long startDate = doc.getLong("startDate");
            Long endDate = doc.getLong("endDate");
            Long createdAt = doc.getLong("createdAt");
            Long updatedAt = doc.getLong("updatedAt");
            Double totalPrice = doc.getDouble("totalPrice");

            return new Booking(
                    doc.getString("id"),
                    doc.getString("itemId"),
                    doc.getString("itemTitle"),
                    doc.getString("itemImage"),
                    doc.getString("renterId"),
                    doc.getString("renterName"),
                    doc.getString("ownerId"),
                    doc.getString("ownerName"),
                    startDate != null ? startDate : System.currentTimeMillis(),
                    endDate != null ? endDate : System.currentTimeMillis(),
                    totalPrice != null ? totalPrice : 0.0,
                    parseStatus(statusStr),
                    doc.getString("otp"),
                    doc.getString("pickupLocation"),
                    createdAt != null ? createdAt : System.currentTimeMillis(),
                    updatedAt != null ? updatedAt : System.currentTimeMillis()
            );
        } catch (Exception e) {
            Log.e(TAG, "Error converting document to Booking", e);
            return null;
        }
    }

    private BookingStatus parseStatus(String status) {
        if (status == null) {
            return BookingStatus.PENDING;
        }
        try {
            return BookingStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            return BookingStatus.fromString(status);
        }
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (show) {
            emptyText.setVisibility(View.GONE);
            recyclerView.setVisibility(View.GONE);
        }
    }
}
