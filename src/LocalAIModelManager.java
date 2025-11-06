import javax.swing.JOptionPane;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Manages models on a LocalAI instance via its management API with proper verification.
 */
public class LocalAIModelManager {
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    /**
     * Unloads a model and verifies it's actually unloaded
     */
    public static boolean unloadModel(String baseUrl, String modelName) {
        if (!validateInputs(baseUrl, modelName)) {
            return false;
        }

        String base = normalizeBaseUrl(baseUrl);

        // First, check if the model is actually loaded
        if (!isModelLoaded(base, modelName)) {
            JOptionPane.showMessageDialog(null,
                    String.format("Model '%s' is not currently loaded.", modelName),
                    "Model Not Loaded",
                    JOptionPane.INFORMATION_MESSAGE);
            return true; // Consider it "unloaded" if it's not loaded
        }

        System.out.println("Model '" + modelName + "' is currently loaded. Attempting to unload...");

        // Try the backend shutdown endpoint (most reliable for Docker)
        boolean unloadSuccess = attemptBackendShutdown(base, modelName);

        if (unloadSuccess) {
            // Wait a bit and verify the model is actually unloaded
            boolean verified = verifyModelUnloaded(base, modelName, 5, 1000);

            if (verified) {
                showSuccessMessage(modelName);
                return true;
            } else {
                showVerificationFailedMessage(modelName);
                return false;
            }
        } else {
            showUnloadFailedMessage(modelName);
            return false;
        }
    }

    /**
     * Check if a model is currently loaded and ready
     */
    public static boolean isModelLoaded(String baseUrl, String modelName) {
        try {
            String modelsJson = listModels(baseUrl);
            if (modelsJson == null) return false;

            // Simple check - if the model name appears in the models list, it's probably loaded
            return modelsJson.contains("\"" + modelName + "\"");

        } catch (Exception e) {
            System.err.println("Error checking if model is loaded: " + e.getMessage());
            return false;
        }
    }

    /**
     * Attempt to unload using the backend shutdown endpoint
     */
    private static boolean attemptBackendShutdown(String baseUrl, String modelName) {
        String url = baseUrl + "/backend/shutdown/" + modelName;
        System.out.println("Attempting backend shutdown: " + url);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Backend shutdown response - Status: " + response.statusCode() + ", Body: " + response.body());

            // Some versions return 200, some return 204
            boolean success = response.statusCode() == 200 || response.statusCode() == 204;

            if (success) {
                System.out.println("Backend shutdown API call successful");
            } else {
                System.err.println("Backend shutdown API call failed with status: " + response.statusCode());
            }

            return success;

        } catch (Exception e) {
            System.err.println("Backend shutdown failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Verify the model is actually unloaded by checking multiple times
     */
    private static boolean verifyModelUnloaded(String baseUrl, String modelName, int maxAttempts, long delayMs) {
        System.out.println("Verifying model unload...");

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                System.out.println("Verification attempt " + attempt + "/" + maxAttempts);

                // Wait before checking
                if (attempt > 1) {
                    Thread.sleep(delayMs);
                }

                boolean stillLoaded = isModelLoaded(baseUrl, modelName);

                if (!stillLoaded) {
                    System.out.println("✅ Verification successful - model is unloaded");
                    return true;
                } else {
                    System.out.println("⚠️ Model still appears to be loaded (attempt " + attempt + ")");
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("Error during verification attempt " + attempt + ": " + e.getMessage());
            }
        }

        System.out.println("❌ Verification failed - model still appears to be loaded after " + maxAttempts + " attempts");
        return false;
    }

    /**
     * Forceful unload - tries multiple methods including Docker restart
     */
    public static boolean forceUnloadModel(String baseUrl, String modelName, String dockerContainerName) {
        System.out.println("Attempting forceful unload of model: " + modelName);

        // Try normal unload first
        if (unloadModel(baseUrl, modelName)) {
            return true;
        }

        // If normal unload failed, try Docker restart
        if (dockerContainerName != null && !dockerContainerName.trim().isEmpty()) {
            JOptionPane.showMessageDialog(null,
                    "Normal unload failed. Attempting Docker container restart...",
                    "Forceful Unload",
                    JOptionPane.WARNING_MESSAGE);

            return restartDockerContainer(dockerContainerName);
        }

        return false;
    }

    /**
     * Restart the entire Docker container (nuclear option)
     */
    public static boolean restartDockerContainer(String containerName) {
        try {
            System.out.println("Restarting Docker container: " + containerName);

            Process process = Runtime.getRuntime().exec(
                    new String[]{"docker", "restart", containerName}
            );

            int exitCode = process.waitFor();

            if (exitCode == 0) {
                System.out.println("✅ Docker container restarted successfully");

                // Wait for LocalAI to come back up
                Thread.sleep(10000); // Wait 10 seconds

                JOptionPane.showMessageDialog(null,
                        "Docker container restarted successfully.\nLocalAI should be ready in a few moments.",
                        "Container Restarted",
                        JOptionPane.INFORMATION_MESSAGE);
                return true;
            } else {
                System.err.println("❌ Docker restart failed with exit code: " + exitCode);
                return false;
            }

        } catch (Exception e) {
            System.err.println("Docker restart failed: " + e.getMessage());
            JOptionPane.showMessageDialog(null,
                    "Docker restart failed: " + e.getMessage() +
                            "\n\nPlease restart the container manually:\ndocker restart " + containerName,
                    "Docker Restart Failed",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    /**
     * List all models (existing method)
     */
    public static String listModels(String baseUrl) {
        try {
            String url = normalizeBaseUrl(baseUrl) + "/v1/models";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return response.body();
            } else {
                System.err.println("Failed to list models. Status: " + response.statusCode());
                return null;
            }

        } catch (Exception e) {
            System.err.println("Error listing models: " + e.getMessage());
            return null;
        }
    }

    private static boolean validateInputs(String baseUrl, String modelName) {
        if (baseUrl == null || baseUrl.trim().isEmpty() || modelName == null || modelName.trim().isEmpty()) {
            JOptionPane.showMessageDialog(null,
                    "Base URL or Model Name is missing.",
                    "Unload Error",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    private static String normalizeBaseUrl(String baseUrl) {
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private static void showSuccessMessage(String modelName) {
        String successMsg = String.format(
                "✅ Model '%s' successfully unloaded and verified!\n\n" +
                        "GPU memory should now be freed up.\n" +
                        "You can now load a different model.",
                modelName
        );

        System.out.println(successMsg);
        JOptionPane.showMessageDialog(null, successMsg, "Model Unloaded", JOptionPane.INFORMATION_MESSAGE);
    }

    private static void showVerificationFailedMessage(String modelName) {
        String errorMsg = String.format(
                "⚠️ Model '%s' unload API succeeded but verification failed!\n\n" +
                        "The model might still be loaded due to:\n" +
                        "• Auto-reload features in LocalAI\n" +
                        "• Model persistence settings\n" +
                        "• Docker volume configurations\n\n" +
                        "Try using 'Force Unload' or restart the Docker container.",
                modelName
        );

        System.err.println(errorMsg);
        JOptionPane.showMessageDialog(null, errorMsg, "Verification Failed", JOptionPane.WARNING_MESSAGE);
    }

    private static void showUnloadFailedMessage(String modelName) {
        String errorMsg = String.format(
                "❌ Failed to unload model '%s'!\n\n" +
                        "The unload API call failed.\n" +
                        "This model might not support dynamic unloading.\n\n" +
                        "You may need to restart the Docker container.",
                modelName
        );

        System.err.println(errorMsg);
        JOptionPane.showMessageDialog(null, errorMsg, "Unload Failed", JOptionPane.ERROR_MESSAGE);
    }
}