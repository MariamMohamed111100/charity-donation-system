package com.donationmvc.model;

import com.donationmvc.model.repository.UserRepository;
import com.donationmvc.observer.Subject;
import com.donationmvc.observer.Observer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserModel implements Subject {
    private final UserRepository repository = new UserRepository();
    private final List<Observer> observers = new ArrayList<>();

    public Donor registerDonor(String id, String name, String email) {
        Donor donor = new Donor(id, name, email);
        repository.addDonor(donor);
        notifyObservers();
        return donor;
    }

    public NGO registerNGO(String id, String name, String mission) {
        NGO ngo = new NGO(id, name, mission);
        repository.addNGO(ngo);
        notifyObservers();
        return ngo;
    }

    public Optional<Donor> findDonorById(String id) {
        return repository.findDonorById(id);
    }

    public Optional<NGO> findNGOById(String id) {
        return repository.findNGOById(id);
    }

    public Optional<Donor> findDonorByEmail(String email) {
        return repository.findDonorByEmail(email);
    }

    public List<Donor> getAllDonors() {
        return repository.getAllDonors();
    }

    public List<NGO> getAllNGOs() {
        return repository.getAllNGOs();
    }

    public List<NGO> getVerifiedNGOs() {
        return repository.getVerifiedNGOs();
    }

    public List<NGO> getPendingVerification() {
        return repository.getPendingVerification();
    }

    public boolean verifyNGO(String ngoId) {
        boolean result = repository.verifyNGO(ngoId);
        if (result) {
            notifyObservers();
        }
        return result;
    }

    public int getTotalUsers() {
        return repository.getTotalUsers();
    }

    public void updateDonorDonation(String donorId, double amount) {
        Optional<Donor> donor = findDonorById(donorId);
        donor.ifPresent(d -> d.addDonation(amount));
        notifyObservers();
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