(ns heraldicon.heraldry.ordinary.core
  (:require
   [heraldicon.blazonry :as blazonry]
   [heraldicon.context :as c]
   [heraldicon.heraldry.line.fimbriation :as fimbriation]
   [heraldicon.heraldry.ordinary.options :as ordinary.options]
   [heraldicon.interface :as interface]
   [heraldicon.localization.string :as string]))

(defmethod interface/blazon-component :heraldry/ordinary [context]
  (let [ordinary-type (interface/get-sanitized-data (c/++ context :type))
        line (interface/get-sanitized-data (c/++ context :line))
        voided? (interface/get-sanitized-data (c/++ context :voided :voided?))
        humetty? (interface/get-sanitized-data (c/++ context :humetty :humetty?))
        ordinary-name (case ordinary-type
                        :heraldry.ordinary.type/quarter
                        (let [size (interface/get-sanitized-data (c/++ context :geometry :size))]
                          (string/str-tr (if (< size 100)
                                           "Canton "
                                           "Quarter ")
                                         (blazonry/translate (interface/get-sanitized-data (c/++ context :variant)))))
                        :heraldry.ordinary.type/point
                        (string/str-tr "Point " (blazonry/translate (interface/get-sanitized-data (c/++ context :variant))))
                        (blazonry/translate ordinary-type))
        rest (string/combine " " [ordinary-name
                                  (blazonry/translate-line line)
                                  (when voided? "voided")
                                  (when humetty? "humetty")
                                  (interface/blazon (c/++ context :field))
                                  (fimbriation/blazon context
                                                      :include-lines? true)])
        article (if (re-matches #"(?i)^[aeiouh].*" (string/tr-raw rest :en))
                  "an"
                  "a")]
    (string/combine " " [article rest])))

(defn title [context]
  (let [ordinary-type (interface/get-raw-data (c/++ context :type))]
    (ordinary.options/ordinary-map ordinary-type)))
