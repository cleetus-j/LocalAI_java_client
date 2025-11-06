

    import javax.swing.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

    public class FileSaver {

        public static boolean saveConversation(JFrame parent, String conversation) {
            if (conversation == null || conversation.trim().isEmpty()) {
                JOptionPane.showMessageDialog(parent,
                        "No conversation to save!",
                        "Save Error",
                        JOptionPane.WARNING_MESSAGE);
                return false;
            }

            JFileChooser fileChooser = new JFileChooser();

            // Set file filter for TXT and ZIP files
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                    "Text and ZIP files (*.txt, *.zip)", "txt", "zip"));

            int result = fileChooser.showSaveDialog(parent);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                String fileName = selectedFile.getName().toLowerCase();

                // Ensure proper extension
                if (!fileName.endsWith(".txt") && !fileName.endsWith(".zip")) {
                    // Get the selected filter to determine which extension to use
                    javax.swing.filechooser.FileFilter filter = fileChooser.getFileFilter();
                    if (filter.getDescription().contains("*.txt")) {
                        selectedFile = new File(selectedFile.getParentFile(), selectedFile.getName() + ".txt");
                    } else {
                        selectedFile = new File(selectedFile.getParentFile(), selectedFile.getName() + ".zip");
                    }
                }

                try {
                    if (selectedFile.getName().toLowerCase().endsWith(".zip")) {
                        return saveAsZip(selectedFile, conversation);
                    } else {
                        return saveAsText(selectedFile, conversation);
                    }
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(parent,
                            "Error saving file: " + ex.getMessage(),
                            "Save Error",
                            JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            }
            return false;
        }

        private static boolean saveAsText(File file, String conversation) throws IOException {
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(conversation);
                JOptionPane.showMessageDialog(null,
                        "Conversation saved as text file successfully!",
                        "Save Complete",
                        JOptionPane.INFORMATION_MESSAGE);
                return true;
            }
        }

        private static boolean saveAsZip(File file, String conversation) throws IOException {
            try (FileOutputStream fos = new FileOutputStream(file);
                 ZipOutputStream zos = new ZipOutputStream(fos)) {

                // Create a text file inside the zip
                String txtFileName = "conversation_" + System.currentTimeMillis() + ".txt";
                ZipEntry entry = new ZipEntry(txtFileName);
                zos.putNextEntry(entry);

                // Write the conversation to the zip entry
                byte[] data = conversation.getBytes(StandardCharsets.UTF_8);
                zos.write(data, 0, data.length);
                zos.closeEntry();

                JOptionPane.showMessageDialog(null,
                        "Conversation saved as ZIP file successfully!\n" +
                                "File inside: " + txtFileName,
                        "Save Complete",
                        JOptionPane.INFORMATION_MESSAGE);
                return true;
            }
        }
    }

