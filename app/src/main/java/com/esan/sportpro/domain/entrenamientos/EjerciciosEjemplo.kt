package com.esan.sportpro.domain.entrenamientos

/**
 * Ejercicios de ejemplo, uno por categoría técnica, para empezar la biblioteca (o completarla
 * en una demostración). Cumplen las reglas de [ValidacionBiblioteca]; se crean como cualquier
 * ejercicio del club y luego se pueden editar o desactivar.
 */
object EjerciciosEjemplo {

    data class Plantilla(
        val nombre: String,
        val categoria: CategoriaTecnica,
        val duracionMin: Int,
        val minJugadores: Int,
        val objetivo: String,
        val materiales: String,
        val descripcion: String,
    )

    val plantillas: List<Plantilla> = listOf(
        Plantilla(
            "Rondo de activación 5 vs 2", CategoriaTecnica.CALENTAMIENTO, 12, 7,
            "Activar el cuerpo y la circulación del balón",
            "Balones, 4 conos",
            "En un cuadrado de 10 x 10 metros, cinco jugadores se pasan el balón a uno o dos toques mientras dos intentan recuperarlo. Quien pierde el balón entra al medio.",
        ),
        Plantilla(
            "Conducción en zigzag", CategoriaTecnica.TECNICA_INDIVIDUAL, 10, 1,
            "Mejorar el control del balón en velocidad",
            "Balones, 8 conos",
            "Cada jugador conduce el balón entre una fila de conos separados un metro, usando ambos perfiles. Al final acelera cinco metros y regresa trotando.",
        ),
        Plantilla(
            "Pase y control en triángulo", CategoriaTecnica.PASE_Y_CONTROL, 15, 3,
            "Precisión en el pase y orientación del control",
            "Balones, 3 conos",
            "Tres jugadores forman un triángulo de 8 metros. Pasan a ras del suelo y controlan orientando hacia el siguiente compañero. Cada dos minutos se cambia de sentido.",
        ),
        Plantilla(
            "Remate tras pared", CategoriaTecnica.DEFINICION, 20, 4,
            "Definir con pocos toques frente al arco",
            "Balones, arco, 2 conos",
            "El delantero hace una pared con un compañero en la puerta del área, recibe de frente y remata al primer o segundo toque. Se alterna el pie de remate en cada serie.",
        ),
        Plantilla(
            "Ataque 3 contra 2", CategoriaTecnica.TACTICA_OFENSIVA, 20, 6,
            "Generar superioridad y terminar la jugada",
            "Balones, arco, petos",
            "Tres atacantes salen desde mitad de cancha contra dos defensores. Tienen 10 segundos para rematar. Se busca abrir el campo y atacar el espacio libre.",
        ),
        Plantilla(
            "Basculación defensiva", CategoriaTecnica.TACTICA_DEFENSIVA, 18, 8,
            "Moverse en bloque y cerrar espacios",
            "Balones, petos, 6 conos",
            "Una línea de cuatro defensores se desplaza según el lado donde está el balón, que los atacantes mueven de banda a banda. El entrenador corrige las distancias entre ellos.",
        ),
        Plantilla(
            "Circuito de potencia", CategoriaTecnica.FISICO, 15, 1,
            "Fuerza explosiva del tren inferior",
            "Vallas bajas, escalera de coordinación",
            "Tres estaciones de 40 segundos: saltos de vallas, escalera de coordinación y sprint de diez metros. Un minuto de pausa entre vueltas; se hacen tres vueltas.",
        ),
        Plantilla(
            "Blocaje y caída lateral", CategoriaTecnica.ARQUEROS, 15, 2,
            "Técnica de atajada a media altura",
            "Balones, guantes, colchoneta",
            "Un compañero lanza el balón a media altura a cada lado. El arquero da un paso lateral, se lanza, bloca con ambas manos y se levanta rápido para la siguiente.",
        ),
        Plantilla(
            "Estiramientos y respiración", CategoriaTecnica.VUELTA_A_LA_CALMA, 10, 1,
            "Bajar pulsaciones y recuperar la musculatura",
            "Ninguno",
            "Trote suave de dos minutos y luego estiramientos de cuádriceps, isquiotibiales y gemelos, 30 segundos por lado, con respiración lenta y profunda.",
        ),
    )

    /** Plantillas cuya categoría todavía no tiene ningún ejercicio activo en la biblioteca. */
    fun faltantes(biblioteca: List<EjercicioBiblioteca>): List<Plantilla> {
        val categorias = biblioteca.filter { it.activo }.mapNotNull { it.categoriaTecnica }.toSet()
        val nombres = biblioteca.map { it.nombre.trim().lowercase() }.toSet()
        return plantillas.filter { it.categoria !in categorias && it.nombre.lowercase() !in nombres }
    }
}
