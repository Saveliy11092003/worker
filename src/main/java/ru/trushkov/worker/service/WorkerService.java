package ru.trushkov.worker.service;

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

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkerService {

    public void task(CrackHashManagerRequest request) {
        System.out.println("alphabet " + request.getAlphabet());
        List<String> permutations = Generator.permutation(request.getAlphabet().getSymbols())
                .withRepetitions(request.getMaxLength()).stream().map(list -> String.join("", list)).toList();
        List<String> partPermutations = getPartPermutations(permutations, request.getPartNumber(), request.getPartCount());
        System.out.println(partPermutations);
        List<String> answer = getPossiblePasswords(request.getHash() , partPermutations);
        CrackHashWorkerResponse crackHashWorkerResponse = createCrackHashWorkerResponse(answer, request.getRequestId(), request.getPartNumber());
        sendPossiblePasswords(crackHashWorkerResponse);
    }

    private List<String> getPossiblePasswords(String hash, List<String> permutations) {
        List<String> answer = new ArrayList<>();
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            for (String permutation : permutations) {
                byte[] messageDigest = md.digest(permutation.getBytes(StandardCharsets.UTF_8));
                String pHash = DatatypeConverter.printHexBinary(messageDigest).toUpperCase();
                if (hash.equalsIgnoreCase(pHash)) {
                    answer.add(permutation);
                }
            }
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        System.out.println(answer);
        return answer;
    }

    private void sendPossiblePasswords(CrackHashWorkerResponse crackHashWorkerResponse) {
        // String xmlContent = convertToXml(crackHashWorkerResponse);
     /*   String soapRequest = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                + "xmlns:web=\"http://ccfit.nsu.ru/schema/crack-hash-response\">"
                + "<soapenv:Header/>"
                + "<soapenv:Body>"
                + "<web:CrackHashWorkerResponse>" + xmlContent + "</web:CrackHashWorkerResponse>"
                + "</soapenv:Body>"
                + "</soapenv:Envelope>";*/

   //     HttpHeaders headers = new HttpHeaders();
  //      headers.set("Content-Type", "text/xml");
   //     HttpEntity<String> entity = new HttpEntity<>(xmlContent, headers);

  /*      HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "text/xml");

        RestTemplate restTemplate = new RestTemplate();

        HttpEntity<CrackHashWorkerResponse> entity = new HttpEntity<>(crackHashWorkerResponse, headers);
        ResponseEntity<String> response = restTemplate.exchange(
                "http://localhost:8080/internal/api/manager/hash/crack/task",
                HttpMethod.POST,
                entity,
                String.class
        );
        System.out.println("Response: " + response.getBody());*/

        String soapRequest =
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:crac=\"http://ccfit.nsu.ru/schema/crack-hash-response\">" +
                        "   <soapenv:Header/>" +
                        "   <soapenv:Body>" +
                        "      <crac:CrackHashWorkerResponse>" +
                        "         <RequestId>" + crackHashWorkerResponse.getRequestId() + "</RequestId>" +
                        "         <PartNumber>" + crackHashWorkerResponse.getPartNumber() + "</PartNumber>" +
                        "         <Answers>" +
                        "            <words>" + crackHashWorkerResponse.getAnswers().getWords().get(0) + "</words>" +
                        "         </Answers>" +
                        "      </crac:CrackHashWorkerResponse>" +
                        "   </soapenv:Body>" +
                        "</soapenv:Envelope>";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_XML);

        HttpEntity<String> requestEntity = new HttpEntity<>(soapRequest, headers);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.exchange(
                "http://localhost:8080/internal/api/manager/hash/crack/task",
                HttpMethod.POST,
                requestEntity,
                String.class
        );

        System.out.println("Response: " + response.getBody());


    }

    private static String convertToXml(CrackHashWorkerResponse response) {
        String result = null;
        try {
            JAXBContext context = JAXBContext.newInstance(CrackHashWorkerResponse.class);
            Marshaller marshaller = context.createMarshaller();
            StringWriter writer = new StringWriter();
            marshaller.marshal(response, writer);
            result = writer.toString();
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
        return result;
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

    private List<String> getPartPermutations(List<String> permutations, int partNumber, int partCount) {
        int partSize = permutations.size() / 3;
        int start = partNumber * partSize;
        int end = (partNumber == 2) ? permutations.size() : (partNumber+1) * partSize;
        return permutations.subList(start, end);
    }
}
