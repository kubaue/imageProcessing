package app;

import javafx.scene.image.Image;

import java.util.Map;

public class ImageOperations {

    private ImageConverter imageConverter = new ImageConverter();

    public Image stretch(Image image) {
        Map<Integer, Map<Integer, Canals>> imageMap = imageConverter.toCanals(image);

        return imageConverter.toImage(imageMap);
    }

    public Image negate(Image image) {
        Map<Integer, Map<Integer, Canals>> imageMap = imageConverter.toCanals(image);

        return imageConverter.toImage(imageMap);
    }
}