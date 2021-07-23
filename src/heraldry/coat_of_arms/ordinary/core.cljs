(ns heraldry.coat-of-arms.ordinary.core
  (:require [heraldry.coat-of-arms.ordinary.interface :as ordinary-interface]
            [heraldry.interface :as interface]
            [heraldry.util :as util]))

(defmethod interface/render-component :heraldry.component/ordinary [path parent-path environment context]
  (ordinary-interface/render-ordinary path parent-path environment context))

(defmethod interface/blazon-component :heraldry.component/ordinary [path context]
  (let [ordinary-type (interface/get-sanitized-data (conj path :type) context)
        line (interface/get-sanitized-data (conj path :line) context)
        rest (util/combine " " [(util/translate ordinary-type)
                                (util/translate-line line)
                                (interface/blazon (conj path :field) context)])
        article (if (re-matches #"(?i)^[aeiouh].*" rest)
                  "an"
                  "a")]
    (util/combine " " [article rest])))

(defn title [path context]
  (let [ordinary-type (interface/get-raw-data (conj path :type) context)]
    (util/translate-cap-first ordinary-type)))
