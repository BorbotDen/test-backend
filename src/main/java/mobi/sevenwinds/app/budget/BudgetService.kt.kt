package mobi.sevenwinds.app.budget

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mobi.sevenwinds.app.author.AuthorEntity
import mobi.sevenwinds.app.author.AuthorTable
import mobi.sevenwinds.app.budget.BudgetTable.leftJoin
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime

object BudgetService {
    suspend fun addRecord(body: BudgetRecord): BudgetRecord = withContext(Dispatchers.IO) {
        transaction {
            if (body.authorId != null && AuthorEntity.findById(body.authorId) == null) {
                AuthorEntity.new {
                    this.name = null
                    this.createdDt = DateTime.now()
                }
            }
            val entity = BudgetEntity.new {
                this.year = body.year
                this.month = body.month
                this.amount = body.amount
                this.type = body.type
                this.authorId = body.authorId?.let { EntityID(it, AuthorTable) }
            }

            return@transaction entity.toResponse()
        }
    }

    suspend fun getYearStats(param: BudgetYearParam): BudgetYearStatsResponse = withContext(Dispatchers.IO) {
        transaction {
            var query = BudgetTable
                .leftJoin(AuthorTable, { authorId }, { AuthorTable.id })
                .select { BudgetTable.year eq param.year }
                .limit(param.limit, param.offset)

            if (param.authorName != null) {
                query = query.andWhere { AuthorTable.name.lowerCase() like "%${param.authorName.toLowerCase()}%" }
            }
            val total = query.count()
            val data = query
                .orderBy(BudgetTable.month)
                .map {
                    val authorName = it[AuthorTable.name]
                    if (authorName != null) {
                        BudgetRecordWithAuthor(
                            year = it[BudgetTable.year],
                            month = it[BudgetTable.month],
                            amount = it[BudgetTable.amount],
                            type = it[BudgetTable.type],
                            authorId = it[BudgetTable.authorId]?.value,
                            authorName = authorName,
                            authorCreatedDt = it[AuthorTable.createdDt].toString()
                        )
                    } else {
                        BudgetRecord(
                            year = it[BudgetTable.year],
                            month = it[BudgetTable.month],
                            amount = it[BudgetTable.amount],
                            type = it[BudgetTable.type],
                            authorId = it[BudgetTable.authorId]?.value
                        )
                    }
                }

            val sumByType = data.groupBy {
                when (it) {
                    is BudgetRecordWithAuthor -> it.type.toString()
                    is BudgetRecord -> it.type.toString()
                    else -> throw IllegalArgumentException("Unknown type: ${it::class.simpleName}")
                }

            }.mapValues {
                it.value.sumOf {
                    when (it) {
                        is BudgetRecordWithAuthor -> it.amount
                        is BudgetRecord -> it.amount
                        else -> throw IllegalArgumentException("Unknown type: ${it::class.simpleName}")
                    }
                }
            }

            return@transaction BudgetYearStatsResponse(
                total = total,
                totalByType = sumByType,
                items = data
            )
        }
    }
}
