package com.ginoquin.springboot.app.models.service;

import java.util.List;

import com.ginoquin.springboot.app.models.entity.Tema;

public interface ITemaService {
	
	public List<Tema> findAll();
	public Tema findOne(Long id);
	
	public Tema fecthByIdWithFacturas(String term);

}
