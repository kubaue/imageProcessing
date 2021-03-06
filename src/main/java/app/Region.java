package app;

import java.util.ArrayList;
import java.util.List;

public class Region {

    // all inclusive
    private final ImageMap imageMap;
    private int xStart;
    private int xEnd;
    private int yStart;
    private int yEnd;

    public Region(ImageMap imageMap) {
        this.imageMap = imageMap;
        this.xStart = 0;
        this.xEnd = imageMap.width() - 1;
        this.yStart = 0;
        this.yEnd = imageMap.height() - 1;
    }

    public Region(ImageMap imageMap, int xStart, int xEnd, int yStart, int yEnd) {
        this.imageMap = imageMap;
        this.xStart = xStart;
        this.xEnd = xEnd;
        this.yStart = yStart;
        this.yEnd = yEnd;
    }

    public int size() {
        return (xEnd - xStart) * (yEnd - yStart);
    }

    public List<Region> split() {
        int xBreakingPoint = xStart + ((this.xEnd - this.xStart) / 2);
        int yBreakingPoint = yStart + ((this.yEnd - this.yStart) / 2);
        Region firstSquare = new Region(imageMap, xStart, xBreakingPoint, yStart, yBreakingPoint);
        Region secondSquare = new Region(imageMap, xBreakingPoint + 1, xEnd, yStart, yBreakingPoint);
        Region thirdSquare = new Region(imageMap, xStart, xBreakingPoint, yBreakingPoint + 1, yEnd);
        Region fourthSquare = new Region(imageMap, xBreakingPoint + 1, xEnd, yBreakingPoint + 1, yEnd);

        ArrayList<Region> output = new ArrayList<>();
        output.add(firstSquare);
        output.add(secondSquare);
        output.add(thirdSquare);
        output.add(fourthSquare);
        return output;
    }

    public boolean isHomogeneous(int threshold) {

        if(size() <= 4) {
            return true;
        }

        int currentMax = Integer.MIN_VALUE;
        int currentMin = Integer.MAX_VALUE;
        for (int x = 0; x <= xEnd; x++) {
            for (int y = 0; y <= yEnd; y++) {
                int currentValue = imageMap.getGray(x, y);
                currentMax = Math.max(currentMax, currentValue);
                currentMin = Math.min(currentMin, currentValue);
            }
        }

        return Math.abs(currentMax - currentMin) <= threshold;
    }

    public ImageMap map() {
        return this.imageMap;
    }

    public int xStart() {
        return xStart;
    }

    public int xEnd() {
        return xEnd;
    }

    public int yStart() {
        return yStart;
    }

    public int yEnd() {
        return yEnd;
    }
}
