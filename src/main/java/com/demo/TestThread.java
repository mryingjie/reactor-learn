package com.demo;

/**
 * @Author ZhengYingjie
 * @Date 2019-07-31
 * @Description
 */
public class TestThread {

    static class Demo extends Thread{
        @Override
        public void run() {
            String name = Thread.currentThread().getName();
            System.out.println(name);
            System.out.println( "run");
        }
    }

    public static void main(String[] args) {
        new Demo().start();
        new Thread(() -> {
            System.out.println("bbb");
        }).start();
    }
}
