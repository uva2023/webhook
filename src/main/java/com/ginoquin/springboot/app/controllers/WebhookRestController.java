package com.ginoquin.springboot.app.controllers;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.ginoquin.springboot.app.models.entity.Ejemplo;
import com.ginoquin.springboot.app.models.entity.Ejercicio;
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
	
	private final Map<String, Integer> nivelUsuario = new HashMap<>();
	private String chatId = null;
	
	private Ejercicio ejercicio = null;

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
		String intentPreguntarNivelMusica = "PreguntarNivelMusica  Intent";
		String intentResponderNivelMusica = "ResponderNivelMusica Intent";
		String intentPreguntaTeoria = "PreguntarTeoriaMusica Intent";
		String intentPreguntarEjemplo = "PreguntarEjemploMusica Intent";
		String intentPruebaWebhook = "PruebaWebhook Intent";
		
		String intentPreguntarEjercicioMusica = "PreguntarEjercicioMusica Intent";
		String intentResponderEjercicioMusica = "ResponderEjercicioMusica Intent";
		
		GoogleCloudDialogflowV2QueryResult queryResult = request.getQueryResult(); 
	
		GoogleCloudDialogflowV2Intent intent = queryResult.getIntent();
		String diplayName = intent.getDisplayName();			
		Map<String, Object> parametros = queryResult.getParameters();
		
		this.chatId = request.getSession().split("/")[4]; //obtenemos el id de la sessión
		
		String responseText = "";
		
		if(intentPreguntarNivelMusica.equals(diplayName)) {
			if(!nivelUsuario.containsKey(this.chatId)) {
				Integer nivel = this.obtenerNivel( (String) parametros.get("nivel_musical"));
				this.nivelUsuario.put(this.chatId, nivel);
			}			
		    responseText = this.procesaAprenderMusica(); 
		} else if(intentResponderNivelMusica.equals(diplayName) || intentPreguntaTeoria.equals(diplayName)) {
			responseText = procesaTeoria(parametros);
		} else if(intentPreguntarEjemplo.equals(diplayName)) {
			responseText = this.procesaEjemplo(parametros);
			
		} else if(intentPreguntarEjercicioMusica.equals(diplayName)) {
			responseText = this.procesaEjercicioMusica(parametros);
			
		} else if(intentResponderEjercicioMusica.equals(diplayName)) {
			responseText = this.procesaRespuestaEjercicioMusica(parametros);
			
		} else if(intentPruebaWebhook.equals(diplayName)) {
			responseText = "Bienvenido al Webhook con Spring Boot.";
			
		} else {
			responseText = "Lo siento, no entendí lo que has querido decir.";
			
		}
		
		return responseText.concat("\n¿Necesitas más información? Estoy aquí para ayudarte en lo que necesites.");
	}
	
	private String procesaEjercicioMusica(Map<String, Object> parametros) {
		String tema_interes = (String) parametros.get("tema_interes");
		String responseText = "";
		Tema tema = null;
		if(StringUtils.isNotBlank(tema_interes)) {
			final Integer nivel = this.nivelUsuario.get(chatId);
			boolean tieneNivel = Objects.nonNull(nivel);
			if(tieneNivel) {
				tema = this.temaService.fetchByNombreAndNivelWithEjercicios(tema_interes, nivel);
			} else {
				tema = this.temaService.fetchByNombreWithEjercicios(tema_interes);
			}
					
			if(Objects.nonNull(tema)) {
				List<Ejercicio> ejercicios = tema.getEjercicios();
				Collections.shuffle(ejercicios);
				Ejercicio ejercicio = ejercicios.stream().findAny().orElse(null);
				if(Objects.nonNull(ejercicio)) {
					this.ejercicio = ejercicio; 
					String ejercicioEnunciado =  Objects.isNull(ejercicio) ? "No disponible" : ejercicio.getEnunciado();
					String enlace = StringUtils.isBlank(ejercicio.getEnlace()) ? "No disponible" : ejercicio.getEnlace();
					responseText = ejercicioEnunciado.concat("\nAquí tienes una pista: ").concat(enlace);
				} else {
					responseText = "Lo siento, aún no estan disponibles ejercicios de ese tema, puedes probar con otro tema de teoría musical.";
				}				
				
			} else {
				responseText = "Lo siento, puedes intentar con otro tema de teoría musical.";
				if(tieneNivel) {
					responseText = responseText + "Tu nivel es "+ this.obtenerNivelString(nivel) + ", deberías empezar por otro tema. Por ejemplo: " + this.temasPorNivel();
					
				}
				
			}
		}
		return responseText;
	}
	
	private String procesaRespuestaEjercicioMusica(Map<String, Object> parametros) {
		//se supone que son obligatorios..
		String tema_interes = (String) parametros.get("tema_interes");
		String nota_musical = (String) parametros.get("nota_musical");
		String tipo_escala = (String) parametros.get("tipo_escala");
		
		StringBuilder responseText = new StringBuilder(64);
		//solo para escalas...
		if(tema_interes.equals(this.ejercicio.getTema().getNombre()) 
				&& nota_musical.equals(this.ejercicio.getNota())
				&& tipo_escala.equals(this.ejercicio.getTipoEscala())) {
			responseText.append("¡Genial! has acertado, sigue así.");	
		} else {
			responseText.append("¡Incorrecto! Has fallado, pero tienes otra oportunidad.");	
		}
		
		return responseText.toString();
	}
	
	private String procesaAprenderMusica() {
		StringBuilder responseText = new StringBuilder(64);
		responseText.append("¡Entendido! Gracias por compartir tu nivel musical. ¿Hay algo que te apasione en particular? Me encantaría ayudarte a aprender más sobre ello.");
		responseText.append(" Aquí tienes algunos temas por donde empezar a aprender: ");
		
		responseText.append(this.temasPorNivel());
		return responseText.toString();
	}
	
	private String procesaTeoria(Map<String, Object> parametros) {
		String tema_interes = (String) parametros.get("tema_interes");
		String responseText = "";
		Tema tema = null;
		if(StringUtils.isNotBlank(tema_interes)) {
			final Integer nivel = this.nivelUsuario.get(chatId);
			boolean tieneNivel = Objects.nonNull(nivel);
			if(tieneNivel) {
				tema = temaService.fetchByNombreAndNivelWithEjemplos(tema_interes, nivel);
			} else {
				tema = temaService.fetchByNombreWithEjemplos(tema_interes);
			}
					
			if(Objects.nonNull(tema)) {
				String enlace = StringUtils.isBlank(tema.getEnlace()) ? "No disponible" : tema.getEnlace();
				List<Ejemplo> ejemplos = tema.getEjemplos();
				Ejemplo ejemplo = ejemplos.stream().findAny().orElse(null);
				String ejemploDescrip =  Objects.isNull(ejemplo) ? "No disponible" : ejemplo.getDescripcion();
				responseText = tema.getContenido().concat("\nEjemplo: ").concat(ejemploDescrip).concat("\nEnlace externo: ").concat(enlace);
				
			} else {
				responseText = "Lo siento, puedes intentar con otro tema de teoría musical.";
				if(tieneNivel) {
					responseText = responseText + "Tu nivel es "+ this.obtenerNivelString(nivel) + ", deberías empezar por otro tema. Por ejemplo: " + this.temasPorNivel();
					
				}
				
			}
		}
		return responseText;
	}
	
	private String procesaEjemplo(Map<String, Object> parametros) {
		String tema_interes = (String) parametros.get("tema_interes");
		String nota_musical = (String) parametros.get("nota_musical");
		String tipo_escala = (String) parametros.get("tipo_escala");
		
		String responseText = "";
		Tema tema = null;
		if(StringUtils.isNotBlank(tema_interes)) {
			final Integer nivel = this.nivelUsuario.get(chatId);
			boolean tieneNivel = Objects.nonNull(nivel);
			if(tieneNivel) {
				tema = temaService.fetchByNombreAndNivelWithEjemplos(tema_interes, nivel);
			} else {
				tema = temaService.fetchByNombreWithEjemplos(tema_interes);
			}
				
			if(Objects.nonNull(tema)) {
				List<Ejemplo> ejemplos = tema.getEjemplos();
				Ejemplo ejemplo = null;
				
				if(StringUtils.isBlank(nota_musical) && StringUtils.isBlank(tipo_escala)) {
					Collections.shuffle(ejemplos);
					ejemplo = ejemplos.stream().findAny().orElse(null);
				} else if (StringUtils.isNotBlank(nota_musical) && StringUtils.isNotBlank(tipo_escala)) {					
					ejemplo = ejemplos.stream().filter(e -> (e.getNota().equals(nota_musical) && e.getTipoEscala().equals(tipo_escala))).findAny().orElse(null);
					
				} else {
					ejemplo = ejemplos.stream().findAny().orElse(null);
					
				}

				if(Objects.nonNull(ejemplo)) {
					String enlace = StringUtils.isBlank(ejemplo.getEnlace()) ? "No disponible" : ejemplo.getEnlace();					
					responseText = ejemplo.getDescripcion().concat("\nEnlace externo: ").concat(enlace);
					
				} else {
					responseText = "Lo siento, puedes intentar con otro tema de teoría musical.";
					
				}
				
			} else {
				responseText = "Lo siento, puedes intentar con otro tema de teoría musical.";
				if(tieneNivel) {
					responseText = responseText + "Tu nivel es "+ this.obtenerNivelString(nivel) + ", deberías empezar por otro tema. Por ejemplo: " + this.temasPorNivel();
					
				}
				
			}
		}
		
		return responseText;
	}
	
	private String temasPorNivel() {
		StringJoiner joiner = new StringJoiner(", ");
		final List<Tema> temasNivel = this.temaService.findByNivel(this.nivelUsuario.get(this.chatId));
		
		temasNivel.forEach(t -> {
			
			if(Objects.nonNull(t)) {
				joiner.add("Tema sobre " +t.getNombre());
			
			}			
		});
		return joiner.toString();
	}
	
	private Integer obtenerNivel(String nivelString) {
		if(StringUtils.isBlank(nivelString)) {
			return null;
		}
		
		switch (nivelString) {
		case "Principiante": {
			return 0;
		} case "Intermedio": {
			return 1;
		} case "Avanzado": {
			return 2;
		}
		default:
			return null;
		}
		
	}
	
	private String obtenerNivelString(Integer nivel) {
		if(Objects.isNull(nivel)) {
			return null;
		}
		
		switch (nivel.intValue()) {
		case 0: {
			return "Principiante";
		} case 1: {
			return "Intermedio";
		} case 2: {
			return "Avanzado";
		}
		default:
			return null;
		}
		
	}

}
