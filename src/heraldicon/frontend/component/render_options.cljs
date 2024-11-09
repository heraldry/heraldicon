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
    :border?
    :coat-of-arms-angle]))

(defmethod component/node :heraldry/render-options [_context]
  {:title :string.render-options/title})

(defmethod component/form :heraldry/render-options [_context]
  form)
