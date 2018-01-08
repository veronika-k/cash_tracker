import model._
import slick.jdbc.PostgresProfile.api._
import slick.lifted.Tag

/**
  * Created by inoquea on 08.01.18.
  */
class TokenTable(tag: Tag) extends Table[Token](tag, "token") {
  val userId = column[Int]("userId")
  val token = column[String]("token", O.PrimaryKey)
  val active = column[Boolean]("active")
  val userIdFk = foreignKey("userId_fk", userId, TableQuery[UserTable])(_.id)

  def * = (userId, active, token) <> (Token.apply _ tupled, Token.unapply)
}

object TokenTable {
  val table = TableQuery[TokenTable]
}

class TokenRepo(db: Database) {
  val table = TableQuery[TokenTable]
  def deactivateTokens(newToken: Token) =
  {
    table.filter(_.userId === newToken.userId).map(_.active).update(false)
  }

  def create(token: Token) = {
    db.run(table returning table += token)
  }
}