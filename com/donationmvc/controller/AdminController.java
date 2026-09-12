package com.donationmvc.controller;

import com.donationmvc.model.UserModel;
import com.donationmvc.model.DonationModel;
import com.donationmvc.listener.AdminActionListener;
import com.donationmvc.view.AdminView;

public class AdminController implements AdminActionListener {
    private final UserModel userModel;
    private final DonationModel donationModel;
    private AdminView adminView;

    public AdminController(UserModel userModel, DonationModel donationModel) {
        this.userModel = userModel;
        this.donationModel = donationModel;
    }

    public void setAdminView(AdminView view) {
        this.adminView = view;
        if (adminView != null) {
            adminView.setModels(donationModel, userModel);
            adminView.setAdminActionListener(this);
            donationModel.registerObserver(adminView);
            userModel.registerObserver(adminView);
        }
    }

    @Override
    public void onVerifyNGORequested(String ngoId) {
        boolean success = userModel.verifyNGO(ngoId);
        if (success) {
            System.out.println("[Admin] NGO " + ngoId + " verified successfully!");
        } else {
            System.out.println("[Admin] NGO not found! Please enter full ID like NGO001");
        }
    }

    @Override
    public void onViewReportsRequested() {
        // يتم التعامل معه في AdminView مباشرة
    }

    @Override
    public void onViewUsersRequested() {
        // يتم التعامل معه في AdminView مباشرة
    }

    public void startAdminSession() {
        AdminView view = new AdminView();
        setAdminView(view);
        
        if (view.login()) {
            view.showMenu();
        } else {
            System.out.println("Invalid credentials!");
        }
    }
}