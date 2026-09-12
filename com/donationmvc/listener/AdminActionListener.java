package com.donationmvc.listener;

public interface AdminActionListener {
    void onVerifyNGORequested(String ngoId);
    void onViewReportsRequested();
    void onViewUsersRequested();
}