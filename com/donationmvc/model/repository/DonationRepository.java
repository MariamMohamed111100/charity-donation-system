package com.donationmvc.model.repository;

import com.donationmvc.model.Donation;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DonationRepository {
    private List<Donation> donations = new ArrayList<>();
    private static int nextId = 1;

    public Donation addAndComplete(Donation donation) {
        donation.setId("DON" + String.format("%03d", nextId++));
        donation.complete();  // ✅ أكمل التبرع فور إضافته
        donations.add(donation);
        return donation;
    }

    public List<Donation> findAll() {
        return new ArrayList<>(donations);
    }

    public Optional<Donation> findById(String id) {
        return donations.stream()
                       .filter(d -> d.getId().equals(id))
                       .findFirst();
    }

    public List<Donation> findByDonorId(String donorId) {
        return donations.stream()
                       .filter(d -> d.getDonorId().equals(donorId))
                       .toList();
    }

    public List<Donation> findByNgoId(String ngoId) {
        return donations.stream()
                       .filter(d -> d.getNgoId().equals(ngoId))
                       .toList();
    }

    public boolean remove(String id) {
        return donations.removeIf(d -> d.getId().equals(id));
    }

    public long count() {
        return donations.size();
    }

    public double getTotalDonations() {
        return donations.stream()
                       .filter(d -> d.getStatus().equals("Completed"))  // ✅ فقط Completed
                       .mapToDouble(Donation::getAmount)
                       .sum();
    }
}