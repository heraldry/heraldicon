(ns heraldicon.coat-of-arms.outline
  (:require
   [heraldicon.coat-of-arms.tincture.core :as tincture]
   [heraldicon.interface :as interface]))

(def stroke-width 0.5)

(defn color [context]
  (let [mode (interface/render-option :mode context)]
    (if (= mode :hatching)
      "#000001"
      (tincture/pick :sable context))))

(defn style [context]
  {:stroke (color context)
   :stroke-width stroke-width
   :fill "none"
   :stroke-linecap "round"
   :stroke-linejoin "round"})
