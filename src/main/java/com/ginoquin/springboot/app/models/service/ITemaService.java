package com.ginoquin.springboot.app.models.service;

import java.util.List;

import com.ginoquin.springboot.app.models.entity.Tema;

public interface ITemaService {
	
	public List<Tema> findAll();
	public Tema findOne(Long id);
	
	public Tema fetchByNombreWithEjemplos(String term);
	
	public Tema fetchByNombreAndNivelWithEjemplos(String term, Integer nivel);
	
	public List<Tema> findByNivel(Integer nivel);
	
	public Tema fetchByNombreWithEjercicios(String term);
	
	public Tema fetchByNombreAndNivelWithEjercicios(String term, Integer nivel);

}
