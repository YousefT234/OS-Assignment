import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.Queue;
import javax.swing.*;

class semaphore {

    protected int value = 0;
    protected semaphore() {
        value = 0;
    }
    protected semaphore(int initial) {
        value = initial;
    }
    public synchronized void P() {
        value--;
        if (value < 0)
            try {
                wait();
            } catch (InterruptedException e) {
            }
    }
    public synchronized void V() {
        value++;
        if (value <= 0)
            notify();
    }
}

class Car extends Thread {

    public void run() {

    }
}

class Pump extends Thread {

    public void run() {}
}

public class ServiceStation extends JFrame implements ActionListener {
    private Font font = new Font("Cooper Black", Font.BOLD, 60);
    private Font buttonFont = new Font("Consolas", Font.BOLD, 14);
    private static final Color DARK_BROWN_COLOR = new Color(101, 67, 33);

    private static final Color WHITE_COLOR = Color.WHITE;

    private int bufferSize;
    private int numPumps;
    private int numCars;
    protected Queue<String> queue;
    protected semaphore mutex;
    protected semaphore empty;
    protected semaphore full;
    protected semaphore bays;
    private List<Thread> pumpThreads;
    private List<Thread> carThreads;

    private JPanel leftPanel, mainPanel, inputPanel, queuePanel, pumpsPanel, logPanel;
    private JButton startButton, viewQueueButton, viewPumpsButton, resetButton;
    private JTextField bufferField, pumpsField, carsField;
    private JTextArea logArea;
    private DefaultListModel<String> queueModel;
    private JList<String> queueList;

    private JLayeredPane jLayeredPane;
    private JPanel buttonsPanel;
    private JButton welcomeStartButton;
    private JLabel welcomeLabel;

    public ServiceStation() {
        super("Service Station Simulation");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setExtendedState(getExtendedState() | JFrame.ICONIFIED);
        setResizable(true);
        setLayout(null);
        setupWelcomeScreen();
        setVisible(true);
    }

    private void setupWelcomeScreen() {
        jLayeredPane = new JLayeredPane();
        jLayeredPane.setBounds(0, 0, getWidth(), getHeight());
        jLayeredPane.setBackground(DARK_BROWN_COLOR);
        jLayeredPane.setOpaque(true);
        ImageIcon backgroundIcon = new ImageIcon("4a4a8f3537407909776d89f6240c1c0a.gif");
        JLabel gifLabel = new JLabel(backgroundIcon);
        gifLabel.setBounds(0, 0, getWidth(), getHeight());
        jLayeredPane.add(gifLabel, JLayeredPane.DEFAULT_LAYER);

        welcomeLabel = new JLabel("ServiceStationSimulation");
        welcomeLabel.setFont(font);
        welcomeLabel.setForeground(Color.WHITE);
        welcomeLabel.setBounds(200, 200, 800, 100);
        welcomeLabel.setHorizontalAlignment(SwingConstants.CENTER);
        jLayeredPane.add(welcomeLabel, JLayeredPane.PALETTE_LAYER);

        buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.X_AXIS));
        buttonsPanel.setBounds(400, 500, 400, 50);
        buttonsPanel.setOpaque(false);

        welcomeStartButton = new JButton("Start Simulation");
        welcomeStartButton.setFont(buttonFont);
        welcomeStartButton.setBackground(Color.WHITE);
        welcomeStartButton.setForeground(Color.BLACK);
        welcomeStartButton.addActionListener(this);

        buttonsPanel.add(Box.createHorizontalGlue());
        buttonsPanel.add(welcomeStartButton);
        buttonsPanel.add(Box.createHorizontalGlue());

        jLayeredPane.add(buttonsPanel, JLayeredPane.POPUP_LAYER);
        add(jLayeredPane);
    }

    private void switchToSimulationScreen() {
        remove(jLayeredPane);

        setLayout(new BorderLayout());

        leftPanel = new JPanel(null);
        leftPanel.setBackground(DARK_BROWN_COLOR.darker());
        leftPanel.setPreferredSize(new Dimension(300, getHeight()));
        add(leftPanel, BorderLayout.WEST);

        ImageIcon stationIcon = new ImageIcon("4a4a8f3537407909776d89f6240c1c0a.gif");
        JLabel iconLabel = new JLabel(stationIcon, JLabel.CENTER);
        iconLabel.setBounds(10, 10, 280, 150);
        leftPanel.add(iconLabel);

        startButton = createStyledButton("Start Simulation", 50, 180);
        startButton.addActionListener(this);
        viewQueueButton = createStyledButton("View Queue", 50, 220);
        viewQueueButton.addActionListener(this);
        viewPumpsButton = createStyledButton("View Pumps", 50, 260);
        viewPumpsButton.addActionListener(this);
        resetButton = createStyledButton("Reset", 50, 300);
        resetButton.addActionListener(this);

        mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(WHITE_COLOR);
        add(mainPanel, BorderLayout.CENTER);

        inputPanel = new JPanel(new GridLayout(4, 3, 10, 10));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        inputPanel.add(new JLabel("Buffer Size:"));
        bufferField = new JTextField("5");
        bufferField.setFont(buttonFont);
        inputPanel.add(bufferField);
        inputPanel.add(new JLabel(""));
        inputPanel.add(new JLabel("Num Pumps:"));
        pumpsField = new JTextField("3");
        pumpsField.setFont(buttonFont);
        inputPanel.add(pumpsField);
        inputPanel.add(new JLabel(""));
        inputPanel.add(new JLabel("Num Cars:"));
        carsField = new JTextField("5");
        carsField.setFont(buttonFont);
        inputPanel.add(carsField);
        inputPanel.add(new JLabel(""));
        mainPanel.add(inputPanel, BorderLayout.NORTH);

        queuePanel = new JPanel(new BorderLayout());
        queuePanel.setPreferredSize(new Dimension(200, 400));
        queuePanel.setBorder(BorderFactory.createTitledBorder("Waiting Queue"));
        queueModel = new DefaultListModel<>();
        queueList = new JList<>(queueModel);
        queueList.setFont(buttonFont);
        queuePanel.add(new JScrollPane(queueList), BorderLayout.CENTER);
        mainPanel.add(queuePanel, BorderLayout.WEST);

        pumpsPanel = new JPanel();
        pumpsPanel.setBorder(BorderFactory.createTitledBorder("Service Bays / Pumps"));
        mainPanel.add(pumpsPanel, BorderLayout.CENTER);

        logPanel = new JPanel(new BorderLayout());


        revalidate();
        repaint();
    }

    private JButton createStyledButton(String text, int x, int y) {
        JButton btn = new JButton(text);
        btn.setBackground(Color.WHITE.brighter());
        btn.setForeground(Color.BLACK);
        btn.setFont(buttonFont);
        btn.setBounds(x, y, 200, 30);
        leftPanel.add(btn);
        return btn;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == welcomeStartButton) {
            switchToSimulationScreen();
            return;
        }
        if (e.getSource() == startButton) {
            try {
                startSimulation();
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
                }
    }

    public void startSimulation() throws InterruptedException {
        bufferSize = Integer.parseInt(bufferField.getText());
        numPumps = Integer.parseInt(pumpsField.getText());
        numCars = Integer.parseInt(carsField.getText());

        if (logArea != null) {
            logArea.setText("");
        }
        queueModel.clear();

        queue = new LinkedList<>();

        if (carThreads != null) {
            for (Thread t : carThreads) {
                t.join();
            }
        }

        if (full != null) {
            for (int i = 0; i < numPumps; i++) {
                full.V();
            }
        }

        if (pumpThreads != null) {
            for (Thread t : pumpThreads) {
                t.join();
            }
        }

    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ServiceStation());
    }
}