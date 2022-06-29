(ns heraldicon.util.base64
  (:require
   [goog.crypt :as crypt]
   [goog.crypt.base64 :as b64]))

(defn encode [data]
  (if (string? data)
    (b64/encodeString data)
    (b64/encodeByteArray data)))

(defn decode-utf-8 [data]
  (crypt/utf8ByteArrayToString (b64/decodeStringToByteArray data true)))
