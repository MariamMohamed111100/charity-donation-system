package com.donationmvc.listener;

public interface AddUserListener {
    void onAddDonorRequested(String id, String name, String email);
    void onAddNGORequested(String id, String name, String mission);
}