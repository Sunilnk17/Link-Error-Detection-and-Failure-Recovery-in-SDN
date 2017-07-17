package net.floodlightcontroller.multipathrouting;

import org.openflow.util.HexString;

public class FlowId implements Cloneable, Comparable<FlowId> {
    protected Long src;
    protected Long dst;
    protected short srcPort;
    protected short dstPort;

    public FlowId(Long src, short srcPort,Long dst,short dstPort) {

        super();
        this.src = src;
        this.dst = dst;
        this.srcPort = srcPort;
        this.dstPort = dstPort;
    }

    public Long getSrc() {
        return src;
    }

    public void setSrc(Long src) {
        this.src = src;
    }

    public Long getDst() {
        return dst;
    }

    public void setDst(Long dst) {
        this.dst = dst;
    }

    public short getSrcPort(){
        return srcPort;
    }
    public void setSrcPort(short port){
        this.srcPort = port;
    }
    public short getDstPort(){
        return dstPort;
    }
    public void setDstPort(short port){
        this.dstPort = port;
    }

    @Override
    public int hashCode() {
        final int prime = 2417;
        Long result = new Long(1);
        result = prime * result + ((dst == null) ? 0 : dst.hashCode());
        result = prime * result + ((src == null) ? 0 : src.hashCode());
        result = prime * result + srcPort;
        result = prime * result + dstPort;
        return result.hashCode(); 
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        FlowId other = (FlowId) obj;
        if (dst == null) {
            if (other.dst != null)
                return false;
        } else if (!dst.equals(other.dst))
            return false;
        if (src == null) {
            if (other.src != null)
                return false;
        } else if (!src.equals(other.src))
            return false;
        if (srcPort != other.srcPort)
            return false;
        if (dstPort != other.dstPort)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "FlowId [src=" + HexString.toHexString(this.src) + " , port = "+Short.toString(srcPort)+" dst="
                + HexString.toHexString(this.dst) + " , port = "+Short.toString(dstPort)+" ]";
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    @Override
    public int compareTo(FlowId o) {
        int result = src.compareTo(o.getSrc());
        if (result != 0)
            return result;
        result = dst.compareTo(o.getDst());
        if (result != 0)
            return result;
        if ( srcPort == o.getSrcPort())
            return dstPort - o.getDstPort();
        return srcPort - o.getSrcPort();
    }
}
