package com.example.campusx.data;

import com.example.campusx.model.Item;
import com.example.campusx.model.ItemCategory;
import com.example.campusx.model.User;
import com.example.campusx.model.Booking;
import com.example.campusx.model.BookingStatus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class MockDataRepository {
    private static MockDataRepository instance;
    private List<Item> items;
    private List<User> users;
    private List<Booking> bookings;
    private User currentUser;

    private MockDataRepository() {
        initializeMockData();
    }

    public static synchronized MockDataRepository getInstance() {
        if (instance == null) {
            instance = new MockDataRepository();
        }
        return instance;
    }

    private void initializeMockData() {
        // Initialize mock users
        users = new ArrayList<>();
        users.add(new User("user1", "john@bml.edu.in", "John Doe", null, "Engineering student",
                4.5, 10, 3, 5, false, System.currentTimeMillis(), System.currentTimeMillis()));
        users.add(new User("user2", "jane@bml.edu.in", "Jane Smith", null, "CS major",
                4.8, 15, 5, 8, false, System.currentTimeMillis(), System.currentTimeMillis()));
        
        currentUser = users.get(0);

        // Initialize mock items
        items = new ArrayList<>();
        items.add(new Item("item1", "user2", "Jane Smith", 4.8,
                "MacBook Pro 2021", "14-inch MacBook Pro with M1 chip, perfect for coding and design work",
                ItemCategory.ELECTRONICS, 500.0,
                Arrays.asList("https://via.placeholder.com/400x300/4A90E2/FFFFFF?text=MacBook+Pro"),
                "Library Building", true, System.currentTimeMillis(), System.currentTimeMillis()));
        
        items.add(new Item("item2", "user2", "Jane Smith", 4.8,
                "Scientific Calculator", "Casio FX-991EX, great for engineering calculations",
                ItemCategory.STUDY_GEAR, 50.0,
                Arrays.asList("https://via.placeholder.com/400x300/50C878/FFFFFF?text=Calculator"),
                "Main Campus", true, System.currentTimeMillis(), System.currentTimeMillis()));
        
        items.add(new Item("item3", "user1", "John Doe", 4.5,
                "Camping Tent", "4-person camping tent, perfect for weekend trips",
                ItemCategory.LIFESTYLE, 200.0,
                Arrays.asList("https://via.placeholder.com/400x300/FF6B6B/FFFFFF?text=Camping+Tent"),
                "Sports Complex", true, System.currentTimeMillis(), System.currentTimeMillis()));
        
        items.add(new Item("item4", "user2", "Jane Smith", 4.8,
                "iPad Air", "Latest iPad Air with Apple Pencil, great for note-taking",
                ItemCategory.ELECTRONICS, 300.0,
                Arrays.asList("https://via.placeholder.com/400x300/9B59B6/FFFFFF?text=iPad+Air"),
                "Library Building", true, System.currentTimeMillis(), System.currentTimeMillis()));
        
        items.add(new Item("item5", "user1", "John Doe", 4.5,
                "Engineering Textbooks", "Set of 5 core engineering textbooks",
                ItemCategory.STUDY_GEAR, 100.0,
                Arrays.asList("https://via.placeholder.com/400x300/F39C12/FFFFFF?text=Textbooks"),
                "Main Campus", true, System.currentTimeMillis(), System.currentTimeMillis()));

        // Initialize mock bookings
        bookings = new ArrayList<>();
    }

    public List<Item> getItems() {
        return new ArrayList<>(items);
    }

    public List<Item> getItemsByCategory(ItemCategory category) {
        if (category == ItemCategory.ALL_ITEMS) {
            return getItems();
        }
        List<Item> filtered = new ArrayList<>();
        for (Item item : items) {
            if (item.getCategory() == category) {
                filtered.add(item);
            }
        }
        return filtered;
    }

    public Item getItemById(String itemId) {
        for (Item item : items) {
            if (item.getId().equals(itemId)) {
                return item;
            }
        }
        return null;
    }

    public List<Item> searchItems(String query) {
        List<Item> results = new ArrayList<>();
        String lowerQuery = query.toLowerCase(Locale.ROOT);
        for (Item item : items) {
            if (item.getTitle().toLowerCase(Locale.ROOT).contains(lowerQuery) ||
                item.getDescription().toLowerCase(Locale.ROOT).contains(lowerQuery) ||
                item.getPickupLocation().toLowerCase(Locale.ROOT).contains(lowerQuery)) {
                results.add(item);
            }
        }
        return results;
    }

    public void addItem(Item item) {
        items.add(0, item);
    }

    public List<Item> getItemsByOwnerId(String ownerId) {
        List<Item> ownedItems = new ArrayList<>();
        for (Item item : items) {
            if (item.getOwnerId().equals(ownerId)) {
                ownedItems.add(item);
            }
        }
        return ownedItems;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public List<Booking> getBookings() {
        return new ArrayList<>(bookings);
    }

    public void addBooking(Booking booking) {
        bookings.add(booking);
    }
}
