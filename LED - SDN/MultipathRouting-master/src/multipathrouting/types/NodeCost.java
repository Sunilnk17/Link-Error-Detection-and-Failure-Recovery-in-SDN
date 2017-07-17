package net.floodlightcontroller.multipathrouting;

import org.openflow.util.HexString;
public class NodeCost implements Comparable<NodeCost> {
    private final String nodeStr;
    private final Long node;
    private final int cost;

    public String getDpidStr() {
        return nodeStr;
    }
    public Long getDpid(){
        return node;
    }
    public int getCost() {
        return cost;
    }

    public NodeCost(Long node, int cost) {
        this.node = node;
        this.nodeStr = HexString.toHexString(node);
        this.cost = cost;
    }

    @Override
    public int compareTo(NodeCost o) {
        if (o.cost == this.cost) {
            return (int)(this.node - o.node);
        }
        return this.cost - o.cost;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        NodeCost other = (NodeCost) obj;
        if (node == null) {
            if (other.node != null)
                return false;
        } else if (!node.equals(other.node))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        assert false : "hashCode not designed";
        return 42;
    }

}

