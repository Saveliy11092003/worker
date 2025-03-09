package ru.trushkov.worker.service;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import jakarta.xml.bind.DatatypeConverter;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import lombok.RequiredArgsConstructor;
import org.paukov.combinatorics3.Generator;
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

    public void task(CrackHashManagerRequest request) {
        currentRequestId = request.getRequestId();
        currentIndex.set(0);
        Stream<String> permutations = Generator.permutation(request.getAlphabet().getSymbols())
                .withRepetitions(request.getMaxLength()).stream().map(list -> String.join("", list));
        List<String> answer = getAnswer(permutations, request.getPartNumber(), request.getPartCount(), request.getMaxLength(), request.getHash());
        System.out.println(answer);
        CrackHashWorkerResponse crackHashWorkerResponse = createCrackHashWorkerResponse(answer, request.getRequestId(), request.getPartNumber());
        sendPossiblePasswords(crackHashWorkerResponse);
    }

    private void sendPossiblePasswords(CrackHashWorkerResponse crackHashWorkerResponse) {
        String soapRequest = getXmlMessage(crackHashWorkerResponse);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_XML);

        HttpEntity<String> requestEntity = new HttpEntity<>(soapRequest, headers);

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.exchange(
                "http://manager:8080/internal/api/manager/hash/crack/task",
                HttpMethod.POST,
                requestEntity,
                String.class
        );
    }

    private String getXmlMessage(CrackHashWorkerResponse crackHashWorkerResponse) {
        StringBuilder answerPart = new StringBuilder();
        answerPart.append("<Answers>");
        for (String word : crackHashWorkerResponse.getAnswers().getWords()) {
            answerPart.append("<words>").append(word).append("</words>");
        }
        answerPart.append("</Answers>");
        return "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:crac=\"http://ccfit.nsu.ru/schema/crack-hash-response\">" +
                "   <soapenv:Header/>" +
                "   <soapenv:Body>" +
                "      <crac:CrackHashWorkerResponse>" +
                "         <RequestId>" + crackHashWorkerResponse.getRequestId() + "</RequestId>" +
                "         <PartNumber>" + crackHashWorkerResponse.getPartNumber() + "</PartNumber>" +
                                    answerPart +
                "      </crac:CrackHashWorkerResponse>" +
                "   </soapenv:Body>" +
                "</soapenv:Envelope>";
    }

    private CrackHashWorkerResponse createCrackHashWorkerResponse(List<String> answer, String requestId, Integer number) {
        CrackHashWorkerResponse crackHashWorkerResponse = new CrackHashWorkerResponse();
        crackHashWorkerResponse.setRequestId(requestId);
        crackHashWorkerResponse.setPartNumber(number);
        CrackHashWorkerResponse.Answers answers = new CrackHashWorkerResponse.Answers();
        answers.getWords().addAll(answer);
        crackHashWorkerResponse.setAnswers(answers);
        return crackHashWorkerResponse;
    }

    private List<String> getAnswer(Stream<String> permutations, int partNumber, int partCount, long maxLength, String passwordHash) {
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
