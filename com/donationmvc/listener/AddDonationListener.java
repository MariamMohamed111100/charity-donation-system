package com.donationmvc.listener;

public interface AddDonationListener {
    void onAddDonationRequested(String donorId, String ngoId, double amount);
}