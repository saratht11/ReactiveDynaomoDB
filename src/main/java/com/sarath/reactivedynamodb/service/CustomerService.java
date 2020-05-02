package com.sarath.reactivedynamodb.service;

import com.sarath.reactivedynamodb.domain.Address;
import com.sarath.reactivedynamodb.domain.Customer;
import com.sarath.reactivedynamodb.repos.CustomerRepository;
import com.sarath.reactivedynamodb.util.Result;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.async.SdkPublisher;

import java.util.concurrent.CompletableFuture;

import static com.sarath.reactivedynamodb.util.Result.FAIL;
import static com.sarath.reactivedynamodb.util.Result.SUCCESS;
import static java.lang.String.valueOf;
import static java.time.Instant.now;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public Mono<Result> createNewCustomer(Customer customer) {
        customer.setCreatedTimeStamp(valueOf(now().getEpochSecond()));
        Result createStatus = customerRepository.save(customer)
                .handle((__, ex) -> ex == null ? SUCCESS : FAIL)
                .join();
        return Mono.just(createStatus);

    }

    public Mono<Customer> getCustomerByCustomerId(String customerId) {
        CompletableFuture<Customer> customer = customerRepository.getCustomerByID(customerId)
                .whenComplete((cus, ex) -> {
                    if (null == cus) {
                        throw new IllegalArgumentException("Invalid customerId");
                    }
                })
                .exceptionally(ex -> new Customer());
        return Mono.fromFuture(customer);
    }

    public Mono<Address> queryAddressByCustomerId(String customerId) {
        SdkPublisher<Address> customerAddress = customerRepository.getCustomerAddress(customerId)
                .items()
                .map(Customer::getAddress);
        return Mono.from(customerAddress)
                .onErrorReturn(new Address());
    }

    public Mono<Result> updateExistingCustomer(Customer customer) {
        customer.setCreatedTimeStamp(valueOf(now().getEpochSecond()));
        Result updateStatus = customerRepository.getCustomerByID(customer.getCustomerID())
                .thenApply(retrievedCustomer -> {
                    if (null == retrievedCustomer) {
                        throw new IllegalArgumentException("Invalid CustomerID");
                    }
                    return retrievedCustomer;
                }).thenCompose(__ -> customerRepository.updateCustomer(customer))
                .handle((__, ex) -> ex == null ? SUCCESS : FAIL)
                .join();

        return Mono.just(updateStatus);
    }

    public Mono<Result> updateExistingOrCreateCustomer(Customer customer) {
        customer.setCreatedTimeStamp(valueOf(now().getEpochSecond()));
        Result updateStatus = customerRepository.updateCustomer(customer)
                .handle((__, ex) -> ex == null ? SUCCESS : FAIL)
                .join();
        return Mono.just(updateStatus);
    }

    public Mono<Result> deleteCustomerByCustomerId(String customerId) {
        Result deleteStatus = customerRepository.deleteCustomerById(customerId)
                .handle((__, ex) -> ex == null ? SUCCESS : FAIL)
                .join();
        return Mono.just(deleteStatus);
    }

    public Flux<Customer> getCustomerList() {
        return Flux.from(customerRepository.getAllCustomer().items())
                .onErrorReturn(new Customer());
    }

}
