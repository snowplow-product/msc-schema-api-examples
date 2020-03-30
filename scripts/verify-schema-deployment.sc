interp.repositories.update(
  interp.repositories() ::: List(coursierapi.MavenRepository.of("https://jitpack.io"))
)

@

import $ivy.`com.softwaremill.sttp.client::core:2.0.6`
import $ivy.`com.softwaremill.sttp.client::circe:2.0.6`
import $ivy.`com.softwaremill.sttp.client::json-common:2.0.6`
import $ivy.`com.softwaremill.sttp.client::async-http-client-backend-zio:2.0.6`
import $ivy.`com.softwaremill.sttp.model::core:1.0.2`
import $ivy.`dev.zio::zio:1.0.0-RC18-2`
import $ivy.`dev.zio::izumi-reflect:0.12.0-M0`
import $ivy.`io.circe::circe-core:0.13.0`
import $ivy.`io.circe::circe-generic:0.13.0`
import $ivy.`io.circe::circe-generic-extras:0.13.0`
import $ivy.`io.circe::circe-parser:0.13.0`
import $ivy.`io.circe::circe-literal:0.13.0`
import $ivy.`io.circe::circe-json-schema:0.1.0`
import $ivy.`org.typelevel::cats-core:2.0.0`

import $file.json
import json._
import $file.auth
import auth._
import $file.schemaApi, schemaApi.Schema, schemaApi.checkSchemaDeployment
import $file.constants, constants._

import cats.data.NonEmptyList
import cats.implicits._
import cats.syntax.list.catsSyntaxList
import sttp.client.asynchttpclient.zio.{AsyncHttpClientZioBackend, SttpClient}
import zio._
import zio.console._

@main
def main(manifestPath: String,
         organizationId: String,
         clientId: String,
         clientSecret: String,
         audience: String,
         username: String,
         password: String,
         environment: String): Unit = {

  val program =
    for {
      schemas <- extractUsedSchemasFromManifest(manifestPath)
      _       <- putStrLn(schemas.mkString(s"Checking into $environment environment if these schemas are deployed: ", ", ", ""))
      token   <- getOAuthToken(authServerBaseUrl, clientId, clientSecret, audience, username, password)
      _       <- verifySchemaDeployment(apiBaseUrl, token, organizationId, environment, schemas)
    } yield ()

  Runtime
    .default
    .unsafeRunSync(
      program
        .provideCustomLayer(AsyncHttpClientZioBackend.layer())
        .provideLayer(ZEnv.live)
    )
    .fold(e => e.failureOption.map(throw _), identity)
}



private def verifySchemaDeployment(apiBaseUrl: String,
                                   token: String,
                                   organizationId: String,
                                   environment: String,
                                   schemas: List[Schema.Metadata]): RIO[SttpClient with Console, Unit] =
  ZIO
    .collectAllParN(5)(
      schemas
        .map(schema =>
          checkSchemaDeployment(apiBaseUrl, token, organizationId, environment, schema)
            .map(found => Option.when(!found)(schema))
        )
    )
    .flatMap(results =>
      ZIO
        .fromEither(Either.fromOption(results.flatten.toNel.map(DeploymentCheckFailure(environment, _)), ()).swap)
        .zipRight(putStrLn("All schemas are deployed! You are good to go."))
        .tapError(e => putStrLn(s"Deployment check failed! ${e.getMessage}"))
    )

case class DeploymentCheckFailure(environment: String, schemas: NonEmptyList[Schema.Metadata]) extends Exception {
  override def getMessage: String = schemas
    .map(_.toString)
    .mkString_(s"Some schemas are not deployed on '$environment' environment: ", ", ", "")
}


