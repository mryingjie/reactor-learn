package com.demo;

import java.util.List;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * @Author ZhengYingjie
 * @Date 2019-07-31
 * @Description
 */
public class TestAtomicReferenceFieldUpdater {


    static class Person{

         volatile String name;

        private List<Person> friends;

        static final AtomicReferenceFieldUpdater<Person, String> nameUpdater = AtomicReferenceFieldUpdater.newUpdater(Person.class, String.class, "name");

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<Person> getFriends() {
            return friends;
        }

        public void setFriends(List<Person> friends) {
            this.friends = friends;
        }
    }


    public static void main(String[] args) {
        Person person = new Person();
        person.setName("aaa");
        String bbb = Person.nameUpdater.getAndSet(person, "bbb");

        System.out.println(bbb);
        System.out.println(person.name);

    }


}
