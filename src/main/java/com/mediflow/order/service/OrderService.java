package com.mediflow.order.service;

import com.mediflow.medicine.entity.Medicine;
import com.mediflow.medicine.exception.MedicineNotFoundException;
import com.mediflow.medicine.repository.MedicineRepository;
import com.mediflow.order.dto.OrderRequestDto;
import com.mediflow.order.dto.OrderResponseDto;
import com.mediflow.order.entity.Order;
import com.mediflow.order.entity.OrderStatus;
import com.mediflow.order.repository.OrderRepository;
import com.mediflow.prescription.entity.PrescriptionStatus;
import com.mediflow.prescription.repository.PrescriptionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final MedicineRepository medicineRepository;
    private final PrescriptionRepository prescriptionRepository;

    public OrderService(
            OrderRepository orderRepository,
            MedicineRepository medicineRepository,
            PrescriptionRepository prescriptionRepository) {

        this.orderRepository = orderRepository;
        this.medicineRepository = medicineRepository;
        this.prescriptionRepository = prescriptionRepository;
    }

    // PURPOSE:
    // Creates a new order for the authenticated user.
    // BUSINESS FLOW:
    // Medicine validation → Prescription validation → Stock validation
    // → Price calculation → Stock reduction → Order creation.
    // WHY:
    // All these operations belong to one business transaction.
    @Transactional
    public OrderResponseDto createOrder(
            OrderRequestDto request,
            String userEmail) {

        // PURPOSE:
        // Find the medicine selected by the user.
        // WHY:
        // We need the medicine price, stock and prescription requirement
        // before creating the order.
        Medicine medicine = medicineRepository
                .findById(request.getMedicineId())
                .orElseThrow(() ->
                        new MedicineNotFoundException(
                                "Medicine not found with id: "
                                        + request.getMedicineId()
                        )
                );

        // BUSINESS RULE:
        // Some medicines require a valid prescription before ordering.
        // We check this only when prescriptionRequired is true.
        if (Boolean.TRUE.equals(medicine.getPrescriptionRequired())) {

            // PURPOSE:
            // Check whether this user has an APPROVED prescription
            // for the exact medicine being ordered.
            boolean hasApprovedPrescription =
                    prescriptionRepository
                            .findByUserEmailAndMedicineIdAndStatus(
                                    userEmail,
                                    medicine.getId(),
                                    PrescriptionStatus.APPROVED
                            )
                            .isPresent();

            // BUSINESS RULE:
            // A prescription-required medicine cannot be ordered
            // without an APPROVED prescription.
            if (!hasApprovedPrescription) {

                throw new IllegalArgumentException(
                        "Approved prescription is required for this medicine"
                );
            }
        }

        // BUSINESS RULE:
        // User cannot order more quantity than the currently
        // available stock.
        if (medicine.getStockQuantity()
                < request.getQuantity()) {

            throw new IllegalArgumentException(
                    "Insufficient stock"
            );
        }

        // PURPOSE:
        // Calculate the total order amount using medicine price
        // multiplied by the requested quantity.
        BigDecimal totalPrice =
                medicine.getPrice()
                        .multiply(
                                BigDecimal.valueOf(
                                        request.getQuantity()
                                )
                        );

        // BUSINESS RULE:
        // Reserve the ordered quantity by reducing stock immediately.
        // This prevents the same available stock from being ordered
        // beyond the actual inventory.
        medicine.setStockQuantity(
                medicine.getStockQuantity()
                        - request.getQuantity()
        );

        medicineRepository.save(medicine);

        // PURPOSE:
        // Create the Order entity using authenticated user information
        // and the validated medicine/order details.
        Order order = new Order();

        order.setUserEmail(userEmail);
        order.setMedicineId(request.getMedicineId());
        order.setQuantity(request.getQuantity());
        order.setTotalPrice(totalPrice);

        // BUSINESS RULE:
        // Every newly created order starts with PENDING status because
        // an ADMIN must review the order before it becomes APPROVED.
        order.setStatus(OrderStatus.PENDING);

        order.setOrderDate(LocalDateTime.now());

        // PURPOSE:
        // Persist the newly created order in the database.
        Order savedOrder =
                orderRepository.save(order);

        // PURPOSE:
        // Convert the saved entity into the response DTO
        // that is returned to the API caller.
        return mapToResponseDto(savedOrder);
    }


    // PURPOSE:
    // Returns all orders belonging to the currently authenticated user.
    // WHY:
    // A user should only be able to see their own orders.
    public List<OrderResponseDto> getMyOrders(
            String userEmail) {

        List<Order> orders =
                orderRepository.findByUserEmail(userEmail);

        return orders.stream()
                .map(this::mapToResponseDto)
                .toList();
    }


    // PURPOSE:
    // Returns all orders for ADMIN users with pagination support.
    // WHY:
    // Admin may have many orders, so Pageable avoids loading
    // every order at once.
    public Page<OrderResponseDto> getAllOrders(
            Pageable pageable) {

        return orderRepository
                .findAll(pageable)
                .map(this::mapToResponseDto);
    }


    // PURPOSE:
    // Allows an ADMIN to approve a PENDING order.
    // BUSINESS RULE:
    // Only PENDING orders can move to APPROVED status.
    @Transactional
    public OrderResponseDto approveOrder(Long orderId) {

        // PURPOSE:
        // Find the order that the ADMIN wants to approve.
        Order order =
                orderRepository.findById(orderId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Order not found with id: "
                                                + orderId
                                ));

        // BUSINESS RULE:
        // An order that is already APPROVED or REJECTED
        // cannot be approved again.
        if (order.getStatus() != OrderStatus.PENDING) {

            throw new IllegalArgumentException(
                    "Only PENDING orders can be approved"
            );
        }

        // BUSINESS RULE:
        // After successful ADMIN review, the order becomes APPROVED.
        order.setStatus(OrderStatus.APPROVED);

        Order updatedOrder =
                orderRepository.save(order);

        return mapToResponseDto(updatedOrder);
    }


    // PURPOSE:
    // Allows an ADMIN to reject a PENDING order.
    // BUSINESS RULE:
    // Only PENDING orders can be rejected.
    // IMPORTANT:
    // Stock was reduced when the order was created,
    // therefore rejected orders must return that stock.
    @Transactional
    public OrderResponseDto rejectOrder(
            Long orderId,
            String rejectionReason) {

        // PURPOSE:
        // Find the order that the ADMIN wants to reject.
        Order order =
                orderRepository.findById(orderId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Order not found with id: "
                                                + orderId
                                ));

        // BUSINESS RULE:
        // Only PENDING orders can be rejected.
        if (order.getStatus() != OrderStatus.PENDING) {

            throw new IllegalArgumentException(
                    "Only PENDING orders can be rejected"
            );
        }

        // BUSINESS RULE:
        // Stock was reduced when the order was created.
        // Since the order is being rejected, that reserved stock
        // must be added back to the medicine inventory.
        restoreMedicineStock(order);

        // BUSINESS RULE:
        // Store the rejection status and the reason provided by ADMIN.
        order.setStatus(OrderStatus.REJECTED);
        order.setRejectionReason(rejectionReason);

        Order updatedOrder =
                orderRepository.save(order);

        return mapToResponseDto(updatedOrder);
    }


    // PURPOSE:
    // Restores the ordered quantity back into medicine stock
    // when an order is rejected.
    // WHY:
    // Stock was already reduced during order creation.
    // Without restoring it, rejected orders would permanently
    // decrease inventory even though the medicine was not sold.
    private void restoreMedicineStock(Order order) {

        // PURPOSE:
        // Find the medicine associated with this order.
        Medicine medicine =
                medicineRepository.findById(order.getMedicineId())
                        .orElseThrow(() ->
                                new MedicineNotFoundException(
                                        "Medicine not found with id: "
                                                + order.getMedicineId()
                                ));

        // BUSINESS RULE:
        // Add the rejected order quantity back to available stock.
        medicine.setStockQuantity(
                medicine.getStockQuantity()
                        + order.getQuantity()
        );

        // PURPOSE:
        // Persist the restored inventory quantity in the database.
        medicineRepository.save(medicine);
    }


    // PURPOSE:
    // Converts the internal Order entity into a response DTO.
    // WHY:
    // We should return only the required API response fields
    // instead of exposing the entity directly.
    private OrderResponseDto mapToResponseDto(
            Order order) {

        return new OrderResponseDto(
                order.getId(),
                order.getUserEmail(),
                order.getMedicineId(),
                order.getQuantity(),
                order.getTotalPrice(),
                order.getStatus().name(),
                order.getOrderDate(),
                order.getRejectionReason()
        );
    }
}