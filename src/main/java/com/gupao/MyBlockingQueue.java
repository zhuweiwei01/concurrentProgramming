package com.gupao;

import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 请结合ReentrantLock、Condition实现一个简单的阻塞队列，阻塞队列提供两个方法，一个是put、一个是take
 *
 * 1.当队列为空时，请求take会被阻塞，直到队列不为空
 *
 *
 * 2.当队列满了以后，存储元素的线程需要备阻塞直到队列可以添加数据
 * @param <E>
 */
public class MyBlockingQueue<E> {

    ReentrantLock lock = new ReentrantLock();
    Condition notEmpty = lock.newCondition();
    Condition notFull = lock.newCondition();

    public MyBlockingQueue() {
        this(Integer.MAX_VALUE);//队列默认大小
    }

    public MyBlockingQueue(int capacity) {//指定队列的长度
        if (capacity <= 0) throw new IllegalArgumentException();
        this.capacity = capacity;
    }

    /**
     * 出队
     */
    public E take() throws InterruptedException {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            E x;
            while ( (x = unlinkFirst()) == null)
                notEmpty.await();//队列为空，阻塞线程
            return x;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 在队尾入队
     * @param e
     * @throws InterruptedException
     */
    public void put(E e) throws InterruptedException {
        if (e == null) throw new NullPointerException();
        Node<E> node = new Node<E>(e);
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            while (!linkLast(node))
                notFull.await();//
        } finally {
            lock.unlock();
        }
    }

    private boolean linkLast(Node<E> node) {
        if (count >= capacity) //如果队列元素已经到了最大容量
            return false; //返回false，也就是阻塞当前线程
        Node<E> l = last;
        node.prev = l; //新建一个节点，前节点指向之前的last节点
        last = node;//将last节点设置为新添加的节点
        if (first == null)
            first = node;
        else
            l.next = node;
        ++count;
        notEmpty.signal();//队列不为空，唤醒在take方法中阻塞的线程取元素
        return true;

    }

    /**
     * 取出头节点，并设置下一个节点为头节点
     * @return
     */
    private E unlinkFirst() {
        Node<E> f = first;
        if (f == null)
            return null; //头结点为空，返回空
        Node<E> n = f.next;
        E item = f.item; //头节点包含的元素，即要返回的对象
        f.item = null;
        f.next = f;
        first = n;//将n设置为头节点
        if (n == null)
            last = null;
        else
            n.prev = null;
        --count;
        notFull.signal();
        return item;
    }

    Node<E> first;//队列头节点
    Node<E> last;//队列尾节点
    int count;//包含的元素数量
    int capacity;//队列的容量

    /**
     * 节点
     * @param <E>
     */
    static final class Node<E>{
        E item; //当前节点元素
        Node<E> prev; //前一个节点
        Node<E> next;//后一个节点
        Node(E x) {
            item = x;
        }
    }
}
