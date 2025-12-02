package Entities;

import java.time.LocalDateTime;

public class Evidencia {
    public int id;
    public String link;
    public LocalDateTime CriadoEm;

    public Evidencia(){};

    public Evidencia(String link) {
        this.link = link;
    }
}
