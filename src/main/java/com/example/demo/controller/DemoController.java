package com.example.demo.controller;

import com.example.demo.bean.Sms;
import com.example.demo.service.SqsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoController {


    @Autowired
    private SqsService sqsService;

    @GetMapping("/send/{msg}")
    public String createQueue(@PathVariable(value = "msg") String msg){
        Sms sms = new Sms();
        sms.setMsg(msg);
        sqsService.sendMessage(sms);
        return "OK";
    }

}
