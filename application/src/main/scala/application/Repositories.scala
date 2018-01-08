import java.sql.Timestamp

import slick.jdbc.PostgresProfile.api._

object Repositories {
  val db = Database.forURL(
    "jdbc:postgresql://localhost:5432/airport?user=inoquea&password=11111111")
  val userRepo = new UserRepo(db)
  val walletRepo = new WalletRepo(db)
  val categoryRepo = new CategoryRepo(db)
  val transactionRepo = new TransactionRepo(db)
  val tokenRepo = new TokenRepo(db)

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
  def getUserTransactionParams(userId: Int, categoryId: Int,
                               walletId: Int, timeFrom: Timestamp, timeTo: Timestamp) = {
    getUserWalletsCategoriesTransactionsQuery(userId).filter {
      case (((user, wallet), transaction), category) => if (categoryId != null) category.id === categoryId else true
    }.filter {
      case (((user, wallet), transaction), category) => if (walletId != null) wallet.id === walletId else true
    }.filter {
      case (((user, wallet), transaction), category) => if (timeFrom != null && timeTo != null)
        (transaction.date < timeTo && transaction.date > timeFrom)
      else true
    }
  }

  def getUserTransactionParamsFormatted(userId: Int, categoryId: Int,
                                        walletId: Int, timeFrom: Timestamp, timeTo: Timestamp) = {
    val query = getUserTransactionParams(userId, categoryId, walletId, timeFrom, timeTo)
      .map {
        case (((user, wallet), transaction), category) => (wallet.id, category.name,
          transaction.amount, transaction.date, transaction.isIncome)
      }
    db.run(query.result)
  }

  def getUserCategoryTransactionNumber(userId: Int, walletId: Int, timeFrom: Timestamp, timeTo: Timestamp) = {
    val categoriesTransactionNumber = getUserTransactionParams(userId, null, walletId, timeFrom, timeTo)
      .groupBy { case (((user, wallet), transaction), category) => (category.id, category.name) }
      .map { case ((categoryId, categoryName), group) => (categoryId, categoryName, group.size) }
    val allTransactionNumber = categoriesTransactionNumber.map(_._2).sum
    val categoriesPercents = categoriesTransactionNumber.
      map {
        case (categoryId, categoryName, categorySize) =>
            (categoryId, categoryName, categorySize, allTransactionNumber)
      }
    db.run(categoriesPercents.result)
  }

  def getUserId(login:String, password: String) =
  {
    userRepo.table.filter{ case user => user.login === login && user.password === password}.take(1).map(_.id)
  }

//  def generateToken(login:String, password: String) =
//  {
//    tokenRepo.table.filter( case token => getUserId(login, password).result === token.userId}
//      .map{case(userId, token) => token.status}
//  }

}
