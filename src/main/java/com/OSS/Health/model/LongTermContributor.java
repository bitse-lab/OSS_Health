package com.OSS.Health.model;

import java.time.LocalDate;

public class LongTermContributor {
	private LocalDate time;  // 日期列
    private int number;      // 贡献者数量列

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
}
