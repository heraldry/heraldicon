(ns heraldicon.render.scope
  (:require
   [heraldicon.options :as options]))

(def choices
  [[:string.render-options.scope-choice/achievement :achievement]
   [:string.render-options.scope-choice/coat-of-arms-and-helm :coat-of-arms-and-helm]
   [:string.render-options.scope-choice/coat-of-arms :coat-of-arms]])

(def scope-map
  (options/choices->map choices))
