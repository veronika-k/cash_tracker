import java.security.Timestamp

/**
  * Created by inoquea on 05.01.18.
  */
package object model {

  case class User(id: Int, login: String, password: String)

  case class Wallet(id: Int, userId: Int, balance: BigDecimal)

  case class Category(id: Int, name: String)

  case class Expenditure(id: Long,
                         walletId: Int,
                         categoryId: Int,
                         amount: BigDecimal,
                         date: Timestamp)

  case class Replenishment(id: Long,
                           walletId: Int,
                           amount: BigDecimal,
                           date: Timestamp)

}
