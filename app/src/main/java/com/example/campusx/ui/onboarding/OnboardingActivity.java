package com.example.campusx.ui.onboarding;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.campusx.R;
import com.example.campusx.ui.SystemBarsHelper;
import com.example.campusx.ui.auth.CampusVerificationActivity;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class OnboardingActivity extends AppCompatActivity {
    private ViewPager2 viewPager;
    private TextView skipButton;
    private MaterialButton nextButton;
    private View indicator1, indicator2, indicator3;
    private List<OnboardingPage> pages;
    private int currentPage = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);
        SystemBarsHelper.applySystemBarPadding(findViewById(R.id.onboarding_root));

        // Hide action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        initViews();
        setupPages();
        setupViewPager();
        setupListeners();
    }

    private void initViews() {
        viewPager = findViewById(R.id.view_pager);
        skipButton = findViewById(R.id.skip_button);
        nextButton = findViewById(R.id.next_button);
        indicator1 = findViewById(R.id.indicator_1);
        indicator2 = findViewById(R.id.indicator_2);
        indicator3 = findViewById(R.id.indicator_3);
    }

    private void setupPages() {
        pages = new ArrayList<>();
        pages.add(new OnboardingPage(
                R.drawable.ic_campusx_logo,
                getString(R.string.onboarding_title_1),
                getString(R.string.onboarding_desc_1)
        ));
        pages.add(new OnboardingPage(
                R.drawable.ic_campusx_logo,
                getString(R.string.onboarding_title_2),
                getString(R.string.onboarding_desc_2)
        ));
        pages.add(new OnboardingPage(
                R.drawable.ic_campusx_logo,
                getString(R.string.onboarding_title_3),
                getString(R.string.onboarding_desc_3)
        ));
    }

    private void setupViewPager() {
        OnboardingAdapter adapter = new OnboardingAdapter(pages);
        viewPager.setAdapter(adapter);
        
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                currentPage = position;
                updateIndicators(position);
                updateNextButton(position);
            }
        });
    }

    private void setupListeners() {
        skipButton.setOnClickListener(v -> navigateToVerification());
        
        nextButton.setOnClickListener(v -> {
            if (currentPage < pages.size() - 1) {
                viewPager.setCurrentItem(currentPage + 1);
            } else {
                navigateToVerification();
            }
        });
    }

    private void updateIndicators(int position) {
        indicator1.setBackgroundResource(position == 0 ? R.drawable.indicator_active : R.drawable.indicator_inactive);
        indicator2.setBackgroundResource(position == 1 ? R.drawable.indicator_active : R.drawable.indicator_inactive);
        indicator3.setBackgroundResource(position == 2 ? R.drawable.indicator_active : R.drawable.indicator_inactive);
    }

    private void updateNextButton(int position) {
        if (position == pages.size() - 1) {
            nextButton.setText(R.string.get_started);
        } else {
            nextButton.setText(R.string.next);
        }
    }

    private void navigateToVerification() {
        Intent intent = new Intent(this, CampusVerificationActivity.class);
        startActivity(intent);
        finish();
    }
}
