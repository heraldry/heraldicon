(ns heraldicon.frontend.component.render-options
  (:require
   [heraldicon.frontend.component.core :as component]
   [heraldicon.frontend.element.core :as element]))

(defn- form [context]
  (element/elements
   context
   [:scope
    :escutcheon
    :flag-aspect-ratio-preset
    :flag-height
    :flag-width
    :flag-swallow-tail
    :flag-tail-point-height
    :flag-tail-tongue
    :mode
    :theme
    :texture
    :texture-displacement?
    :shiny?
    :escutcheon-shadow?
    :escutcheon-outline?
    :outline?
    :squiggly?
    :coat-of-arms-angle]))

(defmethod component/node-data :heraldry/render-options [_context]
  {:title :string.render-options/title})

(defmethod component/form-data :heraldry/render-options [_context]
  form)
