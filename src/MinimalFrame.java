import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

// NOTE: This class assumes FileSaver.java, FileLoader.java, and LocalAIModelManager.java are compiled and available.
public class MinimalFrame {
    private static JTextArea responseArea;
    private static JTextField apiEndpointField;
    private static JComboBox<String> modelComboBox;
    private static JComboBox<String> onlineModelComboBox;
    private static JTextArea inputArea;
    private static JTextArea chatArea; // Made instance variable for access in window listener

    // --- MAIN METHOD ---
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame();
            frame.setTitle("AI Chat Client");
            frame.setSize(1280, 1400);
            frame.setResizable(false);
            frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); // Changed to handle manually

            // Add window listener to handle exit with save prompt
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    promptForSaveAndExit(frame);
                }
            });

            frame.setLayout(null);

            // Create radio buttons
            JRadioButton networkButton = new JRadioButton("Network");
            JRadioButton onlineButton = new JRadioButton("Online");
            networkButton.setSelected(false);

            // Group the radio buttons
            ButtonGroup group = new ButtonGroup();
            group.add(networkButton);
            group.add(onlineButton);

            // Create buttons
            JButton loadButton = new JButton("Load Convo");
            JButton saveButton = new JButton("Save Convo");
            JButton sendButton = new JButton("Send");
            JButton refreshButton = new JButton("Refresh Models");

            // Helper method to safely get the ActionListener (needed for radio buttons)
            ActionListener[] networkListeners = networkButton.getActionListeners();
            ActionListener[] onlineListeners = onlineButton.getActionListeners();

            // --- START: COMPLETE KEY BINDING SETUP (F1, F2, F9, F10, F11, F12) ---
            InputMap inputMap = frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
            ActionMap actionMap = frame.getRootPane().getActionMap();

            // F1 BINDING (Network Radio Button)
            final String F1_ACTION_KEY = "selectNetworkModel";
            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0), F1_ACTION_KEY);
            actionMap.put(F1_ACTION_KEY, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (!networkButton.isSelected() && networkListeners.length > 0) {
                        networkButton.setSelected(true);
                        networkListeners[0].actionPerformed(e);
                    }
                }
            });

            // F2 BINDING (Online Radio Button)
            final String F2_ACTION_KEY = "selectOnlineModel";
            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), F2_ACTION_KEY);
            actionMap.put(F2_ACTION_KEY, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (!onlineButton.isSelected() && onlineListeners.length > 0) {
                        onlineButton.setSelected(true);
                        onlineListeners[0].actionPerformed(e);
                    }
                }
            });

            // F9 BINDING (Send Button)
            final String F9_ACTION_KEY = "sendButtonF9Press";
            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0), F9_ACTION_KEY);
            actionMap.put(F9_ACTION_KEY, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (sendButton.isEnabled()) {
                        sendButton.doClick();
                    }
                }
            });

            // F10 BINDING (Load Convo Button)
            final String F10_ACTION_KEY = "loadButtonF10Press";
            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0), F10_ACTION_KEY);
            actionMap.put(F10_ACTION_KEY, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (loadButton.isEnabled()) {
                        loadButton.doClick();
                    }
                }
            });

            // F11 BINDING (Save Convo Button)
            final String F11_ACTION_KEY = "saveButtonF11Press";
            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0), F11_ACTION_KEY);
            actionMap.put(F11_ACTION_KEY, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (saveButton.isEnabled()) {
                        saveButton.doClick();
                    }
                }
            });

            // F12 BINDING (Refresh Models Button)
            final String F12_ACTION_KEY = "refreshButtonF12Press";
            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F12, 0), F12_ACTION_KEY);
            actionMap.put(F12_ACTION_KEY, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (refreshButton.isEnabled()) {
                        refreshButton.doClick();
                    }
                }
            });

            // --- END: COMPLETE KEY BINDING SETUP ---

            // Create scrollable text areas
            chatArea = new JTextArea(); // Now instance variable
            chatArea.setEditable(false);
            JScrollPane scrollPane1 = new JScrollPane(chatArea);

            responseArea = new JTextArea();
            responseArea.setEditable(false);
            JScrollPane scrollPane2 = new JScrollPane(responseArea);

            // Create input text area
            inputArea = new JTextArea();
            inputArea.setLineWrap(true);
            inputArea.setWrapStyleWord(true);
            JScrollPane inputScrollPane = new JScrollPane(inputArea);

            // Create text field for API endpoint
            apiEndpointField = new JTextField();
            apiEndpointField.setText("http://192.168.0.25:8080");

            // Create combo box for LocalAI model selection
            modelComboBox = new JComboBox<>();
            modelComboBox.addItem("Loading...");

            // Create second combo box for online model selection
            onlineModelComboBox = new JComboBox<>();
            onlineModelComboBox.addItem("Deepseek");
            onlineModelComboBox.addItem("Gemini");
            onlineModelComboBox.addItem("ChatGPT");
            onlineModelComboBox.setEnabled(false);

            // Set positions
            networkButton.setBounds(10, 10, 100, 25);
            onlineButton.setBounds(120, 10, 100, 25);
            loadButton.setBounds(230, 10, 100, 25);
            saveButton.setBounds(340, 10, 100, 25);
            apiEndpointField.setBounds(450, 10, 200, 25);
            refreshButton.setBounds(920, 10, 120, 25);
            modelComboBox.setBounds(660, 10, 120, 25);
            onlineModelComboBox.setBounds(790, 10, 120, 25);

            scrollPane1.setBounds(10, 50, 1260, 600);
            inputScrollPane.setBounds(10, 710, 1000, 200);
            scrollPane2.setBounds(10, 920, 1260, 200);
            sendButton.setBounds(1020, 710, 100, 25);

            // Add action listeners
            networkButton.addActionListener(e -> {
                modelComboBox.setEnabled(true);
                onlineModelComboBox.setEnabled(false);
                apiEndpointField.setEnabled(true);
                refreshButton.setEnabled(true);
                //scanForModels();
            });

            onlineButton.addActionListener(e -> {
                modelComboBox.setEnabled(false);
                onlineModelComboBox.setEnabled(true);
                apiEndpointField.setEnabled(false);
                refreshButton.setEnabled(false);
            });

            refreshButton.addActionListener(e -> scanForModels());
            sendButton.addActionListener(new SendButtonListener(chatArea, onlineButton));

            // --- UPDATED ACTION LISTENERS FOR EXTERNAL FILE HANDLERS ---
            loadButton.addActionListener(e -> {
                String loadedContent = FileLoader.loadFile(frame); // Use frame as parent
                if (loadedContent != null) {
                    chatArea.setText(loadedContent.trim());
                }
            });
            saveButton.addActionListener(e -> FileSaver.saveConversation(frame, chatArea.getText()));
            // --- END UPDATED LISTENERS ---

            // Ctrl+Enter for Send is on the inputArea, which is correct
            inputArea.addKeyListener(new java.awt.event.KeyAdapter() {
                public void keyPressed(java.awt.event.KeyEvent e) {
                    if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER && e.isControlDown()) {
                        sendButton.doClick();
                        e.consume();
                    }
                }
            });

            // Add to frame
            frame.add(networkButton);
            frame.add(onlineButton);
            frame.add(loadButton);
            frame.add(saveButton);
            frame.add(refreshButton);
            frame.add(apiEndpointField);
            frame.add(modelComboBox);
            frame.add(onlineModelComboBox);
            frame.add(inputScrollPane);
            frame.add(scrollPane1);
            frame.add(scrollPane2);
            frame.add(sendButton);
            //scanForModels();
            frame.setVisible(true);
        });
    }

    // --- NEW METHOD: Prompt for save on exit ---
    private static void promptForSaveAndExit(JFrame frame) {
        String conversation = chatArea.getText().trim();

        // Only prompt if there's actual conversation content
        if (!conversation.isEmpty() &&
                !conversation.equals("You: \n\nAI: ") && // Default empty state
                !conversation.equals("You: \n\n")) {

            int result = JOptionPane.showConfirmDialog(
                    frame,
                    "Would you like to save the conversation before exiting?",
                    "Save Conversation?",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE
            );

            if (result == JOptionPane.YES_OPTION) {
                // Save the conversation (same as clicking the Save Convo button)
                FileSaver.saveConversation(frame, conversation);
                System.exit(0);
            } else if (result == JOptionPane.NO_OPTION) {
                // Exit without saving
                System.exit(0);
            }
            // If CANCEL, do nothing (window stays open)
        } else {
            // No conversation to save, just exit
            System.exit(0);
        }
    }

    // --- UTILITY AND CLIENT METHODS ---

    // Generic POST sender for services that use API key in the Authorization header (Deepseek, ChatGPT)
    private static String sendPostRequestWithApiKey(String endpoint, String requestBody, String apiKey) throws IOException {
        URL url = new URL(endpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("Authorization", "Bearer " + apiKey);
        connection.setDoOutput(true);
        connection.setConnectTimeout(120000);
        connection.setReadTimeout(120000);

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine);
                }
                return response.toString();
            }
        } else {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
                StringBuilder errorResponse = new StringBuilder();
                String errorLine;
                while ((errorLine = br.readLine()) != null) {
                    errorResponse.append(errorLine);
                }
                throw new IOException("Online API error " + responseCode + ": " + errorResponse);
            }
        }
    }

    // Logic to route the request and securely retrieve API key from environment
    private static String sendOnlineChatRequest(String model, String prompt) throws IOException {
        if (model == null) {
            throw new IOException("No online model selected.");
        }

        String escapedPrompt = prompt.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");

        if (model.equals("Deepseek") || model.equals("ChatGPT")) {

            String apiKey;
            String endpoint;
            String llmModel;

            if (model.equals("Deepseek")) {
                apiKey = System.getenv("DEEPSEEK_API_KEY");
                endpoint = "https://api.deepseek.com/chat/completions";
                llmModel = "deepseek-chat";
                if (apiKey == null || apiKey.isEmpty()) throw new IOException("DEEPSEEK_API_KEY environment variable is not set.");
            } else { // ChatGPT (OpenAI)
                apiKey = System.getenv("CHATGPT_API_KEY");
                endpoint = "https://api.openai.com/v1/chat/completions";
                llmModel = "gpt-3.5-turbo";
                if (apiKey == null || apiKey.isEmpty()) throw new IOException("CHATGPT_API_KEY environment variable is not set.");
            }

            // Standard OpenAI-compatible Request Body
            String messagesJson = String.format(
                    "{\"role\": \"user\", \"content\": \"%s\"}",
                    escapedPrompt
            );
            String requestBody = String.format(
                    "{\"model\": \"%s\", \"messages\": [%s], \"stream\": false}",
                    llmModel, messagesJson
            );

            return sendPostRequestWithApiKey(endpoint, requestBody, apiKey);

        } else if (model.equals("Gemini")) {
            String apiKey = System.getenv("GEMINI_API_KEY");
            if (apiKey == null || apiKey.isEmpty()) {
                throw new IOException("GEMINI_API_KEY environment variable is not set. Cannot connect to Gemini.");
            }

            String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey;

            // Gemini Request Body format
            String requestBody = String.format(
                    "{\"contents\": [{\"role\": \"user\", \"parts\": [{\"text\": \"%s\"}]}]}",
                    escapedPrompt
            );

            LocalAIClient dummyClient = new LocalAIClient("");
            return dummyClient.sendPostRequest(endpoint, requestBody);
        }

        throw new IOException("Unsupported online model: " + model);
    }

    private static void scanForModels() {
        String endpoint = apiEndpointField.getText().trim();
        if (endpoint.isEmpty()) {
            return;
        }

        new Thread(() -> {
            try {
                LocalAIClient client = new LocalAIClient(endpoint);
                String modelsResponse = client.getAvailableModels();

                SwingUtilities.invokeLater(() -> {
                    modelComboBox.removeAllItems();

                    if (modelsResponse != null && !modelsResponse.isEmpty()) {
                        java.util.List<String> models = parseModelsFromJson(modelsResponse);

                        if (!models.isEmpty()) {
                            for (String model : models) {
                                modelComboBox.addItem(model);
                            }
                            JOptionPane.showMessageDialog(null,
                                    "Found " + models.size() + " models",
                                    "Model Scan",
                                    JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            modelComboBox.addItem("No models found");
                            responseArea.setText("Raw response: " + modelsResponse);
                        }
                    } else {
                        modelComboBox.addItem("No response");
                    }
                });

            } catch (IOException ex) {
                SwingUtilities.invokeLater(() -> {
                    modelComboBox.removeAllItems();
                    modelComboBox.addItem("Connection failed");
                    responseArea.setText("Error scanning for models: " + ex.getMessage());
                    JOptionPane.showMessageDialog(null,
                            "Failed to scan for models: " + ex.getMessage(),
                            "Scan Error",
                            JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }

    private static java.util.List<String> parseModelsFromJson(String jsonResponse) {
        java.util.List<String> models = new java.util.ArrayList<>();
        try {
            String dataSection = extractJsonArray(jsonResponse);
            if (dataSection != null) {
                String[] modelObjects = dataSection.split("},\\s*\\{");
                for (String modelObj : modelObjects) {
                    String modelId = extractJsonField(modelObj);
                    if (modelId != null && !modelId.isEmpty()) {
                        models.add(modelId);
                    }
                }
            }
        } catch (Exception e) {
            responseArea.setText("Parse error. Raw: " + jsonResponse);
        }
        return models;
    }

    private static String extractJsonArray(String json) {
        int startIndex = json.indexOf("\"data\"" + ":");
        if (startIndex == -1) startIndex = json.indexOf("\"data\"" + "\":");
        if (startIndex == -1) return null;

        startIndex = json.indexOf("[", startIndex);
        if (startIndex == -1) return null;

        int bracketCount = 1;
        int endIndex = startIndex + 1;

        while (endIndex < json.length() && bracketCount > 0) {
            char c = json.charAt(endIndex);
            if (c == '[') bracketCount++;
            else if (c == ']') bracketCount--;
            endIndex++;
        }

        if (bracketCount == 0) {
            return json.substring(startIndex + 1, endIndex - 1);
        }
        return null;
    }

    private static String extractJsonField(String json) {
        int startIndex = json.indexOf("\"id\"" + ":");
        if (startIndex == -1) startIndex = json.indexOf("\"id\"" + "\":");
        if (startIndex == -1) return null;

        startIndex = json.indexOf("\"", startIndex + "\"id\"".length());
        if (startIndex == -1) return null;

        int endIndex = json.indexOf("\"", startIndex + 1);
        if (endIndex == -1) return null;

        return json.substring(startIndex + 1, endIndex);
    }

    private static String unescapeJson(String input) {
        return input.replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    // LocalAI Client implementation
    private static class LocalAIClient {
        private final String baseUrl;

        public LocalAIClient(String baseUrl) {
            this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        }

        public String getAvailableModels() throws IOException {
            String endpoint = baseUrl + "/v1/models";
            return sendGetRequest(endpoint);
        }

        public String sendChatRequestWithHistory(String model, java.util.List<Message> messages) throws IOException {
            String endpoint = baseUrl + "/v1/chat/completions";

            StringBuilder messagesJson = new StringBuilder();
            for (int i = 0; i < messages.size(); i++) {
                if (i > 0) messagesJson.append(",");
                messagesJson.append(messages.get(i).toJson());
            }

            String requestBody = String.format(
                    "{\"model\": \"%s\", \"messages\": [%s], " +
                            "\"max_tokens\": 16000, \"temperature\": 0.7, \"stream\": false}",
                    model, messagesJson
            );

            return sendPostRequest(endpoint, requestBody);
        }

        private String sendGetRequest(String endpoint) throws IOException {
            URL url = new URL(endpoint);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine);
                    }
                    return response.toString();
                }
            } else {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
                    StringBuilder errorResponse = new StringBuilder();
                    String errorLine;
                    while ((errorLine = br.readLine()) != null) {
                        errorResponse.append(errorLine);
                    }
                    throw new IOException("HTTP error " + responseCode + ": " + errorResponse);
                }
            }
        }

        // Generic POST Request method (used by LocalAI and Gemini)
        public String sendPostRequest(String endpoint, String requestBody) throws IOException {
            URL url = new URL(endpoint);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);
            connection.setConnectTimeout(120000);
            connection.setReadTimeout(120000);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine);
                    }
                    return response.toString();
                }
            } else {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
                    StringBuilder errorResponse = new StringBuilder();
                    String errorLine;
                    while ((errorLine = br.readLine()) != null) {
                        errorResponse.append(errorLine);
                    }
                    throw new IOException("HTTP error " + responseCode + ": " + errorResponse);
                }
            }
        }
    }

    // Message class is required for conversation history
    private static class Message {
        private final String role;
        private final String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String toJson() {
            String escapedContent = content.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
            return String.format("{\"role\": \"%s\", \"content\": \"%s\"}", role, escapedContent);
        }
    }


    // Send Button Action Listener - MODIFIED FOR ONLINE/LOCAL ROUTING
    private static class SendButtonListener implements ActionListener {
        private final JTextArea chatArea;
        private final JRadioButton onlineButton;

        public SendButtonListener(JTextArea chatArea, JRadioButton onlineButton) {
            this.chatArea = chatArea;
            this.onlineButton = onlineButton;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            String userInput = inputArea.getText().trim();
            if (userInput.isEmpty()) return;

            chatArea.append("You: " + userInput + "\n\n");
            inputArea.setText("");

            new Thread(() -> {
                try {
                    String response;
                    String formattedResponse;
                    String currentModel;

                    if (onlineButton.isSelected()) {
                        // --- ONLINE MODEL LOGIC ---
                        currentModel = (String) onlineModelComboBox.getSelectedItem();

                        if (currentModel == null) {
                            throw new IOException("No online model selected.");
                        }

                        response = sendOnlineChatRequest(currentModel, userInput);
                        formattedResponse = extractContentForOnlineModel(response, currentModel);

                    } else {
                        // --- LOCALAI LOGIC ---
                        String endpoint = apiEndpointField.getText().trim();
                        currentModel = (String) modelComboBox.getSelectedItem();

                        if (currentModel == null || currentModel.equals("Loading...") || currentModel.equals("Connection failed") ||
                                currentModel.equals("No models found") || currentModel.equals("No response")) {
                            throw new IOException("No valid LocalAI model selected.");
                        }

                        LocalAIClient client = new LocalAIClient(endpoint);
                        java.util.List<Message> conversationHistory = parseConversationHistory(chatArea.getText());
                        conversationHistory.add(new Message("user", userInput));

                        response = client.sendChatRequestWithHistory(currentModel, conversationHistory);
                        formattedResponse = extractContentRobustly(response);
                    }

                    SwingUtilities.invokeLater(() -> {
                        String displayModel = onlineButton.isSelected() ? currentModel : "AI";

                        chatArea.append(displayModel + ": " + formattedResponse + "\n\n");
                        responseArea.setText("Raw Response (" + displayModel + ", length: " + response.length() + "):\n" + response);
                        chatArea.setCaretPosition(chatArea.getDocument().getLength());

                        if (response.contains("\"finish_reason\":\"length\"")) {
                            JOptionPane.showMessageDialog(null,
                                    "⚠️ Response was TRUNCATED due to token limits!\n" +
                                            "The response might be incomplete.",
                                    "Response Truncated",
                                    JOptionPane.WARNING_MESSAGE);
                        }
                    });

                } catch (IOException ex) {
                    SwingUtilities.invokeLater(() -> {
                        responseArea.setText("Error: " + ex.getMessage());
                        JOptionPane.showMessageDialog(null,
                                "Failed to connect: " + ex.getMessage(),
                                "Connection Error",
                                JOptionPane.ERROR_MESSAGE);
                    });
                }
            }).start();
        }

        private java.util.List<Message> parseConversationHistory(String chatText) {
            java.util.List<Message> history = new java.util.ArrayList<>();
            if (chatText == null || chatText.trim().isEmpty()) {
                return history;
            }

            String[] lines = chatText.split("\n");
            StringBuilder currentMessage = new StringBuilder();
            String currentRole = null;

            for (String line : lines) {
                String potentialRole = null;
                String contentLine = line;

                if (line.startsWith("You: ")) {
                    potentialRole = "user";
                    contentLine = line.substring(5);
                } else if (line.startsWith("AI: ")) {
                    potentialRole = "assistant";
                    contentLine = line.substring(4);
                } else if (line.endsWith(": ")) { // Heuristic for online model name (e.g., Deepseek: )
                    String prefix = line.substring(0, line.indexOf(": "));
                    if (onlineModelComboBox.getItemCount() > 0) {
                        for(int i = 0; i < onlineModelComboBox.getItemCount(); i++) {
                            if(prefix.equals(onlineModelComboBox.getItemAt(i))) {
                                potentialRole = "assistant";
                                contentLine = line.substring(prefix.length() + 2); // Remove "Model: "
                                break;
                            }
                        }
                    }
                }

                if (potentialRole != null) {
                    if (currentRole != null && currentMessage.length() > 0) {
                        history.add(new Message(currentRole, currentMessage.toString().trim()));
                        currentMessage.setLength(0);
                    }
                    currentRole = potentialRole;
                    currentMessage.append(contentLine);
                } else if (!line.trim().isEmpty()) {
                    if (currentMessage.length() > 0) {
                        currentMessage.append("\n");
                    }
                    currentMessage.append(line);
                }
            }

            if (currentRole != null && currentMessage.length() > 0) {
                history.add(new Message(currentRole, currentMessage.toString().trim()));
            }

            return history;
        }

        private String extractContentRobustly(String jsonResponse) {
            String content = extractUsingKeyPath(jsonResponse, new String[]{"choices", "0", "message", "content"});
            if (content != null) {
                return unescapeJson(content);
            }
            String contentWithQuotes = extractContentWithQuoteHandling(jsonResponse);
            return Objects.requireNonNullElseGet(contentWithQuotes, () -> "❌ Failed to parse response. Raw JSON:\n" + jsonResponse);
        }

        private String extractContentForOnlineModel(String jsonResponse, String model) {
            if (model.equals("Deepseek") || model.equals("ChatGPT")) {
                String content = extractUsingKeyPath(jsonResponse, new String[]{"choices", "0", "message", "content"});
                if (content != null) {
                    return unescapeJson(content);
                }
                return "❌ Failed to parse response from " + model + ". Raw JSON:\n" + jsonResponse;

            } else if (model.equals("Gemini")) {
                String content = extractUsingKeyPath(jsonResponse, new String[]{"candidates", "0", "content", "parts", "0", "text"});
                if (content != null) {
                    return unescapeJson(content);
                }
                return "❌ Failed to parse response from Gemini. Raw JSON:\n" + jsonResponse;
            }

            return "Online model (" + model + ") parser not implemented. Raw JSON:\n" + jsonResponse;
        }

        private String extractUsingKeyPath(String json, String[] keys) {
            try {
                String currentJson = json;
                for (int i = 0; i < keys.length; i++) {
                    String key = keys[i];
                    String searchPattern = "\"" + key + "\":";
                    int keyIndex = currentJson.indexOf(searchPattern);
                    if (keyIndex == -1) return null;

                    int valueStart = currentJson.indexOf(':', keyIndex) + 1;
                    if (valueStart == 0) return null;

                    while (valueStart < currentJson.length() && Character.isWhitespace(currentJson.charAt(valueStart))) {
                        valueStart++;
                    }

                    if (valueStart >= currentJson.length()) return null;

                    char firstChar = currentJson.charAt(valueStart);

                    if (firstChar == '{') {
                        int braceCount = 1;
                        int endIndex = valueStart + 1;
                        while (endIndex < currentJson.length() && braceCount > 0) {
                            char c = currentJson.charAt(endIndex);
                            if (c == '{') braceCount++;
                            else if (c == '}') braceCount--;
                            endIndex++;
                        }
                        if (braceCount == 0) {
                            currentJson = currentJson.substring(valueStart, endIndex);
                        } else {
                            return null;
                        }
                    } else if (firstChar == '[') {
                        int bracketCount = 1;
                        int endIndex = valueStart + 1;
                        while (endIndex < currentJson.length() && bracketCount > 0) {
                            char c = currentJson.charAt(endIndex);
                            if (c == '[') bracketCount++;
                            else if (c == ']') bracketCount--;
                            endIndex++;
                        }
                        if (bracketCount == 0) {
                            currentJson = currentJson.substring(valueStart, endIndex);

                            if (i + 1 < keys.length) {
                                try {
                                    int arrayIndex = Integer.parseInt(keys[i + 1]);
                                    String[] arrayElements = parseJsonArray(currentJson);
                                    if (arrayIndex >= 0 && arrayIndex < arrayElements.length) {
                                        currentJson = arrayElements[arrayIndex];
                                        i++;
                                    } else {
                                        return null;
                                    }
                                } catch (NumberFormatException e) {
                                    return null;
                                }
                            }
                        } else {
                            return null;
                        }
                    } else if (firstChar == '"') {
                        int endIndex = valueStart + 1;
                        boolean escaped = false;
                        while (endIndex < currentJson.length()) {
                            char c = currentJson.charAt(endIndex);
                            if (escaped) {
                                escaped = false;
                            } else if (c == '\\') {
                                escaped = true;
                            } else if (c == '"') {
                                if (i == keys.length - 1) {
                                    return currentJson.substring(valueStart + 1, endIndex);
                                } else {
                                    currentJson = currentJson.substring(valueStart + 1, endIndex);
                                    break;
                                }
                            }
                            endIndex++;
                        }
                        if (endIndex >= currentJson.length()) return null;
                    } else {
                        int endIndex = valueStart;
                        while (endIndex < currentJson.length()) {
                            char c = currentJson.charAt(endIndex);
                            if (c == ',' || c == '}' || c == ']' || Character.isWhitespace(c)) {
                                break;
                            }
                            endIndex++;
                        }
                        String value = currentJson.substring(valueStart, endIndex).trim();
                        if (i == keys.length - 1) {
                            return value;
                        } else {
                            currentJson = value;
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("Key path extraction failed: " + e.getMessage());
            }
            return null;
        }

        private String[] parseJsonArray(String jsonArray) {
            java.util.List<String> elements = new java.util.ArrayList<>();
            try {
                if (!jsonArray.startsWith("[") || !jsonArray.endsWith("]")) {
                    return new String[0];
                }

                String content = jsonArray.substring(1, jsonArray.length() - 1).trim();
                if (content.isEmpty()) {
                    return new String[0];
                }

                int braceCount = 0;
                int bracketCount = 0;
                boolean inString = false;
                boolean escaped = false;
                int start = 0;

                for (int i = 0; i < content.length(); i++) {
                    char c = content.charAt(i);

                    if (escaped) {
                        escaped = false;
                    } else if (c == '\\') {
                        escaped = true;
                    } else if (c == '"' && braceCount == 0 && bracketCount == 0) {
                        inString = !inString;
                    } else if (!inString) {
                        if (c == '{') braceCount++;
                        else if (c == '}') braceCount--;
                        else if (c == '[') bracketCount++;
                        else if (c == ']') bracketCount--;
                        else if (c == ',' && braceCount == 0 && bracketCount == 0) {
                            elements.add(content.substring(start, i).trim());
                            start = i + 1;
                        }
                    }
                }

                if (start < content.length()) {
                    elements.add(content.substring(start).trim());
                }

            } catch (Exception e) {
                System.out.println("Array parsing failed: " + e.getMessage());
            }
            return elements.toArray(new String[0]);
        }

        private String extractContentWithQuoteHandling(String json) {
            try {
                int contentIndex = json.indexOf("\"content\":");
                if (contentIndex == -1) return null;

                int quoteStart = json.indexOf('"', contentIndex + 10);
                if (quoteStart == -1) return null;

                StringBuilder content = new StringBuilder();
                boolean escaped = false;
                int pos = quoteStart + 1;

                while (pos < json.length()) {
                    char c = json.charAt(pos);
                    if (escaped) {
                        content.append('\\').append(c);
                        escaped = false;
                    } else if (c == '\\') {
                        escaped = true;
                    } else if (c == '"') {
                        return unescapeJson(content.toString());
                    } else {
                        content.append(c);
                    }
                    pos++;
                }
            } catch (Exception e) {
                System.out.println("Direct content extraction failed: " + e.getMessage());
            }
            return null;
        }
    }
}