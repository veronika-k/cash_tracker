import model._
import slick.lifted.Tag
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.Future

class UserTable (tag: Tag) extends Table[User](tag, "users"){
  val id = column[Int]("id", O.PrimaryKey)
  val login = column[String]("login")
  val password = column[String]("password")
  def * = (id, login, password) <> (User.apply _ tupled, User.unapply)
}

object UserTable {
  val table = TableQuery[UserTable]
}

class UserRepo(db:Database) {
  val table = TableQuery[UserTable]

  def create(user: User) = db.run(table returning table += user)

  def update(user: User) = db.run(table.filter(_.id === user.id).update(user))

  def getById(id: Int) = db.run(table.filter(_.id === id).result.headOption)
}