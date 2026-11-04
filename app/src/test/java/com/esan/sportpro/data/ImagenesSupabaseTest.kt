package com.esan.sportpro.data

import com.esan.sportpro.data.imagenes.esImagenDeSupabase
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImagenesSupabaseTest {

    @Test
    fun `reconoce las imagenes del bucket de Supabase`() {
        assertTrue(
            esImagenDeSupabase(
                "https://ghowgsannwmpuwncwrcc.supabase.co/storage/v1/object/public/imagenes/jugadores/uid/foto.jpg",
            ),
        )
    }

    @Test
    fun `ignora las URLs antiguas de Firebase y las vacias`() {
        assertFalse(esImagenDeSupabase("https://firebasestorage.googleapis.com/v0/b/sportpro/o/foto.jpg"))
        assertFalse(esImagenDeSupabase(""))
    }
}
