(ns heraldicon.render.outline
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.tincture :as tincture]
   [heraldicon.interface :as interface]))

(def stroke-width 0.5)

(defn color [context]
  (let [mode (interface/render-option :mode context)]
    (if (= mode :hatching)
      "#000001"
      (tincture/pick :sable (c/clear-tincture-mapping context)))))

(defn style [context]
  {:stroke (color context)
   :stroke-width stroke-width
   :fill "none"
   :stroke-linecap "round"
   :stroke-linejoin "round"})
