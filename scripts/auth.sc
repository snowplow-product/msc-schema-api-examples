import $ivy.`com.softwaremill.sttp.client::core:2.0.6`
import $ivy.`com.softwaremill.sttp.client::circe:2.0.6`
import $ivy.`com.softwaremill.sttp.client::json-common:2.0.6`
import $ivy.`com.softwaremill.sttp.client::async-http-client-backend-zio:2.0.6`
import $ivy.`com.softwaremill.sttp.model::core:1.0.2`
import $ivy.`dev.zio::zio:1.0.0-RC18-2`
import $ivy.`io.circe::circe-core:0.13.0`
import $ivy.`io.circe::circe-generic:0.13.0`
import $ivy.`io.circe::circe-parser:0.13.0`
import $ivy.`io.circe::circe-literal:0.13.0`

import io.circe._
import io.circe.literal._
import sttp.client._
import sttp.client.circe._
import sttp.client.asynchttpclient.zio.SttpClient
import zio._

def getOAuthToken(authServerBaseUrl: String,
                  clientId: String,
                  clientSecret: String,
                  audience: String,
                  username: String,
                  password: String): RIO[SttpClient, String] = {
  val request = basicRequest
    .body(
      json"""
          {
            "client_id": ${clientId},
            "client_secret": ${clientSecret},
            "audience": ${audience},
            "grant_type": "password",
            "username": ${username},
            "password": ${password}
          }
          """
    )
    .post(uri"${authServerBaseUrl}/oauth/token")
    .response(asJsonAlways[Json])

  for {
    response <- SttpClient.send(request)
    json     <- Task.fromEither(response.body)
    token    <- Task.fromEither(json.hcursor.get[String]("access_token"))
  } yield token
}