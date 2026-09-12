package com.donationmvc.controller;

import com.donationmvc.model.DonationModel;
import com.donationmvc.model.UserModel;
import com.donationmvc.model.Donor;
import com.donationmvc.model.NGO;
import com.donationmvc.listener.AddUserListener;
import com.donationmvc.view.NGOView;
import com.donationmvc.view.LoginView;
import java.util.Optional;

public class UserController implements AddUserListener {
    private final UserModel userModel;
    private final DonationModel donationModel;
    private LoginView loginView;
    private NGOView ngoView;

    public UserController(UserModel userModel, DonationModel donationModel) {
        this.userModel = userModel;
        this.donationModel = donationModel;
    }

    public void setLoginView(LoginView view) {
        this.loginView = view;
        if (loginView != null) {
            loginView.setAddUserListener(this);
        }
    }

    public void setNGOView(NGOView view) {
        this.ngoView = view;
        if (ngoView != null) {
            ngoView.setModels(donationModel, userModel);
            donationModel.registerObserver(ngoView);
            userModel.registerObserver(ngoView);
        }
    }

    @Override
    public void onAddDonorRequested(String id, String name, String email) {
        Optional<Donor> existingDonor = userModel.findDonorById(id);
        if (existingDonor.isPresent()) {
            loginView.showMessage("Donor ID already exists!");
            return;
        }
        
        Optional<Donor> existingEmail = userModel.findDonorByEmail(email);
        if (existingEmail.isPresent()) {
            loginView.showMessage("Email already registered!");
            return;
        }
        
        userModel.registerDonor(id, name, email);
        loginView.showMessage("Donor registered successfully!");
    }

    @Override
    public void onAddNGORequested(String id, String name, String mission) {
        Optional<NGO> existingNGO = userModel.findNGOById(id);
        if (existingNGO.isPresent()) {
            loginView.showMessage("NGO ID already exists!");
            return;
        }
        
        userModel.registerNGO(id, name, mission);
        loginView.showMessage("NGO registration submitted for verification!");
    }

    public boolean loginDonor(String donorId) {
        return userModel.findDonorById(donorId).isPresent();
    }

    public boolean loginNGO(String ngoId) {
        return userModel.findNGOById(ngoId).isPresent();
    }

    public void startNGOSession(String ngoId) {
        NGOView view = new NGOView(ngoId);
        setNGOView(view);
        view.showMenu();
    }
}