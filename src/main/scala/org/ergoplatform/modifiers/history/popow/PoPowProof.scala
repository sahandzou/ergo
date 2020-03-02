package org.ergoplatform.modifiers.history.popow

import org.ergoplatform.modifiers.history.{Header, HeaderSerializer}
import org.ergoplatform.modifiers.history.popow.PoPowAlgos.{bestArg, lowestCommonAncestor, maxLevelOf}
import scorex.core.serialization.ScorexSerializer
import scorex.util.serialization.{Reader, Writer}
import scorex.util.Extensions.LongOps

/**
  * A structure representing NiPoPow proof as a persistent modifier.
  *
  * For details, see the foundational paper:
  *
  * [KMZ17] Non-Interactive Proofs of Proof-of-Work https://eprint.iacr.org/2017/963.pdf
  *
  * @param m          - security parameter (min μ-level superchain length)
  * @param k          - security parameter (min suffix length, >= 1)
  * @param prefix     - proof prefix headers
  * @param suffixHead - first header of the suffix
  * @param suffixTail - tail of the proof suffix headers
  */
case class PoPowProof(m: Int,
                      k: Int,
                      prefix: Seq[PoPowHeader],
                      suffixHead: PoPowHeader,
                      suffixTail: Seq[Header]) {

  def serializer: ScorexSerializer[PoPowProof] = PoPowProofSerializer

  def headersChain: Seq[Header] = prefixHeaders ++ suffixHeaders

  def prefixHeaders: Seq[Header] = prefix.map(_.header)

  def suffixHeaders: Seq[Header] = suffixHead.header +: suffixTail

  def chainOfLevel(l: Int): Seq[PoPowHeader] = prefix.filter(x => maxLevelOf(x.header) >= l)

  /**
    * Implementation of the ≥ algorithm from [KMZ17], see Algorithm 4
    *
    * @param that - PoPoW proof to compare with
    * @return whether this PoPoW proof is better than "that"
    */
  def isBetterThan(that: PoPowProof): Boolean = {
    val (thisDivergingChain, thatDivergingChain) = lowestCommonAncestor(headersChain, that.headersChain)
      .map(h => headersChain.filter(_.height > h.height) -> that.headersChain.filter(_.height > h.height))
      .getOrElse(headersChain -> that.headersChain)
    bestArg(thisDivergingChain)(m) > bestArg(thatDivergingChain)(m)
  }
}

object PoPowProofSerializer extends ScorexSerializer[PoPowProof] {

  override def serialize(obj: PoPowProof, w: Writer): Unit = {
    w.putUInt(obj.m)
    w.putUInt(obj.k)
    w.putUInt(obj.prefix.size)
    obj.prefix.foreach { h =>
      val hBytes = h.bytes
      w.putUInt(hBytes.length)
      w.putBytes(hBytes)
    }
    val suffixHeadBytes = obj.suffixHead.bytes
    w.putUInt(suffixHeadBytes.length)
    w.putBytes(obj.suffixHead.bytes)
    w.putUInt(obj.suffixTail.size)
    obj.suffixTail.foreach { h =>
      val hBytes = h.bytes
      w.putUInt(hBytes.length)
      w.putBytes(hBytes)
    }
  }

  override def parse(r: Reader): PoPowProof = {
    val m = r.getUInt().toIntExact
    val k = r.getUInt().toIntExact
    val prefixSize = r.getUInt().toIntExact
    val prefix = (0 until prefixSize).map { _ =>
      val size = r.getUInt().toIntExact
      PoPowHeaderSerializer.parseBytes(r.getBytes(size))
    }
    val suffixHeadSize = r.getUInt().toIntExact
    val suffixHead = PoPowHeaderSerializer.parseBytes(r.getBytes(suffixHeadSize))
    val suffixSize = r.getUInt().toIntExact
    val suffixTail = (0 until suffixSize).map { _ =>
      val size = r.getUInt().toIntExact
      HeaderSerializer.parseBytes(r.getBytes(size))
    }
    PoPowProof(m, k, prefix, suffixHead, suffixTail)
  }

}