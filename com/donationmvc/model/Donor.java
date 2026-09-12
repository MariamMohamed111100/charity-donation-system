package com.donationmvc.model;

public class Donor {
    private String id;
    private String name;
    private String email;
    private double totalDonated;

    public Donor(String id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.totalDonated = 0.0;
    }

    // Getters and Setters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public double getTotalDonated() { return totalDonated; }
    
    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void addDonation(double amount) { this.totalDonated += amount; }

    @Override
    public String toString() {
        return "Donor[ID: " + id + ", Name: " + name + ", Email: " + email + ", Total: $" + totalDonated + "]";
    }
}