package org.example.repository;

import org.example.model.Order;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class OrderRepository {

    private static final OrderRepository INSTANCE = new OrderRepository();
    private final Map<Long, Order> orderStore = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(0);

    private OrderRepository() {}

    public static OrderRepository getInstance() {
        return INSTANCE;
    }

    public Order save(Order order) {
        long newId = idGenerator.incrementAndGet();
        order.setId(newId);
        order.setDate(LocalDate.now());
        order.calculateTotalCost();
        orderStore.put(newId, order);
        return order;
    }

    public Optional<Order> findById(Long id) {
        return Optional.ofNullable(orderStore.get(id));
    }

    public List<Order> findAll() {
        return new ArrayList<>(orderStore.values());
    }

    public Optional<Order> update(Long id, Order orderToUpdate) {
        return findById(id).map(existingOrder -> {
            orderToUpdate.setId(id);
            orderToUpdate.setDate(existingOrder.getDate());
            orderToUpdate.calculateTotalCost();
            orderStore.put(id, orderToUpdate);
            return orderToUpdate;
        });
    }

    public boolean deleteById(Long id) {
        return orderStore.remove(id) != null;
    }

    public void clear() {
        orderStore.clear();
        idGenerator.set(0);
    }
}