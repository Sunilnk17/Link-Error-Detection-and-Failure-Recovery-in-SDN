package net.floodlightcontroller.multipathrouting;


import org.openflow.util.HexString;
public class LinkWithCost{
    protected long src;
    protected short srcPort;
    protected long dst;
    protected short dstPort;
    protected int cost;

    public LinkWithCost(long srcDpid,short srcPort,long dstDpid,short dstPort,int cost){
        this.src = srcDpid;
        this.srcPort = srcPort;
        this.dst = dstDpid;
        this.dstPort = dstPort;
        this.cost = cost;
    }
    
    public long getSrcDpid(){
        return src;
    }

    public long getDstDpid(){
        return dst;
    }
    public short getSrcPort(){
        return srcPort;
    }
    public short getDstPort(){
        return dstPort;
    }
    public int getCost(){
        return cost;
    }

    public void setCost(int cost){
        this.cost = cost;
    }
    public String toString() {
        return "LinkWithCost [src=" + HexString.toHexString(this.src) 
                + " outPort="
                + (srcPort & 0xffff)
                + ", dst=" + HexString.toHexString(this.dst)
                + ", inPort="
                + (dstPort & 0xffff)
                + ", cost ="
                + cost
                + "]";
    }
    
    public int hashCode() {
        final int prime = 56;
        int result = 1;
        result = prime * result + (int) (dst ^ (dst >>> 32));
        result = prime * result + dstPort;
        result = prime * result + (int) (src ^ (src >>> 32));
        result = prime * result + srcPort;
        result = prime * result + cost;
        return result;
    }


    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        LinkWithCost other = (LinkWithCost)obj;
        if (dst != other.dst)
            return false;
        if (dstPort != other.dstPort)
            return false;
        if (src != other.src)
            return false;
        if (srcPort != other.srcPort)
            return false;
        if (cost  != other.cost)
            return false;
        return true;
    }


    public LinkWithCost getInverse(){
        return new LinkWithCost(dst,dstPort,src,srcPort,cost);
    }


}
