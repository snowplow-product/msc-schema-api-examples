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

import $file.schemaApi, schemaApi.Schema

import io.circe._
import io.circe.literal._
import io.circe.generic.auto._
import io.circe.parser._
import sttp.client._
import sttp.client.circe._
import sttp.client.asynchttpclient.zio.{AsyncHttpClientZioBackend, SttpClient}
import zio._

import scala.io.{BufferedSource, Source}

def extractUsedSchemasFromManifest(path: String): Task[List[Schema.Metadata]] =
  readFileToString(path) >>= parseJson >>= validateSelfDescribingJsonAndExtractData >>= getUsedSchemas

private def readFileToString(path: String): Task[String] = {
  val openFile: String => Task[BufferedSource] = path => Task.effect(Source.fromFile(path, "UTF-8"))
  val closeFile: BufferedSource => UIO[Unit] = bs => Task.effect(bs.close()).ignore

  openFile(path).bracket(closeFile) { file =>
    Task.effect(file.getLines.mkString)
  }
}

private def parseJson(jsonString: String): Task[Json] =
  Task.fromEither(parse(jsonString))

// TODO:
//   - extract "schema" and fetch schema
//   - extract "data"
//   - validate "data" against "schema"
//   - return data
private def validateSelfDescribingJsonAndExtractData(json: Json): Task[Json] =
  Task.fromEither(json.hcursor.get[Json]("data"))

private def getUsedSchemas(jsonData: Json): Task[List[Schema.Metadata]] =
  Task.fromEither(jsonData.hcursor.get[List[Schema.Metadata]]("schemas"))