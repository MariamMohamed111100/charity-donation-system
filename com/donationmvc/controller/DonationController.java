package com.donationmvc.controller;

import com.donationmvc.model.DonationModel;
import com.donationmvc.model.UserModel;
import com.donationmvc.model.Donor;
import com.donationmvc.model.NGO;
import com.donationmvc.listener.AddDonationListener;
import com.donationmvc.view.DonorView;
import java.util.Optional;

public class DonationController implements AddDonationListener {
    private final DonationModel donationModel;
    private final UserModel userModel;
    private DonorView donorView;

    public DonationController(DonationModel donationModel, UserModel userModel) {
        this.donationModel = donationModel;
        this.userModel = userModel;
    }

    public void setDonorView(DonorView view) {
        this.donorView = view;
        if (donorView != null) {
            donorView.setModels(donationModel, userModel);
            donorView.setAddDonationListener(this);
            donationModel.registerObserver(donorView);
            userModel.registerObserver(donorView);
        }
    }

    @Override
    public void onAddDonationRequested(String donorId, String ngoId, double amount) {
        Optional<Donor> donorOpt = userModel.findDonorById(donorId);
        if (!donorOpt.isPresent()) {
            System.out.println("Donor not found!");
            return;
        }
        
        Optional<NGO> ngoOpt = userModel.findNGOById(ngoId);
        if (!ngoOpt.isPresent()) {
            System.out.println("NGO not found!");
            return;
        }
        
        NGO ngo = ngoOpt.get();
        if (!ngo.isVerified()) {
            System.out.println("NGO is not verified yet!");
            return;
        }
        
        donationModel.addDonation(donorId, ngoId, amount);
        userModel.updateDonorDonation(donorId, amount);
        System.out.println("Donation completed successfully!");
    }

    public void startDonorSession(String donorId) {
        DonorView view = new DonorView(donorId);
        setDonorView(view);
        view.showMenu();
    }
}