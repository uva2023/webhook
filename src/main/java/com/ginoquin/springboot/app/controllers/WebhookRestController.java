package com.ginoquin.springboot.app.controllers;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.ginoquin.springboot.app.models.entity.Ejemplo;
import com.ginoquin.springboot.app.models.entity.Tema;
import com.ginoquin.springboot.app.models.service.ITemaService;
import com.google.api.client.json.JsonGenerator;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.dialogflow.v3.model.GoogleCloudDialogflowV2Intent;
import com.google.api.services.dialogflow.v3.model.GoogleCloudDialogflowV2IntentMessage;
import com.google.api.services.dialogflow.v3.model.GoogleCloudDialogflowV2IntentMessageText;
import com.google.api.services.dialogflow.v3.model.GoogleCloudDialogflowV2QueryResult;
import com.google.api.services.dialogflow.v3.model.GoogleCloudDialogflowV2WebhookRequest;
import com.google.api.services.dialogflow.v3.model.GoogleCloudDialogflowV2WebhookResponse;

import io.micrometer.common.util.StringUtils;

@RestController
public class WebhookRestController {

	@Autowired
	private GsonFactory gsonFactory;
	
	@Autowired
	private ITemaService temaService;

	private Logger log = LoggerFactory.getLogger(getClass());

	@PostMapping(value = { "/Respuesta" }, produces = { MediaType.APPLICATION_JSON_VALUE })
	public String service(@RequestBody String rawData) {
		try {
			log.info("Petición recibida: " + rawData);
			GoogleCloudDialogflowV2WebhookRequest request = gsonFactory.createJsonParser(rawData)
					.parse(GoogleCloudDialogflowV2WebhookRequest.class);

			// Step 2. Process the request
			String responseText = this.procesarRequest(request);
			
			// Step 3. Build the response message
			GoogleCloudDialogflowV2IntentMessage msg = new GoogleCloudDialogflowV2IntentMessage();
			GoogleCloudDialogflowV2IntentMessageText text = new GoogleCloudDialogflowV2IntentMessageText();
			text.setText(Arrays.asList(responseText));
			msg.setText(text);

			GoogleCloudDialogflowV2WebhookResponse response = new GoogleCloudDialogflowV2WebhookResponse();
			response.setFulfillmentMessages(Arrays.asList(msg));
			StringWriter stringWriter = new StringWriter();
			JsonGenerator jsonGenerator = gsonFactory.createJsonGenerator(stringWriter);
			jsonGenerator.enablePrettyPrint();
			jsonGenerator.serialize(response);
			jsonGenerator.flush();
			return stringWriter.toString();

		} catch (IOException e) {
			e.printStackTrace();
			log.error("Error en WebhookRestController.service ", e);
			return "Error manejando el intent - " + e.getMessage();
		}
	}
	
	private String procesarRequest(GoogleCloudDialogflowV2WebhookRequest request) {
		String intentResponderMusica = "ResponderNivelMusica Intent";
		String intentPruebaWebhook = "PruebaWebhook Intent";
		String intentPreguntaTeoria = "PreguntarTeoriaMusica Intent";
		String intentPreguntarEjemplo = "PreguntarEjemploMusica Intent";
		
		GoogleCloudDialogflowV2QueryResult queryResult = request.getQueryResult(); 
	
		GoogleCloudDialogflowV2Intent intent = queryResult.getIntent();
		String diplayName = intent.getDisplayName();			
		Map<String, Object> parametros = queryResult.getParameters();
		
		String responseText = "";
		if(intentResponderMusica.equals(diplayName) || intentPreguntaTeoria.equals(diplayName)) {
			responseText = procesaTeoria(parametros);
			
		} else if(intentPreguntarEjemplo.equals(diplayName)) {
			responseText = this.procesaEjemplo(parametros);
			
		} else if(intentPruebaWebhook.equals(diplayName)) {
			responseText = "Bienvenido al Webhook con Spring Boot.";
			
		} else {
			responseText = "Lo siento, no entendí eso.";
			
		}
		
		return responseText.concat("\n¿Necesitas más información?");
	}
	
	private String procesaTeoria(Map<String, Object> parametros) {
		String tema_interes = (String) parametros.get("tema_interes");
		String responseText = "";
		if(StringUtils.isNotBlank(tema_interes)) {
			Tema tema = temaService.fecthByIdWithFacturas(tema_interes);		
			if(Objects.nonNull(tema)) {
				String enlace = StringUtils.isBlank(tema.getEnlace()) ? "No disponible" : tema.getEnlace();
				List<Ejemplo> ejemplos = tema.getEjemplos();
				Ejemplo ejemplo = ejemplos.stream().findAny().orElse(null);
				String ejemploDescrip =  Objects.isNull(ejemplo) ? "No disponible" : ejemplo.getDescripcion();
				responseText = tema.getContenido().concat("\nEjemplo: ").concat(ejemploDescrip).concat("\nEnlace externo: ").concat(enlace);
				
			} else {
				responseText = "Lo siento, puedes intentar con otro tema de teoría musical.";
				
			}
		}
		return responseText;
	}
	
	private String procesaEjemplo(Map<String, Object> parametros) {
		String tema_interes = (String) parametros.get("tema_interes");
		String nota_musical = (String) parametros.get("nota_musical");
		String tipo_escala = (String) parametros.get("tipo_escala");
		
		String responseText = "";
		if(StringUtils.isNotBlank(tema_interes)) {
			Tema tema = temaService.fecthByIdWithFacturas(tema_interes);		
			if(Objects.nonNull(tema)) { 
				List<Ejemplo> ejemplos = tema.getEjemplos();
				Ejemplo ejemplo = null;
				if (StringUtils.isNotBlank(nota_musical) && StringUtils.isNotBlank(tipo_escala)) {					
					ejemplo = ejemplos.stream().filter(e -> (e.getNota().equals(nota_musical) && e.getTipoEscala().equals(tipo_escala))).findAny().orElse(null);
					
				} else {
					ejemplo = ejemplos.stream().findAny().orElse(null);
					
				}

				if(Objects.nonNull(ejemplo)) {
					String enlace = StringUtils.isBlank(ejemplo.getEnlace()) ? "No disponible" : ejemplo.getEnlace();					
					responseText = ejemplo.getDescripcion().concat("\nEnlace externo: ").concat(enlace);
					
				} else {
					responseText = "Lo siento, puedes intentar con otra nota musical y otro tipo de escala.";
					
				}
				
			} else {
				responseText = "Lo siento, puedes intentar con otro tema de teoría musical.";
				
			}
		}
		
		return responseText;
	}

}
