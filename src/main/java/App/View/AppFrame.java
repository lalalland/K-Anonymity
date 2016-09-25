package App.View;

import App.Controller.DataSetController;
import App.Controller.KDegreeAlgorithm;
import App.Model.AlgoType;
import App.Model.DataSetModel;
import App.UITasks.DataSetLoaderTask;
import org.apache.commons.lang.SerializationUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Keinan.Gilad on 9/10/2016.
 */
public class AppFrame extends JFrame {
    private static Logger logger = Logger.getLogger(AppFrame.class);
    public static final String ORIGINAL_CHARTS = "Original Charts";
    public static final String EXECUTE = "Execute";
    public static final String ABOUT_TEXT = "Written by Keinan Gilad (keinan.gilad@gmail.com) for MSc in open university.";
    public static final String ALGORITHMS = "Algorithms";
    public static final String CHOOSE_K = "K";
    public static final String DATA_SETS = "Data Sets";
    private static final String FRAME_TITLE = "K-Anonymity Algorithm Simulator";
    private static final Boolean initDataSets = true;

    @Autowired
    private DataSetController dataSetController;

    @Autowired
    private KDegreeAlgorithm kDegreeAlgorithm;

    private List<String> dataSetsNames;
    private ButtonGroup buttonGroupAlgorithms;
    private ButtonGroup buttonGroupForK;
    private ButtonGroup buttonGroupForDataSets;
    private HashMap<String, JProgressBar> dataSetToProgressBar;
    private HashMap<String, JComponent> dataSetToChartPanel;
    private JTabbedPane tabbedPane;
    private JPanel dataSetPickerPanel;
    private JPanel kPanel;
    private JPanel algorithmPanel;

    @SuppressWarnings("ALL")
    public void initUIComponents() {
        // set layout properties
        final Container contentPane = getContentPane();
        GroupLayout layout = new GroupLayout(contentPane);
        contentPane.setLayout(layout);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        setTitle(FRAME_TITLE);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(true);
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        // radio buttons for algorithms
        initAlgorithms();

        // radio buttons for choosing K
        initK();

        // radio buttons for choosing Data Sets
        initDataSetControls();

        JSeparator sp1 = new JSeparator();

        // execute button
        final JButton executeButton = new JButton(EXECUTE);
        executeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                runAlgorithm();
            }
        });

        // setting the Layout
        layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                        // column 0
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(algorithmPanel)

                                // level 2
                                .addComponent(executeButton)
                        )
                        // column 1
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(kPanel)
                        )
                        // column 2
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(dataSetPickerPanel)
                        )
                )
                .addComponent(tabbedPane)
                .addComponent(sp1)
        );

        layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                        // row 0
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addGroup(layout.createSequentialGroup()
                                        .addComponent(algorithmPanel)
                                )
                                .addGroup(layout.createSequentialGroup()
                                        .addComponent(kPanel)
                                )

                                .addGroup(layout.createSequentialGroup()
                                        .addComponent(dataSetPickerPanel)
                                )
                        )
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(executeButton)
                        )
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(sp1)
                                .addComponent(tabbedPane)
                        )
                )
        );

        MenuBar menubar = new MenuBar();
        final Menu fileMenu = new Menu("File");
        final MenuItem aboutItem = new MenuItem("About");
        aboutItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(contentPane, ABOUT_TEXT);
            }
        });
        fileMenu.add(aboutItem);
        menubar.add(fileMenu);
        this.setMenuBar(menubar);
        pack();
    }

    private void runAlgorithm() {
        // create the input context with all the needed information for execute
        final String algorithm = buttonGroupAlgorithms.getSelection().getActionCommand();
        final String k = buttonGroupForK.getSelection().getActionCommand();
        final String dataSet = buttonGroupForDataSets.getSelection().getActionCommand();

        // run the algorithm
        Thread thread = new Thread(new Runnable() {
            public void run() {
                DataSetModel dataSetToModel = dataSetController.getDataSetToModel(dataSet);
                if (dataSetToModel == null) {
                    return;
                }
                DataSetModel originalClone = (DataSetModel) SerializationUtils.clone(dataSetToModel);
                logger.debug("Start Algorithm " + algorithm + "Event on " + dataSet + " with K= " + k);

                if (AlgoType.KDegree.toString().equals(algorithm)) {
                    long before = System.currentTimeMillis();
                    DataSetModel annonymizeData = kDegreeAlgorithm.annonymize(originalClone, Integer.valueOf(k));
                    long after = System.currentTimeMillis();
                    annonymizeData.setEdgeAdded(annonymizeData.getEdges().size() - dataSetToModel.getEdges().size());
                    annonymizeData.setDuration(after - before);
                    annonymizeData.setAnonymized(true);
                    annonymizeData.setTitle(String.format("%s Annonymized with %s", k, algorithm));
                    addViewToPanel(annonymizeData);

                } else if (AlgoType.KNeighborhood.toString().equals(algorithm)) {

                } else {
                    logger.debug("Not valid execution.");
                }
            }
        });
        thread.start();
    }

    private void addViewToPanel(DataSetModel dataSetToModel) {
        TableView table = new TableView(dataSetToModel);
        JPanel chartPanel = (JPanel) dataSetToChartPanel.get(dataSetToModel.getDataSet());
        chartPanel.add(table);
        chartPanel.revalidate();
        chartPanel.repaint();
        logger.debug("Done Graph Event");
    }

    private void initAlgorithms() {
        algorithmPanel = new JPanel();
        algorithmPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), ALGORITHMS, TitledBorder.CENTER, TitledBorder.TOP));
        algorithmPanel.setLayout(new BoxLayout(algorithmPanel, BoxLayout.Y_AXIS));
        buttonGroupAlgorithms = new ButtonGroup();

        for (AlgoType type : AlgoType.values()) {
            JRadioButton radioButtonAlgorithm = new JRadioButton(type.name());
            radioButtonAlgorithm.setActionCommand(AlgoType.KDegree.toString());
            radioButtonAlgorithm.setSelected(true);
            buttonGroupAlgorithms.add(radioButtonAlgorithm);
            algorithmPanel.add(radioButtonAlgorithm);
        }
    }

    private void initK() {
        kPanel = new JPanel();
        kPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), CHOOSE_K, TitledBorder.CENTER, TitledBorder.TOP));
        kPanel.setLayout(new BoxLayout(kPanel, BoxLayout.Y_AXIS));
        buttonGroupForK = new ButtonGroup();

        for (int i = 1; i < 5; i++) {
            String k = String.valueOf(i * 5);
            JRadioButton radioButton = new JRadioButton(k);
            radioButton.setActionCommand(k);
            radioButton.setSelected(true);
            buttonGroupForK.add(radioButton);
            kPanel.add(radioButton);
        }
    }

    private void initDataSetControls() {
        // init main panel:
        dataSetPickerPanel = new JPanel();
        dataSetPickerPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), DATA_SETS, TitledBorder.CENTER, TitledBorder.TOP));
        GroupLayout layout = new GroupLayout(dataSetPickerPanel);
        dataSetPickerPanel.setLayout(layout);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        // init radio panel
        JPanel radioButtonsPanel = new JPanel();
        radioButtonsPanel.setLayout(new BoxLayout(radioButtonsPanel, BoxLayout.Y_AXIS));

        // init progress panel
        JPanel progressBarPanel = new JPanel();
        progressBarPanel.setLayout(new BoxLayout(progressBarPanel, BoxLayout.Y_AXIS));

        // init charts
        tabbedPane = new JTabbedPane();
        dataSetsNames = dataSetController.getDataSetsNames();
        dataSetToProgressBar = new HashMap<>();
        buttonGroupForDataSets = new ButtonGroup();
        dataSetToChartPanel = new HashMap<>();

        // iterating the data sets:
        for (final String dataSet : dataSetsNames) {
            JRadioButton dataSetRadioButton = new JRadioButton(dataSet);
            dataSetRadioButton.setActionCommand(dataSet);
            dataSetRadioButton.setSelected(true);
            buttonGroupForDataSets.add(dataSetRadioButton);

            // create the data set progress bar for loading
            JProgressBar progressBar = new JProgressBar();
            progressBar.setBounds(0, 0, 100, 100);
            progressBar.setValue(0);
            progressBar.setStringPainted(true);
            progressBar.setMaximumSize(new Dimension(100, 25));
            dataSetToProgressBar.put(dataSet, progressBar);

            // add to panel:
            radioButtonsPanel.add(dataSetRadioButton);
            progressBarPanel.add(progressBar);

            // init the chart
            JPanel chartsPanel = new JPanel();
            chartsPanel.setPreferredSize(new Dimension(500, 500));
            chartsPanel.setLayout(new GridLayout(0, 3));
            chartsPanel.setAutoscrolls(true);
            dataSetToChartPanel.put(dataSet, chartsPanel);

            // add a new tab:
            tabbedPane.add(chartsPanel, dataSet);

            // load the data set into persistence
            if (initDataSets) {
                createDataSetTask(dataSet);
            }
        }

        // setting the panel layout
        layout.setHorizontalGroup(layout.createSequentialGroup()
                .addComponent(radioButtonsPanel)
                .addComponent(progressBarPanel)
        );

        layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(radioButtonsPanel)
                .addComponent(progressBarPanel)
        );
    }

    private void createDataSetTask(String dataSet) {
        DataSetLoaderTask task = new DataSetLoaderTask(dataSet, dataSetController);
        task.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                String propertyName = evt.getPropertyName();
                final String dataSet = ((DataSetLoaderTask) evt.getSource()).getDataSetName();
                if ("progress".equals(propertyName)) {
                    // update the progress bar with recent value
                    int progress = (Integer) evt.getNewValue();
                    JProgressBar progressBar = dataSetToProgressBar.get(dataSet);
                    progressBar.setValue(progress);
                    //logger.debug("Progress DataSet Event: " + dataSet + " Progress:" + progress);
                } else if ("done".equals(propertyName)) {
                    logger.debug("Start Graph Event: " + dataSet);
                    Thread thread = new Thread(new Runnable() {
                        public void run() {
                            DataSetModel dataSetToModel = dataSetController.getDataSetToModel(dataSet);
                            addViewToPanel(dataSetToModel);
                        }
                    });
                    thread.start();
                }
            }
        });
        task.execute();
    }
}