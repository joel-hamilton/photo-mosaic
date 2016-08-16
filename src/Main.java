import javax.imageio.ImageIO;
import javax.imageio.stream.FileImageOutputStream;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;


/**
 * Created by Joel on 2016-02-20.
 */
public class Main {
    private JPanel mainPanel;
    private JButton selectDirButton;
    private JButton selectImgButton;
    private JSlider pixelSlider;
    private JLabel pixelSliderLabel;
    private JLabel imgSliderLabel;
    private JSlider imgSlider;
    private JButton sampleImagesButton;
    private JProgressBar progressBar1;
    private BufferedImage originalImg;
    private Path dirPath;
    private BufferedImage originalResized;
    private ArrayList mosaicList;
    private int pixelSize;

    private static Main instance = null;

    public static void main(String[] args) {
        instance = new Main();
    }

    public static Main getInstance(){
        return instance;
    }

    private Main() {
        javax.swing.SwingUtilities.invokeLater(() -> createAndShowGUI());
    }

    private void createAndShowGUI(){
        JFrame frame = new JFrame("Image Mosaic");
        frame.setContentPane(mainPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);

        selectImgButton.addActionListener(e -> selectImg(mainPanel));
        selectDirButton.addActionListener(e -> selectDirectory(mainPanel));
        sampleImagesButton.addActionListener(e -> selectDefaultDirectory(mainPanel));
        imgSliderLabel.setText(Integer.toString(imgSlider.getValue()));
        pixelSliderLabel.setText(Integer.toString(pixelSlider.getValue()));
        imgSliderLabel.setText(Integer.toString(imgSlider.getValue()) + "px");
        pixelSliderLabel.setText(Integer.toString(pixelSlider.getValue()) + "px");

        imgSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                JSlider source = (JSlider)e.getSource();
                imgSliderLabel.setText(Integer.toString(source.getValue()) + "px");
            }
        });
        pixelSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                JSlider source = (JSlider)e.getSource();
                pixelSliderLabel.setText(Integer.toString(source.getValue()) + "px");
            }
        });
    }

    private void selectImg(Component parent) {
        JFileChooser chooser = new JFileChooser();
        File dir = new File(System.getProperty("user.home"));
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "JPG Images", "jpg", "jpeg");
        chooser.setFileFilter(filter);
        chooser.setCurrentDirectory(dir);
        int returnVal = chooser.showOpenDialog(parent);
        if(returnVal == JFileChooser.APPROVE_OPTION) {
            originalImg = ImageTools.convertFileToImage(chooser.getSelectedFile());
            selectDirButton.setEnabled(true);
            sampleImagesButton.setEnabled(true);
            progressBar1.setValue(0);
            progressBar1.setVisible(false);

        }
    }

    private void selectDirectory(Component parent) {
        JFileChooser chooser = new JFileChooser();
        File dir = new File(System.getProperty("user.home"));
        File[] fileArray;
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setCurrentDirectory(dir);
        int returnVal = chooser.showOpenDialog(parent);
        if(returnVal == JFileChooser.APPROVE_OPTION) {
            dirPath = chooser.getSelectedFile().toPath();
            Thread t = new Thread() {
                public void run() {
                    makeMosaic();
                }
            };
            t.start();
        }
        progressBar1.setVisible(true);
    }
    private void selectDefaultDirectory(Component parent) {
            dirPath = new File("sample_images").toPath();
        System.out.println(dirPath);
        Thread t = new Thread() {
            public void run() {
                makeMosaic();
            }
        };
        t.start();
    }

    private void makeMosaic(){
        pixelSize = pixelSlider.getValue();
        int imgSize = imgSlider.getValue();

        //make array of all images in selected directory
        ArrayList pixelImgArray = ImageTools.makeImageArray(dirPath, pixelSize);
        if (pixelImgArray == null){
            return;
        }
        progressBar1.setValue(10);
        //make array of color info from original image
        progressBar1.setValue(30);
        originalResized = ImageTools.resizeImage(originalImg, imgSize);
        progressBar1.setValue(50);
        BufferedImage[] originalImgArray  = ImageTools.makeOriginalImgArray(pixelSize, pixelSize, originalResized);
        progressBar1.setValue(70);
        ArrayList avColorArray = ImageTools.makeAvColorArray(originalImgArray);

        //make mosaic
        progressBar1.setValue(90);
        mosaicList = ImageTools.createCompositeArray(avColorArray, pixelImgArray);

        progressBar1.setValue(100);
        showDialog(mosaicList);
    }

    /**
     * creates JPG from image ArrayList and displays it in the system's default viewer
     * @param mosaicList image ArrayList
     */
    private void showDialog(ArrayList mosaicList){
        Object[] options = {"Save",
                "Open",
                "Cancel"};
        int n = JOptionPane.showOptionDialog(mainPanel,
                "Save or open image?",
                "All done",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[2]);
        switch(n){
            case 0:
                saveImage(mosaicList);
                break;
            case 1:
                openImage(mosaicList);
                break;
            case 2://// TODO: 2016-02-23 close dialog gracefully 
                break;
            default:
                new Error("no selection made");
        }

    }

    /**
     * opens image in default JPG viewer
     * @param mosaicList
     */
    private void openImage(ArrayList mosaicList) {//TODO can't open this temp file until virtual machine exits
        BufferedImage mosaic = new BufferedImage(originalResized.getWidth(), originalResized.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = mosaic.createGraphics();
        File temp = null;

        int xPos = 0;
        int yPos = 0;
        for(int i=0;i<mosaicList.size();i++){
            if (i!=0)
                xPos += pixelSize;
            if(i != 0 && i % (originalResized.getWidth()/pixelSize) == 0){
                yPos += pixelSize;
                xPos = 0;
            }
            g2.drawImage((BufferedImage)mosaicList.get(i), xPos, yPos, null);
        }
        try {
            temp = File.createTempFile("ImgMosaicTemp", ".jpg", new File(System.getProperty("user.home")));
            FileImageOutputStream output = new FileImageOutputStream(temp);
            try {
                ImageIO.write(mosaic, "jpg", output);
                Desktop.getDesktop().open(temp);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                output.close();
                g2.dispose();
                temp.deleteOnExit();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * pulls up save dialog, after Mosaic created
     * @param mosaicList
     */
    private void saveImage(ArrayList mosaicList) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Specify a file to save");
        File fileToSave = null;

        int userSelection = fileChooser.showSaveDialog(mainPanel);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            fileToSave = fileChooser.getSelectedFile();
            if (!fileToSave.toString().endsWith(".jpg")) {
                System.out.println("doesn't end with jpg");
                fileToSave = new File(fileToSave.toString() + ".jpg");
            }
        }

        BufferedImage mosaic = new BufferedImage(originalResized.getWidth(), originalResized.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = mosaic.createGraphics();
        File temp = null;

        int xPos = 0;
        int yPos = 0;
        for(int i=0;i<mosaicList.size();i++){
            if (i!=0)
                xPos += pixelSize;
            if(i != 0 && i % (originalResized.getWidth()/pixelSize) == 0){
                yPos += pixelSize;
                xPos = 0;
            }
            g2.drawImage((BufferedImage)mosaicList.get(i), xPos, yPos, null);
        }
        try{
            FileImageOutputStream output = new FileImageOutputStream(fileToSave);
            try {
                ImageIO.write(mosaic, "jpg", output);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                output.close();
                g2.dispose();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public JProgressBar getProgressBar(){
        return progressBar1;
    }
    public JPanel getMainPanel(){
        return mainPanel;
    }
}
