package com.example.campusx.data;

import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.campusx.model.Booking;
import com.example.campusx.model.Item;
import com.example.campusx.model.User;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseRepository {
    private static final String TAG = "FirebaseRepository";
    private static FirebaseRepository instance;
    
    private final FirebaseAuth auth;
    private final FirebaseFirestore db;
    private final FirebaseStorage storage;
    
    // Collection names
    private static final String USERS_COLLECTION = "users";
    private static final String ITEMS_COLLECTION = "items";
    private static final String BOOKINGS_COLLECTION = "bookings";
    
    private FirebaseRepository() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
    }
    
    public static synchronized FirebaseRepository getInstance() {
        if (instance == null) {
            instance = new FirebaseRepository();
        }
        return instance;
    }
    
    // ==================== Authentication ====================
    
    public Task<AuthResult> signInWithEmail(String email, String password) {
        return auth.signInWithEmailAndPassword(email, password);
    }
    
    public Task<AuthResult> createUserWithEmail(String email, String password) {
        return auth.createUserWithEmailAndPassword(email, password);
    }
    
    public Task<Void> sendPasswordResetEmail(String email) {
        return auth.sendPasswordResetEmail(email);
    }
    
    public Task<Void> sendSignInLinkToEmail(String email, com.google.firebase.auth.ActionCodeSettings actionCodeSettings) {
        return auth.sendSignInLinkToEmail(email, actionCodeSettings);
    }
    
    public boolean isSignInWithEmailLink(String emailLink) {
        return auth.isSignInWithEmailLink(emailLink);
    }
    
    public Task<AuthResult> signInWithEmailLink(String email, String emailLink) {
        return auth.signInWithEmailLink(email, emailLink);
    }
    
    public FirebaseUser getCurrentFirebaseUser() {
        return auth.getCurrentUser();
    }
    
    public void signOut() {
        auth.signOut();
    }
    
    public String getCurrentUserId() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }
    
    // ==================== User Operations ====================
    
    public Task<Void> createUser(User user) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("id", user.getId());
        userData.put("email", user.getEmail());
        userData.put("name", user.getName());
        userData.put("profileImageUrl", user.getProfileImageUrl());
        userData.put("bio", user.getBio());
        userData.put("rating", user.getRating());
        userData.put("ratingCount", user.getRatingCount());
        userData.put("listingCount", user.getListingCount());
        userData.put("rentalCount", user.getRentalCount());
        userData.put("isAdmin", user.isAdmin());
        userData.put("createdAt", user.getCreatedAt());
        userData.put("lastActive", user.getLastActive());
        
        return db.collection(USERS_COLLECTION)
                .document(user.getId())
                .set(userData);
    }
    
    public Task<DocumentSnapshot> getUser(String userId) {
        return db.collection(USERS_COLLECTION)
                .document(userId)
                .get();
    }
    
    public Task<Void> updateUser(String userId, Map<String, Object> updates) {
        updates.put("updatedAt", System.currentTimeMillis());
        return db.collection(USERS_COLLECTION)
                .document(userId)
                .update(updates);
    }
    
    // ==================== Item Operations ====================
    
    public Task<Void> createItem(Item item) {
        Map<String, Object> itemData = new HashMap<>();
        itemData.put("id", item.getId());
        itemData.put("ownerId", item.getOwnerId());
        itemData.put("ownerName", item.getOwnerName());
        itemData.put("ownerRating", item.getOwnerRating());
        itemData.put("title", item.getTitle());
        itemData.put("description", item.getDescription());
        itemData.put("category", item.getCategory().name());
        itemData.put("pricePerDay", item.getPricePerDay());
        itemData.put("images", item.getImages());
        itemData.put("pickupLocation", item.getPickupLocation());
        itemData.put("isAvailable", item.isActive());
        itemData.put("createdAt", item.getCreatedAt());
        itemData.put("updatedAt", item.getUpdatedAt());
        
        return db.collection(ITEMS_COLLECTION)
                .document(item.getId())
                .set(itemData);
    }
    
    public Task<DocumentSnapshot> getItem(String itemId) {
        return db.collection(ITEMS_COLLECTION)
                .document(itemId)
                .get();
    }
    
    public Task<QuerySnapshot> getAllItems() {
        return db.collection(ITEMS_COLLECTION)
                .whereEqualTo("isAvailable", true)
                .get();
    }
    
    public Task<QuerySnapshot> getItemsByCategory(String category) {
        return db.collection(ITEMS_COLLECTION)
                .whereEqualTo("category", category)
                .whereEqualTo("isAvailable", true)
                .get();
    }
    
    public Task<QuerySnapshot> getItemsByOwner(String ownerId) {
        return db.collection(ITEMS_COLLECTION)
                .whereEqualTo("ownerId", ownerId)
                .get();
    }
    
    public Task<QuerySnapshot> searchItems(String query) {
        // Note: For better search, consider using Algolia or similar service
        return db.collection(ITEMS_COLLECTION)
                .whereEqualTo("isAvailable", true)
                .orderBy("title")
                .startAt(query)
                .endAt(query + "\uf8ff")
                .get();
    }
    
    public Task<Void> updateItem(String itemId, Map<String, Object> updates) {
        updates.put("updatedAt", System.currentTimeMillis());
        return db.collection(ITEMS_COLLECTION)
                .document(itemId)
                .update(updates);
    }
    
    public Task<Void> deleteItem(String itemId) {
        return db.collection(ITEMS_COLLECTION)
                .document(itemId)
                .delete();
    }
    
    // ==================== Booking Operations ====================
    
    public Task<Void> createBooking(Booking booking) {
        Map<String, Object> bookingData = new HashMap<>();
        bookingData.put("id", booking.getId());
        bookingData.put("itemId", booking.getItemId());
        bookingData.put("itemTitle", booking.getItemTitle());
        bookingData.put("itemImage", booking.getItemImage());
        bookingData.put("renterId", booking.getRenterId());
        bookingData.put("renterName", booking.getRenterName());
        bookingData.put("ownerId", booking.getListerId());
        bookingData.put("ownerName", booking.getListerName());
        bookingData.put("startDate", booking.getStartDate());
        bookingData.put("endDate", booking.getEndDate());
        bookingData.put("totalPrice", booking.getTotalPrice());
        bookingData.put("status", booking.getStatus().name());
        bookingData.put("otp", booking.getOtp());
        bookingData.put("pickupLocation", booking.getPickupLocation());
        bookingData.put("createdAt", booking.getCreatedAt());
        bookingData.put("updatedAt", booking.getUpdatedAt());
        
        return db.collection(BOOKINGS_COLLECTION)
                .document(booking.getId())
                .set(bookingData);
    }
    
    public Task<DocumentSnapshot> getBooking(String bookingId) {
        return db.collection(BOOKINGS_COLLECTION)
                .document(bookingId)
                .get();
    }
    
    public Task<QuerySnapshot> getBookingsByRenter(String renterId) {
        return db.collection(BOOKINGS_COLLECTION)
                .whereEqualTo("renterId", renterId)
                .get();
    }
    
    public Task<QuerySnapshot> getBookingsByOwner(String ownerId) {
        return db.collection(BOOKINGS_COLLECTION)
                .whereEqualTo("ownerId", ownerId)
                .get();
    }
    
    public Task<Void> updateBookingStatus(String bookingId, String status) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);
        updates.put("updatedAt", System.currentTimeMillis());
        
        return db.collection(BOOKINGS_COLLECTION)
                .document(bookingId)
                .update(updates);
    }
    
    // ==================== Storage Operations ====================
    
    public Task<UploadTask.TaskSnapshot> uploadItemImage(String itemId, Uri imageUri, int imageIndex) {
        StorageReference imageRef = storage.getReference()
                .child("items")
                .child(itemId)
                .child("image_" + imageIndex + ".jpg");
        
        return imageRef.putFile(imageUri);
    }
    
    public Task<Uri> getItemImageUrl(String itemId, int imageIndex) {
        StorageReference imageRef = storage.getReference()
                .child("items")
                .child(itemId)
                .child("image_" + imageIndex + ".jpg");
        
        return imageRef.getDownloadUrl();
    }
    
    public Task<UploadTask.TaskSnapshot> uploadProfileImage(String userId, Uri imageUri) {
        StorageReference imageRef = storage.getReference()
                .child("profiles")
                .child(userId)
                .child("profile.jpg");
        
        return imageRef.putFile(imageUri);
    }
    
    public Task<Uri> getProfileImageUrl(String userId) {
        StorageReference imageRef = storage.getReference()
                .child("profiles")
                .child(userId)
                .child("profile.jpg");
        
        return imageRef.getDownloadUrl();
    }
    
    public Task<Void> deleteItemImages(String itemId) {
        StorageReference itemRef = storage.getReference()
                .child("items")
                .child(itemId);
        
        return itemRef.delete();
    }
}
