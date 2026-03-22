package com.mediclear.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mediclear.backend.dto.AnalysisResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.*;

@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    private final WebClient webClient;
    private final ObjectMapper mapper;

    public GeminiService() {
        // AI Studio Endpoint
        this.webClient = WebClient.create("https://generativelanguage.googleapis.com");
        this.mapper = new ObjectMapper();
        this.mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public AnalysisResult analyze(List<Map<String, Object>> parts) throws Exception {
        return analyzeWithRetry(parts, 0);
    }

    private AnalysisResult analyzeWithRetry(List<Map<String, Object>> parts, int attempt) throws Exception {
        // Universal Schema Fallback: Prepend system prompt to user parts for legacy compatibility
        List<Map<String, Object>> optimizedParts = new ArrayList<>();
        optimizedParts.add(Map.of("text", "SYSTEM DIRECTIVE: " + getSystemPrompt()));
        optimizedParts.addAll(parts);

        Map<String, Object> content = Map.of("role", "user", "parts", optimizedParts);
        
        // Remove beta fields that cause 400 errors for non-whitelisted projects
        Map<String, Object> generationConfig = Map.of(
            "temperature", 0.3,
            "maxOutputTokens", 2048
        );

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(content),
                "generationConfig", generationConfig
        );

        // v1beta required for systemInstruction
        // gemini-1.5-flash-latest is the definitive stable alias in v1beta
        String uri = "/v1beta/models/gemini-1.5-flash-latest:generateContent?key=" + apiKey;
        
        System.out.println("[GeminiService] Calling AI Studio: v1beta (Attempt: " + (attempt + 1) + ")");

        try {
            Map<String, Object> response = webClient.post()
                    .uri(uri)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(java.time.Duration.ofSeconds(30))
                    .block();

            if (response == null || !response.containsKey("candidates")) {
                throw new RuntimeException("Invalid response from Gemini API");
            }

            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            Map<String, Object> contentBlock = (Map<String, Object>) candidates.get(0).get("content");
            List<Map<String, Object>> responseParts = (List<Map<String, Object>>) contentBlock.get("parts");
            String jsonText = (String) responseParts.get(0).get("text");

            // Safe parse markdown wraps
            if (jsonText.startsWith("```json")) {
                jsonText = jsonText.substring(7, jsonText.length() - 3).trim();
            } else if (jsonText.startsWith("```")) {
                jsonText = jsonText.substring(3, jsonText.length() - 3).trim();
            }

            return mapper.readValue(jsonText, AnalysisResult.class);

        } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
            if (e.getStatusCode().value() == 429 && attempt < 2) {
                System.out.println("[GeminiService] Rate limited (429). Retrying in 2 seconds...");
                Thread.sleep(2000);
                return analyzeWithRetry(parts, attempt + 1);
            }
            System.err.println("[GeminiService] API Error: " + e.getResponseBodyAsString());
            throw new RuntimeException("Gemini returned " + e.getStatusCode() + ": " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            System.err.println("[GeminiService] Error: " + e.getMessage());
            throw e;
        }
    }

    private String getSystemPrompt() {
        return "You are the core intelligence engine for MediClear, a real-time emergency pre-authorization and hospital orchestration system. Process all inputs and produce a battle-ready JSON payload.\n\n" +
               "INPUTS: Voice/text transcript + images of medical documents, pill bottles, handwritten notes.\n\n" +
               "INSTRUCTIONS: 1. SYNTHESIZE all inputs 2. PREDICT trajectory 3. ESI TRIAGE 1-5 4. ICD/CPT CODES 5. HOSPITAL RESOURCES 6. PRE-AUTHORIZE 7. FAMILY ALERT 8. FLAG GAPS.\n\n" +
               "CONSTRAINTS: Output ONLY valid JSON. No markdown. No backticks.\n\n" +
               "OUTPUT SCHEMA: {\n" +
               "  \"patient_context\": { \"chief_complaint\": \"string\", \"estimated_age\": 0, \"extracted_medications\": [\"array\"], \"identified_allergies\": [\"array\"], \"identified_risks\": [\"array\"], \"communication_barrier\": false, \"language\": \"string\" },\n" +
               "  \"clinical_assessment\": { \"clinical_reasoning\": \"string\", \"esi_triage_level\": \"Level 1-5\", \"deterioration_risk\": { \"likelihood\": \"HIGH/MED/LOW\", \"predicted_trajectory\": \"string\", \"window_minutes\": 0 }, \"missing_critical_data\": [\"array\"] },\n" +
               "  \"administrative_routing\": { \"insurance_carrier_detected\": \"string\", \"pre_authorization_required\": false, \"anticipated_icd_10_codes\": [{\"code\": \"string\", \"description\": \"string\", \"verified\": false}], \"anticipated_cpt_codes\": [{\"code\": \"string\", \"description\": \"string\", \"verified\": false, \"priority\": \"string\"}], \"confidence_score_overall\": 0 },\n" +
               "  \"hospital_orchestration\": { \"resources_to_activate\": [\"array\"], \"bed_type_required\": \"string\", \"specialist_required\": \"string\", \"primary_action_directive\": \"ALL-CAPS\" },\n" +
               "  \"family_communication\": { \"alert_message\": \"string\", \"next_of_kin_detected\": \"string\", \"consent_status\": \"string\" }\n" +
               "}";
    }
}
