import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Joel on 2016-02-20.
 */
public class Main {
    private JPanel mainPanel;
    private JButton selectDirButton;
    private JButton selectImgButton;
    private JSlider pixelSlider;
    private JLabel pixelSliderLabel;
    private JButton makeButton;
    private JLabel imgSliderLabel;
    private JSlider imgSlider;
    private BufferedImage originalImg;
    private Path dirPath;
    private BufferedImage originalResized;
    private ArrayList mosaicList;
    private int pixelSize;

    public static void main(String[] args) {
        new Main();
    }

    public Main() {
        javax.swing.SwingUtilities.invokeLater(() -> createAndShowGUI());
    }

    private void createAndShowGUI(){
        JFrame frame = new JFrame("Test");
        frame.setContentPane(mainPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);

        selectImgButton.addActionListener(e -> selectImg(mainPanel));
        selectDirButton.addActionListener(e -> selectDirectory(mainPanel));
        makeButton.addActionListener(new MakeButtonListener());
        imgSliderLabel.setText(Integer.toString(imgSlider.getValue()));
        pixelSliderLabel.setText(Integer.toString(pixelSlider.getValue()));
        imgSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                JSlider source = (JSlider)e.getSource();
                imgSliderLabel.setText(Integer.toString(source.getValue()));
            }
        });
        selectDirButton.setEnabled(false);
        pixelSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                JSlider source = (JSlider)e.getSource();
                pixelSliderLabel.setText(Integer.toString(source.getValue()));
            }
        });
        selectDirButton.setEnabled(false);
        makeButton.setEnabled(false);
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
            System.out.println("You chose to open this file: " +
                    chooser.getSelectedFile().getName());
            originalImg = ImageTools.convertFileToImage(chooser.getSelectedFile());
            selectDirButton.setEnabled(true);
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
            makeButton.setEnabled(true);
        }
    }

    private class MakeButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            pixelSize = pixelSlider.getValue();
            int imgSize = imgSlider.getValue();

            //make array of all images in selected directory
            ArrayList pixelImgArray = ImageTools.makeImageArray(dirPath, pixelSize);

            //make array of color info from original image
            originalResized = ImageTools.resizeImage(originalImg, imgSize);
            BufferedImage[] originalImgArray  = ImageTools.makeOriginalImgArray(pixelSize, pixelSize, originalResized);
            ArrayList avColorArray = ImageTools.makeAvColorArray(originalImgArray);

            //make mosaic
            mosaicList = ImageTools.createCompositeArray(avColorArray, pixelImgArray);
            displayImage(mosaicList);
        }
    }

    /**
     * creates JPG from image ArrayList and displays it in the system's default viewer
     * @param mosaicList image ArrayList
     */
    private void displayImage(ArrayList mosaicList){
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
            //temp = File.createTempFile("ImgMosaicTemp", ".jpg", new File(System.getProperty("user.home")));
            temp = new File(System.getProperty("user.home")+ "/image.jpg");
//            ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
            FileImageOutputStream output = new FileImageOutputStream(temp);
//            writer.setOutput(output);
//            writer.write(mosaic);
            ImageIO.write(mosaic, "jpg", output);
//            Desktop.getDesktop().open(temp);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            g2.dispose();
            temp.deleteOnExit();
        }
    }
}
