package com.ginoquin.springboot.app.models.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.ginoquin.springboot.app.models.entity.Tema;

public interface ITemaDao extends JpaRepository<Tema, Long> {
	
	@Query("select t from Tema t left join fetch t.ejemplos f where t.nombre like ?1 ")
	public Tema fecthByNombreWithEjemplos(String term);
	
}
