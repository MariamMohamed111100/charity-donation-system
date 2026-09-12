package com.donationmvc.view;

import com.donationmvc.model.*;
import com.donationmvc.model.repository.AdminRepository;
import com.donationmvc.observer.Observer;
import com.donationmvc.listener.AdminActionListener;
import java.util.List;
import java.util.Scanner;

public class AdminView implements Observer {
    private DonationModel donationModel;
    private UserModel userModel;
    private AdminActionListener adminActionListener;
    private Scanner scanner = new Scanner(System.in);
    private AdminRepository adminRepo = new AdminRepository();

    public void setModels(DonationModel donationModel, UserModel userModel) {
        this.donationModel = donationModel;
        this.userModel = userModel;
    }

    public void setAdminActionListener(AdminActionListener listener) {
        this.adminActionListener = listener;
    }

    public boolean login() {
        System.out.println("\n=== ADMIN LOGIN ===");
        System.out.print("Username: ");
        String username = scanner.nextLine();
        System.out.print("Password: ");
        String password = scanner.nextLine();

        return adminRepo.authenticate(username, password);
    }

    public void showDashboard() {
        System.out.println("\n=== ADMIN DASHBOARD ===");
        System.out.println("Total Users: " + userModel.getTotalUsers());
        System.out.println("Total Donations: " + donationModel.getTotalDonationsCount());
        System.out.println("Total Amount: $" + donationModel.getTotalDonationsAmount());
        System.out.println("Pending Verifications: " + userModel.getPendingVerification().size());
        System.out.println("========================\n");
    }

    public void showUsers() {
        System.out.println("\n=== ALL USERS ===");
        
        System.out.println("\n--- DONORS ---");
        List<Donor> donors = userModel.getAllDonors();
        if (donors.isEmpty()) {
            System.out.println("No donors yet.");
        } else {
            donors.forEach(System.out::println);
        }
        
        System.out.println("\n--- NGOs ---");
        List<NGO> ngos = userModel.getAllNGOs();
        if (ngos.isEmpty()) {
            System.out.println("No NGOs yet.");
        } else {
            ngos.forEach(System.out::println);
        }
        
        System.out.println("================\n");
    }

    public void showDonations() {
        System.out.println("\n=== ALL DONATIONS ===");
        List<Donation> donations = donationModel.getAllDonations();
        if (donations.isEmpty()) {
            System.out.println("No donations yet.");
        } else {
            donations.forEach(System.out::println);
        }
        System.out.println("======================\n");
    }

    public void showPendingVerifications() {
        List<NGO> pending = userModel.getPendingVerification();
        System.out.println("\n=== PENDING VERIFICATIONS ===");
        if (pending.isEmpty()) {
            System.out.println("No pending verifications.");
        } else {
            pending.forEach(n -> System.out.println(n.getId() + " - " + n.getName()));
            
            System.out.print("\nEnter NGO ID to verify (or 'cancel'): ");
            String ngoId = scanner.nextLine();
            
            if (!ngoId.equalsIgnoreCase("cancel") && adminActionListener != null) {
                adminActionListener.onVerifyNGORequested(ngoId);                
            }
        }
        System.out.println("==============================\n");
    }

    public void showReports() {
        System.out.println("\n=== SYSTEM REPORTS ===");
        System.out.println("1. Financial Summary");
        System.out.println("2. User Statistics");
        System.out.println("3. NGO Performance");
        System.out.print("Choose report: ");
        
        String choice = scanner.nextLine();
        
        switch (choice) {
            case "1":
                System.out.println("\n--- Financial Summary ---");
                System.out.println("Total Donations: $" + donationModel.getTotalDonationsAmount());
                System.out.println("Total Transactions: " + donationModel.getTotalDonationsCount());
                break;
            case "2":
                System.out.println("\n--- User Statistics ---");
                System.out.println("Total Donors: " + userModel.getAllDonors().size());
                System.out.println("Total NGOs: " + userModel.getAllNGOs().size());
                System.out.println("Verified NGOs: " + userModel.getVerifiedNGOs().size());
                break;
            case "3":
                System.out.println("\n--- NGO Performance ---");
                List<NGO> ngos = userModel.getVerifiedNGOs();
                if (ngos.isEmpty()) {
                    System.out.println("No verified NGOs yet.");
                } else {
                    ngos.forEach(n -> {
                        double total = donationModel.getNGODonations(n.getId()).stream()
                                                   .filter(d -> d.getStatus().equals("Completed"))
                                                   .mapToDouble(Donation::getAmount)
                                                   .sum();
                        System.out.println(n.getName() + ": $" + total);
                    });
                }
                break;
            default:
                System.out.println("Invalid choice!");
        }
    }

    public void showMenu() {
        while (true) {
            System.out.println("\n1. View Dashboard");
            System.out.println("2. View All Users");
            System.out.println("3. View All Donations");
            System.out.println("4. Pending Verifications");
            System.out.println("5. Generate Reports");
            System.out.println("6. Logout");
            System.out.print("Choose: ");
            
            String choice = scanner.nextLine();
            
            switch (choice) {
                case "1":
                    showDashboard();
                    break;
                case "2":
                    showUsers();
                    break;
                case "3":
                    showDonations();
                    break;
                case "4":
                    showPendingVerifications();
                    break;
                case "5":
                    showReports();
                    break;
                case "6":
                    System.out.println("Logging out...");
                    return;
                default:
                    System.out.println("Invalid choice!");
            }
        }
    }

    @Override
    public void update() {
        System.out.println("\n[System updated - refreshing dashboard...]");
        showDashboard();
    }
}