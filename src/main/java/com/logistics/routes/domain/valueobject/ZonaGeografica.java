package com.logistics.routes.domain.valueobject;

import ch.hsr.geohash.GeoHash;
import com.logistics.routes.domain.exception.ZonaGeograficaInvalidaException;

public record ZonaGeografica(String hash) {

    public ZonaGeografica {
        if (hash == null) {
            throw new ZonaGeograficaInvalidaException("El hash de zona geográfica no puede ser nulo");
        }
        if (hash.length() != 4) {
            throw new ZonaGeograficaInvalidaException(
                "El hash de zona geográfica debe tener exactamente 4 caracteres, pero tiene: " + hash.length()
            );
        }
    }

    public static ZonaGeografica from(double lat, double lon) {
        String hash = GeoHash.geoHashStringWithCharacterPrecision(lat, lon, 4);
        return new ZonaGeografica(hash);
    }
}
