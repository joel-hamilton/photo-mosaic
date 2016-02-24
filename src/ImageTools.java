import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Created by Joel on 2016-02-20.
 */
public class ImageTools {

    public static BufferedImage convertFileToImage(File imageFile){
        BufferedImage image = null;

        try {
            image = ImageIO.read(imageFile);
        }
        catch (IOException ex){
            ex.printStackTrace();
        }
        catch(IllegalArgumentException e){
            e.printStackTrace();
        }
        finally{
            if (image != null)
                image.flush();
        }
        return image;
    }

    /**
     *
     * @param image original image
     * @param x long side, in pixels
     * @return resized image, maintains aspect ratio
     *
     */
    public static BufferedImage resizeImage(BufferedImage image, int x){
        int y;
        double ratio;
        ratio = (double)image.getWidth()/image.getHeight();
        y = (int)(x/ratio);

        Image scaled = image.getScaledInstance(x, y, Image.SCALE_AREA_AVERAGING);
        BufferedImage smallImage = new BufferedImage(scaled.getWidth(null), scaled.getHeight(null), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = smallImage.createGraphics();

        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.drawImage(scaled, 0, 0, x, y, null);
        g2.dispose();
        return smallImage;
    }

    /**
     *
     * @param image original image
     * @param x side length
     * @return resized image, scaled square
     */
    public static BufferedImage resizeSquare(BufferedImage image, int x){
        double ratio;

        Image scaled = image.getScaledInstance(x, x, Image.SCALE_AREA_AVERAGING);
        BufferedImage smallImage = new BufferedImage(scaled.getWidth(null), scaled.getHeight(null), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = smallImage.createGraphics();

        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.drawImage(scaled, 0, 0, x, x, null);
        g2.dispose();
        return smallImage;
    }

    /**
     *
     * @param x
     * @param y
     * @param image original image (which mosaic will be based one)
     * @return Array of image chunks, size x & y
     */
    public static BufferedImage[] makeOriginalImgArray(int x, int y, BufferedImage image){
        int rows = image.getHeight()/x;
        int cols = image.getWidth()/y;

        int count = 0;
        BufferedImage dividedArray[] = new BufferedImage[rows*cols];

        for (int r = 0;r < rows ;r++){
            for (int c = 0; c < cols;c++){
                dividedArray[count] = new BufferedImage(x, y, image.getType());
                Graphics2D gr = dividedArray[count].createGraphics();
                gr.drawImage(image, 0, 0, x, y, x * c, y * r, x * c + x, y * r + y, null);
                gr.dispose();
                count++;
            }
        }
        return dividedArray;
    }

    /**
     *
     * @param list input image array(made from original JPG)
     * @return ArrayList of Colors, corresponding to each chunk in the image array
     */
    public static ArrayList makeAvColorArray(BufferedImage[] list) {
        ArrayList colorList = new ArrayList<Color>();
        for (int i =0; i< list.length;i++) {
            Color average = ImageTools.getAvColor(list[i]);
            colorList.add(average);
        }
        return colorList;
    }

    /**
     *
     * @param image image to get color from
     * @return Color average color, based on random selection of int "sampleSize" pixels
     */
    public static Color getAvColor(BufferedImage image){
        int reds = 0;
        int greens = 0;
        int blues = 0;
        int sqrt = 5; //root of sampled pixels, change this instead of sampleSize
        int sampleSize = sqrt * sqrt;    //number of pixels to sample
        Color avColor;

        for (int x=0; x<sqrt; x++){
            for (int y=0; y<sqrt; y++){
                int width = (int)(Math.random()*image.getWidth());
                int height = (int)(Math.random()*image.getHeight());
                Color pixelColor = new Color(image.getRGB(width, height));
                reds += pixelColor.getRed();
                greens += pixelColor.getGreen();
                blues += pixelColor.getBlue();
            }
        }
        avColor = (new Color(reds/sampleSize, greens/sampleSize, blues/sampleSize));
        return avColor;
    }

    /**
     *
     * @param dirPath directory
     * @param size resize size
     * @return ArrayList of all JPG images in a given directory (iscluding subdirectories) (resized)
     */
    public static ArrayList makeImageArray(Path dirPath, int size){
        ArrayList imageList = new ArrayList<>();

        try{
            Files.walk(dirPath).forEach(filePath -> {
                String fileString = filePath.toString();
                if (Files.isRegularFile(filePath) && (fileString.endsWith("jpg") || (fileString.endsWith("jpeg")))) {
                    File file = filePath.toFile();
                    try {
                        BufferedImage image = ImageIO.read(file);
                        image = resizeSquare(image, size);
                        imageList.add(image);
                    }
                    catch(Exception e){
                        e.printStackTrace();
                    }
                }
            });
        }
        catch(IOException e){
            e.printStackTrace();
        }
        return imageList;
    }

    /**
     * @param original color data, by which to sort
     * @param imagesArray images to be sorted
     * @return images organized by color
     */
    public static ArrayList<BufferedImage> createCompositeArray(ArrayList<Color> original, ArrayList<BufferedImage> imagesArray){
        ArrayList<BufferedImage> composite = new ArrayList<>(original.size());
        Color[] colorArray = getColorArray(imagesArray);

        for (Color c:original){
            BufferedImage closest = imagesArray.get(findClosestMatch(c, colorArray));
            composite.add(closest);
        }
        return composite;
    }

    /**
     *
     * @param list source images
     * @return average Color array for the images in "list"
     */
    public static Color[] getColorArray(ArrayList<BufferedImage> list){
        Color[] colorArray = new Color[list.size()];
        for (int i = 0; i < list.size(); i++){
            colorArray[i] = getAvColor(list.get(i));
        }
        return colorArray;
    }

    /**
     *
     * @param c color to match
     * @param list color list to pick from
     * @return
     */
    public static int findClosestMatch(Color c, Color[] list){
        double tempDistance;
        Color tempColor;
        double lowestDistance = 1000;
        int positionClosestMatch = 0;

        for (int i = 0; i < list.length; i++){
            tempColor = list[i];
            tempDistance = getColorDistance(c, tempColor);
            if (tempDistance < lowestDistance){
                lowestDistance = tempDistance;
                positionClosestMatch = i;
            }
        }
        return positionClosestMatch;
    }

    /**
     *
     * @param c1 color 1
     * @param c2 color 2
     * @return distance between colors
     */
    public static double getColorDistance(Color c1, Color c2) {
        double rmean = ( c1.getRed() + c2.getRed() )/2;
        int r = c1.getRed() - c2.getRed();
        int g = c1.getGreen() - c2.getGreen();
        int b = c1.getBlue() - c2.getBlue();
        double weightR = 2 + rmean/256;
        double weightG = 4.0;
        double weightB = 2 + (255-rmean)/256;

        return Math.sqrt(weightR*r*r + weightG*g*g + weightB*b*b);
    }
}
