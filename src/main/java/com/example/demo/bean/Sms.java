package com.example.demo.bean;

public class Sms {

    private String msg;

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    @Override
    public String toString() {
        return "Sms{" +
                "msg='" + msg + '\'' +
                '}';
    }
}
