import slick.jdbc.PostgresProfile.api._

object Repositories{
  val db = Database.forURL("jdbc:postgresql://localhost:5432/airport?user=inoquea&password=11111111")
  val userRepo = new UserRepo(db)
  val walletRepo = new WalletRepo(db)
  val categoryRepo = new CategoryRepo(db)
  val transactionRepo = new TransactionRepo(db)

  def getUserWalletQuery(userId: Int) = {
    userRepo.table.filter(_.id === userId)
      .join(walletRepo.table).on{case (user, wallet) => user.id === wallet.userId}
  }

  def getUserTransactionsQuery(userId: Int) =
  {
    getUserWalletQuery(userId).join(transactionRepo.table)
      .on{case ((user, wallet), transaction) => wallet.id === transaction.walletId }
  }

  def getUserCategoriesQuery(userId:Int) =
  {
    userRepo.table.filter(_.id === userId)
      .join(categoryRepo.table).on{case (user, category) => user.id === category.userId}

  }

  def getUserCategoryQuery(userId:Int, categoryId: Int) =
  {
    getUserCategoriesQuery(userId).filter{case (user, category) => category.id === categoryId}
  }

  def getUserCategoryTransactionsQuery(userId:Int, categoryId: Int) =
  {
    getUserCategoryQuery(userId:Int, categoryId: Int).join(transactionRepo.table)
      .on{case ((user, category), transaction) => category.id === transaction.categoryId }
  }

  def getUserWalletCategoryTransactionsQuery(userId:Int, categoryId: Int, walletId:Int)=
  {
    getUserCategoryTransactionsQuery(userId:Int, categoryId: Int)
      .join(walletRepo.table).on{case (((user, category), transaction), wallet) =>
      transaction.walletId === wallet.id}
  }
}
