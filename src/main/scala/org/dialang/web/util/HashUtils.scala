package org.dialang.web.util

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

import org.apache.commons.codec.binary.Base64

object HashUtils {

  def getHash(data: String, secret: String) = {

    val mac = Mac.getInstance("HmacSHA1")
    mac.init(new SecretKeySpec(secret.getBytes, "HmacSHA1"))

    val hashBytes
       = mac.doFinal(data.getBytes)

    Base64.encodeBase64String(hashBytes) 
  }
}
