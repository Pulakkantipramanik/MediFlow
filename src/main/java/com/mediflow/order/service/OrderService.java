package com.mediflow.order.service;

import com.mediflow.medicine.entity.Medicine;
import com.mediflow.medicine.exception.MedicineNotFoundException;
import com.mediflow.medicine.exception.OrderNotFoundException;
import com.mediflow.medicine.repository.MedicineRepository;
import com.mediflow.order.dto.OrderRejectRequestDto;
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


    /// PURPOSE:
// Approves a pending order by an administrator.
//
// WHY:
// Approval is a controlled workflow step.
// Only PENDING orders can be approved.
    @Transactional
    public OrderResponseDto approveOrder(Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(
                        "Order not found with id: " + orderId
                ));

        // PURPOSE:
        // Prevent invalid state transitions.
        //
        // WHY:
        // APPROVED, PROCESSING or REJECTED orders
        // must not be approved again.
        validateCurrentStatus(
                order,
                OrderStatus.PENDING,
                "approved"
        );

        order.setStatus(OrderStatus.APPROVED);

        return mapToResponseDto(
                orderRepository.save(order)
        );
    }

    // PURPOSE:
// Rejects an order only when it is still pending.
//
// WHY:
// An order that is already approved or processing
// should not be rejected through this endpoint.
    @Transactional
    public OrderResponseDto rejectOrder(
            Long orderId,
            OrderRejectRequestDto request) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(
                        "Order not found with id: " + orderId
                ));

        // PURPOSE:
        // Allow rejection only from PENDING state.
        //
        // WHY:
        // This prevents an already approved/processing order
        // from moving backward to REJECTED.
        validateCurrentStatus(
                order,
                OrderStatus.PENDING,
                "rejected"
        );

        order.setStatus(OrderStatus.REJECTED);

        order.setRejectionReason(
                request.getRejectionReason()
        );

        // PURPOSE:
        // Restore stock because the order will no longer be fulfilled.
        //
        // WHY:
        // Stock was reduced when the order was created.
        // Rejected orders must return that quantity to inventory.
        restoreMedicineStock(order);

        return mapToResponseDto(
                orderRepository.save(order)
        );
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
                                new OrderNotFoundException(
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
    // PURPOSE:
// Validates whether an order is currently in the expected status
// before performing a business operation.
//
// WHY:
// An order should not be approved/rejected from an invalid state.
// Keeping this validation in one method avoids repeating the same
// status-checking logic in multiple methods.
    private void validateCurrentStatus(
            Order order,
            OrderStatus expectedStatus,
            String action) {

        if (order.getStatus() != expectedStatus) {
            throw new IllegalArgumentException(
                    "Order cannot be " + action +
                            " because current status is " + order.getStatus()
            );
        }
    }
    // PURPOSE:
// Cancels an order belonging to the logged-in user.
//
// WHY:
// A user must not be able to cancel another user's order.
// Also, cancellation is allowed only while the order is PENDING.
    @Transactional
    public OrderResponseDto cancelOrder(
            Long orderId,
            String userEmail) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(
                        "Order not found with id: " + orderId
                ));

        // PURPOSE:
        // Verify that the order belongs to the logged-in user.
        //
        // WHY:
        // Without ownership validation, one user could cancel
        // another user's order by simply knowing its ID.
        if (!order.getUserEmail().equals(userEmail)) {
            throw new IllegalArgumentException(
                    "You are not allowed to cancel this order"
            );
        }

        // PURPOSE:
        // Cancellation is allowed only from PENDING state.
        //
        // WHY:
        // Once admin approves the order, payment/processing
        // workflow may already have started, so the user cannot
        // cancel it through this endpoint.
        validateCurrentStatus(
                order,
                OrderStatus.PENDING,
                "cancelled"
        );

        // PURPOSE:
        // Restore the medicine quantity that was reserved
        // when the order was created.
        //
        // WHY:
        // Cancelled orders will not consume the medicine,
        // so the reserved stock must become available again.
        restoreMedicineStock(order);

        order.setStatus(OrderStatus.CANCELLED);

        return mapToResponseDto(
                orderRepository.save(order)
        );
    }
    // PURPOSE:
// Moves an order from PROCESSING to SHIPPED.
//
// WHY:
// Only an order that has entered the processing stage
// can be shipped. This prevents invalid status jumps.
    @Transactional
    public OrderResponseDto shipOrder(Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(
                        "Order not found with id: " + orderId
                ));

        // PURPOSE:
        // Allow shipping only when the order is PROCESSING.
        //
        // WHY:
        // An order cannot be shipped before payment/processing
        // has been completed.
        validateCurrentStatus(
                order,
                OrderStatus.PROCESSING,
                "shipped"
        );

        order.setStatus(OrderStatus.SHIPPED);

        return mapToResponseDto(
                orderRepository.save(order)
        );
    }
    // PURPOSE:
// Moves an order from SHIPPED to DELIVERED.
//
// WHY:
// Delivery should happen only after the order has actually
// been marked as shipped.
    @Transactional
    public OrderResponseDto deliverOrder(Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(
                        "Order not found with id: " + orderId
                ));

        // PURPOSE:
        // Allow delivery only for SHIPPED orders.
        //
        // WHY:
        // This prevents an order from directly jumping from
        // PROCESSING or APPROVED to DELIVERED.
        validateCurrentStatus(
                order,
                OrderStatus.SHIPPED,
                "delivered"
        );

        order.setStatus(OrderStatus.DELIVERED);

        return mapToResponseDto(
                orderRepository.save(order)
        );
    }
}