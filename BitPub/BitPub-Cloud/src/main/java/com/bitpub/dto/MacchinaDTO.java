package com.bitpub.dto;

import lombok.Data;

/**
 * DTO che rappresenta lo stato fisico e di rete di un tavolo da gioco.
 */
@Data
public class MacchinaDTO {
    private Long id;
    private String nome;
    private String tipoGioco;
    private boolean attiva;             // True = Edge connesso al Broker
    private boolean attuatoreSbloccato; // True = Palline fisicamente libere
}