package com.nttdata.bankaccountservice.service.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nttdata.bankaccountservice.dto.PaymentDto;
import com.nttdata.bankaccountservice.repository.BankAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class Consumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(Consumer.class);

    private final ObjectMapper objectMapper;

    @Autowired
    public Consumer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    @Autowired
    private BankAccountRepository bankAccountRepository;

    @KafkaListener(topics = "primary-account" , groupId = "default")
    public void makePrimaryAccountKafka(String message) {
        LOGGER.info("consumiendo mensaje " + message.trim());
        bankAccountRepository.findById(message).subscribe(account -> {
            LOGGER.info("makePrimaryAccountKafka found account " + account);
            account.setPrimaryAccount(true);
            bankAccountRepository.save(account).subscribe();
        });
    }

    @KafkaListener(topics = "pay-seller" , groupId = "default")
    public void receivePaymentForBootCoins(String message) throws JsonProcessingException {
        LOGGER.info("receiving payment for boot coins " + message);
        PaymentDto paymentDto = objectMapper.readValue(message, PaymentDto.class);
        bankAccountRepository.findById(paymentDto.getAccountId()).subscribe(account -> {
            LOGGER.info("receivePaymentForBootCoins found account " + account);
            account.setAmount(account.getAmount() + paymentDto.getPaymentAmount());
            bankAccountRepository.save(account).subscribe();
        });
    }
}
