(ns heraldicon.heraldry.ordinary.core
  (:require
   [heraldicon.heraldry.line.fimbriation :as fimbriation]
   [heraldicon.heraldry.ordinary.interface :as ordinary.interface]
   [heraldicon.heraldry.ordinary.options :as ordinary.options]
   [heraldicon.context :as c]
   [heraldicon.interface :as interface]
   [heraldicon.util :as util]))

(defmethod interface/render-component :heraldry.component/ordinary [context]
  (ordinary.interface/render-ordinary context))

(defmethod interface/blazon-component :heraldry.component/ordinary [context]
  (let [ordinary-type (interface/get-sanitized-data (c/++ context :type))
        line (interface/get-sanitized-data (c/++ context :line))
        voided? (interface/get-sanitized-data (c/++ context :voided :voided?))
        humetty? (interface/get-sanitized-data (c/++ context :humetty :humetty?))
        ordinary-name (case ordinary-type
                        :heraldry.ordinary.type/quarter
                        (let [size (interface/get-sanitized-data (c/++ context :geometry :size))]
                          (util/str-tr (if (< size 100)
                                         "Canton "
                                         "Quarter ")
                                       (util/translate (interface/get-sanitized-data (c/++ context :variant)))))
                        :heraldry.ordinary.type/point
                        (util/str-tr "Point " (util/translate (interface/get-sanitized-data (c/++ context :variant))))
                        (util/translate ordinary-type))
        rest (util/combine " " [ordinary-name
                                (util/translate-line line)
                                (when voided? "voided")
                                (when humetty? "humetty")
                                (interface/blazon (c/++ context :field))
                                (fimbriation/blazon context
                                                    :include-lines? true)])
        article (if (re-matches #"(?i)^[aeiouh].*" (util/tr-raw rest :en))
                  "an"
                  "a")]
    (util/combine " " [article rest])))

(defn title [context]
  (let [ordinary-type (interface/get-raw-data (c/++ context :type))]
    (ordinary.options/ordinary-map ordinary-type)))
