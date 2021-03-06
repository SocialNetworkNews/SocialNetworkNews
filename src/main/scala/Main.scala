import com.danielasfregola.twitter4s.entities.enums.AccessType
import com.danielasfregola.twitter4s.entities.enums.AccessType.AccessType
import com.danielasfregola.twitter4s.entities.{AccessToken, ConsumerToken}
import com.danielasfregola.twitter4s.{TwitterAuthenticationClient, TwitterRestClient, TwitterStreamingClient}
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.StrictLogging
import twitterCrawler.RestAPISingleton

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

/**
  * @author MTRNord <freifunknews@nordgedanken.blog>
  * @todo rework to make this more generic
  * @version 0.1.0
  *
  */
object Main extends App with StrictLogging {
  val conf: Config = ConfigFactory.load
  val consumerToken =
    ConsumerToken(key = conf.getString("twitter.consumer.key"), secret = conf.getString("twitter.consumer.secret"))
  val TwitterAuthClient = new TwitterAuthenticationClient(consumerToken)

  val write_access: AccessType = AccessType.Write

  val reqToken =
    TwitterAuthClient.requestToken(x_auth_access_type = Some(write_access))

  reqToken onComplete {
    case Success(token) =>
      println(
        TwitterAuthClient
          .authenticateUrl(token = token.token, force_login = false)
      )

      val pin = scala.io.StdIn.readLine("Insert Pin: ")

      val AccessTokenResp =
        Await.result(
          TwitterAuthClient.accessToken(token.token, pin),
          10 seconds
        )

      val accessToken = AccessToken(
        key = AccessTokenResp.accessToken.key,
        secret = AccessTokenResp.accessToken.secret
      )

      val streamingClient =
        TwitterStreamingClient.apply(
          accessToken = accessToken,
          consumerToken = consumerToken
        )
      val restClient = TwitterRestClient.apply(
        accessToken = accessToken,
        consumerToken = consumerToken
      )

      //val sender = new twitterSender.Sender(restClient = restClient)
      //sender.sendHelloWorld

      RestAPISingleton.setRestAPI(restClient)

      val stream = new twitterCrawler.StreamingApi(
        streamingClient = streamingClient
      )
      stream.fetchTweets

      /*val test = new GenerateNewsPaper
      test.execute(null)*/

      generator.Scheduler.main()
    case Failure(err) => logger.error(err.toString)
  }

  /**
    * Keep alive workaround
    */
  val waitFunc = Future {
    while (true) {
      Thread.sleep(1000)
    }
  }

  Await.result(waitFunc, scala.concurrent.duration.Duration.Inf)
}
