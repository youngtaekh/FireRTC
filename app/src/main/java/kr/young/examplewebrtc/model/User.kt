package kr.young.examplewebrtc.model

class User constructor() {
    var documentId: String? = null
    lateinit var id: String
    lateinit var name: String

    constructor(
        id: String,
        name: String
    ): this() {
        this.id = id
        this.name = name
    }

    constructor(documentId: String, data: Map<String, Any>): this() {
        this.documentId = documentId
        this.id = data["id"]!! as String
        this.name = data["name"]!! as String
    }

    fun toMap(): HashMap<String, String> {
        return hashMapOf(
            "id" to id,
            "name" to name
        )
    }

    companion object {
        const val COLLECTION = "users"
    }
}
