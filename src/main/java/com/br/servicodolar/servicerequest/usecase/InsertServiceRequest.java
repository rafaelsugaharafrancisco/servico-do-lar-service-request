package com.br.servicodolar.servicerequest.usecase;

import com.br.servicodolar.servicerequest.client.CostumerAPI;
import com.br.servicodolar.servicerequest.client.ServiceAPI;
import com.br.servicodolar.servicerequest.client.ServiceProviderAPI;
import com.br.servicodolar.servicerequest.domain.*;
import com.br.servicodolar.servicerequest.domain.entity.Order;
import com.br.servicodolar.servicerequest.domain.entity.Schedule;
import com.br.servicodolar.servicerequest.domain.entity.StatusOrder;
import com.br.servicodolar.servicerequest.repository.OrderRepository;
import com.br.servicodolar.servicerequest.usecase.model.OrderDTO;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class InsertServiceRequest {

    private CostumerAPI costumerAPI;

    private OrderRepository orderRepository;
    private ServiceAPI serviceAPI;
    private ServiceProviderAPI serviceProviderAPI;

    public InsertServiceRequest(CostumerAPI costumerAPI, OrderRepository orderRepository, ServiceAPI serviceAPI, ServiceProviderAPI serviceProviderAPI) {
        this.costumerAPI = costumerAPI;
        this.orderRepository = orderRepository;
        this.serviceAPI = serviceAPI;
        this.serviceProviderAPI = serviceProviderAPI;
    }

    public Order execute(OrderDTO dto) {

        validateInAPI(dto);

        Order order = getOrder(dto);

        validateInDB(order);

        return this.orderRepository.save(order);

    }

    private void validateInDB(Order order) {
        ValidationSchedule validationSchedule = new ValidationScheduleInDB();

        List<Order> orderList = this.orderRepository.findAllByYearAndCostumerId(order.getYear(), order.getCostumerId());
        ValidationInDB validationInDB = new ValidationInDBForCostumer(orderList);
        validationInDB.validateIfServiceExistInDataBase(order);
        validationInDB.validateIfDateTimeOfServiceExistInDB(order, validationSchedule);

        orderList = this.orderRepository.findAllByYearAndServiceProviderId(order.getYear(), order.getServiceProviderId());
        validationInDB = new ValidationInDBForServiceProvider(orderList);
        validationInDB.validateIfServiceExistInDataBase(order);
        validationInDB.validateIfDateTimeOfServiceExistInDB(order, validationSchedule);
    }

    private void validateInAPI(OrderDTO dto) {
        ValidationInAPI validationInAPI  = new ValidationInAPIForCostumer(costumerAPI);
        validationInAPI.validateIfExistWithAPI(dto.costumerId());

        validationInAPI = new ValidationInAPIForService(serviceAPI);
        validationInAPI.validateIfExistWithAPI(dto.serviceId());

        validationInAPI = new ValidationInAPIForServiceProvider(serviceProviderAPI);
        validationInAPI.validateIfExistWithAPI(dto.serviceProviderId());
    }

    private Order getOrder(OrderDTO dto) {

        double totalCost = new CostCalculator().execute(this.serviceAPI, dto.serviceId());

        var order = new Order();
        order.setCostumerId(dto.costumerId());
        order.setServiceProviderId(dto.serviceProviderId());
        order.setServiceId(dto.serviceId());
        order.setStatusOrder(StatusOrder.ABERTO);
        order.setYear(LocalDate.now().getYear());
        order.setOpeningDate(LocalDate.now());
        order.setSchedule(new Schedule(dto.serviceStarDate(), dto.serviceStartTime(), dto.serviceFinishDate(), dto.serviceFinishTime()));
        order.setTotalServiceCost(totalCost);
        order.setUpdatedDateTime(LocalDateTime.now());

        return order;
    }

}