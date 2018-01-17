import model._
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

object Repositories {
  val db = Database.forURL(
    "jdbc:postgresql://localhost:5432/airport?user=inoquea&password=11111111")
  val userRepo = new UserRepo(db)
  val walletRepo = new WalletRepo(db)
  val categoryRepo = new CategoryRepo(db)
  val transactionRepo = new TransactionRepo(db)
  val tokenRepo = new TokenRepo(db)

  def init(): Unit = {
    Await.result(db.run(walletRepo.table.schema.create), Duration.Inf)
    Await.result(db.run(userRepo.table.schema.create), Duration.Inf)
    Await.result(db.run(tokenRepo.table.schema.create), Duration.Inf)
    Await.result(db.run(categoryRepo.table.schema.create), Duration.Inf)
    Await.result(db.run(transactionRepo.table.schema.create), Duration.Inf)

  }

  def getUserWalletQuery(userId: Int) = {
    userRepo.table
      .filter(_.id === userId)
      .join(walletRepo.table)
      .on { case (user, wallet) => user.id === wallet.userId }
  }

  def getUserTransactionsQuery(userId: Int) = {
    getUserWalletQuery(userId)
      .join(transactionRepo.table)
      .on {
        case ((user, wallet), transaction) => wallet.id === transaction.walletId
      }
  }

  def getUserCategoriesQuery(userId: Int) = {
    userRepo.table
      .filter(_.id === userId)
      .join(categoryRepo.table)
      .on { case (user, category) => user.id === category.userId }

  }

  def getUserCategoryQuery(userId: Int, categoryId: Int) = {
    getUserCategoriesQuery(userId).filter {
      case (user, category) => category.id === categoryId
    }
  }

  def getUserCategoryTransactionsQuery(userId: Int, categoryId: Int) = {
    getUserCategoryQuery(userId: Int, categoryId: Int)
      .join(transactionRepo.table)
      .on {
        case ((user, category), transaction) =>
          category.id === transaction.categoryId
      }
  }

  def getUserWalletCategoryTransactionsQuery(userId: Int,
                                             categoryId: Int,
                                             walletId: Int) = {
    getUserCategoryTransactionsQuery(userId: Int, categoryId: Int)
      .join(walletRepo.table)
      .on {
        case (((user, category), transaction), wallet) =>
          transaction.walletId === wallet.id
      }
  }

  def getUserWalletsCategoriesTransactionsQuery(userId: Int) = {
    getUserTransactionsQuery(userId)
      .join(categoryRepo.table)
      .on {
        case (((user, wallet), transaction), category) =>
          category.id === transaction.categoryId
      }
  }

  //change Null
  //  def getUserTransactionParams(userId: Int, categoryId: Int,
  //                               walletId: Int, timeFrom: Timestamp, timeTo: Timestamp) = {
  //    getUserWalletsCategoriesTransactionsQuery(userId).filter {
  //      case (((user, wallet), transaction), category) => if (categoryId != null) category.id === categoryId else true
  //    }.filter {
  //      case (((user, wallet), transaction), category) => if (walletId != null) wallet.id === walletId else true
  //    }.filter {
  //      case (((user, wallet), transaction), category) => if (timeFrom != null && timeTo != null)
  //        (transaction.date < timeTo && transaction.date > timeFrom)
  //      else true
  //    }
  //  }

  //  def getUserTransactionParamsFormatted(userId: Int, categoryId: Int,
  //                                        walletId: Int, timeFrom: Timestamp, timeTo: Timestamp) = {
  //    val query = getUserTransactionParams(userId, categoryId, walletId, timeFrom, timeTo)
  //      .map {
  //        case (((user, wallet), transaction), category) => (wallet.id, category.name,
  //          transaction.amount, transaction.date, transaction.isIncome)
  //      }
  //    db.run(query.result)
  //  }
  //
  //  def getUserCategoryTransactionNumber(userId: Int, walletId: Int, timeFrom: Timestamp, timeTo: Timestamp) = {
  //    val categoriesTransactionNumber = getUserTransactionParams(userId, null, walletId, timeFrom, timeTo)
  //      .groupBy { case (((user, wallet), transaction), category) => (category.id, category.name) }
  //      .map { case ((categoryId, categoryName), group) => (categoryId, categoryName, group.size) }
  //    val allTransactionNumber = categoriesTransactionNumber.map(_._2).sum
  //    val categoriesPercents = categoriesTransactionNumber.
  //      map {
  //        case (categoryId, categoryName, categorySize) =>
  //            (categoryId, categoryName, categorySize, allTransactionNumber)
  //      }
  //    db.run(categoriesPercents.result)
  //  }

  def getUserId(login: String, password: String) = {
    userRepo.table.filter { case user => user.login === login && user.password === password }.map(_.id)
  }

  def deactivateToken(userId: Int) = {
    db.run(tokenRepo.table.filter(_.userId === userId)
      .map(_.active).update(false))
  }

  //  def generateToken(login:String, password: String): Future[Token] =
  //  {
  //    val token = java.util.UUID.randomUUID().toString
  //    db.run(getUserId(login, password).result).onComplete{
  //      case Success(userId) => deactivateToken(userId.head).onComplete{
  //        case Success(_) => {
  //          return tokenRepo.create(new Token(userId.head, true, token))
  //        }
  //      }
  //    }
  //    return tokenRepo.getToken(token)
  //  }

  def generateTokenRun(login: String, password: String, token: String) = {
    for {userId <- db.run(getUserId(login, password).result)
         deactivate <- deactivateToken(userId.head)
         created <- tokenRepo.create(new Token(userId.head, true, token))
         result <- tokenRepo.getToken(token)} yield result

  }

  def generateToken(login: String, password: String) = {
    val token = java.util.UUID.randomUUID().toString
    generateTokenRun(login, password, token)
  }

  def verifyUserQuery(token: String) = {
    for {isActive <- db.run(tokenRepo.table.filter(_.token === token)
      .map(_.active).result.headOption)
                      } yield {isActive match {
      case None => false
      case Some(active) => active}
    }
  }

  def getUserIdFromTokenQuery(token: String) ={
    tokenRepo.table.filter(_.token === token)
      .map(_.userId)
  }

  def getWalletBalance(token: String, walletId:Int) =
  {
    val userId = getUserIdFromTokenQuery(token)
    val query = walletRepo.table.
      filter{ case wallet => wallet.id === walletId && (wallet.userId in userId)}
      .map(_.balance)
    db.run(query.result.headOption)
  }
}
