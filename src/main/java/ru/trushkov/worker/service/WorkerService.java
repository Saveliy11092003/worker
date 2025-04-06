package ru.trushkov.worker.service;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import jakarta.xml.bind.DatatypeConverter;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import lombok.RequiredArgsConstructor;
import org.paukov.combinatorics3.Generator;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
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

    private volatile String currentRequestId;

    @Value("${url.possible.password}")
    private String urlPossiblePassword;

    @Value("${exchange.name}")
    private String exchangeName;

    private final AmqpTemplate amqpTemplate;

    @RabbitListener(queues = "${queue.request}")
    public void task(CrackHashManagerRequest request) {
        currentRequestId = request.getRequestId();
        System.out.println("I am here");
        System.out.println("Request " + request.getRequestId() + " " + request.getHash() + " " + request.getMaxLength());
        currentIndex.set(0);
        Stream<String> permutations = Generator.permutation(request.getAlphabet().getSymbols())
                .withRepetitions(request.getMaxLength()).stream().map(list -> String.join("", list));
        List<String> answer = getAnswer(permutations, request);
        System.out.println(answer);
        CrackHashWorkerResponse crackHashWorkerResponse = createCrackHashWorkerResponse(answer, request);
        sendPossiblePasswords(crackHashWorkerResponse);
    }

    @Bean
    public MessageConverter converter() {
        return new Jackson2JsonMessageConverter();
    }

    private void sendPossiblePasswords(CrackHashWorkerResponse crackHashWorkerResponse) {
  //      String soapRequest = getXmlMessage(crackHashWorkerResponse);
   //     HttpHeaders headers = new HttpHeaders();
  //      headers.setContentType(MediaType.TEXT_XML);

       // HttpEntity<String> requestEntity = new HttpEntity<>(soapRequest, headers);

        System.out.println("do rabbit");
        amqpTemplate.convertAndSend(exchangeName, "task.manager", crackHashWorkerResponse);
        //RestTemplate restTemplate = new RestTemplate();
       // restTemplate.exchange(
      //          urlPossiblePassword,
      //          HttpMethod.POST,
    //            requestEntity,
     //           String.class
    //    );
        System.out.println("posle rabbit");
    }

    private String getXmlMessage(CrackHashWorkerResponse crackHashWorkerResponse) {
        return  getEnvelopeBegin() +
                getHeader() +
                getBody(crackHashWorkerResponse) +
                getEnvelopeEnd();
    }

    private String getEnvelopeBegin() {
        return "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                "xmlns:crac=\"http://ccfit.nsu.ru/schema/crack-hash-response\">";
    }

    private String getEnvelopeEnd() {
        return "</soapenv:Envelope>";
    }

    private String getHeader() {
        return "<soapenv:Header/>";
    }

    private String getBody(CrackHashWorkerResponse crackHashWorkerResponse) {
        StringBuilder answerPart = new StringBuilder();
        answerPart.append("<Answers>");
        for (String word : crackHashWorkerResponse.getAnswers().getWords()) {
            answerPart.append("<words>").append(word).append("</words>");
        }
        answerPart.append("</Answers>");
        return  "   <soapenv:Body>" +
                "      <crac:CrackHashWorkerResponse>" +
                "         <RequestId>" + crackHashWorkerResponse.getRequestId() + "</RequestId>" +
                "         <PartNumber>" + crackHashWorkerResponse.getPartNumber() + "</PartNumber>" +
                                                answerPart +
                "      </crac:CrackHashWorkerResponse>" +
                "   </soapenv:Body>";
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
