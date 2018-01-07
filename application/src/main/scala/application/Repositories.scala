package application

import slick.jdbc.PostgresProfile.api._

object Repositories{
  val db = Database.forURL("jdbc:postgresql://localhost:5432/airport?user=inoquea&password=11111111")
  val userRepo = new UserRepo(db)
  val walletRepo = new UserRepo(db)
  val categoryRepo = new UserRepo(db)
  val transactionRepo = new UserRepo(db)

}
