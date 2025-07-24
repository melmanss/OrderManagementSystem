package org.example.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.model.Order;
import org.example.repository.OrderRepository;

import java.io.IOException;
import java.util.Optional;
import java.util.stream.Collectors;

@WebServlet("/orders/*")
public class OrderServlet extends HttpServlet {

    private OrderRepository orderRepository;
    private ObjectMapper objectMapper;

    @Override
    public void init() throws ServletException {
        this.orderRepository = OrderRepository.getInstance();
        this.objectMapper = new ObjectMapper();

        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Optional<Long> orderIdOpt = extractId(req);

        if (orderIdOpt.isPresent()) {
            Optional<Order> orderOptional = orderRepository.findById(orderIdOpt.get());
            if (orderOptional.isPresent()) {
                sendAsJson(resp, orderOptional.get(), HttpServletResponse.SC_OK);
            } else {
                sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Order not found");
            }
        } else {
            sendAsJson(resp, orderRepository.findAll(), HttpServletResponse.SC_OK);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String jsonBody = req.getReader().lines().collect(Collectors.joining());
            Order newOrder = objectMapper.readValue(jsonBody, Order.class);

            Order savedOrder = orderRepository.save(newOrder);

            sendAsJson(resp, savedOrder, HttpServletResponse.SC_CREATED);
        } catch (Exception e) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid request body: " + e.getMessage());
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Optional<Long> orderIdOpt = extractId(req);
        if (orderIdOpt.isEmpty()) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Order ID is required in the path.");
            return;
        }

        Long orderId = orderIdOpt.get();

        try {
            String jsonBody = req.getReader().lines().collect(Collectors.joining());
            Order orderToUpdate = objectMapper.readValue(jsonBody, Order.class);

            Optional<Order> updatedOrderOptional = orderRepository.update(orderId, orderToUpdate);
            if (updatedOrderOptional.isPresent()) {
                sendAsJson(resp, updatedOrderOptional.get(), HttpServletResponse.SC_OK);
            } else {
                sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Order not found for update");
            }
        } catch (Exception e) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid request body: " + e.getMessage());
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Optional<Long> orderIdOpt = extractId(req);
        if (orderIdOpt.isEmpty()) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Order ID is required in the path.");
            return;
        }

        if (orderRepository.deleteById(orderIdOpt.get())) {
            resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
        } else {
            sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Order not found for deletion");
        }
    }

    private Optional<Long> extractId(HttpServletRequest req) {
        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.parseLong(pathInfo.substring(1)));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private void sendAsJson(HttpServletResponse resp, Object obj, int status) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setStatus(status);
        resp.getWriter().write(objectMapper.writeValueAsString(obj));
    }

    private void sendError(HttpServletResponse resp, int status, String message) throws IOException {
        resp.sendError(status, message);
    }
}