package com.br.servicodolar.orderofservice.usecase;

import com.br.servicodolar.orderofservice.client.CostumerAPI;
import com.br.servicodolar.orderofservice.client.ServiceAPI;
import com.br.servicodolar.orderofservice.client.ServiceProviderAPI;
import com.br.servicodolar.orderofservice.domain.*;
import com.br.servicodolar.orderofservice.domain.entity.Order;
import com.br.servicodolar.orderofservice.domain.entity.Schedule;
import com.br.servicodolar.orderofservice.domain.entity.StatusOrder;
import com.br.servicodolar.orderofservice.repository.OrderRepository;
import com.br.servicodolar.orderofservice.usecase.model.OrderOfServiceDTO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class InsertOrderOfService {

    private CostumerAPI costumerAPI;

    private OrderRepository orderRepository;
    private ServiceAPI serviceAPI;
    private ServiceProviderAPI serviceProviderAPI;

    public InsertOrderOfService(CostumerAPI costumerAPI, OrderRepository orderRepository, ServiceAPI serviceAPI, ServiceProviderAPI serviceProviderAPI) {
        this.costumerAPI = costumerAPI;
        this.orderRepository = orderRepository;
        this.serviceAPI = serviceAPI;
        this.serviceProviderAPI = serviceProviderAPI;
    }

    public Order execute(OrderOfServiceDTO dto) {

        Order order = getOrder(dto);

        validateInAPI(order);

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

    private void validateInAPI(Order order) {
        ValidationInAPI validationInAPI  = new ValidationInAPIForCostumer(costumerAPI);
        validationInAPI.validateIfExistWithAPI(order.getCostumerId());

        validationInAPI = new ValidationInAPIForService(serviceAPI);
        validationInAPI.validateIfExistWithAPI(order.getServiceId());

        validationInAPI = new ValidationInAPIForServiceProvider(serviceProviderAPI);
        validationInAPI.validateIfExistWithAPI(order.getServiceProviderId());
    }

    private static Order getOrder(OrderOfServiceDTO dto) {
        var order = new Order();
        order.setCostumerId(dto.costumerId());
        order.setServiceProviderId(dto.serviceProviderId());
        order.setServiceId(dto.serviceId());
        order.setStatusOrder(StatusOrder.ABERTO);
        order.setYear(LocalDate.now().getYear());
        order.setOpeningDate(LocalDate.now());
        order.setSchedule(new Schedule(dto.serviceStarDate(), dto.serviceStartTime(), dto.serviceFinishDate(), dto.serviceFinishTime()));
        order.setTotalServiceCost(100.25);
        order.setUpdatedDateTime(LocalDateTime.now());

        return order;
    }

}
