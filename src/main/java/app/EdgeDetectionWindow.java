package app;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.Arrays;
import java.util.List;

import static app.LinearFilters.*;
import static app.MatScalingUtils.*;

/**
 * Klasa reprezentująca okno wykrywania krawędzi w obrazie.
 */
public class EdgeDetectionWindow {

    /**
     * Wartości wskazujące rodzaj opracji na pikselach brzegowych.
     */
    private static final int BORDER_MINIMUM = 254;
    private static final int BORDER_MAXIMUM = 255;

    /**
     * Wysokość panelu opcji.
     */
    private static final int OPTIONS_HEIGHT = 190;

    /**
     * Minimalna szerokość okna.
     */
    private static final int MINIMAL_WIDTH = 550;
    private final App app;

    /**
     * Podgląd obrazu przed i po operacji.
     */
    private ImageView beforeImageView;
    private ImageView afterImageView;

    private BarChart<String, Number> beforeImageHistogram;
    private BarChart<String, Number> afterImageHistogram;
    private VBox afterImageViewHbox;

    /**
     * Elementy okna.
     */
    private Stage stage;
    private VBox vBox;
    private HBox hBox;
    /**
     * Obrazy przed i po operacji.
     */
    private Image before;
    private Image after;

    /**
     * Wartość zwielokrotnienia operacji.
     */
    private double times;

    /**
     * List dostępnych masek
     */
    private List<Mask3x3> masks;

    /**
     * Aktualnie wybrana przez użytkownika maska.
     */
    private Mask3x3 currentMask;

    /**
     * Aktualnie wybrana przez użytkownika operacja na pikselach brzegowych.
     */
    private int currentBorderType;

    /**
     * Wartość pikseli brzegowych (jeśli wybrana stała wartość)
     */
    private Scalar border;

    /**
     * Aktualnie wybrany przez użytkownika sposób skalowania.
     */
    private byte currentScalingMethod;


    public EdgeDetectionWindow(Image image, App app) {
        this.app = app;
        before = image;
        masks = Arrays.asList(EDGE_DETECTION_1, EDGE_DETECTION_2, EDGE_DETECTION_3, EDGE_DETECTION_4);

        ToggleGroup options = new ToggleGroup();
        RadioButton mask1 = createMaskRadioButton(options, EDGE_DETECTION_1);
        RadioButton mask2 = createMaskRadioButton(options, EDGE_DETECTION_2);
        RadioButton mask3 = createMaskRadioButton(options, EDGE_DETECTION_3);
        RadioButton mask4 = createMaskRadioButton(options, EDGE_DETECTION_4);
        mask1.setSelected(true);
        handleOptionChanges(options);

        currentMask = EDGE_DETECTION_1;
        currentBorderType = Core.BORDER_CONSTANT;
        currentScalingMethod = METHOD_3;
        times = 1;

        createBeforeImageView();
        createAfterImageView();

        this.beforeImageHistogram = buildHistogram(beforeImageView.getImage());
        this.afterImageHistogram = buildHistogram(afterImageView.getImage());

        VBox beforeImageViewHbox = new VBox(beforeImageView, beforeImageHistogram);
        beforeImageViewHbox.setAlignment(Pos.CENTER);
        VBox afterImageViewHbox = new VBox(afterImageView, afterImageHistogram);
        this.afterImageViewHbox = afterImageViewHbox;
        afterImageViewHbox.setAlignment(Pos.CENTER);
        hBox = new HBox(beforeImageViewHbox, afterImageViewHbox);
        hBox.setAlignment(Pos.CENTER);

        Button save = new Button("Kontynuuj");
        save.setOnAction(event -> {
            app.updateImage(after);
            stage.close();
        });

        HBox buttonsHbox = new HBox(save);
        buttonsHbox.setSpacing(15);
        buttonsHbox.setAlignment(Pos.CENTER);
        VBox buttonsTimesVbox = new VBox(buttonsHbox);
        buttonsTimesVbox.setAlignment(Pos.CENTER);
        buttonsTimesVbox.setSpacing(15);

        VBox borderVBox = createBorderOptions();
        VBox scalingVBox = createScalingOptions();

        HBox masksHBox = new HBox(mask1, mask2, mask3, mask4);
        masksHBox.setSpacing(15);
        masksHBox.setPrefHeight(60);
        VBox radioHBox = new VBox(borderVBox, scalingVBox);
        masksHBox.setAlignment(Pos.CENTER);
        radioHBox.setSpacing(15);

        VBox buttons = new VBox(new VBox(masksHBox, radioHBox), buttonsTimesVbox);
        buttons.setPadding(new Insets(13, 10, 10, 0));
        buttons.setSpacing(15);
        buttons.setAlignment(Pos.CENTER);
        vBox = new VBox(hBox, buttons);

        Scene scene = createScene(beforeImageViewHbox, afterImageViewHbox);

        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);

        stage.setScene(scene);
        stage.setTitle("Detekcja krawędzi");
        save.requestFocus();
        stage.showAndWait();
    }


    /**
     * Oblicza wielkość okna na podstawie wielkości obrazów i tworzy układ okna.
     *
     * @param beforeImageViewHbox obszar z podglądem obrazu przed zmianami
     * @param afterImageViewHbox  obszar z podglądem obrazu po zmianach
     * @return <tt>Scene</tt> z układem okna
     */
    private Scene createScene(VBox beforeImageViewHbox, VBox afterImageViewHbox) {
        double windowWidth = Math.max(MINIMAL_WIDTH, afterImageView.getBoundsInLocal().getWidth() * 2);
        Scene scene = new Scene(vBox, windowWidth, 900);
        scene.setOnKeyPressed(event -> {
            if (KeyCode.ESCAPE.equals(event.getCode())) stage.close();
        });
        beforeImageViewHbox.setPrefWidth(windowWidth / 2);
        afterImageViewHbox.setPrefWidth(windowWidth / 2);
        return scene;
    }

    /**
     * Tworzy obszar z opcjami dotyczącymi pikseli brzegowych.
     *
     * @return obszar z opcjami dotyczącymi pikseli brzegowych.
     */
    private VBox createBorderOptions() {
        ToggleGroup borderTypeGroup = new ToggleGroup();
        Label borderTypeLabel = new Label("Piksele brzegowe:");

        RadioButton replicatedBorder = new RadioButton("Bez zmian");
        replicatedBorder.setUserData(Core.BORDER_CONSTANT);
        replicatedBorder.setToggleGroup(borderTypeGroup);
        replicatedBorder.setSelected(true);

        RadioButton reflectedBorder = new RadioButton("Powielenie pikseli brzegowych");
        reflectedBorder.setUserData(Core.BORDER_REPLICATE);
        reflectedBorder.setToggleGroup(borderTypeGroup);

        RadioButton existingBorder = new RadioButton("Istniejące sąsiedztwo");
        existingBorder.setUserData(Core.BORDER_DEFAULT);
        existingBorder.setToggleGroup(borderTypeGroup);

        borderTypeGroup.selectedToggleProperty().addListener((observable, oldValue, newValue)
                -> handleBorderOptionChange(newValue));

        VBox vBox = new VBox(borderTypeLabel, replicatedBorder, reflectedBorder, existingBorder);
        vBox.setAlignment(Pos.CENTER);
        return vBox;
    }

    /**
     * Obsługuję zmianę opcji dotyczących pikseli brzegowych.
     *
     * @param newValue wybrana opcja
     */
    private void handleBorderOptionChange(Toggle newValue) {
        int selected = (int) newValue.getUserData();
        if (selected == BORDER_MINIMUM) {
            currentBorderType = Core.BORDER_CONSTANT;
            border = new Scalar(0, 0, 0, 255);
        } else if (selected == BORDER_MAXIMUM) {
            currentBorderType = Core.BORDER_CONSTANT;
            border = new Scalar(255, 255, 255, 255);
        } else {
            currentBorderType = selected;
            border = null;
        }

        reloadPreview();
    }

    /**
     * Tworzy obszar z opcjami dotyczącymi skalowania obrazu wynikowego.
     *
     * @return obszar z opcjami dotyczącymi skalowania obrazu wynikowego.
     */
    private VBox createScalingOptions() {
        ToggleGroup scalingTypeGroup = new ToggleGroup();
        Label borderTypeLabel = new Label("Skalowanie:");

        RadioButton method1 = new RadioButton("Równomierne");
        method1.setUserData(METHOD_1);
        method1.setToggleGroup(scalingTypeGroup);

        RadioButton method2 = new RadioButton("Trójwartościowe");
        method2.setUserData(METHOD_2);
        method2.setToggleGroup(scalingTypeGroup);

        RadioButton method3 = new RadioButton("Obcinające");
        method3.setUserData(METHOD_3);
        method3.setToggleGroup(scalingTypeGroup);
        method3.setSelected(true);

        scalingTypeGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            currentScalingMethod = (byte) newValue.getUserData();
            reloadPreview();
        });

        VBox vBox = new VBox(borderTypeLabel, method3, method1, method2);
        vBox.setAlignment(Pos.CENTER);
        return vBox;
    }

    /**
     * Obsługuje zmianę dowolnej opcji przez użytkownika (ustawia maskę i odświeża podgląd)
     *
     * @param options
     */
    private void handleOptionChanges(ToggleGroup options) {
        options.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                changeCurrentMask(newValue);
            }
        });
    }

    /**
     * Zmienia maskę na wybraną przez użytkownika i odświeża podgląd.
     *
     * @param newValue nazwa wybranej przez użytkownika maski
     */
    private void changeCurrentMask(Toggle newValue) {
        String maskName = newValue.getUserData().toString();

        masks.stream()
                .filter(mask -> maskName.equals(mask.getName()))
                .findFirst()
                .ifPresent(this::setAsCurrentMask);

        reloadPreview();
    }

    /**
     * Ustawia maskę jako aktualnie wybraną.
     *
     * @param mask3x3 maska do ustawienia
     */
    private void setAsCurrentMask(Mask3x3 mask3x3) {
        currentMask = mask3x3;
    }

    /**
     * Tworzy podgląd obrazu po operacji.
     */
    private void createAfterImageView() {
        after = applyMask(EDGE_DETECTION_1);
        afterImageView = new ImageView(after);
        afterImageView.setPreserveRatio(true);
        afterImageView.setFitWidth(400);
        afterImageView.setFitHeight(400);
    }

    /**
     * Tworzy RadioButton dla wybranej maski i przypisuje go do podanego ToggleGroup
     *
     * @param options do przypisania utworzonego RadioButton.
     * @param mask    maska dla której utworzony zostanie RadioButton
     * @return <tt>RadioButton</tt> dla maski
     */
    private RadioButton createMaskRadioButton(ToggleGroup options, Mask3x3 mask) {
        RadioButton maskButton = new RadioButton(mask.toString());
        maskButton.setUserData(mask.getName());
        maskButton.setToggleGroup(options);
        maskButton.setPrefHeight(50);
        return maskButton;
    }

    /**
     * Tworzy podgląd obrazu przed operacją.
     */
    private void createBeforeImageView() {
        beforeImageView = new ImageView((before));
        beforeImageView.setPreserveRatio(true);
        beforeImageView.setFitWidth(400);
        beforeImageView.setFitHeight(400);
    }

    /**
     * Stosuje wybraną maskę i odświeża podgląd.
     */
    private void reloadPreview() {
        after = applyMask(currentMask);
        afterImageView.setImage(after);

        this.afterImageHistogram = buildHistogram(after);
        afterImageViewHbox.getChildren().set(1, this.afterImageHistogram);
    }

    /**
     * Przeprowadza operację filtrowania wybraną maską. Jeśli suma wartości w masce
     * jest równa 0, maska zostaje przekonwertowana na skalę szarości.
     * Po operacji przeprowadzane jest skalowanie zgodnie z opcją wybraną przez
     * użytkownika.
     *
     * @param mask maska do operacji
     * @return obraz wynikowy.
     */
    private Image applyMask(Mask3x3 mask) {
        Mat image = ImageUtils.imageToMat(before);

        if (mask.getKernelSize() == 1 && currentScalingMethod == METHOD_3) {
            applyMask(mask, image);
        } else {
            applyMaskWithColorConversion(mask, image);
        }

        MatScalingUtils.scale(image, currentScalingMethod);
        return ImageUtils.mat2Image(image);
    }

    /**
     * Przeprowadza operację filtrowania wybraną maską na podanym obiekcie Mat.
     *
     * @param mask  maska do operacji
     * @param image obiekt Mat z obrazem
     */
    private void applyMask(Mask3x3 mask, Mat image) {
        for (int i = 0; i < times; i++) {
            FilteringUtils.applyMaskWithBlur(image, mask, currentBorderType, border);
        }
    }

    /**
     * Konwertuje obraz na szaroodcieniowy i przeprowadza operację filtrowania
     * wybraną maską na podanym obiekcie Mat.
     *
     * @param mask  maska do operacji
     * @param image obiekt Mat z obrazem
     */
    private void applyMaskWithColorConversion(Mask3x3 mask, Mat image) {
        Imgproc.cvtColor(image, image, Imgproc.COLOR_RGB2GRAY);

        for (int i = 0; i < times; i++) {
            FilteringUtils.applyMaskWithBlur(image, mask, currentBorderType, border);
        }
    }

    private BarChart<String, Number> buildHistogram(Image image) {
        HistogramPainter histogramPainter = new HistogramPainter(image);
        BarChart<String, Number> histogram = histogramPainter.paintChart();

        histogram.setMaxWidth(150);
        histogram.setMaxHeight(150);

        histogram.getData().addAll(
                histogramPainter.getSeriesRed(),
                histogramPainter.getSeriesGreen(),
                histogramPainter.getSeriesBlue());

        return histogram;
    }

}
