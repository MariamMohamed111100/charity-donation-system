package com.donationmvc.model;

import com.donationmvc.model.repository.DonationRepository;
import com.donationmvc.observer.Subject;
import com.donationmvc.observer.Observer;
import java.util.ArrayList;
import java.util.List;

public class DonationModel implements Subject {
    private final DonationRepository repository = new DonationRepository();
    private final List<Observer> observers = new ArrayList<>();

    public Donation addDonation(String donorId, String ngoId, double amount) {
        Donation donation = new Donation("", donorId, ngoId, amount);
        donation.complete();  // ✅ هنا بيحول Status من Pending إلى Completed
        Donation saved = repository.addAndComplete(donation);
        notifyObservers();
        return saved;
}

    public List<Donation> getAllDonations() {
        return repository.findAll();
    }

    public List<Donation> getDonorDonations(String donorId) {
        return repository.findByDonorId(donorId);
    }

    public List<Donation> getNGODonations(String ngoId) {
        return repository.findByNgoId(ngoId);
    }

    public double getTotalDonationsAmount() {
        return repository.getTotalDonations();
    }

    public long getTotalDonationsCount() {
        return repository.count();
    }

    @Override
    public void registerObserver(Observer o) {
        observers.add(o);
    }

    @Override
    public void removeObserver(Observer o) {
        observers.remove(o);
    }

    @Override
    public void notifyObservers() {
        for (Observer o : observers) {
            o.update();
        }
    }
}