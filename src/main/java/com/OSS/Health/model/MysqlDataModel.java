package com.OSS.Health.model;

import java.time.LocalDate;

public class MysqlDataModel {
	private String id; // 标识列
	private LocalDate time;  // 日期列
    private int number;      // 数量列
    private String s1; // 冗余字符串列
    
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public LocalDate getTime() {
        return time;
    }

    public void setTime(LocalDate time) {
        this.time = time;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }
    
    public String getS1() {
        return s1;
    }

    public void setS1(String s1) {
        this.s1 = s1;
    }
}
