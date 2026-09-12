package com.donationmvc.view;

import com.donationmvc.listener.AddUserListener;
import java.util.Scanner;

public class LoginView {
    private AddUserListener addUserListener;
    private Scanner scanner = new Scanner(System.in);

    public void setAddUserListener(AddUserListener listener) {
        this.addUserListener = listener;
    }

    public String showMainMenu() {
        System.out.println("\n=== CHARITY DONATION SYSTEM ===");
        System.out.println("1. Login as Donor");
        System.out.println("2. Login as NGO");
        System.out.println("3. Login as Admin");
        System.out.println("4. Register as Donor");
        System.out.println("5. Register as NGO");
        System.out.println("6. Exit");
        System.out.print("Choose: ");
        
        return scanner.nextLine();
    }

    public String showDonorLogin() {
        System.out.println("\n=== DONOR LOGIN ===");
        System.out.print("Enter Donor ID: ");
        return scanner.nextLine();
    }

    public String showNGOLogin() {
        System.out.println("\n=== NGO LOGIN ===");
        System.out.print("Enter NGO ID: ");
        return scanner.nextLine();
    }

    public void showDonorRegistration() {
        System.out.println("\n=== DONOR REGISTRATION ===");
        System.out.print("Enter ID: ");
        String id = scanner.nextLine();
        System.out.print("Enter Name: ");
        String name = scanner.nextLine();
        System.out.print("Enter Email: ");
        String email = scanner.nextLine();

        if (addUserListener != null) {
            addUserListener.onAddDonorRequested(id, name, email);
            System.out.println("Registration successful! Your ID: " + id);
        }
    }

    public void showNGORegistration() {
        System.out.println("\n=== NGO REGISTRATION ===");
        System.out.print("Enter ID: ");
        String id = scanner.nextLine();
        System.out.print("Enter Name: ");
        String name = scanner.nextLine();
        System.out.print("Enter Mission: ");
        String mission = scanner.nextLine();

        if (addUserListener != null) {
            addUserListener.onAddNGORequested(id, name, mission);
            System.out.println("Registration submitted! Awaiting verification. Your ID: " + id);
        }
    }

    public void showMessage(String message) {
        System.out.println("\n[System]: " + message);
    }
}