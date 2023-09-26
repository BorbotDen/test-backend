package mobi.sevenwinds.app.author

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.IntIdTable

object AuthorTable : IntIdTable("author") {
    val name = varchar("name", 100).nullable()
    val createdDt = datetime("created_dt")
}

class AuthorEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<AuthorEntity>(AuthorTable)

    var name by AuthorTable.name
    var createdDt by AuthorTable.createdDt

    fun toResponse(): AuthorResponse {
        return AuthorResponse(name?:"", createdDt)
    }
}

