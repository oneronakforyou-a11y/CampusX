package com.example.campusx.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.campusx.R;
import com.example.campusx.data.FirebaseRepository;
import com.example.campusx.ui.SystemBarsHelper;
import com.google.android.material.button.MaterialButton;

import java.util.Locale;

public class OtpVerificationActivity extends AppCompatActivity {
    private static final String TAG = "OtpVerification";
    private static final long COUNTDOWN_TIME = 60000; // 60 seconds

    private TextView emailText, timerText, resendButton, pinPreviewText;
    private EditText[] otpDigits;
    private MaterialButton verifyButton;
    private ProgressBar progressBar;
    private CountDownTimer countDownTimer;
    private String email;
    private String correctPin;
    private FirebaseRepository firebaseRepo;
    private boolean isVerifying;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp_verification);
        SystemBarsHelper.applySystemBarPadding(findViewById(R.id.otp_verification_root));

        // Hide action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        email = getIntent().getStringExtra("email");
        correctPin = getIntent().getStringExtra("pin");
        firebaseRepo = FirebaseRepository.getInstance();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(correctPin)) {
            Toast.makeText(this, "Missing verification details. Please request a new PIN.", Toast.LENGTH_LONG).show();
            navigateBackToVerification();
            return;
        }

        initViews();
        setupOtpInputs();
        setupListeners();
        startTimer();
    }

    private void initViews() {
        emailText = findViewById(R.id.email_text);
        pinPreviewText = findViewById(R.id.pin_preview_text);
        timerText = findViewById(R.id.timer_text);
        resendButton = findViewById(R.id.resend_button);
        verifyButton = findViewById(R.id.verify_button);
        progressBar = findViewById(R.id.progress_bar);

        emailText.setText(email);
        pinPreviewText.setText(getString(R.string.demo_pin_format, correctPin));

        // Only use 4 digits
        otpDigits = new EditText[]{
                findViewById(R.id.otp_digit_1),
                findViewById(R.id.otp_digit_2),
                findViewById(R.id.otp_digit_3),
                findViewById(R.id.otp_digit_4)
        };
        
        // Hide the extra 2 digits
        findViewById(R.id.otp_digit_5).setVisibility(View.GONE);
        findViewById(R.id.otp_digit_6).setVisibility(View.GONE);
    }

    private void setupOtpInputs() {
        for (int i = 0; i < otpDigits.length; i++) {
            final int index = i;
            otpDigits[i].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() == 1 && index < otpDigits.length - 1) {
                        otpDigits[index + 1].requestFocus();
                    } else if (s.length() == 1 && index == otpDigits.length - 1) {
                        // Auto-verify when last digit is entered
                        verifyPin();
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });

            otpDigits[i].setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (otpDigits[index].getText().toString().isEmpty() && index > 0) {
                        otpDigits[index - 1].requestFocus();
                    }
                }
                return false;
            });
        }

        // Focus on first digit
        otpDigits[0].requestFocus();
    }

    private void setupListeners() {
        verifyButton.setOnClickListener(v -> verifyPin());
        resendButton.setOnClickListener(v -> resendPin());
    }

    private void startTimer() {
        countDownTimer = new CountDownTimer(COUNTDOWN_TIME, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long seconds = millisUntilFinished / 1000;
                timerText.setText(getString(R.string.resend_in, String.format(Locale.getDefault(), "0:%02d", seconds)));
            }

            @Override
            public void onFinish() {
                timerText.setVisibility(View.GONE);
                resendButton.setVisibility(View.VISIBLE);
            }
        }.start();
    }

    private void verifyPin() {
        if (isVerifying) {
            return;
        }

        StringBuilder enteredPin = new StringBuilder();
        for (EditText digit : otpDigits) {
            enteredPin.append(digit.getText().toString());
        }

        if (enteredPin.length() != 4) {
            Toast.makeText(this, "Please enter complete 4-digit PIN", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);
        isVerifying = true;

        // Verify PIN matches
        if (enteredPin.toString().equals(correctPin)) {
            // PIN is correct, sign in with email and padded password
            String password = "PIN" + correctPin; // Pad to 6 characters for Firebase
            
            firebaseRepo.signInWithEmail(email, password)
                    .addOnSuccessListener(authResult -> {
                        Log.d(TAG, "Sign in successful");
                        isVerifying = false;
                        showLoading(false);
                        Toast.makeText(this, "Welcome to CampusX!", Toast.LENGTH_SHORT).show();
                        navigateToMain();
                    })
                    .addOnFailureListener(e -> {
                        isVerifying = false;
                        showLoading(false);
                        Log.e(TAG, "Error signing in", e);
                        Toast.makeText(this, "Sign in failed. Please try again.", Toast.LENGTH_SHORT).show();
                    });
        } else {
            isVerifying = false;
            showLoading(false);
            Toast.makeText(this, "Incorrect PIN. Please try again.", Toast.LENGTH_LONG).show();
            // Clear all fields
            for (EditText digit : otpDigits) {
                digit.setText("");
            }
            otpDigits[0].requestFocus();
        }
    }

    private void resendPin() {
        resendButton.setVisibility(View.GONE);
        timerText.setVisibility(View.VISIBLE);
        startTimer();
        
        Toast.makeText(this, "PIN: " + correctPin, Toast.LENGTH_LONG).show();
    }

    private void showLoading(boolean show) {
        if (show) {
            verifyButton.setVisibility(View.INVISIBLE);
            progressBar.setVisibility(View.VISIBLE);
            for (EditText digit : otpDigits) {
                digit.setEnabled(false);
            }
        } else {
            verifyButton.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
            for (EditText digit : otpDigits) {
                digit.setEnabled(true);
            }
        }
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, com.example.campusx.ui.main.MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateBackToVerification() {
        Intent intent = new Intent(this, CampusVerificationActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}
