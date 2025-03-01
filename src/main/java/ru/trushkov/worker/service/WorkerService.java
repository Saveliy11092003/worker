package ru.trushkov.worker.service;

import jakarta.xml.bind.DatatypeConverter;
import org.paukov.combinatorics3.Generator;
import org.springframework.stereotype.Service;
import ru.nsu.ccfit.schema.crack_hash_request.CrackHashManagerRequest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

@Service
public class WorkerService {

    public void task(CrackHashManagerRequest request) {
        System.out.println("alphabet " + request.getAlphabet());
        List<String> permutations = Generator.permutation(request.getAlphabet().getSymbols())
                .withRepetitions(request.getMaxLength()).stream().map(list -> String.join("", list)).toList();
        List<String> partPermutations = getPartPermutations(permutations, request.getPartNumber(), request.getPartCount());
        System.out.println(partPermutations);
        getPossiblePasswords(request.getHash() , partPermutations);
    }

    private void getPossiblePasswords(String hash, List<String> permutations) {
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
    }

    private List<String> getPartPermutations(List<String> permutations, int partNumber, int partCount) {
        int partSize = permutations.size() / 3;
        int start = partNumber * partSize;
        int end = (partNumber == 2) ? permutations.size() : (partNumber+1) * partSize;
        return permutations.subList(start, end);
    }

}
