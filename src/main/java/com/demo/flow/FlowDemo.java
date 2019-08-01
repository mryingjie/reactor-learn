package com.demo.flow;

import org.junit.Test;

import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;

/**
 * @Author ZhengYingjie
 * @Date 2019-07-15
 * @Description
 */
public class FlowDemo {

    class StringSubscriber implements  Flow.Subscriber<String>{

        private final String name;

        public StringSubscriber(String name){
            this.name = name;
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            System.out.println("["+name+"]"+"开始订阅");
            //向服务端反向请求
            subscription.request(1);
        }

        @Override
        public void onNext(String item) {
            System.out.println("["+name+"] item:"+item);
        }

        @Override
        public void onError(Throwable throwable) {

        }

        @Override
        public void onComplete() {
            System.out.println("completed !!");
        }
    }


    @Test
    public void testFlow(){

        SubmissionPublisher<String> publisher = new SubmissionPublisher<>();
        try(publisher) {
            publisher.subscribe(new StringSubscriber("A"));
            publisher.subscribe(new StringSubscriber("B"));
            publisher.subscribe(new StringSubscriber("C"));

            publisher.submit("Hellow World1");
            publisher.submit("Hellow World3");
        }

        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }




}
