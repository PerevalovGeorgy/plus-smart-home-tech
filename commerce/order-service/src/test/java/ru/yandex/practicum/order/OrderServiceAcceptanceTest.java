package ru.yandex.practicum.order;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import feign.Request;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import ru.yandex.practicum.order.dto.CreateOrderRequest;
import ru.yandex.practicum.order.dto.OrderItemRequest;
import ru.yandex.practicum.order.dto.ProductDto;
import ru.yandex.practicum.order.dto.ReserveResponse;
import ru.yandex.practicum.order.feign.InventoryClient;
import ru.yandex.practicum.order.feign.ProductClient;
import ru.yandex.practicum.order.repository.OrderRepository;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
@SuppressWarnings("unchecked")
class OrderServiceAcceptanceTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private OrderRepository orderRepository;

    @MockBean private ProductClient productClient;
    @MockBean private InventoryClient inventoryClient;

    private static final long LAMP_ID = 1L;
    private static final long PLUG_ID = 2L;
    private static final long BROKEN_ID = 3L;


    @Test
    void shouldCreateOrderWithProductSnapshotAndFindItByIdAndEmail() throws Exception {
        when(productClient.getProductById(LAMP_ID)).thenReturn(lamp());
        when(productClient.getProductById(PLUG_ID)).thenReturn(plug());
        stubReserveSuccess();

        CreateOrderRequest request = new CreateOrderRequest(
                "Acceptance Buyer",
                "acceptance-buyer@example.com",
                List.of(
                        new OrderItemRequest(LAMP_ID, 2),
                        new OrderItemRequest(PLUG_ID, 1)
                )
        );

        MvcResult createResponse = postJson("/api/orders", request);

        assertThat(status(createResponse))
                .as("POST /api/orders должен создавать заказ и возвращать HTTP 201 Created")
                .isEqualTo(201);

        Map<String, Object> created = readMap(createResponse);
        Long orderId = asLong(created.get("id"));
        assertThat(orderId).isNotNull();
        assertThat(created.get("status"))
                .as("Новый заказ в новом сценарии сохраняется в статусе CONFIRMED")
                .isEqualTo("CONFIRMED");
        assertThat(asDecimal(created.get("totalPrice")))
                .as("totalPrice считается по ценам из product-service: 3490*2 + 1290*1")
                .isEqualByComparingTo("8270.00");

        List<Map<String, Object>> items = (List<Map<String, Object>>) created.get("items");
        assertThat(items).hasSize(2);
        assertThat(items)
                .as("Позиции содержат снимок названия из product-service")
                .anySatisfy(item -> assertThat(item).containsEntry("productName", "Acceptance Smart Lamp"))
                .anySatisfy(item -> assertThat(item).containsEntry("productName", "Acceptance Smart Plug"));


        verify(productClient, times(1)).getProductById(LAMP_ID);
        verify(productClient, times(1)).getProductById(PLUG_ID);
        verify(inventoryClient, times(1)).reserveStock(argThat(r ->
                r != null && r.productId().equals(LAMP_ID) && r.quantity() == 2));
        verify(inventoryClient, times(1)).reserveStock(argThat(r ->
                r != null && r.productId().equals(PLUG_ID) && r.quantity() == 1));
        verify(inventoryClient, never()).releaseStock(any());

        MvcResult byId = mvc.perform(get("/api/orders/{id}", orderId)).andReturn();
        assertThat(status(byId)).isEqualTo(200);
        assertThat(readMap(byId).get("customerEmail")).isEqualTo("acceptance-buyer@example.com");

        MvcResult byEmail = mvc.perform(get("/api/orders/by-email")
                .param("email", "acceptance-buyer@example.com")).andReturn();
        assertThat(status(byEmail)).isEqualTo(200);
        assertThat(readList(byEmail))
                .anySatisfy(item -> assertThat(item)
                        .containsEntry("customerEmail", "acceptance-buyer@example.com"));
    }


    @Test
    void shouldFetchProductOnceAndReserveSummedQuantityForRepeatedProduct() throws Exception {
        when(productClient.getProductById(LAMP_ID)).thenReturn(lamp());
        stubReserveSuccess();

        CreateOrderRequest request = new CreateOrderRequest(
                "Repeat Buyer",
                "repeat-buyer@example.com",
                List.of(
                        new OrderItemRequest(LAMP_ID, 2),
                        new OrderItemRequest(LAMP_ID, 1)
                )
        );

        MvcResult response = postJson("/api/orders", request);
        assertThat(status(response)).isEqualTo(201);

        verify(productClient, times(1)).getProductById(LAMP_ID);
        verify(inventoryClient, times(1)).reserveStock(argThat(r ->
                r != null && r.productId().equals(LAMP_ID) && r.quantity() == 3));

        Map<String, Object> created = readMap(response);
        assertThat(asDecimal(created.get("totalPrice")))
                .as("totalPrice = 3490 * 3")
                .isEqualByComparingTo("10470.00");
        assertThat((List<?>) created.get("items"))
                .as("В заказе присутствует позиция с товаром Acceptance Smart Lamp")
                .anySatisfy(item -> assertThat((Map<String, Object>) item)
                        .containsEntry("productName", "Acceptance Smart Lamp"));
    }


    @Test
    void shouldNotCreateOrderWhenProductIsInactive() throws Exception {
        when(productClient.getProductById(LAMP_ID)).thenReturn(
                new ProductDto(LAMP_ID, "Acceptance Smart Lamp", "desc",
                        new BigDecimal("3490.00"), false));

        long before = orderRepository.count();

        CreateOrderRequest request = new CreateOrderRequest(
                "Inactive Buyer",
                "inactive-buyer@example.com",
                List.of(new OrderItemRequest(LAMP_ID, 1))
        );

        MvcResult response = postJson("/api/orders", request);

        assertThat(status(response))
                .as("Неактивный товар → 422 Unprocessable Entity")
                .isEqualTo(422);
        assertThat(String.valueOf(readMap(response).get("message")))
                .as("Сообщение сформулировано на уровне сценария заказа")
                .containsIgnoringCase("снят с продажи");

        verify(inventoryClient, never()).reserveStock(any());
        verify(inventoryClient, never()).releaseStock(any());
        assertThat(orderRepository.count())
                .as("Заказ не сохраняется")
                .isEqualTo(before);
    }


    @Test
    void shouldNotCreateOrderWhenProductNotFound() throws Exception {
        when(productClient.getProductById(LAMP_ID)).thenThrow(notFoundException(LAMP_ID));

        long before = orderRepository.count();

        CreateOrderRequest request = new CreateOrderRequest(
                "Missing Product Buyer",
                "missing-product-buyer@example.com",
                List.of(new OrderItemRequest(LAMP_ID, 1))
        );

        MvcResult response = postJson("/api/orders", request);

        assertThat(status(response))
                .as("Отсутствующий товар → 422 Unprocessable Entity")
                .isEqualTo(422);
        assertThat(String.valueOf(readMap(response).get("message")))
                .as("Сообщение на уровне сценария заказа, без деталей Feign")
                .containsIgnoringCase("не найден");

        verify(inventoryClient, never()).reserveStock(any());
        verify(inventoryClient, never()).releaseStock(any());
        assertThat(orderRepository.count())
                .as("Заказ не сохраняется")
                .isEqualTo(before);
    }

    @Test
    void shouldNotCreateOrderWhenStockReservationFails() throws Exception {
        when(productClient.getProductById(LAMP_ID)).thenReturn(lamp());
        when(inventoryClient.reserveStock(any()))
                .thenReturn(new ReserveResponse(false, 0, "not enough"));

        long before = orderRepository.count();

        CreateOrderRequest request = new CreateOrderRequest(
                "NoStock Buyer",
                "nostock-buyer@example.com",
                List.of(new OrderItemRequest(LAMP_ID, 5))
        );

        MvcResult response = postJson("/api/orders", request);

        assertThat(status(response)).isEqualTo(422);
        assertThat(orderRepository.count()).isEqualTo(before);
        verify(inventoryClient, never()).releaseStock(any());
    }


    @Test
    void shouldReleaseReservedItemWhenLaterReservationFails() throws Exception {
        when(productClient.getProductById(LAMP_ID)).thenReturn(lamp());
        when(productClient.getProductById(BROKEN_ID)).thenReturn(
                new ProductDto(BROKEN_ID, "Broken Product", "desc",
                        new BigDecimal("100.00"), true));

        when(inventoryClient.reserveStock(argThat(r ->
                r != null && r.productId().equals(LAMP_ID))))
                .thenReturn(new ReserveResponse(true, 100, "ok"));
        when(inventoryClient.reserveStock(argThat(r ->
                r != null && r.productId().equals(BROKEN_ID))))
                .thenReturn(new ReserveResponse(false, 0, "not enough"));
        when(inventoryClient.releaseStock(any()))
                .thenReturn(new ReserveResponse(true, 100, "released"));

        long before = orderRepository.count();

        CreateOrderRequest request = new CreateOrderRequest(
                "Compensation Buyer",
                "compensation-buyer@example.com",
                List.of(
                        new OrderItemRequest(LAMP_ID, 1),
                        new OrderItemRequest(BROKEN_ID, 1)
                )
        );

        MvcResult response = postJson("/api/orders", request);

        assertThat(status(response)).isEqualTo(422);
        assertThat(orderRepository.count()).isEqualTo(before);

        verify(inventoryClient, times(1)).releaseStock(argThat(r ->
                r != null && r.productId().equals(LAMP_ID) && r.quantity() == 1));
        verify(inventoryClient, never()).releaseStock(argThat(r ->
                r != null && r.productId().equals(BROKEN_ID)));
    }


    @Test
    void shouldReturnBadRequestForInvalidOrderPayload() throws Exception {
        CreateOrderRequest invalid = new CreateOrderRequest(
                "",
                "not-an-email",
                List.of()
        );

        MvcResult response = postJson("/api/orders", invalid);

        assertThat(status(response))
                .as("Невалидное тело → 400 Bad Request")
                .isEqualTo(400);
        assertThat(readMap(response)).containsKeys("message", "validationErrors");
    }


    private static ProductDto lamp() {
        return new ProductDto(LAMP_ID, "Acceptance Smart Lamp", "desc",
                new BigDecimal("3490.00"), true);
    }

    private static ProductDto plug() {
        return new ProductDto(PLUG_ID, "Acceptance Smart Plug", "desc",
                new BigDecimal("1290.00"), true);
    }

    private void stubReserveSuccess() {
        when(inventoryClient.reserveStock(any()))
                .thenReturn(new ReserveResponse(true, 100, "ok"));
        when(inventoryClient.releaseStock(any()))
                .thenReturn(new ReserveResponse(true, 100, "released"));
    }

    private MvcResult postJson(String url, Object body) throws Exception {
        return mvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andReturn();
    }

    private static int status(MvcResult result) {
        return result.getResponse().getStatus();
    }

    private Map<String, Object> readMap(MvcResult result) throws Exception {
        return json.readValue(
                result.getResponse().getContentAsString(StandardCharsets.UTF_8),
                new TypeReference<>() {});
    }

    private List<Map<String, Object>> readList(MvcResult result) throws Exception {
        return json.readValue(
                result.getResponse().getContentAsString(StandardCharsets.UTF_8),
                new TypeReference<>() {});
    }

    private static Long asLong(Object value) {
        return value == null ? null : ((Number) value).longValue();
    }

    private static BigDecimal asDecimal(Object value) {
        return new BigDecimal(value.toString());
    }

    private static FeignException notFoundException(long productId) {
        Request request = Request.create(
                Request.HttpMethod.GET,
                "http://product-service/api/products/" + productId,
                Map.of(),
                new byte[0],
                StandardCharsets.UTF_8
        );
        return new FeignException.NotFound("not found", request, null, null);
    }
}