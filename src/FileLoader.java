import javax.swing.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FileLoader {

    public static String loadFile(JFrame parent) {
        JFileChooser fileChooser = new JFileChooser();

        // Set file filter for TXT and ZIP files
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Text and ZIP files (*.txt, *.zip)", "txt", "zip"));

        int result = fileChooser.showOpenDialog(parent);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            String fileName = selectedFile.getName().toLowerCase();

            try {
                if (fileName.endsWith(".zip")) {
                    return loadFromZip(selectedFile);
                } else if (fileName.endsWith(".txt")) {
                    return loadFromText(selectedFile);
                } else {
                    JOptionPane.showMessageDialog(parent,
                            "Unsupported file type. Please select a .txt or .zip file.",
                            "Unsupported Format",
                            JOptionPane.ERROR_MESSAGE);
                    return null;
                }
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(parent,
                        "Error loading file: " + ex.getMessage(),
                        "Load Error",
                        JOptionPane.ERROR_MESSAGE);
                return null;
            }
        }
        return null;
    }

    private static String loadFromText(File file) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }

    private static String loadFromZip(File file) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(file))) {
            ZipEntry entry;
            StringBuilder content = new StringBuilder();
            boolean foundTextFile = false;

            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName().toLowerCase();

                // Look for text files in the zip
                if (entryName.endsWith(".txt") && !entry.isDirectory()) {
                    foundTextFile = true;
                    // Read the text file from zip
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        baos.write(buffer, 0, len);
                    }

                    String fileContent = baos.toString(StandardCharsets.UTF_8.name());
                    content.append("=== File: ").append(entryName).append(" ===\n");
                    content.append(fileContent).append("\n\n");
                }
            }

            if (!foundTextFile) {
                throw new IOException("No text files found in the ZIP archive");
            }

            return content.toString();
        }
    }
}