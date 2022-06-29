(ns heraldicon.frontend.component.render-options
  (:require
   [heraldicon.frontend.interface :as ui.interface]))

(defn- form [context]
  (ui.interface/form-elements
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

(defmethod ui.interface/component-node-data :heraldry/render-options [_context]
  {:title :string.render-options/title})

(defmethod ui.interface/component-form-data :heraldry/render-options [_context]
  {:form form})
