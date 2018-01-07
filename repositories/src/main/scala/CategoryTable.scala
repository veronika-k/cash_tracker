import model._
import slick.lifted.Tag
import slick.jdbc.PostgresProfile.api._

class CategoryTable(tag: Tag) extends Table[Category](tag, "category"){
    val id = column[Int]("id", O.PrimaryKey)
    val userId = column[Int]("userId")
    val name= column[String]("name")
    val userIdFk = foreignKey("userId_fk", userId, TableQuery[UserTable])(_.id)
    def * = (id, userId, name) <> (Category.apply _ tupled, Category.unapply)
}
object CategoryTable {
  val table = TableQuery[CategoryTable]
}

class CategoryRepo(db:Database) {
  val table = TableQuery[CategoryTable]

  def create(category: Category) = db.run(table returning table += category)

  def update(category: Category) = db.run(table.filter(_.id === category.id).update(category))

  def getById(id: Int) = db.run(table.filter(_.id === id).result.headOption)
}
