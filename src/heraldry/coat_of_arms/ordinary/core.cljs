(ns heraldry.coat-of-arms.ordinary.core
  (:require
   [heraldry.coat-of-arms.line.fimbriation :as fimbriation]
   [heraldry.coat-of-arms.ordinary.interface :as ordinary-interface]
   [heraldry.coat-of-arms.ordinary.options :as ordinary-options]
   [heraldry.interface :as interface]
   [heraldry.util :as util]))

(defmethod interface/render-component :heraldry.component/ordinary [path parent-path environment context]
  (ordinary-interface/render-ordinary path parent-path environment context))

(defmethod interface/blazon-component :heraldry.component/ordinary [path context]
  (let [ordinary-type (interface/get-sanitized-data (conj path :type) context)
        line (interface/get-sanitized-data (conj path :line) context)
        ordinary-name (case ordinary-type
                        :heraldry.ordinary.type/quarter
                        (let [size (interface/get-sanitized-data (conj path :geometry :size) context)]
                          (util/str-tr (if (< size 100)
                                         "Canton "
                                         "Quarter ")
                                       (util/translate (interface/get-sanitized-data (conj path :variant) context))))
                        :heraldry.ordinary.type/point
                        (util/str-tr "Point " (util/translate (interface/get-sanitized-data (conj path :variant) context)))
                        (util/translate ordinary-type))
        rest (util/combine " " [ordinary-name
                                (util/translate-line line)
                                (interface/blazon (conj path :field) context)
                                (fimbriation/blazon path context
                                                    :include-lines? true)])
        article (if (re-matches #"(?i)^[aeiouh].*" (util/tr-raw rest :en))
                  "an"
                  "a")]
    (util/combine " " [article rest])))

(defn title [path context]
  (let [ordinary-type (interface/get-raw-data (conj path :type) context)]
    (ordinary-options/ordinary-map ordinary-type)))
