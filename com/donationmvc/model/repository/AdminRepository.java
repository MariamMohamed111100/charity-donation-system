package com.donationmvc.model.repository;

import com.donationmvc.model.Admin;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AdminRepository {
    private List<Admin> admins = new ArrayList<>();

    public AdminRepository() {
        // Add default admin
        admins.add(new Admin("ADM001", "admin", "admin123"));
    }

    public Optional<Admin> findByUsername(String username) {
        return admins.stream()
                    .filter(a -> a.getUsername().equals(username))
                    .findFirst();
    }

    public Optional<Admin> findById(String id) {
        return admins.stream()
                    .filter(a -> a.getId().equals(id))
                    .findFirst();
    }

    public List<Admin> getAllAdmins() {
        return new ArrayList<>(admins);
    }

    public Admin addAdmin(Admin admin) {
        admins.add(admin);
        return admin;
    }

    public boolean authenticate(String username, String password) {
        return admins.stream()
                    .anyMatch(a -> a.getUsername().equals(username) && 
                                  a.getPassword().equals(password));
    }
}