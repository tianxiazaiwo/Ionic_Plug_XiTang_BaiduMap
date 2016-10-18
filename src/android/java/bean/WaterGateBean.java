package com.sdr.sluicemapplugin.bean;

import java.io.Serializable;
import java.util.List;

/**
 * Created by HeYongFeng on 2016/10/14.
 */
public class WaterGateBean implements Serializable{

    /**
     * IsMain : 0
     * LGTD : 120.89881
     * LTTD : 30.97678
     * SGSCD : DGYSZ
     * Tm : 1476288000000
     * UPZ : 0
     * address : 测试地址3
     * autoType : 监视
     * gateList : [{"id":3,"name":"东耕圩水闸","sgpcd":"zDGYSZ_A","sgscd":"DGYSZ","status":1,"statusTm":1476166236000}]
     * id : 3
     * name : 东耕圩水闸
     * type : 水闸
     * videoCount : 1
     * wLInsuring : 1.1
     * wLPoints : 0
     * wLWarning : 2.2
     */

    private int IsMain;
    private double LGTD;
    private double LTTD;
    private String SGSCD;
    private long Tm;
    private double UPZ;
    private String address;
    private String autoType;
    private int id;
    private String name;
    private String type;
    private int videoCount;
    private double wLInsuring;
    private int wLPoints;
    private double wLWarning;
    /**
     * id : 3
     * name : 东耕圩水闸
     * sgpcd : zDGYSZ_A
     * sgscd : DGYSZ
     * status : 1
     * statusTm : 1476166236000
     */

    private List<GateListBean> gateList;

    public int getIsMain() {
        return IsMain;
    }

    public void setIsMain(int IsMain) {
        this.IsMain = IsMain;
    }

    public double getLGTD() {
        return LGTD;
    }

    public void setLGTD(double LGTD) {
        this.LGTD = LGTD;
    }

    public double getLTTD() {
        return LTTD;
    }

    public void setLTTD(double LTTD) {
        this.LTTD = LTTD;
    }

    public String getSGSCD() {
        return SGSCD;
    }

    public void setSGSCD(String SGSCD) {
        this.SGSCD = SGSCD;
    }

    public long getTm() {
        return Tm;
    }

    public void setTm(long Tm) {
        this.Tm = Tm;
    }

    public double getUPZ() {
        return UPZ;
    }

    public void setUPZ(double UPZ) {
        this.UPZ = UPZ;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getAutoType() {
        return autoType;
    }

    public void setAutoType(String autoType) {
        this.autoType = autoType;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getVideoCount() {
        return videoCount;
    }

    public void setVideoCount(int videoCount) {
        this.videoCount = videoCount;
    }

    public double getWLInsuring() {
        return wLInsuring;
    }

    public void setWLInsuring(double wLInsuring) {
        this.wLInsuring = wLInsuring;
    }

    public int getWLPoints() {
        return wLPoints;
    }

    public void setWLPoints(int wLPoints) {
        this.wLPoints = wLPoints;
    }

    public double getWLWarning() {
        return wLWarning;
    }

    public void setWLWarning(double wLWarning) {
        this.wLWarning = wLWarning;
    }

    public List<GateListBean> getGateList() {
        return gateList;
    }

    public void setGateList(List<GateListBean> gateList) {
        this.gateList = gateList;
    }
    public static class GateListBean {
        private int id;
        private String name;
        private String sgpcd;
        private String sgscd;
        private int status;
        private long statusTm;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getSgpcd() {
            return sgpcd;
        }

        public void setSgpcd(String sgpcd) {
            this.sgpcd = sgpcd;
        }

        public String getSgscd() {
            return sgscd;
        }

        public void setSgscd(String sgscd) {
            this.sgscd = sgscd;
        }

        public int getStatus() {
            return status;
        }

        public void setStatus(int status) {
            this.status = status;
        }

        public long getStatusTm() {
            return statusTm;
        }

        public void setStatusTm(long statusTm) {
            this.statusTm = statusTm;
        }
        @Override
        public String toString() {
            return "GateListBean{" +
                    "id=" + id +
                    ", name='" + name + '\'' +
                    ", sgpcd='" + sgpcd + '\'' +
                    ", sgscd='" + sgscd + '\'' +
                    ", status=" + status +
                    ", statusTm=" + statusTm +
                    '}';
        }
    }
}
