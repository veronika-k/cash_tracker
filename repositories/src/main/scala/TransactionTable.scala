import java.sql.Timestamp

import model._
import slick.lifted.Tag
import slick.jdbc.PostgresProfile.api._

class TransactionTable(tag: Tag) extends Table[Transaction](tag, "transaction"){
  val id = column[Long]("id", O.PrimaryKey)
  val walletId = column[Int]("walletId")
  val categoryId = column[Int]("categoryId")
  val amount = column[BigDecimal]("amount", O.SqlType("decimal(10,2)"))
  val date = column[Timestamp]("date")
  val isIncome= column[Boolean]("is_income")
  val walletIdFk = foreignKey("walletId_fk", walletId, TableQuery[WalletTable])(_.id)
  val categoryIdFk = foreignKey("categoryId_fk", categoryId, TableQuery[CategoryTable])(_.id)
  def * = (id, walletId , categoryId, amount, date, isIncome) <> (Transaction.apply _ tupled, Transaction.unapply)
}
object TransactionTable {
  val table = TableQuery[TransactionTable]
}

class TransactionRepo(db:Database) {
  val table = TableQuery[TransactionTable]

  def create(transaction: Transaction) = db.run(table returning table += transaction)

  def update(transaction: Transaction) = db.run(table.filter(_.id === transaction.id).update(transaction))

  def getById(id: Long) = db.run(table.filter(_.id === id).result.headOption)
}
