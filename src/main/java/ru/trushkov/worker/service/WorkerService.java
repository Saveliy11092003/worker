package ru.trushkov.worker.service;

import jakarta.xml.bind.DatatypeConverter;
import lombok.RequiredArgsConstructor;
import org.paukov.combinatorics3.Generator;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import ru.nsu.ccfit.schema.crack_hash_request.CrackHashManagerRequest;
import ru.nsu.ccfit.schema.crack_hash_response.CrackHashWorkerResponse;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class WorkerService {

    private AtomicInteger currentIndex = new AtomicInteger();

    private volatile String currentRequestId = "";

    @Value("${exchange.name}")
    private String exchangeName;

    private final AmqpTemplate amqpTemplate;

    @RabbitListener(queues = "request_queue_1")
    public void task(CrackHashManagerRequest request) {
        System.out.println("Worker do start");
        try {
            Thread.sleep(15000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        currentRequestId = request.getRequestId();
        System.out.println("I am here");
        System.out.println("Request " + request.getRequestId() + " " + request.getHash() + " " + request.getMaxLength());
        currentIndex.set(0);
        Stream<String> permutations = Generator.permutation(request.getAlphabet().getSymbols())
                .withRepetitions(request.getMaxLength()).stream().map(list -> String.join("", list));
        List<String> answer = getAnswer(permutations, request);
        System.out.println(answer);
        CrackHashWorkerResponse crackHashWorkerResponse = createCrackHashWorkerResponse(answer, request);
        System.out.println("Worker do send");
        sendPossiblePasswords(crackHashWorkerResponse);
        System.out.println("Worker do end");
    }

    @Bean
    public MessageConverter converter() {
        return new Jackson2JsonMessageConverter();
    }

    private void sendPossiblePasswords(CrackHashWorkerResponse crackHashWorkerResponse) {
        amqpTemplate.convertAndSend(exchangeName, "task.manager", crackHashWorkerResponse,
                message -> {
                    message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    return message;
                });
    }

    private CrackHashWorkerResponse createCrackHashWorkerResponse(List<String> answer, CrackHashManagerRequest request) {
        CrackHashWorkerResponse crackHashWorkerResponse = new CrackHashWorkerResponse();
        crackHashWorkerResponse.setRequestId(request.getRequestId());
        crackHashWorkerResponse.setPartNumber(request.getPartNumber());
        CrackHashWorkerResponse.Answers answers = new CrackHashWorkerResponse.Answers();
        answers.getWords().addAll(answer);
        crackHashWorkerResponse.setAnswers(answers);
        return crackHashWorkerResponse;
    }

    private List<String> getAnswer(Stream<String> permutations, CrackHashManagerRequest request) {
        int partNumber = request.getPartNumber();
        int maxLength = request.getMaxLength();
        int partCount = request.getPartCount();
        String passwordHash = request.getHash();
        long totalSize = (long) Math.pow(36, maxLength);
        long partSize = totalSize / partCount;
        long start = partNumber * partSize;
        long end = (partNumber == (partCount-1)) ? totalSize : (partNumber+1) * partSize;
        return permutations
                .skip(start)
                .limit(end-start)
                .filter(permutation -> {
                    currentIndex.incrementAndGet();
                    return hash(permutation).equalsIgnoreCase(passwordHash);
                })
                .toList();
    }

    private String hash(String permutation) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(permutation.getBytes(StandardCharsets.UTF_8));
            return DatatypeConverter.printHexBinary(messageDigest).toUpperCase();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public Integer getCurrentIndex(String requestId) {
        System.out.println("requestId" + requestId);
        System.out.println("currentRequestId " + currentRequestId);
        if (currentRequestId.equals(requestId)) {
            return currentIndex.get();
        }
        return 0;
    }

}
