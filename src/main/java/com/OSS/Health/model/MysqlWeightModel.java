package com.OSS.Health.model;

public class MysqlWeightModel {
	private String id; // 标识列
    private double weight;      // 数量列
    private double A; //前25%的当前值
    private double B; //前50%的当前值
    private double C; //前75%的当前值
    
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }
    
    public double getA() {
        return A;
    }

    public void setA(double A) {
        this.A = A;
    }
    
    public double getB() {
        return B;
    }

    public void setB(double B) {
        this.B = B;
    }
    
    public double getC() {
        return C;
    }

    public void setC(double C) {
        this.C = C;
    }
}
