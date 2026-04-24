package com.logistics.routes.domain;

import com.logistics.routes.domain.exception.ZonaGeograficaInvalidaException;
import com.logistics.routes.domain.valueobject.ZonaGeografica;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ZonaGeograficaTest {

    @Test
    void from_genera_hash_de_5_caracteres() {
        ZonaGeografica zona = ZonaGeografica.from(4.7110, -74.0721); // Bogotá
        assertNotNull(zona.hash());
        assertEquals(5, zona.hash().length());
    }

    @Test
    void constructor_falla_con_hash_nulo() {
        assertThrows(ZonaGeograficaInvalidaException.class,
                () -> new ZonaGeografica(null));
    }

    @Test
    void constructor_falla_con_hash_de_longitud_incorrecta() {
        assertThrows(ZonaGeograficaInvalidaException.class,
                () -> new ZonaGeografica("abc"));      // 3 chars — muy corto
        assertThrows(ZonaGeograficaInvalidaException.class,
                () -> new ZonaGeografica("abcd"));     // 4 chars — un char menos
        assertThrows(ZonaGeograficaInvalidaException.class,
                () -> new ZonaGeografica("abcdef"));   // 6 chars — un char de más
    }

    @Test
    void dos_zonas_con_mismo_hash_son_iguales() {
        ZonaGeografica z1 = ZonaGeografica.from(4.7110, -74.0721);
        ZonaGeografica z2 = ZonaGeografica.from(4.7110, -74.0721);
        assertEquals(z1, z2);
    }
}
