(ns heraldry.coat-of-arms.ordinary.core
  (:require [heraldry.frontend.util :as frontend-util]))

(defn render [{:keys [type] :as ordinary} parent environment context]
  #_(let [function (get kinds-function-map type)]
      [function ordinary parent environment context]))

(defn title [ordinary]
  (-> ordinary :type frontend-util/translate-cap-first))
