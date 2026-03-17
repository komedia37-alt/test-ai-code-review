package com.example.demo.service;

import com.alibaba.fastjson.JSON;
import com.example.demo.bean.Sms;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

@Service
public class SqsService {

    private Logger logger = LoggerFactory.getLogger(SqsService.class);

    private int sleepStep = 1;

    @Resource
    private SqsClient sqsClient;

    @Resource
    private Executor executor;

    private String queueUrl;

    @PostConstruct
    public void init() {
        //如果队列不存在，才会创建队列
        queueUrl =  createQueue("sms");
        //启动消费者监听器
        consumerListener();
    }
    /**
    　* @Description: 创建队列
    　* @author 
    　* @Date 2021/3/12
    　*/
    public String createQueue(String queueName) {
        CreateQueueRequest createQueueRequest = CreateQueueRequest.builder().queueName(queueName).build();
        sqsClient.createQueue(createQueueRequest);
        GetQueueUrlResponse getQueueUrlResponse = sqsClient.getQueueUrl(GetQueueUrlRequest.builder().queueName(queueName).build());
        return getQueueUrlResponse.queueUrl();
    }

    /**
    　* @Description: 发送消息 Sms类为自己的实体类
    　* @author
    　* @Date 2021/3/12
    　*/
    public void sendMessage(Sms sms) {
//        try {
            String json = JSON.toJSONString(sms);
            SendMessageRequest sendMessageRequest = SendMessageRequest.builder().queueUrl(queueUrl).messageBody(json).build();
            sqsClient.sendMessage(sendMessageRequest);
//        } catch (Exception e) {
//            logger.error("sendMessage error:{}",sms,e);
//        }
    }

    /**
    　* @Description: 接受消息
    　* @author 
    　* @Date 2021/3/12
    　*/
    public List<Message> receiveMessage() {
        try {
            ReceiveMessageRequest receiveMessageRequest = ReceiveMessageRequest.builder().queueUrl(queueUrl).maxNumberOfMessages(3).waitTimeSeconds(20).build();
            List<Message> messages = sqsClient.receiveMessage(receiveMessageRequest).messages();
            return messages;
        } catch (Exception e) {
            logger.error("receiveMessage error ",e);
            return new ArrayList();
        }
    }

    /**
    　* @Description: 删除消息
    　* @author
    　* @Date 2021/3/12
    　*/
    public boolean deleteMessages(List<Message> messages) {
        try {
            messages.forEach(message -> {
                DeleteMessageRequest deleteMessageRequest = DeleteMessageRequest.builder().queueUrl(queueUrl).receiptHandle(message.receiptHandle()).build();
                sqsClient.deleteMessage(deleteMessageRequest);
            });
            return true;
        } catch (Exception e) {
           logger.error("deleteMessages error ",e);
        }

        return false;
    }

    /**
    　* @Description: 消费者监听器
    　* @author 
    　* @Date 2021/3/9
    　*/
    public void consumerListener() {
        logger.info("start consumerListener");
        executor.execute(() -> {
            while (true) {
                try{
                    handleMessage();
                }catch (Exception e){
                    logger.error("Handle Message error ", e);
                }
            }
        });
    }

    //TODO 测试同一条消息, 处理失败后, 会被SQS重发多少次!
    //TODO 测试同一条消息, 处理失败后, 能否在massage中拿到它已被SQS重发了多少次!(后面可能根据它来自己做存储)
    private void handleMessage() {
        List<Message> messages = receiveMessage();
        if (messages == null || messages.size() == 0){
            //降频: 750ms, 1500ms, 3000s
            sleep(sleepStep * 750);
            if (sleepStep < 4){
                sleepStep = sleepStep * 2;
            }
            return;
        }

        sleepStep = 1;

        //消费
        List<Message> failedMessages = new ArrayList<>();
        for (Message message : messages) {
            try{
                //TODO 推送到印度MQ
                String body = message.body();
                Sms sms = JSON.parseObject(body, Sms.class);
                System.out.println(sms);

            }catch (Exception e){
                logger.error("Consuming Message error " + message, e);
                failedMessages.add(message);
            }
        }

        //先移除处理失败的消息
        if (failedMessages.size() != 0) {
            messages.removeAll(failedMessages);
        }


        //删除: 最大程度, 确保消息处理后能被成功删除!
        //fixme 但是都是有风险(消费完未删除,突然关机/停掉java服务)!
        for (int i = 0; i < 10; i++) {
            boolean deleted = deleteMessages(messages);
            if (deleted){
                break;
            }
        }
    }

    private void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}