
import model._
import slick.jdbc.PostgresProfile.api._
import slick.lifted.Tag

class WalletTable(tag: Tag) extends Table[Wallet](tag, "wallet") {
  val id = column[Int]("id", O.PrimaryKey)
  val userId = column[Int]("userId")
  val balance = column[BigDecimal]("balance", O.SqlType("decimal(10,2)"))
  val userIdFk = foreignKey("userId_fk", userId, TableQuery[UserTable])(_.id)
  def * = (id, userId, balance) <> (Wallet.apply _ tupled, Wallet.unapply)
}

object WalletTable {
  val table = TableQuery[WalletTable]
}

class WalletRepo(db: Database) {
  val table = TableQuery[WalletTable]

  def create(wallet: Wallet) = db.run(table returning table += wallet)

  def update(wallet: Wallet) = db.run(table.filter(_.id === wallet.id).update(wallet))

  def getById(id: Int) = db.run(table.filter(_.id === id).result.headOption)
}