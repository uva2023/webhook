package com.ginoquin.springboot.app.models.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ginoquin.springboot.app.models.dao.ITemaDao;
import com.ginoquin.springboot.app.models.entity.Tema;

@Service
public class TemaServiceImpl implements ITemaService {

	@Autowired
	private ITemaDao temaDao;

	@Override
	@Transactional(readOnly = true)
	public List<Tema> findAll() {
		return this.temaDao.findAll();
	}

	@Override
	@Transactional(readOnly = true)
	public Tema findOne(Long id) {
		return this.temaDao.findById(id).orElse(null);
	}

	@Override
	public Tema fetchByNombreWithEjemplos(String term) {
		return this.temaDao.fetchByNombreWithEjemplos(term);
	}

	@Override
	public Tema fetchByNombreAndNivelWithEjemplos(String term, Integer nivel) {
		return this.temaDao.fetchByNombreAndNivelWithEjemplos(term, nivel);
	}

	@Override
	public List<Tema> findByNivel(Integer nivel) {
		return this.temaDao.findByNivel(nivel);
	}

	@Override
	public Tema fetchByNombreWithEjercicios(String term) {
		return this.temaDao.fetchByNombreWithEjercicios(term);
	}

	@Override
	public Tema fetchByNombreAndNivelWithEjercicios(String term, Integer nivel) {
		return this.temaDao.fetchByNombreAndNivelWithEjercicios(term, nivel);
	}
	
}
