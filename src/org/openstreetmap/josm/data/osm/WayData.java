// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.ArrayList;
import java.util.List;

/**
 * GWT
 * 
 * changelog
 *  made class gwt-serializable
 *   field 'nodes': private -> public
 *                  List -> ArrayList
 */

public class WayData extends PrimitiveData {

    public /* private */ ArrayList<Long> nodes = new ArrayList<Long>();

    public WayData() {

    }

    public WayData(WayData data) {
        super(data);
        nodes.addAll(data.getNodes());
    }

    public List<Long> getNodes() {
        return nodes;
    }

    public void setNodes(List<Long> nodes) {
        this.nodes = new ArrayList<Long>(nodes);
    }

    @Override
    public WayData makeCopy() {
        return new WayData(this);
    }

    @Override
    public String toString() {
        return super.toString() + " WAY" + nodes.toString();
    }

    @Override
    public OsmPrimitiveType getType() {
        return OsmPrimitiveType.WAY;
    }
}
