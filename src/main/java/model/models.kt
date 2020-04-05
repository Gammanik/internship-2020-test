package model

import java.util.*
import kotlin.collections.ArrayList

data class Point(val latitude: Float, val longitude: Float) {
    override fun toString(): String {
        return "${latitude},${longitude}"
    }
}
data class Participants(val passengers: Collection<Person>, val drivers: ArrayList<Person>)
data class Person(val id: UUID, val finishPoint: Point)

data class DriverInfoDuration(val text: String, val value: Double)
data class DriverInfo(val distance: Any, val duration: DriverInfoDuration)

data class ElementsModel(val elements: ArrayList<DriverInfo>)

data class DistanceMatrix(
        val destination_addresses: ArrayList<String>,
        val origin_addresses: ArrayList<String>,
        val rows: ArrayList<ElementsModel>,
        val status: String)