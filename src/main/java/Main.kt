import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.json.GsonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.get
import kotlinx.coroutines.*
import model.*
import java.nio.file.*
import java.util.*
import kotlin.collections.ArrayList

object CONST {
    val API_KEY: String = Files.readString(Path.of("src/main/resources/API_KEY.txt"))
    const val MAPS_BASE_URL: String = "https://maps.googleapis.com/maps/api/distancematrix/json"
    val client = HttpClient(Apache) {
        install(JsonFeature) {
            serializer = GsonSerializer()
        }
    }
}

fun main() {
    val (passengers, drivers) = readPoints()
    val sortedDriversResponse = passengers.map { suggestDriversAsync(it, drivers) }

    runBlocking {
        passengers.zip(sortedDriversResponse.awaitAll()).forEach { passToDrivers ->
            println("Passenger point: ${passToDrivers.first.finishPoint}")
            passToDrivers.second.forEach { println("${it.finishPoint}") }
        }
    }
}

fun suggestDriversAsync(passenger: Person, drivers: ArrayList<Person>): Deferred<Collection<Person>> =
    GlobalScope.async { getDistancesAsync(passenger, drivers).await().rows[0].elements
                .mapIndexed { i, el -> Pair(el.duration.value, drivers[i]) }
                .sortedBy { it.first }
                .map { it.second }
    }

suspend fun getDistancesAsync(passenger: Person, drivers: Collection<Person>): Deferred<DistanceMatrix> =
    GlobalScope.async(Dispatchers.IO) {
        val driversCoords: String = drivers.joinToString(separator = "%7C")
            { "${it.finishPoint.latitude}%2C${it.finishPoint.longitude}" }

        val urlStr = "${CONST.MAPS_BASE_URL}?origins=${passenger.finishPoint}" +
            "&destinations=${driversCoords}&key=${CONST.API_KEY}"

        CONST.client.get<DistanceMatrix>(urlStr)
    }

private fun readPoints(): Participants {
    val pathToResource = Paths.get(Point::class.java.getResource("../latlons").toURI())
    val allPoints = Files.readAllLines(pathToResource).map { asPoint(it) }.shuffled()
    val passengers = allPoints.slice(0..9).map { Person(UUID.randomUUID(), it) }
    val drivers = ArrayList(allPoints.slice(10..19).map { Person(UUID.randomUUID(), it) })
    return Participants(passengers, drivers)
}

private fun asPoint(it: String): Point {
    val (lat, lon) = it.split(", ")
    return Point(lat.toFloat(), lon.toFloat())
}
