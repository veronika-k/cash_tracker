import java.sql.Timestamp

package object model {

  case class User(id: Int, login: String, password: String)

  case class Wallet(id: Int, userId: Int, balance: BigDecimal)

  case class Category(id: Int, userId:Int, name: String)

  case class Transaction(id: Long,
                         walletId: Int,
                         categoryId: Int,
                         amount: BigDecimal,
                         date: Timestamp,
                         isIncome: Boolean)


}


