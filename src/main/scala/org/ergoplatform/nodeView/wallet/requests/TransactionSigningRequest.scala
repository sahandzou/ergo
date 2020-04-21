package org.ergoplatform.nodeView.wallet.requests

import io.circe.{Decoder, Encoder, Json}
import org.ergoplatform.http.api.ApiCodecs
import org.ergoplatform.modifiers.mempool.UnsignedErgoTransaction
import org.ergoplatform.wallet.secrets.{DhtSecretWrapper, DlogSecretWrapper, PrimitiveSecretKey}

/**
  * Basic trait for externally provided hint for interpreter (to be used during script execution and signature
  * generation).
  */
trait Hint

/**
  * Externally provided secret (to be used once for a transaction to sign)
  * @param key - the secret
  */
case class OneTimeSecret(key: PrimitiveSecretKey) extends Hint

/**
  * A request to sign a transaction
  * @param utx - unsigned transaction
  * @param hints - hints for interpreter (such as additional one-time secrets)
  * @param inputs - hex-encoded input boxes bytes for the unsigned transaction (optional)
  * @param dataInputs - hex-encoded data-input boxes bytes for the unsigned transaction (optional)
  */
case class TransactionSigningRequest(utx: UnsignedErgoTransaction,
                                     hints: Seq[Hint],
                                     inputs: Option[Seq[String]],
                                     dataInputs: Option[Seq[String]]){

  lazy val dlogs: Seq[DlogSecretWrapper] = hints.flatMap{ h => h match{
    case OneTimeSecret(d: DlogSecretWrapper) => Some(d)
    case _ => None
  }}

  lazy val dhts: Seq[DhtSecretWrapper] = hints.flatMap{ h => h match{
    case OneTimeSecret(d: DhtSecretWrapper) => Some(d)
    case _ => None
  }}

}


object TransactionSigningRequest extends ApiCodecs {
  import io.circe.syntax._

  implicit val encoder: Encoder[TransactionSigningRequest] = { tsr =>
    Json.obj(
      "tx" -> tsr.utx.asJson,
      "secrets" -> Json.obj(
        "dlog" -> tsr.dlogs.asJson,
        "dht" -> tsr.dhts.asJson
      ),
      "inputsRaw" -> tsr.inputs.asJson,
      "dataInputsRaw" -> tsr.dataInputs.asJson
    )
  }

  implicit val decoder: Decoder[TransactionSigningRequest] = { cursor =>
    for {
      tx <- cursor.downField("tx").as[UnsignedErgoTransaction]
      dlogs <- cursor.downField("secrets").downField("dlog").as[Option[Seq[DlogSecretWrapper]]]
      dhts <- cursor.downField("secrets").downField("dht").as[Option[Seq[DhtSecretWrapper]]]
      inputs <- cursor.downField("inputsRaw").as[Option[Seq[String]]]
      dataInputs <- cursor.downField("dataInputsRaw").as[Option[Seq[String]]]
      secrets = (dlogs.getOrElse(Seq.empty) ++ dhts.getOrElse(Seq.empty)).map(OneTimeSecret.apply)
    } yield TransactionSigningRequest(tx, secrets, inputs, dataInputs)
  }

}
