package iz.mkao.mirasalon.di

/**
 * Bridges Kotlin object identity to Swift.
 * `AnyScreen` hashing/equality uses the Kotlin `equals`/`hashCode`
 * contract instead of unstable `String(describing:)` of ObjC-exported objects.
 */
object KotlinObjectEquality {
    fun areEqual(a: Any?, b: Any?): Boolean = a == b
    fun hashCode(of: Any?): Int = of?.hashCode() ?: 0
}
