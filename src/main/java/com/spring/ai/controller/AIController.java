package com.spring.ai.controller;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.openai.*;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.ai.openai.audio.speech.SpeechPrompt;
import org.springframework.ai.openai.audio.speech.SpeechResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;


@RestController
public class AIController {

	
	@Autowired
	private OpenAiImageModel aiImageModel;
	
	@Autowired
	private ChatModel chatModel;
	
	@Autowired
	private OpenAiAudioTranscriptionModel aiAudioTranscriptionModel;
	
	@Autowired
	private OpenAiAudioSpeechModel aiAudioSpeechModel;
	
	  private static final String OPENAI_API_URL = "";
	    private static final String OPENAI_API_KEY = ""; // Replace with your API key

	    @GetMapping("/generate-prompt/{prompt}")
	    public ResponseEntity<String> generatePrompt(@PathVariable("prompt") String prompt) {
	        // Prepare headers
	        HttpHeaders headers = new HttpHeaders();
	        headers.setContentType(MediaType.APPLICATION_JSON);
	        headers.set("Authorization", "Bearer " + OPENAI_API_KEY);

	        Map<String, Object> body = new HashMap<>();
	        body.put("model", "gpt-4o-mini"); 
	        body.put("messages", Collections.singletonList(
	                Map.of("role", "user", "content", prompt)
	        ));
	       
	        body.put("temperature", 0.7);
	        body.put("max_tokens", 100);

	        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);

	    
	        RestTemplate restTemplate = new RestTemplate();
	        ResponseEntity<String> response;
	        try {
	            response = restTemplate.postForEntity(OPENAI_API_URL, requestEntity, String.class);
	        } catch (Exception e) {
	            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
	        }

	        // Return the response
	        return ResponseEntity.ok(response.getBody());
	    }
	
	
	
	
//	    @GetMapping("/generate-image/{prompt}")
//	    public String generateImage1(@PathVariable("prompt") String prompt) {
//	        // Prepare headers
//	        HttpHeaders headers = new HttpHeaders();
//	        headers.setContentType(MediaType.APPLICATION_JSON);
//	        headers.set("Authorization", "Bearer " + OPENAI_API_KEY);
//
//	        // Prepare body for the request
//	        Map<String, Object> body = new HashMap<>();
//	        body.put("prompt", prompt);  
//	        body.put("n", 1); 
//	        body.put("size", "1024x1024"); 
//
//	        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);
//
//	        RestTemplate restTemplate = new RestTemplate();
//	        String imageUrl = "";
//	        try {
//	            // Call OpenAI API for image generation
//	            ResponseEntity<String> response = restTemplate.postForEntity(
//	                "https://api.openai.com/v1/images/generations", requestEntity, String.class);
//	            imageUrl = response.getBody();  // Get the URL of the generated image
//	        } catch (Exception e) {
//	            return "Error: " + e.getMessage();
//	        }
//
//	        return imageUrl;
//	    }
	
	
	
	
	@GetMapping("/image/{prompt}")
	public String generateImage(@PathVariable("prompt") String prompt) {
		
	ImageResponse response = 	aiImageModel.call(
				new ImagePrompt(prompt, OpenAiImageOptions.builder()
						.withHeight(1024)
						.withQuality("hd")
						.withWidth(1024)
						.withN(1)	
						.build()));
		
		return response.getResult().getOutput().getUrl();
		
	}
	
	
	
	@GetMapping("/image-to-text")
	public String generateImageToText() {
		String response = ChatClient.create(chatModel).prompt()
				          .user(userSpec -> userSpec.text("Explain what to do you see in that Image")
				        		.media(MimeTypeUtils.IMAGE_JPEG,
				        				  new FileSystemResource("")))
				            .call()
				            .content();
		
		return response;
				        		
	}
	
	@GetMapping("audio-to-text")
	public String generateTranscription() {
		
		OpenAiAudioTranscriptionOptions options = OpenAiAudioTranscriptionOptions.builder()
				             .withLanguage("en")
				             .withResponseFormat(OpenAiAudioApi.TranscriptResponseFormat.TEXT)
				             .withTemperature(0f)
				             .build();
		
		
		AudioTranscriptionPrompt prompt = new AudioTranscriptionPrompt
				        (new FileSystemResource(""),options);
		
		AudioTranscriptionResponse response = aiAudioTranscriptionModel.call(prompt);
		
		return response.getResult().getOutput();
	}
	
	@GetMapping("/text-to-audio/{prompt}")
	public org.springframework.http.ResponseEntity<Resource> generateAudio(@PathVariable("prompt") String prompt) {
		OpenAiAudioSpeechOptions option = OpenAiAudioSpeechOptions.builder()
				                         .withModel("tts-1")
				                         .withSpeed(1.0f)
				                         .withVoice(OpenAiAudioApi.SpeechRequest.Voice.NOVA)
				
				.build();
		
		SpeechPrompt speechPrompt = new SpeechPrompt(prompt,option);
		
		SpeechResponse response =  aiAudioSpeechModel.call(speechPrompt);
		byte[] responseByte  = response.getResult().getOutput();
		
		ByteArrayResource byteArrayResource = new ByteArrayResource(responseByte);
		
		  return ResponseEntity.ok()
	                .contentType(MediaType.APPLICATION_OCTET_STREAM)
	                .contentLength(byteArrayResource.contentLength())
	                .header(HttpHeaders.CONTENT_DISPOSITION,
	                        ContentDisposition.attachment()
	                                .filename("whatever.mp3")
	                                .build().toString())
	                .body(byteArrayResource);
	}
	
}
