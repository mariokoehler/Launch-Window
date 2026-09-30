package de.mkoehler.launchwindow.devtools;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Standalone dev tool (never shipped, see dev-tools' pom comment) for
 * authoring the clickable-building hotspot polygons the main-menu-as-map
 * idea needs (see {@code MenuHotspotPrototype} and {@code design.md}'s
 * "Current status" entry on the idea). Loads a menu image, lets the user
 * place polygon vertices by hand, and saves the named, finished polygons to
 * a {@code <image>.hotspots.json} sidecar file next to it.
 * <p>
 * <b>Coordinate convention:</b> saved vertices are in raw image-pixel
 * space - top-left origin, Y increasing downward, exactly how the source
 * PNG's own pixels are indexed. Any flip needed for a Y-up rendering
 * convention (libGDX) is the loading side's job, not this tool's - see
 * {@code MenuHotspotPrototype#create()}, which does exactly that flip for
 * its own (arbitrary, hand-typed) demo polygon.
 * <p>
 * Controls: left-click adds a point to the polygon in progress, right-click
 * removes its last point, Enter finishes and names it (prompting for a
 * name; finishing starts a new polygon automatically), Escape discards the
 * in-progress polygon. Mouse wheel zooms, centered on the cursor. Only
 * finished (named) polygons are saved.
 *
 * @author Mario Koehler
 */
public final class HotspotEditor extends JFrame {

    private static final float MIN_ZOOM = 0.05f;
    private static final float MAX_ZOOM = 8f;
    private static final float ZOOM_STEP = 1.15f;
    private static final String SIDECAR_SUFFIX = ".hotspots.json";

    private static final Color FINISHED_FILL = new Color(0.35f, 0.75f, 1f, 0.22f);
    private static final Color FINISHED_OUTLINE = new Color(0.35f, 0.75f, 1f, 0.9f);
    private static final Color SELECTED_FILL = new Color(1f, 0.82f, 0.2f, 0.3f);
    private static final Color SELECTED_OUTLINE = new Color(1f, 0.82f, 0.2f, 0.95f);
    private static final Color IN_PROGRESS_LINE = new Color(1f, 1f, 1f, 0.9f);
    private static final Color IN_PROGRESS_POINT = new Color(1f, 0.35f, 0.35f, 0.95f);
    private static final double NUDGE_STEP = 1;
    private static final double NUDGE_STEP_FAST = 10;

    private final ObjectMapper mapper = new ObjectMapper();

    private final Canvas canvas = new Canvas();
    private final JScrollPane scrollPane = new JScrollPane(canvas);
    private final DefaultListModel<NamedPolygon> polygonListModel = new DefaultListModel<>();
    private final JList<NamedPolygon> polygonList = new JList<>(polygonListModel);
    private final JLabel statusLabel = new JLabel(" ");
    private final JMenuItem saveItem = new JMenuItem("Save");
    private final JMenuItem saveAsItem = new JMenuItem("Save As...");
    private final JMenuItem importItem = new JMenuItem("Import Polygons From...");

    private BufferedImage image;
    private File imageFile;
    private File jsonFile;
    private final List<Point2D.Float> currentPoints = new ArrayList<>();
    private float zoom = 1f;
    private boolean dirty;
    private File lastDirectory;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new HotspotEditor().setVisible(true));
    }

    private HotspotEditor() {
        super("Launch Window - hotspot editor");
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (confirmDiscardIfDirty("Exit without saving?")) {
                    dispose();
                    System.exit(0);
                }
            }
        });

        setJMenuBar(buildMenuBar());
        setLayout(new BorderLayout());

        canvas.setBackground(new Color(40, 40, 40));
        canvas.setFocusable(true);
        scrollPane.getVerticalScrollBar().setUnitIncrement(24);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(24);
        add(scrollPane, BorderLayout.CENTER);
        add(buildSidePanel(), BorderLayout.EAST);
        statusLabel.setBorder(new EmptyBorder(4, 8, 4, 8));
        add(statusLabel, BorderLayout.SOUTH);

        installCanvasMouseHandling();
        installKeyBindings();
        updateWindowTitle();
        updateStatus();
        updateSaveEnablement();

        setSize(1400, 900);
        setLocationRelativeTo(null);
    }

    private JMenuBar buildMenuBar() {
        JMenuBar bar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");

        JMenuItem openItem = new JMenuItem("Open Image...");
        openItem.setAccelerator(KeyStroke.getKeyStroke("control O"));
        openItem.addActionListener(e -> openImage());
        fileMenu.add(openItem);

        saveItem.setAccelerator(KeyStroke.getKeyStroke("control S"));
        saveItem.addActionListener(e -> save(jsonFile));
        fileMenu.add(saveItem);

        saveAsItem.setAccelerator(KeyStroke.getKeyStroke("control shift S"));
        saveAsItem.addActionListener(e -> saveAs());
        fileMenu.add(saveAsItem);

        fileMenu.addSeparator();
        importItem.addActionListener(e -> importPolygons());
        fileMenu.add(importItem);

        fileMenu.addSeparator();
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING)));
        fileMenu.add(exitItem);

        bar.add(fileMenu);
        return bar;
    }

    private JPanel buildSidePanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBorder(new EmptyBorder(8, 8, 8, 8));
        panel.setPreferredSize(new Dimension(240, 0));

        panel.add(new JLabel("Finished polygons:"), BorderLayout.NORTH);

        polygonList.addListSelectionListener(e -> canvas.repaint());
        polygonList.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("DELETE"), "deleteSelected");
        polygonList.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("BACK_SPACE"), "deleteSelected");
        polygonList.getActionMap().put("deleteSelected", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteSelectedPolygon();
            }
        });
        // Overrides JList's own up/down-navigate bindings on this same input map: with a
        // polygon selected (via mouse click), arrow keys nudge it whole instead of moving
        // the selection - that's the point, per the user's request. Shift+arrow nudges
        // further for faster seasonal-variant repositioning.
        bindNudge(polygonList, "UP", 0, -NUDGE_STEP);
        bindNudge(polygonList, "DOWN", 0, NUDGE_STEP);
        bindNudge(polygonList, "LEFT", -NUDGE_STEP, 0);
        bindNudge(polygonList, "RIGHT", NUDGE_STEP, 0);
        bindNudge(polygonList, "shift UP", 0, -NUDGE_STEP_FAST);
        bindNudge(polygonList, "shift DOWN", 0, NUDGE_STEP_FAST);
        bindNudge(polygonList, "shift LEFT", -NUDGE_STEP_FAST, 0);
        bindNudge(polygonList, "shift RIGHT", NUDGE_STEP_FAST, 0);
        panel.add(new JScrollPane(polygonList), BorderLayout.CENTER);

        JButton deleteButton = new JButton("Delete Selected");
        deleteButton.addActionListener(e -> deleteSelectedPolygon());

        JTextArea instructions = new JTextArea(
                "Left-click: add point\n"
                + "Right-click: remove last point\n"
                + "Enter: finish + name polygon\n"
                + "Escape: discard in-progress polygon\n"
                + "Mouse wheel: zoom (at cursor)\n"
                + "Delete: remove selected polygon\n"
                + "Arrows (list selected): nudge polygon " + (int) NUDGE_STEP + "px\n"
                + "Shift+Arrows: nudge " + (int) NUDGE_STEP_FAST + "px\n"
                + "File > Import Polygons From...: load another\n"
                + "image's polygons as a starting point");
        instructions.setEditable(false);
        instructions.setOpaque(false);
        instructions.setFont(instructions.getFont().deriveFont(Font.PLAIN, 11f));
        instructions.setLineWrap(true);
        instructions.setWrapStyleWord(true);

        JPanel south = new JPanel(new BorderLayout(0, 8));
        south.add(deleteButton, BorderLayout.NORTH);
        south.add(instructions, BorderLayout.SOUTH);
        panel.add(south, BorderLayout.SOUTH);

        return panel;
    }

    private void installCanvasMouseHandling() {
        canvas.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                canvas.requestFocusInWindow();
                if (image == null) {
                    return;
                }
                float imgX = clamp(e.getX() / zoom, 0, image.getWidth());
                float imgY = clamp(e.getY() / zoom, 0, image.getHeight());
                if (SwingUtilities.isLeftMouseButton(e)) {
                    currentPoints.add(new Point2D.Float(imgX, imgY));
                } else if (SwingUtilities.isRightMouseButton(e)) {
                    if (!currentPoints.isEmpty()) {
                        currentPoints.remove(currentPoints.size() - 1);
                    }
                }
                canvas.repaint();
                updateStatus();
            }
        });
        canvas.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (image != null) {
                    float imgX = e.getX() / zoom;
                    float imgY = e.getY() / zoom;
                    statusLabel.setText(String.format(Locale.ROOT,
                            "%.0f, %.0f   zoom %.0f%%   in-progress points: %d   polygons: %d",
                            imgX, imgY, zoom * 100, currentPoints.size(), polygonListModel.size()));
                }
            }
        });
        canvas.addMouseWheelListener(e -> {
            if (image == null) {
                return;
            }
            Point viewPos = scrollPane.getViewport().getViewPosition();
            Point cursorInView = e.getPoint();
            int offsetX = cursorInView.x - viewPos.x;
            int offsetY = cursorInView.y - viewPos.y;
            float imgX = cursorInView.x / zoom;
            float imgY = cursorInView.y / zoom;

            float factor = e.getWheelRotation() < 0 ? ZOOM_STEP : 1f / ZOOM_STEP;
            zoom = clamp(zoom * factor, MIN_ZOOM, MAX_ZOOM);
            canvas.updateSizeForZoom();
            canvas.revalidate();

            Point newViewPos = new Point(Math.round(imgX * zoom) - offsetX, Math.round(imgY * zoom) - offsetY);
            scrollPane.getViewport().setViewPosition(clampViewPosition(newViewPos));
            canvas.repaint();
            updateStatus();
        });
    }

    private void installKeyBindings() {
        JComponent root = getRootPane();
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ENTER"), "finishPolygon");
        root.getActionMap().put("finishPolygon", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                finishCurrentPolygon();
            }
        });
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"), "discardPolygon");
        root.getActionMap().put("discardPolygon", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!currentPoints.isEmpty()) {
                    currentPoints.clear();
                    canvas.repaint();
                    updateStatus();
                }
            }
        });
    }

    private void openImage() {
        if (!confirmDiscardIfDirty("Discard unsaved polygons and open a different image?")) {
            return;
        }
        JFileChooser chooser = new JFileChooser(lastDirectory);
        chooser.setFileFilter(new FileNameExtensionFilter("Images (png, jpg, jpeg)", "png", "jpg", "jpeg"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();
        lastDirectory = file.getParentFile();
        BufferedImage loaded;
        try {
            loaded = ImageIO.read(file);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Failed to read " + file + ": " + ex.getMessage(),
                    "Open failed", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (loaded == null) {
            JOptionPane.showMessageDialog(this, file + " is not a readable image.",
                    "Open failed", JOptionPane.ERROR_MESSAGE);
            return;
        }

        image = loaded;
        imageFile = file;
        jsonFile = sidecarFor(file);
        polygonListModel.clear();
        currentPoints.clear();
        dirty = false;

        zoom = computeFitZoom();
        canvas.updateSizeForZoom();

        loadSidecarIfPresent();
        updateWindowTitle();
        updateSaveEnablement();
        updateStatus();
        canvas.requestFocusInWindow();

        // revalidate()+repaint() alone sometimes leaves the viewport showing stale (blank)
        // content until some unrelated event forces a relayout, e.g. resizing the window by
        // hand - deferring one tick via invokeLater reliably avoids that, presumably because
        // it lets the JFileChooser dialog's own teardown finish first.
        SwingUtilities.invokeLater(() -> {
            scrollPane.revalidate();
            canvas.revalidate();
            scrollPane.getViewport().repaint();
            canvas.repaint();
        });
    }

    private void loadSidecarIfPresent() {
        if (!jsonFile.exists()) {
            return;
        }
        try {
            HotspotFile data = mapper.readValue(jsonFile, HotspotFile.class);
            if (data.getImageWidth() != image.getWidth() || data.getImageHeight() != image.getHeight()) {
                JOptionPane.showMessageDialog(this,
                        "Warning: " + jsonFile.getName() + " was authored for a "
                        + data.getImageWidth() + "x" + data.getImageHeight() + " image, but "
                        + imageFile.getName() + " is " + image.getWidth() + "x" + image.getHeight()
                        + ". Polygon placement may be off.",
                        "Image size mismatch", JOptionPane.WARNING_MESSAGE);
            }
            for (NamedPolygon polygon : data.getPolygons()) {
                polygonListModel.addElement(polygon);
            }
            dirty = false;
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Failed to load " + jsonFile + ": " + ex.getMessage(),
                    "Load failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void finishCurrentPolygon() {
        if (image == null) {
            return;
        }
        if (currentPoints.size() < 3) {
            statusLabel.setText("Need at least 3 points before finishing a polygon (have " + currentPoints.size() + ").");
            return;
        }
        String name = JOptionPane.showInputDialog(this, "Polygon name:", "Name polygon", JOptionPane.PLAIN_MESSAGE);
        canvas.requestFocusInWindow();
        if (name == null) {
            return;
        }
        name = name.trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Name must not be empty.", "Invalid name", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int existingIndex = indexOfPolygonNamed(name);
        if (existingIndex >= 0) {
            int choice = JOptionPane.showConfirmDialog(this,
                    "A polygon named \"" + name + "\" already exists. Replace it?",
                    "Duplicate name", JOptionPane.YES_NO_OPTION);
            if (choice != JOptionPane.YES_OPTION) {
                return;
            }
            polygonListModel.remove(existingIndex);
        }

        NamedPolygon polygon = new NamedPolygon();
        polygon.setName(name);
        List<ImagePoint> points = new ArrayList<>(currentPoints.size());
        for (Point2D.Float p : currentPoints) {
            points.add(new ImagePoint(p.x, p.y));
        }
        polygon.setPoints(points);
        polygonListModel.addElement(polygon);

        currentPoints.clear();
        dirty = true;
        canvas.repaint();
        updateStatus();
    }

    private void deleteSelectedPolygon() {
        int index = polygonList.getSelectedIndex();
        if (index < 0) {
            return;
        }
        polygonListModel.remove(index);
        dirty = true;
        canvas.repaint();
        updateStatus();
    }

    private void bindNudge(JComponent component, String keyStroke, double dx, double dy) {
        String actionKey = "nudge_" + keyStroke.replace(' ', '_');
        component.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(keyStroke), actionKey);
        component.getActionMap().put(actionKey, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                nudgeSelectedPolygon(dx, dy);
            }
        });
    }

    private void nudgeSelectedPolygon(double dx, double dy) {
        NamedPolygon polygon = polygonList.getSelectedValue();
        if (polygon == null) {
            return;
        }
        for (ImagePoint point : polygon.getPoints()) {
            point.setX(point.getX() + dx);
            point.setY(point.getY() + dy);
        }
        dirty = true;
        canvas.repaint();
        updateStatus();
    }

    private void importPolygons() {
        if (image == null) {
            return;
        }
        JFileChooser chooser = new JFileChooser(lastDirectory);
        chooser.setFileFilter(new FileNameExtensionFilter("Hotspot JSON (*.json)", "json"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();
        lastDirectory = file.getParentFile();

        HotspotFile data;
        try {
            data = mapper.readValue(file, HotspotFile.class);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Failed to read " + file + ": " + ex.getMessage(),
                    "Import failed", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (data.getImageWidth() != image.getWidth() || data.getImageHeight() != image.getHeight()) {
            JOptionPane.showMessageDialog(this,
                    "Warning: " + file.getName() + " was authored for a "
                    + data.getImageWidth() + "x" + data.getImageHeight() + " image, but the current image is "
                    + image.getWidth() + "x" + image.getHeight() + ". Imported polygons will likely need nudging.",
                    "Image size mismatch", JOptionPane.WARNING_MESSAGE);
        }

        int replaced = 0;
        for (NamedPolygon polygon : data.getPolygons()) {
            int existingIndex = indexOfPolygonNamed(polygon.getName());
            if (existingIndex >= 0) {
                polygonListModel.remove(existingIndex);
                replaced++;
            }
            polygonListModel.addElement(polygon);
        }
        if (!data.getPolygons().isEmpty()) {
            dirty = true;
        }
        canvas.repaint();
        statusLabel.setText("Imported " + data.getPolygons().size() + " polygon(s) from " + file.getName()
                + (replaced > 0 ? " (" + replaced + " replaced existing)." : "."));
        updateWindowTitle();
    }

    private int indexOfPolygonNamed(String name) {
        for (int i = 0; i < polygonListModel.size(); i++) {
            if (polygonListModel.get(i).getName().equals(name)) {
                return i;
            }
        }
        return -1;
    }

    private void save(File target) {
        if (image == null || target == null) {
            return;
        }
        HotspotFile data = new HotspotFile();
        data.setImageWidth(image.getWidth());
        data.setImageHeight(image.getHeight());
        List<NamedPolygon> polygons = new ArrayList<>(polygonListModel.size());
        for (int i = 0; i < polygonListModel.size(); i++) {
            polygons.add(polygonListModel.get(i));
        }
        data.setPolygons(polygons);

        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(target, data);
            jsonFile = target;
            dirty = false;
            statusLabel.setText("Saved " + target.getName() + " (" + polygons.size() + " polygon(s)).");
            updateWindowTitle();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Failed to save " + target + ": " + ex.getMessage(),
                    "Save failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveAs() {
        if (image == null) {
            return;
        }
        JFileChooser chooser = new JFileChooser(jsonFile != null ? jsonFile.getParentFile() : lastDirectory);
        chooser.setSelectedFile(jsonFile != null ? jsonFile : sidecarFor(imageFile));
        chooser.setFileFilter(new FileNameExtensionFilter("Hotspot JSON (*.hotspots.json)", "json"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File target = chooser.getSelectedFile();
        if (!target.getName().toLowerCase(Locale.ROOT).endsWith(".json")) {
            target = new File(target.getParentFile(), target.getName() + ".json");
        }
        save(target);
    }

    private boolean confirmDiscardIfDirty(String message) {
        if (!dirty) {
            return true;
        }
        int choice = JOptionPane.showConfirmDialog(this, message, "Unsaved changes", JOptionPane.YES_NO_OPTION);
        return choice == JOptionPane.YES_OPTION;
    }

    private void updateSaveEnablement() {
        saveItem.setEnabled(image != null);
        saveAsItem.setEnabled(image != null);
        importItem.setEnabled(image != null);
    }

    private void updateWindowTitle() {
        StringBuilder title = new StringBuilder("Launch Window - hotspot editor");
        if (imageFile != null) {
            title.append("  -  ").append(imageFile.getName());
        }
        if (dirty) {
            title.append(" *");
        }
        setTitle(title.toString());
    }

    private void updateStatus() {
        if (image == null) {
            statusLabel.setText("Open an image to begin (File > Open Image...).");
            return;
        }
        statusLabel.setText(String.format(Locale.ROOT,
                "zoom %.0f%%   in-progress points: %d   polygons: %d",
                zoom * 100, currentPoints.size(), polygonListModel.size()));
        updateWindowTitle();
    }

    private float computeFitZoom() {
        Dimension extent = scrollPane.getViewport().getExtentSize();
        if (extent.width <= 0 || extent.height <= 0 || image == null) {
            return 1f;
        }
        float fitX = (float) extent.width / image.getWidth();
        float fitY = (float) extent.height / image.getHeight();
        return clamp(Math.min(1f, Math.min(fitX, fitY)), MIN_ZOOM, MAX_ZOOM);
    }

    private Point clampViewPosition(Point p) {
        Dimension viewSize = canvas.getPreferredSize();
        Dimension extent = scrollPane.getViewport().getExtentSize();
        int maxX = Math.max(0, viewSize.width - extent.width);
        int maxY = Math.max(0, viewSize.height - extent.height);
        return new Point(Math.max(0, Math.min(p.x, maxX)), Math.max(0, Math.min(p.y, maxY)));
    }

    private static File sidecarFor(File imageFile) {
        String name = imageFile.getName();
        int dot = name.lastIndexOf('.');
        String base = dot >= 0 ? name.substring(0, dot) : name;
        return new File(imageFile.getParentFile(), base + SIDECAR_SUFFIX);
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    /**
     * Renders the loaded image at the current zoom, plus every finished
     * polygon (highlighted if selected in the side list) and the
     * in-progress one being placed.
     */
    private final class Canvas extends JPanel {

        void updateSizeForZoom() {
            if (image == null) {
                setPreferredSize(new Dimension(800, 600));
                return;
            }
            setPreferredSize(new Dimension(Math.round(image.getWidth() * zoom), Math.round(image.getHeight() * zoom)));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (image == null) {
                g.setColor(Color.LIGHT_GRAY);
                g.drawString("File > Open Image...", 20, 20);
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    zoom >= 1f ? RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
                            : RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(image, 0, 0, Math.round(image.getWidth() * zoom), Math.round(image.getHeight() * zoom), null);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            NamedPolygon selected = polygonList.getSelectedValue();
            for (int i = 0; i < polygonListModel.size(); i++) {
                NamedPolygon polygon = polygonListModel.get(i);
                boolean isSelected = polygon == selected;
                drawFinishedPolygon(g2, polygon, isSelected);
            }
            drawInProgressPolygon(g2);
            g2.dispose();
        }

        private void drawFinishedPolygon(Graphics2D g2, NamedPolygon polygon, boolean isSelected) {
            List<ImagePoint> points = polygon.getPoints();
            if (points.isEmpty()) {
                return;
            }
            int[] xs = new int[points.size()];
            int[] ys = new int[points.size()];
            for (int i = 0; i < points.size(); i++) {
                xs[i] = Math.round((float) (points.get(i).getX() * zoom));
                ys[i] = Math.round((float) (points.get(i).getY() * zoom));
            }
            g2.setColor(isSelected ? SELECTED_FILL : FINISHED_FILL);
            g2.fillPolygon(xs, ys, xs.length);
            g2.setColor(isSelected ? SELECTED_OUTLINE : FINISHED_OUTLINE);
            g2.drawPolygon(xs, ys, xs.length);

            g2.setColor(Color.WHITE);
            g2.drawString(polygon.getName(), xs[0] + 4, ys[0] - 4);
        }

        private void drawInProgressPolygon(Graphics2D g2) {
            if (currentPoints.isEmpty()) {
                return;
            }
            g2.setColor(IN_PROGRESS_LINE);
            for (int i = 0; i < currentPoints.size() - 1; i++) {
                Point2D.Float a = currentPoints.get(i);
                Point2D.Float b = currentPoints.get(i + 1);
                g2.drawLine(Math.round(a.x * zoom), Math.round(a.y * zoom), Math.round(b.x * zoom), Math.round(b.y * zoom));
            }
            if (currentPoints.size() >= 3) {
                Point2D.Float first = currentPoints.get(0);
                Point2D.Float last = currentPoints.get(currentPoints.size() - 1);
                Color old = g2.getColor();
                g2.setColor(new Color(1f, 1f, 1f, 0.35f));
                g2.drawLine(Math.round(last.x * zoom), Math.round(last.y * zoom), Math.round(first.x * zoom), Math.round(first.y * zoom));
                g2.setColor(old);
            }
            g2.setColor(IN_PROGRESS_POINT);
            for (Point2D.Float p : currentPoints) {
                int px = Math.round(p.x * zoom);
                int py = Math.round(p.y * zoom);
                g2.fillOval(px - 3, py - 3, 6, 6);
            }
        }
    }
}
