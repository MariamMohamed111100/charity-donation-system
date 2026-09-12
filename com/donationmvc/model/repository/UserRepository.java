package com.donationmvc.model.repository;

import com.donationmvc.model.Donor;
import com.donationmvc.model.NGO;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserRepository {
    private List<Donor> donors = new ArrayList<>();
    private List<NGO> ngos = new ArrayList<>();

    // Donor methods
    public Donor addDonor(Donor donor) {
        donors.add(donor);
        return donor;
    }

    public Optional<Donor> findDonorById(String id) {
        return donors.stream()
                    .filter(d -> d.getId().equals(id))
                    .findFirst();
    }

    public Optional<Donor> findDonorByEmail(String email) {
        return donors.stream()
                    .filter(d -> d.getEmail().equalsIgnoreCase(email))
                    .findFirst();
    }

    public List<Donor> getAllDonors() {
        return new ArrayList<>(donors);
    }

    // NGO methods
    public NGO addNGO(NGO ngo) {
        ngos.add(ngo);
        return ngo;
    }

    public Optional<NGO> findNGOById(String id) {
        return ngos.stream()
                  .filter(n -> n.getId().equals(id))
                  .findFirst();
    }

    public List<NGO> getAllNGOs() {
        return new ArrayList<>(ngos);
    }

    public List<NGO> getVerifiedNGOs() {
        return ngos.stream()
                  .filter(NGO::isVerified)
                  .toList();
    }

    public List<NGO> getPendingVerification() {
        return ngos.stream()
                  .filter(n -> !n.isVerified())
                  .toList();
    }

    public boolean verifyNGO(String ngoId) {
        Optional<NGO> ngo = findNGOById(ngoId);
        if (ngo.isPresent()) {
            ngo.get().verify();
            return true;
        }
        return false;
    }

    public int getTotalUsers() {
        return donors.size() + ngos.size();
    }
}