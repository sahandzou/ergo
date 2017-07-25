package org.ergoplatform.nodeView.state

import org.ergoplatform.modifiers.ErgoPersistentModifier
import scorex.core.transaction.state.MinimalState.VersionTag
import ErgoState.Digest
import scorex.core.utils.ScorexLogging
import scala.util.Try

/**
  * Minimal state variant which is storing only digest of UTXO authenticated as a dynamic dictionary.
  * See https://eprint.iacr.org/2016/994 for details on this mode.
  */
class DigestState extends ErgoState[DigestState] with ScorexLogging {
  override lazy val rootHash: Digest = ???

  override def version: VersionTag = ???

  override def validate(mod: ErgoPersistentModifier): Try[Unit] = mod match {
    case a: Any => log.info(s"Modifier not validated: $a"); Try(this)
  }

  override def applyModifier(mod: ErgoPersistentModifier): Try[DigestState] = mod match {
    case a: Any => log.info(s"Unhandlend modifier: $a"); Try(this)
  }

  override def rollbackTo(version: VersionTag): Try[DigestState] = ???
}
