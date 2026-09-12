package com.donationmvc.view;

import com.donationmvc.model.*;
import com.donationmvc.observer.Observer;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

public class NGOView implements Observer {
    private DonationModel donationModel;
    private UserModel userModel;
    private String currentNGOId;
    private Scanner scanner = new Scanner(System.in);

    public NGOView(String ngoId) {
        this.currentNGOId = ngoId;
    }

    public void setModels(DonationModel donationModel, UserModel userModel) {
        this.donationModel = donationModel;
        this.userModel = userModel;
    }

    public void showDashboard() {
        System.out.println("\n=== NGO DASHBOARD ===");
        Optional<NGO> ngo = userModel.findNGOById(currentNGOId);
        if (ngo.isPresent()) {
            NGO n = ngo.get();
            System.out.println("Name: " + n.getName());
            System.out.println("Mission: " + n.getMission());
            System.out.println("Verified: " + (n.isVerified() ? "YES" : "NO"));
            System.out.println("Campaigns: " + n.getCampaigns().size());
        }
        System.out.println("====================\n");
    }

    public void showDonations() {
        List<Donation> donations = donationModel.getNGODonations(currentNGOId);
        System.out.println("\n=== DONATIONS RECEIVED ===");
        if (donations.isEmpty()) {
            System.out.println("No donations received yet.");
        } else {
            double total = donations.stream()
                                  .filter(d -> d.getStatus().equals("Completed"))
                                  .mapToDouble(Donation::getAmount)
                                  .sum();
            System.out.println("Total Received: $" + total);
            donations.forEach(System.out::println);
        }
        System.out.println("===========================\n");
    }

    public void addCampaign() {
        Optional<NGO> ngo = userModel.findNGOById(currentNGOId);
        if (ngo.isPresent()) {
            System.out.print("Enter campaign name: ");
            String campaign = scanner.nextLine();
            ngo.get().addCampaign(campaign);
            System.out.println("Waiting for admin verification");
        }
    }

    public void showMenu() {
        while (true) {
            System.out.println("\n1. View Dashboard");
            System.out.println("2. View Donations");
            System.out.println("3. Add Campaign");
            System.out.println("4. Logout");
            System.out.print("Choose: ");
            
            String choice = scanner.nextLine();
            
            switch (choice) {
                case "1":
                    showDashboard();
                    break;
                case "2":
                    showDonations();
                    break;
                case "3":
                    addCampaign();
                    break;
                case "4":
                    System.out.println("Logging out...");
                    return;
                default:
                    System.out.println("Invalid choice!");
            }
        }
    }

    @Override
    public void update() {
        System.out.println("\n[Data updated - refreshing dashboard...]");
        showDashboard();
    }
}