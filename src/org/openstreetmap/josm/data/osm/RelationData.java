// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.ArrayList;
import java.util.List;

/**
 * GWT
 * 
 * changelog
 *  optimization for gwt-serialization:
 *    field 'members': List -> ArrayList
 */

public class RelationData extends PrimitiveData {

    public /* private */ ArrayList<RelationMemberData> members = new ArrayList<RelationMemberData>();

    public RelationData() {

    }

    public RelationData(RelationData data) {
        super(data);
        members.addAll(data.members);
    }

    public List<RelationMemberData> getMembers() {
        return members;
    }

    public void setMembers(List<RelationMemberData> memberData) {
        members = new ArrayList<RelationMemberData>(memberData);
    }

    @Override
    public RelationData makeCopy() {
        return new RelationData(this);
    }

    @Override
    public String toString() {
        return super.toString() + " REL " + members;
    }

    @Override
    public OsmPrimitiveType getType() {
        return OsmPrimitiveType.RELATION;
    }
}
