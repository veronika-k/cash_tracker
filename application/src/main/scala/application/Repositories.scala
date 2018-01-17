import java.sql.Timestamp

import io.circe.Decoder
import io.circe.parser.decode
import model._
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.Await
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


  def getUserId(login: String, password: String) = {
    userRepo.table.filter { case user => user.login === login && user.password === password }.map(_.id)
  }

  def deactivateToken(userId: Int) = {
    db.run(tokenRepo.table.filter(_.userId === userId)
      .map(_.active).update(false))
  }


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
    } yield {
      isActive match {
        case None => false
        case Some(active) => active
      }
    }
  }

  def getUserIdFromTokenQuery(token: String) = {
    tokenRepo.table.filter(_.token === token)
      .map(_.userId)
  }

  def getWalletBalance(token: String, walletId: Int) = {
    val userId = getUserIdFromTokenQuery(token)
    val query = walletRepo.table.
      filter { case wallet => wallet.id === walletId && (wallet.userId in userId) }
      .map(_.balance)
    db.run(query.result.headOption)
  }

  def getSpendingStatistics(token: String, walletId: Int) = {
    val userId = getUserIdFromTokenQuery(token)
    //    val userId = userRepo.table.filter{ user => user.id in getUserIdFromTokenQuery(token)}.map(_.id)
    val categoryQuery = categoryRepo.table
      .filter { case category => category.userId in userId }
    val transactionQuery = transactionRepo.table.filter(_.isIncome === false)
    val walletQuery = walletRepo.table.filter(_.id === walletId)
    val categoryTransactionsQuery = categoryQuery.join(transactionQuery)
      .on {
        case (category, transaction) =>
          category.id === transaction.categoryId
      }
      .join(walletQuery)
      .on {
        case ((category, transaction), wallet) =>
          transaction.walletId === wallet.id
      }
      .map { case ((category, transaction), wallet) => (category.id, category.name, transaction.amount) }
    val spendedAmount = categoryTransactionsQuery.map(_._3).sum
    val resultQuery = categoryTransactionsQuery
      .groupBy { case (categId, categName, _) => (categId, categName) }
      .map { case ((_, categName), group) => (categName, group.map(_._3).sum) }
      .map { case (name, sum) => (name, sum / spendedAmount) }
    db.run(resultQuery.result)
  }

  def getTransactions(token: String, walletId: Int, fromDate: Option[String],
                      toDate: Option[String], category: Option[Int]) = {
    val userId = getUserIdFromTokenQuery(token)
    val categoryQuery = category match {
      case Some(categ) => categoryRepo.table
        .filter { category => (category.id === categ) && (category.userId in userId) }
      case None => categoryRepo.table
        .filter { category => category.userId in userId }
    }
    val transactionQuery = (fromDate, toDate) match {
      case (Some(fD), Some(tD)) => {
        val frDate = decode[Timestamp](fD)
        val tDate = decode[Timestamp](tD)
        (frDate, tDate) match {
          case (Right(frDate), Right(tDate)) =>
            transactionRepo.table.filter { transaction =>
              transaction.walletId === walletId && transaction.date > frDate && transaction.date < tDate
            }
          case _ => transactionRepo.table.take(0)
        }
      }
      case (None, None) => transactionRepo.table.filter(_.walletId === walletId)
      case (_, _) => transactionRepo.table.take(0)
    }
    val categoryTransactionsQuery = categoryQuery.join(transactionQuery)
      .on {
        case (category, transaction) =>
          category.id === transaction.categoryId
      }
      .map { case (category, transaction) => (category.name,
        transaction.isIncome, transaction.amount, transaction.date)
      }
    db.run(categoryTransactionsQuery.result)
  }

  def createUser(id: Decoder.Result[Int],
                 login: Decoder.Result[String],
                 password: Decoder.Result[String]) = {
    (id, login, password) match {
      case (Right(userId), Right(userLogin), Right(userPassw)) => {
        userRepo.create(User(userId, userLogin, userPassw))
      }
      case (_, _, _) => db.run(userRepo.table.take(0).result.headOption)
    }
  }

  def createWallet(token: String, walletId: Int, balance: BigDecimal) = {
    val userId = getUserIdFromTokenQuery(token)
    val user = db.run(userRepo.table.filter(_.id in userId).map(_.id).result)
    for (u <- user) yield walletRepo.create(Wallet(walletId, u.head, balance))
  }

  def addTransactionToWallet(walletId: Int, amount: BigDecimal, isIncome: Boolean) = {
    def getBalance = db.run(walletRepo.table.filter(_.id === walletId).map(_.balance).result)

    def updateBalance(balance: BigDecimal) = if (isIncome)
      db.run(walletRepo.table.filter(_.id === walletId).map(_.balance).update(balance + amount))
    else
      db.run(walletRepo.table.filter(_.id === walletId).map(_.balance).update(balance - amount))

    for {balance <- getBalance
         result <- updateBalance(balance.head)
    } yield result
  }

  def createTransaction(token: String, id: Long, walletId: Int,
                        categoryId: Int, amount: BigDecimal, transDate: Timestamp, isIncome: Boolean) = {
    val userId = getUserIdFromTokenQuery(token)
    val category = categoryRepo.table
      .filter { category => (category.id === categoryId) && (category.userId in userId) }.map(_.id)
    val wallet = walletRepo.table
      .filter { wallet => (wallet.id === walletId) && (wallet.userId in userId) }.map(_.id)

    for {
      catSize <- db.run(category.size.result)
      wallSize <- db.run(wallet.size.result)
      createTrans <- transactionRepo.create(
        Transaction(id, walletId, categoryId, amount, transDate, isIncome)
      )
      add <- addTransactionToWallet(walletId, amount, isIncome) if catSize > 0 && wallSize > 0} yield createTrans
  }
}

