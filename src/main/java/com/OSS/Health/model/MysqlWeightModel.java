package com.OSS.Health.model;

public class MysqlWeightModel {
	private String id; // 标识列
    private double weight;      // 数量列
    
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
}
