package com.histranger.cartoongenerator.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

@Service
public class StoryService {

    @Autowired
    private RestTemplate restTemplate;

    // 기본 엔드포인트 (기본 URL에 이후 경로 추가)
    private final String azureOpenAIEndpoint = "https://storytoimage9207049316.services.ai.azure.com/models";
    private final String azureDalleEndpoint = "https://Stable-Image-Ultra-hggxd.eastus.models.ai.azure.com";

    // API 호출에 사용할 Bearer 토큰 (각각 다르게 사용)
    private final String openAIBearerToken = "G1OqRQXWFaU69EuUWWFyPNCa8iaZ3EoZ5NDaXFUSz21BzLrE4ueFJQQJ99BDACYeBjFXJ3w3AAAAACOGEuSu";
    private final String dalleBearerToken = "9neetIU1NiMxOdeNJYd9sFu0dvQtmnDF";

    public byte[] processStory(String storyText) throws Exception {
        // 1. Azure OpenAI 호출 → 프롬프트 생성
        String prompt = generatePrompt(storyText);

        // 2. Azure DALL-E3 호출 → 이미지 생성 (흑백)
        byte[] imageBytes = generateImage(prompt);

        // 3. PDF 생성 (이미지를 단일 페이지에 삽입)
        return createPdfWithImage(imageBytes);
    }

    private String generatePrompt(String storyText) {
        // 최종 URL에 경로와 쿼리 파라미터 추가
        String url = azureOpenAIEndpoint + "/chat/completions?api-version=2024-05-01-preview";

        // 요청 페이로드 구성
        Map<String, String> requestPayload = new HashMap<>();
        requestPayload.put("story", storyText);

        // HTTP 헤더 구성
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + openAIBearerToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("extra-parameters", "pass-through");

        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestPayload, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(url, requestEntity, Map.class);
        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            return (String) response.getBody().get("prompt");
        }
        return "Default prompt based on story: " + storyText;
    }

    private byte[] generateImage(String prompt) {
        String url = azureDalleEndpoint + "/chat/completions?api-version=2024-05-01-preview";

        // 요청 페이로드 구성
        Map<String, String> requestPayload = new HashMap<>();
        requestPayload.put("prompt", prompt);
        requestPayload.put("style", "black-and-white");

        // HTTP 헤더 구성
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + dalleBearerToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("extra-parameters", "pass-through");

        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestPayload, headers);

        ResponseEntity<byte[]> response = restTemplate.postForEntity(url, requestEntity, byte[].class);
        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            return response.getBody();
        }
        return new byte[0];
    }

    private byte[] createPdfWithImage(byte[] imageBytes) throws Exception {
        PDDocument document = new PDDocument();
        PDPage page = new PDPage();
        document.addPage(page);

        PDImageXObject pdImage = PDImageXObject.createFromByteArray(document, imageBytes, "generatedImage");
        PDPageContentStream contentStream = new PDPageContentStream(document, page);
        // 이미지 위치 및 크기는 상황에 맞게 조정
        contentStream.drawImage(pdImage, 50, 400, 500, 300);
        contentStream.close();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        document.save(baos);
        document.close();
        return baos.toByteArray();
    }
}
