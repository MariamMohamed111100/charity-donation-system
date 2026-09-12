package com.donationmvc.model;

import java.time.LocalDate;

public class Donation {
    private String id;
    private String donorId;
    private String ngoId;
    private double amount;
    private LocalDate date;
    private String status;

    public Donation(String id, String donorId, String ngoId, double amount) {
        this.id = id;
        this.donorId = donorId;
        this.ngoId = ngoId;
        this.amount = amount;
        this.date = LocalDate.now();
        this.status = "Pending";
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDonorId() { return donorId; }
    public String getNgoId() { return ngoId; }
    public double getAmount() { return amount; }
    public LocalDate getDate() { return date; }
    public String getStatus() { return status; }
    
    public void setStatus(String status) { this.status = status; }
    public void complete() { this.status = "Completed"; }
    public void cancel() { this.status = "Cancelled"; }

    @Override
    public String toString() {
        return "Donation[ID: " + id + ", Donor: " + donorId + ", NGO: " + ngoId + 
               ", Amount: $" + amount + ", Date: " + date + ", Status: " + status + "]";
    }
}