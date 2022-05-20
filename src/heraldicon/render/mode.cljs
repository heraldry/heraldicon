(ns heraldicon.render.mode
  (:require
   [heraldicon.options :as options]))

(def choices
  [[:string.render-options.mode-choice/colours :colours]
   [:string.render-options.mode-choice/catching :hatching]])

(def mode-map
  (options/choices->map choices))
