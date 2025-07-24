package org.example.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.model.Order;
import org.example.model.Product;
import org.example.repository.OrderRepository;
import org.example.servlet.OrderServlet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServletTest {

    @Spy
    private OrderRepository orderRepository = OrderRepository.getInstance();

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private OrderServlet orderServlet;

    private ObjectMapper objectMapper;
    private StringWriter stringWriter;
    private PrintWriter printWriter;

    @BeforeEach
    void setUp() throws Exception {

        orderRepository.clear();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        stringWriter = new StringWriter();
        printWriter = new PrintWriter(stringWriter);

        lenient().when(response.getWriter()).thenReturn(printWriter);

        orderServlet.init();
    }

    @Test
    void testGetOrderById_Success() throws Exception {

        Order order = createTestOrder();
        Order savedOrder = orderRepository.save(order);
        when(request.getPathInfo()).thenReturn("/" + savedOrder.getId());

        orderServlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_OK);
        verify(response).setContentType("application/json");
        String jsonResponse = stringWriter.toString();
        assertTrue(jsonResponse.contains("\"id\":" + savedOrder.getId()));
        assertTrue(jsonResponse.contains("\"name\":\"Test Product\""));
    }

    @Test
    void testGetOrderById_NotFound() throws Exception {

        when(request.getPathInfo()).thenReturn("/999");

        orderServlet.doGet(request, response);

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND, "Order not found");
    }

    @Test
    void testCreateOrder() throws Exception {

        Order order = createTestOrder();
        String orderJson = objectMapper.writeValueAsString(order);
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader(orderJson)));

        orderServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_CREATED);
        String jsonResponse = stringWriter.toString();

        assertTrue(jsonResponse.contains("\"id\":1"));

        assertTrue(jsonResponse.contains("\"cost\":100.50"));
    }

    @Test
    void testUpdateOrder_Success() throws Exception {

        Order savedOrder = orderRepository.save(createTestOrder());

        Order updatedOrderData = new Order();
        Product updatedProduct = new Product(2L, "Updated Product", new BigDecimal("250.75"));
        updatedOrderData.setProducts(Collections.singletonList(updatedProduct));

        String updatedJson = objectMapper.writeValueAsString(updatedOrderData);

        when(request.getPathInfo()).thenReturn("/" + savedOrder.getId());
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader(updatedJson)));

        orderServlet.doPut(request, response);

        verify(response).setStatus(HttpServletResponse.SC_OK);
        String jsonResponse = stringWriter.toString();
        assertTrue(jsonResponse.contains("\"name\":\"Updated Product\""));
        assertTrue(jsonResponse.contains("\"cost\":250.75"));
    }

    @Test
    void testDeleteOrder_Success() throws Exception {

        Order savedOrder = orderRepository.save(createTestOrder());
        when(request.getPathInfo()).thenReturn("/" + savedOrder.getId());

        orderServlet.doDelete(request, response);

        verify(response).setStatus(HttpServletResponse.SC_NO_CONTENT);
        assertTrue(orderRepository.findById(savedOrder.getId()).isEmpty());
    }

    @Test
    void testDeleteOrder_NotFound() throws Exception {

        when(request.getPathInfo()).thenReturn("/999");

        orderServlet.doDelete(request, response);

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND, "Order not found for deletion");
    }

    private Order createTestOrder() {
        Product product = new Product(1L, "Test Product", new BigDecimal("100.50"));
        Order order = new Order();
        order.setProducts(Collections.singletonList(product));
        return order;
    }
}