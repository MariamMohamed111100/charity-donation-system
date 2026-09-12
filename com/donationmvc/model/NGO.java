package com.donationmvc.model;

import java.util.ArrayList;
import java.util.List;

public class NGO {
    private String id;
    private String name;
    private String mission;
    private boolean verified;
    private List<String> campaigns;

    public NGO(String id, String name, String mission) {
        this.id = id;
        this.name = name;
        this.mission = mission;
        this.verified = false;
        this.campaigns = new ArrayList<>();
    }

    // Getters and Setters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getMission() { return mission; }
    public boolean isVerified() { return verified; }
    public List<String> getCampaigns() { return campaigns; }
    
    public void verify() { this.verified = true; }
    public void addCampaign(String campaign) { campaigns.add(campaign); }

    @Override
    public String toString() {
        return "NGO[ID: " + id + ", Name: " + name + ", Verified: " + verified + "]";
    }
}