import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.imageio.ImageIO;
import java.io.*;
import java.util.Deque;
import java.util.ArrayDeque;

public class PhotoEditor extends JFrame {
    private BufferedImage originalImage;
    private BufferedImage editedImage;
    private JLabel imageLabel;
private Deque<BufferedImage> undoStack = new ArrayDeque<>();
  
    private float brightness = 1.0f;
    private float contrast = 1.0f;
    private double zoom = 1.0;

    public PhotoEditor() {
        setTitle("Photo Editor");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Image display area
        imageLabel = new JLabel("", JLabel.CENTER);
        JScrollPane scrollPane = new JScrollPane(imageLabel);
        add(scrollPane, BorderLayout.CENTER);

        // Buttons and sliders
        JPanel controls = new JPanel(new GridLayout(0, 1));

        JButton loadBtn = new JButton("Load Image");
        loadBtn.addActionListener(e -> loadImage());
        controls.add(loadBtn);
JButton undoBtn = new JButton("Undo");
undoBtn.addActionListener(e -> undo());
controls.add(undoBtn);

        JButton saveBtn = new JButton("Save Image");
        saveBtn.addActionListener(e -> saveImage());
        controls.add(saveBtn);

        JButton grayBtn = new JButton("Grayscale");
        grayBtn.addActionListener(e -> applyGrayscale());
        controls.add(grayBtn);

        JButton sepiaBtn = new JButton("Sepia");
        sepiaBtn.addActionListener(e -> applySepia());
        controls.add(sepiaBtn);

        JButton rotateBtn = new JButton("Rotate 90°");
        rotateBtn.addActionListener(e -> rotateImage());
        controls.add(rotateBtn);

        JButton zoomInBtn = new JButton("Zoom In");
        zoomInBtn.addActionListener(e -> zoomImage(1.25));
        controls.add(zoomInBtn);

        JButton zoomOutBtn = new JButton("Zoom Out");
        zoomOutBtn.addActionListener(e -> zoomImage(0.8));
        controls.add(zoomOutBtn);

        controls.add(new JLabel("Brightness"));
        JSlider brightnessSlider = new JSlider(0, 200, 100);
        brightnessSlider.addChangeListener(e -> {
            brightness = brightnessSlider.getValue() / 100f;
            applyAdjustments();
        });
        controls.add(brightnessSlider);

        controls.add(new JLabel("Contrast"));
        JSlider contrastSlider = new JSlider(0, 200, 100);
        contrastSlider.addChangeListener(e -> {
            contrast = contrastSlider.getValue() / 100f;
            applyAdjustments();
        });
        controls.add(contrastSlider);

        add(controls, BorderLayout.WEST);
        setSize(1000, 700);
        setVisible(true);
    }
private void pushUndo() {
    if (editedImage != null) {
        undoStack.push(deepCopy(editedImage));
    }
}
private void undo() {
    if (!undoStack.isEmpty()) {
        editedImage = undoStack.pop();
        updateImageDisplay();
    } else {
        showError("Nothing to undo!");
    }
}

    private void loadImage() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                originalImage = ImageIO.read(chooser.getSelectedFile());
                editedImage = deepCopy(originalImage);
                updateImageDisplay();
            } catch (IOException ex) {
                showError("Failed to load image.");
            }
        }
    }

    private void saveImage() {
        if (editedImage == null) return;
        JFileChooser chooser = new JFileChooser();
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                ImageIO.write(editedImage, "png", chooser.getSelectedFile());
            } catch (IOException ex) {
                showError("Failed to save image.");
            }
        }
    }

    private void applyGrayscale() {
        if (editedImage == null) return;
        pushUndo();
        for (int y = 0; y < editedImage.getHeight(); y++) {
            for (int x = 0; x < editedImage.getWidth(); x++) {
                Color color = new Color(editedImage.getRGB(x, y));
                int gray = (int)(0.3 * color.getRed() + 0.59 * color.getGreen() + 0.11 * color.getBlue());
                Color newColor = new Color(gray, gray, gray);
                editedImage.setRGB(x, y, newColor.getRGB());
            }
        }
        updateImageDisplay();
    }

    private void applySepia() {
        if (editedImage == null) return;
        pushUndo();
        for (int y = 0; y < editedImage.getHeight(); y++) {
            for (int x = 0; x < editedImage.getWidth(); x++) {
                Color color = new Color(editedImage.getRGB(x, y));
                int r = color.getRed();
                int g = color.getGreen();
                int b = color.getBlue();
                int tr = Math.min((int)(0.393 * r + 0.769 * g + 0.189 * b), 255);
                int tg = Math.min((int)(0.349 * r + 0.686 * g + 0.168 * b), 255);
                int tb = Math.min((int)(0.272 * r + 0.534 * g + 0.131 * b), 255);
                editedImage.setRGB(x, y, new Color(tr, tg, tb).getRGB());
            }
        }
        updateImageDisplay();
    }

    private void applyAdjustments() {
        if (originalImage == null) return;
        pushUndo();
        editedImage = deepCopy(originalImage);
        RescaleOp op = new RescaleOp(contrast, (brightness - 1f) * 255, null);
        op.filter(editedImage, editedImage);
        updateImageDisplay();
    }

    private void rotateImage() {
        if (editedImage == null) return;
        pushUndo();
        int w = editedImage.getWidth();
        int h = editedImage.getHeight();
        BufferedImage rotated = new BufferedImage(h, w, editedImage.getType());
        Graphics2D g2d = rotated.createGraphics();
        g2d.rotate(Math.toRadians(90), h / 2.0, h / 2.0);
        g2d.translate((h - w) / 2, (h - w) / 2);
        g2d.drawImage(editedImage, 0, 0, null);
        g2d.dispose();
        editedImage = rotated;
        originalImage = deepCopy(rotated); // Update base for adjustments
        updateImageDisplay();
    }

    private void zoomImage(double factor) {
        
        if (editedImage == null) return;
        pushUndo();
        zoom *= factor;
        updateImageDisplay();
    }

    private void updateImageDisplay() {
        if (editedImage == null) return;
        int w = (int)(editedImage.getWidth() * zoom);
        int h = (int)(editedImage.getHeight() * zoom);
        Image scaled = editedImage.getScaledInstance(w, h, Image.SCALE_SMOOTH);
        imageLabel.setIcon(new ImageIcon(scaled));
    }

    private BufferedImage deepCopy(BufferedImage img) {
        ColorModel cm = img.getColorModel();
        boolean alpha = cm.isAlphaPremultiplied();
        WritableRaster raster = img.copyData(null);
        return new BufferedImage(cm, raster, alpha, null);
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(PhotoEditor::new);
    }
}
 
