import com.donationmvc.model.*;
import com.donationmvc.controller.*;
import com.donationmvc.view.*;

public class App {
    public static void main(String[] args) {
        // Initialize models
        UserModel userModel = new UserModel();
        DonationModel donationModel = new DonationModel();
        
        // Initialize controllers
        UserController userController = new UserController(userModel, donationModel);
        DonationController donationController = new DonationController(donationModel, userModel);
        AdminController adminController = new AdminController(userModel, donationModel);
        
        // Initialize login view
        LoginView loginView = new LoginView();
        userController.setLoginView(loginView);
        
        // Add some sample data
        initializeSampleData(userModel, donationModel);
        
        System.out.println("=== CHARITY DONATION MANAGEMENT SYSTEM ===");
        System.out.println("Initializing system...");
        System.out.println("Sample data loaded successfully!\n");
        
        // Main application loop
        while (true) {
            String choice = loginView.showMainMenu();
            
            switch (choice) {
                case "1": // Login as Donor
                    String donorId = loginView.showDonorLogin();
                    if (userController.loginDonor(donorId)) {
                        donationController.startDonorSession(donorId);
                    } else {
                        loginView.showMessage("Donor not found!");
                    }
                    break;
                    
                case "2": // Login as NGO
                    String ngoId = loginView.showNGOLogin();
                    if (userController.loginNGO(ngoId)) {
                        userController.startNGOSession(ngoId);
                    } else {
                        loginView.showMessage("NGO not found!");
                    }
                    break;
                    
                case "3": // Login as Admin
                    adminController.startAdminSession();
                    break;
                    
                case "4": // Register as Donor
                    loginView.showDonorRegistration();
                    break;
                    
                case "5": // Register as NGO
                    loginView.showNGORegistration();
                    break;
                    
                case "6": // Exit
                    System.out.println("\nThank you for using Charity System!");
                    System.out.println("Goodbye!");
                    return;
                    
                default:
                    loginView.showMessage("Invalid choice!");
            }
        }
    }
    
    private static void initializeSampleData(UserModel userModel, DonationModel donationModel) {
        // Add sample donors
        userModel.registerDonor("DON001", "Alice Johnson", "alice@email.com");
        userModel.registerDonor("DON002", "Bob Smith", "bob@email.com");
        userModel.registerDonor("DON003", "Charlie Brown", "charlie@email.com");
        
        // Add sample NGOs
        userModel.registerNGO("NGO001", "Save the Children", "Helping children in need");
        userModel.registerNGO("NGO002", "Red Cross", "Emergency relief worldwide");
        userModel.registerNGO("NGO003", "World Food Program", "Fighting hunger globally");
        
        // Verify some NGOs
        userModel.verifyNGO("NGO001");
        userModel.verifyNGO("NGO002");
        
        // Add sample donations
        donationModel.addDonation("DON001", "NGO001", 100.0);
        donationModel.addDonation("DON002", "NGO001", 50.0);
        donationModel.addDonation("DON003", "NGO002", 200.0);
        donationModel.addDonation("DON001", "NGO002", 75.0);
    }
}