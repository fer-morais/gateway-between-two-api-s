package com.morais.fernanda.repository;

import com.morais.fernanda.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PedidoRepository extends JpaRepository<Pedido, Long > {
}