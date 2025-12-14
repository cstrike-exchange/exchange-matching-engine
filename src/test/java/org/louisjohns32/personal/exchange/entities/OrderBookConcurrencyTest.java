package org.louisjohns32.personal.exchange.entities;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.louisjohns32.personal.exchange.constants.Side;

@Disabled
public class OrderBookConcurrencyTest {

    private OrderBook orderBook;
    private static final String SYMBOL = "SYMBOL";

    // Added to increase contention between threads, increasing chance of race conditions
    private void sleepWithRandomDelay() {
        try {
            Thread.sleep((long) (Math.random() * 4 + 1));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @BeforeEach
    public void setup() {
        orderBook = new OrderBook(SYMBOL);
    }

    @Test
    public void multipleThreads_addOrdersConcurrently() throws InterruptedException, ExecutionException {
        int numThreads = 1000;
        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < numThreads; i++) {
            long orderId = i + 1;
            futures.add(executorService.submit(() -> {
                sleepWithRandomDelay();
                Order order = new Order(orderId, SYMBOL, Side.BUY, 2.0, 100.0);
                orderBook.addOrder(order);
            }));
        }

        // wait for completion
        for (Future<?> future : futures) {
            future.get();
        }

        executorService.shutdown();
        assertThat(orderBook.getHighestBidLevel().getOrders().size()).isEqualTo(numThreads);
    }

    @Test
    public void multipleThreads_addOrdersToDifferentPriceLevels() throws InterruptedException, ExecutionException {
        int numThreads = 1000;
        numThreads *= 3;
        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < numThreads; i++) {
            long orderId = i + 1;
            double price = 100.0 + (i % 3); // 3 different price levels
            futures.add(executorService.submit(() -> {
                sleepWithRandomDelay();
                Order order = new Order(orderId, SYMBOL, Side.BUY, 1.5, price);
                orderBook.addOrder(order);
            }));
        }

        // wait for completion
        for (Future<?> future : futures) {
            future.get();
        }

        executorService.shutdown();

        assertThat(orderBook.getHighestBidLevel().getPrice()).isEqualTo(102.0);
        assertThat(orderBook.getLowestAskLevel()).isNull();

        assertThat(orderBook.getLevel(100, Side.BUY).getOrders().size()).isEqualTo((numThreads / 3));
        assertThat(orderBook.getLevel(101, Side.BUY).getOrders().size()).isEqualTo((numThreads / 3));
        assertThat(orderBook.getLevel(102, Side.BUY).getOrders().size()).isEqualTo((numThreads / 3));
    }

    @Test
    public void multipleThreads_removeOrdersConcurrently() throws InterruptedException, ExecutionException {
        int numOrders = 1000;
        ExecutorService executorService = Executors.newFixedThreadPool(numOrders);
        List<Order> orders = new ArrayList<>();
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < numOrders; i++) {
            Order order = new Order((long) (i + 1), SYMBOL, Side.BUY, 2.0, 100.0);
            orders.add(order);
            orderBook.addOrder(order);
        }

        for (Order order : orders) {
            futures.add(executorService.submit(() -> {
                sleepWithRandomDelay();
                orderBook.removeOrder(order);
            }));
        }

        for (Future<?> future : futures) {
            future.get();
        }

        executorService.shutdown();

        for (Order order : orders) {
            assertThat(orderBook.getOrderById(order.getId())).isNull();
        }
        assertThat(orderBook.getHighestBidLevel()).isNull(); // Should be null if all were removed
    }

    @Test
    public void multipleThreads_removeFromDifferentPriceLevels() throws InterruptedException, ExecutionException {
        int numOrders = 1000;
        ExecutorService executorService = Executors.newFixedThreadPool(numOrders);
        List<Order> orders = new ArrayList<>();
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < numOrders; i++) {
            double price = 100.0 + (i % 3); // 3 different price levels
            Order order = new Order((long) (i + 1), SYMBOL, Side.BUY, 2.0, price);
            orders.add(order);
            orderBook.addOrder(order);
        }

        for (Order order : orders) {
            futures.add(executorService.submit(() -> {
                sleepWithRandomDelay();
                orderBook.removeOrder(order);
            }));
        }

        for (Future<?> future : futures) {
            future.get();
        }

        executorService.shutdown();

        for (Order order : orders) {
            assertThat(orderBook.getOrderById(order.getId())).isNull();
        }

        assertThat(orderBook.getLevel(100, Side.BUY)).isNull();
        assertThat(orderBook.getLevel(101, Side.BUY)).isNull();
        assertThat(orderBook.getLevel(102, Side.BUY)).isNull();
    }

    @Test
    public void repeatedly_removeAndAddOrderToSameLevelConcurrently() throws InterruptedException, ExecutionException {
        int iterations = 100;
        double price = 100.0;
        ExecutorService executorService = Executors.newFixedThreadPool(2);

        for (int i = 0; i < iterations; i++) {
            long originalOrderId = i * 2 + 1;
            long newOrderId = i * 2 + 2;

            Order originalOrder = new Order(originalOrderId, SYMBOL, Side.BUY, 1.0, price);
            Order newOrder = new Order(newOrderId, SYMBOL, Side.BUY, 1.0, price);

            orderBook.addOrder(originalOrder);
            Future<?> removeFuture = executorService.submit(() -> {
                sleepWithRandomDelay();
                orderBook.removeOrder(originalOrder);
            });

            Future<?> addFuture = executorService.submit(() -> {
                sleepWithRandomDelay();
                orderBook.addOrder(newOrder);
            });

            removeFuture.get();
            addFuture.get();

            OrderBookLevel level = orderBook.getLevel(price, Side.BUY);
            assertThat(level).isNotNull();
            assertThat(level.getOrders())
                    .allMatch(order -> order.getId() == newOrderId)
                    .hasSize(1);
            orderBook.removeOrder(newOrder);
        }

        executorService.shutdown();
    }
}