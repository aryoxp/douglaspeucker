package ap.mobile.douglaspeuckerlib;

import android.graphics.Color;

import java.util.ArrayList;

/**
 * Created by aryo on 29/1/16.
 */
public class RouteBoxer {

    private final ArrayList<RouteBoxer.LatLng> route;
    private final int distance;
    private RouteBoxer.LatLngBounds bounds;
    private RouteBoxer.IRouteBoxer iRouteBoxer;
    private ArrayList<RouteBoxer.Box> boxes = new ArrayList<>();
    private ArrayList<RouteBoxer.Box> routeBoxesH;
    private ArrayList<RouteBoxer.Box> routeBoxesV;

    public RouteBoxer(ArrayList<RouteBoxer.LatLng> route, int distance) {
        this.route = route;
        this.distance = distance;
    }

    public void setRouteBoxerInterface(RouteBoxer.IRouteBoxer iRouteBoxer) {
        this.iRouteBoxer = iRouteBoxer;
    }

    public static ArrayList<RouteBoxer.Box> box(ArrayList<RouteBoxer.LatLng> path, int distance) {
        RouteBoxer routeBoxer = new RouteBoxer(path, distance);
        return routeBoxer.box();
    }

    public static ArrayList<RouteBoxer.Box> box(ArrayList<RouteBoxer.LatLng> path, int distance, RouteBoxer.IRouteBoxer iRouteBoxer) {
        RouteBoxer routeBoxer = new RouteBoxer(path, distance);
        routeBoxer.setRouteBoxerInterface(iRouteBoxer);
        return routeBoxer.box();
    }

    public ArrayList<RouteBoxer.Box> box() {

        if(this.iRouteBoxer != null)
            this.iRouteBoxer.onProcess("Initializing...", null);


        double degree = distance / 1.1132 * 0.00001;

        // Getting bounds

        if(this.iRouteBoxer != null)
            this.iRouteBoxer.onProcess("Calculating bounds...", null);

        this.bounds = new RouteBoxer.LatLngBounds();
        //LatLngBounds.Builder builder = LatLngBounds.builder();
        for (RouteBoxer.LatLng point : this.route) {
            this.bounds.include(point);
        }

        if(this.iRouteBoxer != null)
            this.iRouteBoxer.onProcess("Expanding bounds...", null);

        // Expanding bounds

        this.bounds.build();
        RouteBoxer.LatLng southwest = new RouteBoxer.LatLng(this.bounds.southwest.latitude - degree,
                this.bounds.southwest.longitude - degree);
        RouteBoxer.LatLng northeast = new RouteBoxer.LatLng(this.bounds.northeast.latitude + degree,
                this.bounds.northeast.longitude + degree);
        this.bounds = this.bounds.include(southwest).include(northeast).build();

        if(this.iRouteBoxer != null) {
            this.iRouteBoxer.onProcess("Bounds obtained...", null);
            this.iRouteBoxer.onBoundsObtained(this.bounds);
        }

        // Laying out grids

        if(this.iRouteBoxer != null)
            this.iRouteBoxer.onProcess("Overlaying grid...", null);

        RouteBoxer.LatLng sw = this.bounds.southwest;
        RouteBoxer.LatLng ne = new RouteBoxer.LatLng(sw.latitude + degree, sw.longitude + degree);
        int x = 0, y = 0;
        RouteBoxer.Box gridBox;

        do {

            do {
                gridBox = new RouteBoxer.Box(sw, ne, x, y);
                this.boxes.add(gridBox); //box.draw(mMap, Color.BLUE);
                sw = new RouteBoxer.LatLng(sw.latitude, ne.longitude);
                ne = new RouteBoxer.LatLng(sw.latitude + degree, sw.longitude + degree);
                x++;
            } while (gridBox.ne.longitude < this.bounds.northeast.longitude);

            if (gridBox.ne.latitude < this.bounds.northeast.latitude) {
                x = 0;
                sw = new RouteBoxer.LatLng(sw.latitude + degree, this.bounds.southwest.longitude);
                ne = new RouteBoxer.LatLng(sw.latitude + degree, sw.longitude + degree);
            }
            y++;

        } while (gridBox.ne.latitude < this.bounds.northeast.latitude);


        // Center the grids
        // and converts to 2-D array

        if(this.iRouteBoxer != null)
            this.iRouteBoxer.onProcess("Overlaying grid...", null);

        double latDif = boxes.get(boxes.size() - 1).ne.latitude - this.bounds.northeast.latitude;
        double lngDif = boxes.get(boxes.size() - 1).ne.longitude - this.bounds.northeast.longitude;

        RouteBoxer.Box boxArray[][] = new RouteBoxer.Box[x][y];
        for (RouteBoxer.Box bx : boxes) {
            bx.sw = new RouteBoxer.LatLng(bx.sw.latitude - (latDif / 2), bx.sw.longitude - (lngDif / 2));
            bx.ne = new RouteBoxer.LatLng(bx.ne.latitude - (latDif / 2), bx.ne.longitude - (lngDif / 2));
            bx.updateNWSE();
            boxArray[bx.x][bx.y] = bx;
        }

        if(this.iRouteBoxer != null) {
            this.iRouteBoxer.onGridObtained(boxArray, boxes);
            this.iRouteBoxer.onProcess("Traversing...", null);
        }

        // step 2: Traverse all points and mark grid which contains it.
        boxArray = this.traversePointsAndMarkGrids(boxArray);

        if(this.iRouteBoxer != null)
            this.iRouteBoxer.onProcess("Expanding cell marks...", null);

        // step 3: Expand marked cells
        boxArray = this.expandMarks(x, y, boxArray);

        if(this.iRouteBoxer != null)
            this.iRouteBoxer.onProcess("Duplicating array...", null);

        int length = boxArray.length;
        RouteBoxer.Box[][] boxArrayCopy = new RouteBoxer.Box[length][boxArray[0].length];
        for (int i = 0; i < length; i++) {
            System.arraycopy(boxArray[i], 0, boxArrayCopy[i], 0, boxArray[i].length);
        }

        if(this.iRouteBoxer != null)
            this.iRouteBoxer.onProcess("Merging horizontally...", null);
        // step 4: Merge cells and generate boxes
        // 1st Approach: merge cells horizontally
        this.horizontalMerge(x, y, boxArray);

        if(this.iRouteBoxer != null)
            this.iRouteBoxer.onProcess("Merging vertically...", null);
        // 2nd Approach: Merge cells vertically
        this.verticalMerge(x, y, boxArrayCopy);

        if(this.iRouteBoxer != null)
            this.iRouteBoxer.onProcess("Obtaining results...", null);
        // Step 5: return boxes with the least count from both approach
        ArrayList<RouteBoxer.Box> boxes = (this.routeBoxesV.size() >= routeBoxesH.size()) ? this.routeBoxesH : this.routeBoxesV;
        if(this.iRouteBoxer != null)
            this.iRouteBoxer.onBoxesObtained(boxes);

        return boxes;

    }

    private RouteBoxer.Box[][] traversePointsAndMarkGrids(RouteBoxer.Box[][] boxArray) {
        int sizeX = boxArray.length;
        int sizeY = boxArray[0].length;

        int i=0;
        LatLng origin = null, destination;
        ArrayList<Line> lines = new ArrayList<>();

        for (RouteBoxer.LatLng point : this.route) {

            Line l = null;
            double ay1 = 0, ay2 = 0, ax2 = 0, ax1 = 0;

            if (i == 0) {
                origin = point;
            } else {
                destination = point;
                // finding line bounding box
                l = new Line(origin, destination);
                lines.add(l);
                origin = destination;
            }

            if (l != null) {
                ay1 = Math.abs((l.origin.latitude > l.destination.latitude) ? l.origin.latitude : l.destination.latitude);
                ay2 = Math.abs((l.origin.latitude < l.destination.latitude) ? l.origin.latitude : l.destination.latitude);
                ax2 = Math.abs((l.origin.longitude > l.destination.longitude) ? l.origin.longitude : l.destination.longitude);
                ax1 = Math.abs((l.origin.longitude < l.destination.longitude) ? l.origin.longitude : l.destination.longitude);
            }

            i++;

            for (int x = 0; x < sizeX; x++) {
                for (int y = 0; y < sizeY; y++) {
                    RouteBoxer.Box bx = boxArray[x][y];
                    if (bx.marked) continue;
                    if (point.latitude > bx.sw.latitude
                            && point.latitude < bx.ne.latitude
                            && point.longitude > bx.sw.longitude
                            && point.longitude < bx.ne.longitude) {
                        bx.mark();
                        /* // previous algorithm to mark boxes in between marked boxes
                        if (lastBox == null)
                            lastBox = bx;
                        else {

                            int lastX = lastBox.x;
                            int lastY = lastBox.y;
                            int diffX = bx.x - lastX;
                            int diffY = bx.y - lastY;
                            if(diffX < 1) {
                                for(int dx = bx.x - diffX - 1; dx > bx.x; dx--)
                                    boxArray[dx][lastBox.y].mark();
                            }
                            if(diffX > 1) {
                                for(int dx = bx.x - diffX + 1; dx < bx.x; dx++)
                                    boxArray[dx][lastBox.y].mark();
                            }
                            if(diffY < 1){
                                for(int dy = bx.y - diffY - 1; dy > bx.y; dy--)
                                    boxArray[lastBox.x][dy].mark();
                            }
                            if(diffY > 1) {
                                for(int dy = bx.y - diffY + 1; dy < bx.y; dy++)
                                    boxArray[lastBox.x][dy].mark();
                            }

                            lastBox = bx;
                        }
                        */
                    }

                    // if a line exists
                    if (l != null) {
                        // find box bounds
                        double bx2 = Math.abs(bx.se.longitude);
                        double bx1 = Math.abs(bx.nw.longitude);
                        double by2 = Math.abs(bx.se.latitude);
                        double by1 = Math.abs(bx.nw.latitude);

                        // if box of line and grid box intersect
                        if (!(ax1 <= bx2 && ax2 >= bx1 && ay1 <= by2 && ay2 >= by1)) continue;

                        // if the line intersect with current box, mark it!
                        if (l.intersect(bx.sw, bx.ne)) bx.mark();
                    }
                }
            }
        }

        if(this.iRouteBoxer != null) {
            this.iRouteBoxer.onProcess("Traversing and marking complete...", null);
            this.iRouteBoxer.onGridMarked(this.boxes);
        }



        return boxArray;

    }

    private RouteBoxer.Box[][] expandMarks(int x, int y, RouteBoxer.Box[][] boxArray) {
        for(RouteBoxer.Box b:boxes) {
            if(b.marked) {

                // Mark all surrounding cells
                boxArray[b.x-1][b.y-1].expandMark();    //.redraw(mMap, Color.BLACK, Color.GREEN);
                boxArray[b.x - 1][b.y].expandMark();      //.redraw(mMap, Color.BLACK, Color.GREEN);
                boxArray[b.x - 1][b.y + 1].expandMark();    //.redraw(mMap, Color.BLACK, Color.GREEN);
                boxArray[b.x][b.y+1].expandMark();      //.redraw(mMap, Color.BLACK, Color.GREEN);
                boxArray[b.x+1][b.y+1].expandMark();    //.redraw(mMap, Color.BLACK, Color.GREEN);
                boxArray[b.x+1][b.y].expandMark();      //.redraw(mMap, Color.BLACK, Color.GREEN);
                boxArray[b.x + 1][b.y - 1].expandMark();    //.redraw(mMap, Color.BLACK, Color.GREEN);
                boxArray[b.x][b.y - 1].expandMark();      //.redraw(mMap, Color.BLACK, Color.GREEN);
            }
        }

        if(this.iRouteBoxer != null) {
            this.iRouteBoxer.onProcess("Cell marks expanded", null);
            this.iRouteBoxer.onGridMarksExpanded(boxArray, this.boxes);
        }

        return boxArray;
    }

    private void verticalMerge(int x, int y, RouteBoxer.Box[][] boxArray) {
        ArrayList<RouteBoxer.Box> mergedBoxes = new ArrayList<>();
        RouteBoxer.Box vBox = null;
        for(int cx = 0; cx < x; cx++) {
            for (int cy = 0; cy < y; cy++) {
                RouteBoxer.Box b = new RouteBoxer.Box(boxArray[cx][cy].sw, boxArray[cx][cy].ne, cx, cy);
                if(boxArray[cx][cy].marked || boxArray[cx][cy].expandMarked)
                    b.mark();
                if ((b.marked || b.expandMarked)) {
                    if (vBox == null)
                        vBox = b;
                    else vBox.ne = b.ne;
                    if(cy == y-1) {
                        vBox.unexpandMark().unmark();
                        mergedBoxes.add(vBox);
                        vBox = null;
                    }
                } else {
                    if(vBox != null) {
                        vBox.unexpandMark().unmark();
                        mergedBoxes.add(vBox);
                        vBox = null;
                    }
                }
            }
        }

        if(this.iRouteBoxer != null) {
            this.iRouteBoxer.onProcess("Adjoint cells merged.", null);
            this.iRouteBoxer.onMergedAdjointVertically(mergedBoxes);
        }



        this.routeBoxesV = new ArrayList<>();
        RouteBoxer.Box rBox = null;
        for(int i = 0; i < mergedBoxes.size(); i++) {
            RouteBoxer.Box bx = mergedBoxes.get(i);
            if(bx.merged)
                continue;
            rBox = bx;
            for (int j = i + 1; j < mergedBoxes.size(); j++) {
                RouteBoxer.Box b = mergedBoxes.get(j);
                if (b.sw.latitude == rBox.sw.latitude
                        && b.ne.latitude == rBox.ne.latitude
                        && b.sw.longitude == rBox.ne.longitude) {
                    rBox.ne = b.ne;
                    b.merged = true;
                }
            }
            routeBoxesV.add(rBox);

        }

        if(this.iRouteBoxer != null) {
            this.iRouteBoxer.onProcess("Adjoint boxes merged.", null);
            this.iRouteBoxer.onMergedVertically(routeBoxesV);
        }
    }

    private void horizontalMerge(int x, int y, RouteBoxer.Box[][] boxArray) {
        ArrayList<RouteBoxer.Box> mergedBoxes = new ArrayList<>();
        RouteBoxer.Box hBox = null;
        for(int cy = 0; cy < y; cy++) {
            for (int cx = 0; cx < x; cx++) {
                RouteBoxer.Box b = new RouteBoxer.Box(boxArray[cx][cy].sw, boxArray[cx][cy].ne, cx, cy);
                if(boxArray[cx][cy].marked || boxArray[cx][cy].expandMarked)
                    b.mark();
                if ((b.marked || b.expandMarked)) {
                    if (hBox == null)
                        hBox = b;
                    else hBox.ne = b.ne;
                    if(cx == x-1) {
                        hBox.unexpandMark().unmark();
                        mergedBoxes.add(hBox);
                        hBox = null;
                    }
                } else {
                    if(hBox != null) {
                        hBox.unexpandMark().unmark();
                        mergedBoxes.add(hBox);
                        hBox = null;
                    }
                }
            }
        }

        if(this.iRouteBoxer != null) {
            this.iRouteBoxer.onProcess("Adjoint cells merged.", null);
            this.iRouteBoxer.onMergedAdjointHorizontally(mergedBoxes);
        }

        this.routeBoxesH = new ArrayList<>();
        RouteBoxer.Box rBox = null;
        for(int i = 0; i < mergedBoxes.size(); i++) {
            RouteBoxer.Box bx = mergedBoxes.get(i);
            if(bx.merged)
                continue;
            rBox = bx;
            for (int j = i + 1; j < mergedBoxes.size(); j++) {
                RouteBoxer.Box b = mergedBoxes.get(j);
                if (b.sw.longitude == rBox.sw.longitude
                        && b.sw.latitude == rBox.ne.latitude
                        && b.ne.longitude == rBox.ne.longitude) {
                    rBox.ne = b.ne;
                    b.merged = true;
                }
            }
            routeBoxesH.add(rBox);
        }

        if (this.iRouteBoxer != null) {
            this.iRouteBoxer.onProcess("Adjoint boxes merged.", null);
            this.iRouteBoxer.onMergedHorizontally(routeBoxesH);
        }
    }

    public ArrayList<RouteBoxer.Box> getRouteBoxesH() {
        return this.routeBoxesH;
    }
    public ArrayList<RouteBoxer.Box> getRouteBoxesV() {
        return this.routeBoxesV;
    }

    /**
     * Created by aryo on 30/1/16.
     */
    public static interface IRouteBoxer {

        void onBoxesObtained(ArrayList<RouteBoxer.Box> boxes);
        void onBoundsObtained(RouteBoxer.LatLngBounds bounds);
        void onGridObtained(RouteBoxer.Box[][] boxArray, ArrayList<RouteBoxer.Box> boxes);
        void onGridMarked(ArrayList<RouteBoxer.Box> boxes);
        void onGridMarksExpanded(RouteBoxer.Box[][] boxArray, ArrayList<RouteBoxer.Box> boxes);
        void onMergedAdjointVertically(ArrayList<RouteBoxer.Box> boxes);
        void onMergedAdjointHorizontally(ArrayList<RouteBoxer.Box> boxes);
        void onMergedVertically(ArrayList<RouteBoxer.Box> mergedBoxes);
        void onMergedHorizontally(ArrayList<RouteBoxer.Box> mergedBoxes);
        void onProcess(String processInfo, ArrayList<RouteBoxer.Box> boxes);
        void drawLine(LatLng origin, LatLng destination, int color);
        void drawBox(LatLng origin, LatLng destination, int yellow);
        void clearPolygon();

    }

    public class Box {

        public int x;
        public int y;

        public Boolean marked = false;
        public Boolean expandMarked = false;
        public Boolean merged = false;

        public RouteBoxer.LatLng ne;
        public RouteBoxer.LatLng sw;
        public RouteBoxer.LatLng nw;
        public RouteBoxer.LatLng se;

        public Box() {}

        public Box(RouteBoxer.LatLng sw, RouteBoxer.LatLng ne, int x, int y) {
            this.sw = sw;
            this.ne = ne;
            this.x = x;
            this.y = y;
            updateNWSE();
        }

        public void updateNWSE() {
            this.nw = new RouteBoxer.LatLng(this.ne.latitude, this.sw.longitude);
            this.se = new RouteBoxer.LatLng(this.sw.latitude, this.ne.longitude);
        }

        public RouteBoxer.Box mark() { this.marked = true; return this; }
        public RouteBoxer.Box unmark() { this.marked = false; return this; }
        public RouteBoxer.Box expandMark() { this.expandMarked = true; return this; }
        public RouteBoxer.Box unexpandMark() { this.expandMarked = false; return this; }

        public RouteBoxer.Box copy(RouteBoxer.Box box) {
            RouteBoxer.Box b = new RouteBoxer.Box();
            b.x = box.x;
            b.y = box.y;
            b.marked = box.marked;
            b.expandMarked = box.expandMarked;
            b.merged = box.merged;
            b.ne = new RouteBoxer.LatLng(box.ne.latitude, box.ne.longitude);
            b.sw = new RouteBoxer.LatLng(box.sw.latitude, box.sw.latitude);
            return b;
        }

    }

    public static class LatLng {
        public double latitude;
        public double longitude;
        public LatLng(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }

    public class LatLngBounds {

        private ArrayList<RouteBoxer.LatLng> latLngs = new ArrayList<>();

        public RouteBoxer.LatLng southwest;
        public RouteBoxer.LatLng northeast;

        public RouteBoxer.LatLngBounds include(RouteBoxer.LatLng latLng) {
            this.latLngs.add(latLng);
            return this;
        }

        public RouteBoxer.LatLngBounds build() {
            this.southwest = null;
            this.northeast = null;
            double maxLat = 0;
            double maxLng = 0;
            double minLat = 0;
            double minLng = 0;
            for ( RouteBoxer.LatLng latLng:
                    this.latLngs) {

                if(maxLat == 0 && maxLng == 0 && minLat == 0 && minLng == 0) {
                    maxLat = minLat = latLng.latitude;
                    maxLng = minLng = latLng.longitude;
                    continue;
                }

                if(latLng.latitude > maxLat) maxLat = latLng.latitude;
                if(latLng.longitude > maxLng) maxLng = latLng.longitude;
                if(latLng.latitude < minLat) minLat = latLng.latitude;
                if(latLng.longitude < minLng) minLng = latLng.longitude;
            }

            this.southwest = new RouteBoxer.LatLng(minLat, minLng);
            this.northeast = new RouteBoxer.LatLng(maxLat, maxLng);

            return this;
        }
    }

    public class Line
    {

        private LatLng origin;
        private LatLng destination;
        public double m, c; // y = mx + c;

        public Line (LatLng origin, LatLng destination) {

            this.origin = origin;
            this.destination = destination;
            this.m = (destination.latitude - origin.latitude) / (destination.longitude - origin.longitude);
            this.c = origin.latitude - (this.m * origin.longitude);

        }

        public double getY(double x) {
            return this.m * x + c;
        }
        // y = mx + c;
        public double getX(double y) {
            return (y - c) / m;
        }

        public boolean intersect(LatLng to, LatLng td) {

            double y = this.getY(to.longitude);
            if(y <= td.latitude && y >= to.latitude) return true;

            y = this.getY(td.longitude);
            if(y <= td.latitude && y >= to.latitude) return true;

            double x = this.getX(td.latitude);
            if(x >= to.longitude && x <= td.longitude) return true;

            x = this.getX(to.latitude);
            if(x >= to.longitude && x <= td.longitude) return true;

            return false;

        }

    }

}
