package com.example.campusx.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.campusx.R;
import com.example.campusx.data.FirebaseRepository;
import com.example.campusx.model.User;
import com.example.campusx.ui.SystemBarsHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseUser;

import java.util.Locale;

public class CampusVerificationActivity extends AppCompatActivity {
    private static final String TAG = "CampusVerification";
    private static final int MIN_PASSWORD_LENGTH = 6;

    private TextView titleText;
    private TextView subtitleText;
    private TextView errorText;
    private TextView toggleAuthMode;
    private TextView signInTab;
    private TextView signUpTab;
    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;
    private MaterialButton authButton;
    private ProgressBar progressBar;
    private FirebaseRepository firebaseRepo;
    private boolean signUpMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_campus_verification);
        SystemBarsHelper.applySystemBarPadding(findViewById(R.id.campus_verification_root));

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        firebaseRepo = FirebaseRepository.getInstance();
        initViews();
        setupListeners();
        updateAuthMode();
    }

    private void initViews() {
        titleText = findViewById(R.id.title);
        subtitleText = findViewById(R.id.subtitle);
        emailInput = findViewById(R.id.email_input);
        passwordInput = findViewById(R.id.password_input);
        errorText = findViewById(R.id.error_text);
        authButton = findViewById(R.id.get_code_button);
        progressBar = findViewById(R.id.progress_bar);
        toggleAuthMode = findViewById(R.id.toggle_auth_mode);
        signInTab = findViewById(R.id.sign_in_tab);
        signUpTab = findViewById(R.id.sign_up_tab);
    }

    private void setupListeners() {
        authButton.setOnClickListener(v -> validateAndAuthenticate());
        toggleAuthMode.setOnClickListener(v -> setSignUpMode(!signUpMode));
        signInTab.setOnClickListener(v -> setSignUpMode(false));
        signUpTab.setOnClickListener(v -> setSignUpMode(true));
    }

    private void setSignUpMode(boolean signUpMode) {
        this.signUpMode = signUpMode;
        hideError();
        updateAuthMode();
    }

    private void updateAuthMode() {
        titleText.setText(signUpMode ? R.string.create_account : R.string.sign_in);
        subtitleText.setText(signUpMode ? R.string.signup_subtitle : R.string.signin_subtitle);
        authButton.setText(signUpMode ? R.string.create_account : R.string.sign_in);
        toggleAuthMode.setText(signUpMode ? R.string.switch_to_signin : R.string.switch_to_signup);
        updateSegmentTab(signInTab, !signUpMode);
        updateSegmentTab(signUpTab, signUpMode);
    }

    private void updateSegmentTab(TextView tab, boolean selected) {
        tab.setBackgroundResource(selected ? R.drawable.bg_auth_segment_selected : R.drawable.bg_auth_segment_unselected);
        tab.setTextColor(ContextCompat.getColor(this, selected ? R.color.white : R.color.text_secondary));
    }

    private void validateAndAuthenticate() {
        String email = getInputText(emailInput).toLowerCase(Locale.ROOT);
        String password = getInputText(passwordInput);

        if (TextUtils.isEmpty(email)) {
            showError(getString(R.string.error_email_required));
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError(getString(R.string.error_email_invalid));
            return;
        }

        if (TextUtils.isEmpty(password)) {
            showError(getString(R.string.error_password_required));
            return;
        }

        if (password.length() < MIN_PASSWORD_LENGTH) {
            showError(getString(R.string.error_password_short));
            return;
        }

        hideError();
        showLoading(true);

        if (signUpMode) {
            signUp(email, password);
        } else {
            signIn(email, password);
        }
    }

    private String getInputText(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }

    private void signUp(String email, String password) {
        firebaseRepo.createUserWithEmail(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser == null) {
                        showLoading(false);
                        showError(getString(R.string.error_auth_failed));
                        return;
                    }
                    createUserProfile(firebaseUser.getUid(), email);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Sign up failed", e);
                    showLoading(false);
                    showError(getString(R.string.error_signup_failed, cleanError(e)));
                });
    }

    private void signIn(String email, String password) {
        firebaseRepo.signInWithEmail(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser == null) {
                        showLoading(false);
                        showError(getString(R.string.error_auth_failed));
                        return;
                    }
                    ensureUserProfile(firebaseUser.getUid(), email);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Sign in failed", e);
                    showLoading(false);
                    showError(getString(R.string.error_signin_failed, cleanError(e)));
                });
    }

    private void ensureUserProfile(String userId, String email) {
        firebaseRepo.getUser(userId)
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        showLoading(false);
                        navigateToMain();
                    } else {
                        createUserProfile(userId, email);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Could not read user profile, creating it", e);
                    createUserProfile(userId, email);
                });
    }

    private void createUserProfile(String userId, String email) {
        long now = System.currentTimeMillis();
        User user = new User(
                userId,
                email,
                getDefaultName(email),
                null,
                getString(R.string.default_user_bio),
                0.0,
                0,
                0,
                0,
                false,
                now,
                now
        );

        firebaseRepo.createUser(user)
                .addOnSuccessListener(v -> {
                    Log.d(TAG, "User profile created in Firestore: users/" + userId);
                    showLoading(false);
                    navigateToMain();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "User profile creation failed", e);
                    showLoading(false);
                    showError(getString(R.string.error_profile_create_failed, cleanError(e)));
                });
    }

    private String getDefaultName(String email) {
        int atIndex = email.indexOf("@");
        if (atIndex <= 0) {
            return getString(R.string.default_user_name);
        }
        return email.substring(0, atIndex);
    }

    private String cleanError(Exception e) {
        return e.getMessage() == null ? getString(R.string.error_try_again) : e.getMessage();
    }

    private void showError(String message) {
        errorText.setText(message);
        errorText.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        errorText.setVisibility(View.GONE);
    }

    private void showLoading(boolean show) {
        authButton.setVisibility(show ? View.INVISIBLE : View.VISIBLE);
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        emailInput.setEnabled(!show);
        passwordInput.setEnabled(!show);
        toggleAuthMode.setEnabled(!show);
        signInTab.setEnabled(!show);
        signUpTab.setEnabled(!show);
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, com.example.campusx.ui.main.MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
