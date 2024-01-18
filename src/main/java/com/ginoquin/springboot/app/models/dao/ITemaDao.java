package com.ginoquin.springboot.app.models.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.ginoquin.springboot.app.models.entity.Tema;

public interface ITemaDao extends JpaRepository<Tema, Long> {
	
	@Query("select t from Tema t left join fetch t.ejemplos f where t.nombre like ?1 ")
	public Tema fetchByNombreWithEjemplos(String term);

	@Query("select t from Tema t left join fetch t.ejemplos f where t.nombre like ?1 and t.nivel = ?2")
	public Tema fetchByNombreAndNivelWithEjemplos(String term, Integer nivel);

	@Query("select t from Tema t where t.nivel = ?1")
	public List<Tema> findByNivel(Integer nivel);

	@Query("select t from Tema t left join fetch t.ejercicios f where t.nombre like ?1")
	public Tema fetchByNombreWithEjercicios(String term);

	@Query("select t from Tema t left join fetch t.ejercicios f where t.nombre like ?1 and t.nivel = ?2")
	public Tema fetchByNombreAndNivelWithEjercicios(String term, Integer nivel);
	
}
