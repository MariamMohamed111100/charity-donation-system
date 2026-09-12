package com.donationmvc.view;

import com.donationmvc.model.*;
import com.donationmvc.observer.Observer;
import com.donationmvc.listener.AddDonationListener;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

public class DonorView implements Observer {
    private DonationModel donationModel;
    private UserModel userModel;
    private AddDonationListener addDonationListener;
    private String currentDonorId;
    private Scanner scanner = new Scanner(System.in);

    public DonorView(String donorId) {
        this.currentDonorId = donorId;
    }

    public void setModels(DonationModel donationModel, UserModel userModel) {
        this.donationModel = donationModel;
        this.userModel = userModel;
    }

    public void setAddDonationListener(AddDonationListener listener) {
        this.addDonationListener = listener;
    }

    public void showDashboard() {
        System.out.println("\n=== DONOR DASHBOARD ===");
        System.out.println("Logged in as: " + currentDonorId);
        Optional<Donor> donor = userModel.findDonorById(currentDonorId);
        if (donor.isPresent()) {
            Donor d = donor.get();
            System.out.println("Name: " + d.getName());
            System.out.println("Email: " + d.getEmail());
            System.out.println("Total Donated: $" + d.getTotalDonated());
        }
        System.out.println("=====================\n");
    }

    public void showDonationHistory() {
        List<Donation> donations = donationModel.getDonorDonations(currentDonorId);
        System.out.println("\n=== YOUR DONATION HISTORY ===");
        if (donations.isEmpty()) {
            System.out.println("No donations yet.");
        } else {
            donations.forEach(System.out::println);
        }
        System.out.println("=============================\n");
    }

    public void showAvailableNGOs() {
        List<NGO> ngos = userModel.getVerifiedNGOs();
        System.out.println("\n=== VERIFIED NGOs ===");
        if (ngos.isEmpty()) {
            System.out.println("No NGOs available.");
        } else {
            ngos.forEach(n -> System.out.println(n.getId() + " - " + n.getName() + ": " + n.getMission()));
        }
        System.out.println("====================\n");
    }

    public void displayDonationForm() {
        System.out.println("\n=== MAKE A DONATION ===");
        
        showAvailableNGOs();
        
        System.out.print("Enter NGO ID: ");
        String ngoId = scanner.nextLine();
        
        System.out.print("Enter Amount: $");
        double amount = 0;
        try {
            amount = Double.parseDouble(scanner.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("Invalid amount!");
            return;
        }

        if (amount <= 0) {
            System.out.println("Amount must be positive!");
            return;
        }

        if (addDonationListener != null) {
            addDonationListener.onAddDonationRequested(currentDonorId, ngoId, amount);
        }
    }

    public void showMenu() {
        while (true) {
            System.out.println("\n1. View Dashboard");
            System.out.println("2. View Donation History");
            System.out.println("3. View Available NGOs");
            System.out.println("4. Make Donation");
            System.out.println("5. Logout");
            System.out.print("Choose: ");
            
            String choice = scanner.nextLine();
            
            switch (choice) {
                case "1":
                    showDashboard();
                    break;
                case "2":
                    showDonationHistory();
                    break;
                case "3":
                    showAvailableNGOs();
                    break;
                case "4":
                    displayDonationForm();
                    break;
                case "5":
                    System.out.println("Logging out...");
                    return;
                default:
                    System.out.println("Invalid choice!");
            }
        }
    }

    @Override
    public void update() {
        System.out.println("\n[Data updated - refreshing display...]");
        showDashboard();
    }
}