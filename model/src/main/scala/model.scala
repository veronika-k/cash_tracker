import java.sql.Timestamp
import java.util.UUID

import io.circe.Decoder.Result
import io.circe.generic.JsonCodec
import io.circe.{Decoder, _}

package object model {

  @JsonCodec
  case class User(id: Int, login: String, password: String)

  @JsonCodec
  case class Wallet(id: Int, userId: Int, balance: BigDecimal)

  @JsonCodec
  case class Category(id: Int, userId: Int, name: String)

  @JsonCodec
  case class Transaction(id: Long,
                         walletId: Int,
                         categoryId: Int,
                         amount: BigDecimal,
                         date: Timestamp,
                         isIncome: Boolean)

  case class Token(userId: Int,
                   active: Boolean,
                   token: String)

  @JsonCodec
  case class TransactionsQuery(walletId: Int,
                               categoryName: Int,
                               transactionAmount: BigDecimal,
                               transactionDate: Timestamp,
                               transactionType: Boolean)

  @JsonCodec
  case class StatisticsQuery(name: String, percent: BigDecimal)

  @JsonCodec
  case class StatisticsQueryList(list: List[StatisticsQuery])

  @JsonCodec
  case class TransQuery(name: String, isIncome: Boolean, amount: BigDecimal, date: Timestamp)

  @JsonCodec
  case class TransQueryList(list: List[TransQuery])


  implicit val TimestampFormat: Encoder[Timestamp] with Decoder[Timestamp] =
    new Encoder[Timestamp] with Decoder[Timestamp] {
      override def apply(a: Timestamp): Json =
        Encoder.encodeLong.apply(a.getTime)

      override def apply(c: HCursor): Result[Timestamp] =
        Decoder.decodeLong.map(s => new Timestamp(s)).apply(c)
    }
}
