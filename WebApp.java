import com.donationmvc.model.DonationModel;
import com.donationmvc.model.UserModel;
import com.donationmvc.web.ApiServer;
import com.sun.net.httpserver.HttpServer;

import java.net.BindException;

public class WebApp {
    public static void main(String[] args) throws Exception {
        UserModel userModel = new UserModel();
        DonationModel donationModel = new DonationModel();
        initializeSampleData(userModel, donationModel);

        int requestedPort = args.length > 0 ? Integer.parseInt(args[0]) : 8080;

        ApiServer api = null;
        int port = requestedPort;
        int tries = 0;
        while (tries < 6) {
            tries++;
            try {
                api = new ApiServer(port, userModel, donationModel, ApiServer.defaultWebRoot());
                break;
            } catch (BindException e) {
                port++;
            }
        }
        if (api == null) {
            System.out.println("Could not find a free port. Close something and try again!");
            return;
        }
        api.start();
        HttpServer server = api.getServer();
        int usedPort = server.getAddress().getPort();
        System.out.println("=== DONATIONVERSE WEB (with 3D fun) ===");
        System.out.println("Live at: http://localhost:" + usedPort);
        System.out.println("Console version still works with: java App");
        System.out.println("Press Ctrl+C to stop.");
    }

    private static void initializeSampleData(UserModel userModel, DonationModel donationModel) {
        userModel.registerDonor("DON001", "Alice Johnson", "alice@email.com");
        userModel.registerDonor("DON002", "Bob Smith", "bob@email.com");
        userModel.registerDonor("DON003", "Charlie Brown", "charlie@email.com");

        userModel.registerNGO("NGO001", "Save the Children", "Helping children in need");
        userModel.registerNGO("NGO002", "Red Cross", "Emergency relief worldwide");
        userModel.registerNGO("NGO003", "World Food Program", "Fighting hunger globally");
        userModel.registerNGO("NGO004", "Team Whiskers", "Rescuing cats one meow at a time");

        userModel.verifyNGO("NGO001");
        userModel.verifyNGO("NGO002");

        donationModel.addDonation("DON001", "NGO001", 100.0);
        donationModel.addDonation("DON002", "NGO001", 50.0);
        donationModel.addDonation("DON003", "NGO002", 200.0);
        donationModel.addDonation("DON001", "NGO002", 75.0);
    }
}