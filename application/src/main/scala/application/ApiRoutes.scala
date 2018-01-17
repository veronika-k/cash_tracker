import java.sql.Timestamp
import io.circe.parser.decode
import io.circe.syntax._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.Json
import model._

trait ApiRoutes extends FailFastCirceSupport {
  val route = {
    pathPrefix(Segment) { str: String =>
      pathEndOrSingleSlash {
        str match {
          case "get_token" =>
            post {
              entity(as[Json]) { json =>
                val login = json.hcursor.downField("login").as[String]
                val password = json.hcursor.downField("password").as[String]
                (login, password) match {
                  case (Right(userLogin), Right(userPassw)) =>
                    onComplete {
                      Repositories.generateToken(userLogin, userPassw)
                    } {
                      case util.Success(value) => {
                        complete("Token $value generated")
                      }
                      case util.Failure(ex) => complete(NotFound)
                    }
                  case _ => complete(BadRequest)
                }
              }
            }
          case "new_user" =>
            post {
              entity(as[Json]) { json =>
                val id = json.hcursor.downField("id").as[Int]
                val login = json.hcursor.downField("login").as[String]
                val password = json.hcursor.downField("password").as[String]
                (id, login, password) match {
                  case (Right(userId), Right(userLogin), Right(userPassw)) => {
                    onComplete {
                      Repositories.createUser(id, login, password)
                    } {
                      case util.Success(value) => {
                        complete(s"User $userLogin was created")
                      }
                      case util.Failure(ex) => complete(BadRequest)
                    }
                  }
                  case _ => complete(BadRequest)
                }
              }
            }
          case "new_wallet" => {
            parameters('token.as[String].?) { tokenOption =>
              post {
                tokenOption match {
                  case None => complete(BadRequest)
                  case Some(str: String) => {
                    val token = str
                    onComplete(Repositories.verifyUserQuery(token)) {
                      case util.Success(value) => {
                        if (value == true) {
                          entity(as[Json]) { json =>
                            val walletId = json.hcursor.downField("id").as[Int]
                            val walletStartBalance = json.hcursor.downField("start_balance").as[BigDecimal]
                            (walletId, walletStartBalance) match {
                              case (Right(id), Right(balance)) =>
                                onComplete(Repositories.createWallet(token, id, balance)) {
                                  case util.Success(value) => {
                                    complete(s"Wallet was created")
                                  }
                                  case util.Failure(ex) => complete(BadRequest)
                                }
                              case _ => complete(BadRequest)
                            }
                          }
                        }
                        else complete(BadRequest)
                      }
                      case util.Failure(_)
                      => complete(BadRequest)
                    }
                  }
                }

              }
            }
          }
          case "new_transaction" =>
            parameters('token.as[String].?) { tokenOption =>
              post {
                tokenOption match {
                  case None => complete(BadRequest)
                  case Some(str: String) => {
                    val token = str
                    onComplete(Repositories.verifyUserQuery(token)) {
                      case util.Success(value) => {
                        if (value == true) {
                          entity(as[Json]) { json =>
                            val idRes = json.hcursor.downField("id").as[Long]
                            val walletIdRes  = json.hcursor.downField("walletId").as[Int]
                            val categoryIdRes  = json.hcursor.downField("categoryId").as[Int]
                            val amountRes  = json.hcursor.downField("amount").as[BigDecimal]
                            val dateRes  = json.hcursor.downField("date").as[String]
                            val isIncomeRes = json.hcursor.downField("isIncome").as[Boolean]
                            (idRes , walletIdRes , categoryIdRes , amountRes , dateRes , isIncomeRes ) match {
                              case (Right(id), Right(walletId), Right(categoryId), Right(amount),
                              Right(date), Right(isIncome)) =>
                                decode[Timestamp](date) match {
                                  case Right(transDate) =>
                                    onComplete(Repositories.createTransaction(token, id, walletId,
                                      categoryId, amount, transDate, isIncome)) {
                                      case util.Success(value) => {
                                        val typeTransaction = if (isIncome) "Income" else "Outcome"
                                        complete(s"$typeTransaction transaction was created")
                                      }
                                      case util.Failure(ex) => complete(BadRequest)
                                    }
                                  case _ => complete(BadRequest)
                                }

                              case _ => complete(BadRequest)
                            }
                          }
                        }
                        else complete(BadRequest)
                      }
                      case util.Failure(_)
                      => complete(BadRequest)
                    }
                  }
                }

              }
            }

        }
      } ~ {
        pathPrefix(Segment) { str2: String =>
          pathPrefix(IntNumber) { id: Int =>
            parameters('token.as[String].?) { tokenOption =>
              (str, str2, tokenOption) match {
                case ("wallet", "balance", _) =>
                  get {
                    tokenOption match {
                      case None => complete(BadRequest)
                      case Some(str: String) => {
                        val token = str
                        onComplete(Repositories.verifyUserQuery(token)) {
                          case util.Success(value) => {
                            if (value == true)
                              onComplete(Repositories.getWalletBalance(token, id)) {
                                case util.Success(value) => {
                                  value match {
                                    case Some(balance) => complete(s"Balance $id = $value")
                                    case None => complete(NotFound)
                                  }
                                }
                                case util.Failure(_) => complete(NotFound)
                              }
                            else complete(BadRequest)
                          }
                          case util.Failure(_)
                          => complete(BadRequest)
                        }
                      }
                    }
                  }
                case ("wallet", "statistics", _) =>
                  get {
                    tokenOption match {
                      case None => complete(BadRequest)
                      case Some(str: String) => {
                        val token = str
                        onComplete(Repositories.verifyUserQuery(token)) {
                          case util.Success(value) => {
                            if (value == true)
                              onComplete(Repositories.getSpendingStatistics(token, id)) {
                                case util.Success(result) => {
                                  val resultList = result.flatMap {
                                    case (name, percent) => List(new StatisticsQuery(name, {
                                      percent match {
                                        case None => BigDecimal(0.0)
                                        case Some(perc) => perc * 100
                                      }
                                    }))
                                  }.toList
                                  complete(new StatisticsQueryList(resultList)
                                    .asJson)
                                }
                                case util.Failure(_) => complete(NotFound)
                              }
                            else complete(BadRequest)
                          }
                          case util.Failure(_)
                          => complete(BadRequest)
                        }
                      }
                    }
                  }
                case ("wallet", "transactions", _) =>
                  parameters('fromDate.?, 'toDate.?, 'category.as[Int].?) { (fromDate, toDate, category) =>
                    get {
                      tokenOption match {
                        case None => complete(BadRequest)
                        case Some(str: String) => {
                          val token = str
                          onComplete(Repositories.verifyUserQuery(token)) {
                            case util.Success(value) => {
                              if (value == true)
                                onComplete(Repositories.getTransactions(token, id, fromDate,
                                  toDate, category)) {
                                  case util.Success(result) => {
                                    val resultList = result.flatMap {
                                      case (name, isIncome, amount, date) => List(
                                        new TransQuery(name, isIncome, amount, date))
                                    }.toList
                                    complete(new TransQueryList(resultList)
                                      .asJson)
                                  }
                                  case util.Failure(_) => complete(NotFound)
                                }
                              else complete(BadRequest)
                            }
                            case util.Failure(_)
                            => complete(BadRequest)

                          }


                        }
                      }
                    }
                  }
              }
            }

          }
        }
      }
    }
  }
}
